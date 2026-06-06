import asyncio
import logging
import os
from contextlib import asynccontextmanager
from pathlib import Path

from dotenv import load_dotenv
load_dotenv()  # .env → os.environ 주입 (_setup_phoenix 등 os.getenv 사용 전에 실행)

from fastapi import Depends, FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
from prometheus_fastapi_instrumentator import Instrumentator
from sqlalchemy.orm import Session

from app.config import get_settings
from app.database import Base, engine, get_db
from app.kafka import KafkaEventConsumer, KafkaEventPublisher
from app.metrics import (
    chatbot_active_sessions,
    chatbot_handoff_total,
    chatbot_message_total,
    chatbot_satisfaction_score,
    chatbot_session_ended_total,
    chatbot_session_total,
)
from app.schemas import (
    AgentConnectRequest,
    AgentQueueResponse,
    ChatbotCategoryResponse,
    ChatbotFeatureExecuteRequest,
    ChatbotFeatureExecuteResponse,
    ChatbotFeatureResponse,
    ChatbotMessageRequest,
    ChatbotMessageResponse,
    ChatbotStartRequest,
    ChatbotStartResponse,
    ChatbotTransferRequest,
    ChatbotTransferResponse,
    ChatConsultationResponse,
    ChatEndRequest,
    ChatMessageHistoryResponse,
    ChatSendMessageRequest,
    ScenarioSeedResponse,
)
from app.services import (
    CODE_SENDER_AGENT,
    CODE_SENDER_USER,
    ChatbotService,
    ChatService,
    _chat_status,
    _SENDER_LABEL,
)

logger = logging.getLogger(__name__)


def _setup_file_logging() -> None:
    """파일 로그 핸들러 설정. LOG_DIR 환경변수로 경로 지정 가능."""
    log_dir = os.getenv("LOG_DIR", "C:/logs/internet-banking")
    log_file = os.path.join(log_dir, "consultation-service.log")
    try:
        os.makedirs(log_dir, exist_ok=True)
        handler = logging.FileHandler(log_file, encoding="utf-8")
        handler.setFormatter(logging.Formatter(
            "%(asctime)s [%(threadName)s] %(levelname)-5s %(name)s - %(message)s"
        ))
        logging.getLogger().setLevel(logging.INFO)
        logging.getLogger().addHandler(handler)
    except Exception as e:
        logging.getLogger(__name__).warning("[logging] 파일 핸들러 설정 실패: %s", e)


_setup_file_logging()


# settings, static_dir 은 설정값이므로 모듈 수준에 유지
settings = get_settings()
static_dir = Path(__file__).resolve().parents[1] / "static"


async def _handle_contract_created(payload: dict) -> None:
    """deposit-api 에서 ContractCreated 이벤트 수신 시 처리."""
    logger.info(
        "[Kafka] ContractCreated 처리 — customer=%s product=%s contract=%s amount=%s",
        payload.get("customerId", ""),
        payload.get("productId", ""),
        payload.get("contractId", ""),
        payload.get("joinAmount", 0),
    )


async def _handle_chatbot_message_received(payload: dict) -> None:
    """consultation.chatbot.message 토픽 수신 시 로그만 출력.

    DB 저장은 handle_message()의 _record_message()에서 이미 수행하므로
    Consumer에서 중복 저장하지 않는다.
    """
    logger.info(
        "[Kafka] ChatbotMessageReceived — chatbot_consultation_id=%s sequence_no=%s message=%s",
        payload.get("chatbot_consultation_id", ""),
        payload.get("sequence_no", ""),
        payload.get("message_content", ""),
    )


async def _kafka_consume_loop(consumer: KafkaEventConsumer) -> None:
    """카프카 이벤트를 수신해 비즈니스 로직을 처리하는 백그라운드 루프.

    처리 이벤트:
      - ContractCreated (deposit.contract.events): 고객 계약 완료 → RAG 재빌드
      - ChatbotMessageReceived (consultation.chatbot.message): 수신 로그 출력
      - 그 외 이벤트: 구조화된 로그 출력
    """
    try:
        async for message in consumer:
            event_type = message.get("eventType", "UNKNOWN")
            payload    = message.get("payload", {})

            if event_type == "ContractCreated":
                await _handle_contract_created(payload)
            elif event_type == "ChatbotMessageReceived":
                await _handle_chatbot_message_received(payload)
            else:
                logger.info("[Kafka] event=%s payload=%s", event_type, payload)

    except asyncio.CancelledError:
        pass
    except Exception as exc:
        logger.exception("[Kafka] consumer loop 오류: %s", exc)



@asynccontextmanager
async def lifespan(app: FastAPI):
    # ── 싱글턴 생성 및 app.state 등록 ────────────────────────────────────────
    _events = KafkaEventPublisher(settings)
    _consumer = KafkaEventConsumer(settings)

    app.state.events = _events
    app.state.consumer = _consumer

    # ── 기동 ─────────────────────────────────────────────────────────────────
    Base.metadata.create_all(bind=engine)
    await _events.start()
    # chatbot_events / chat_events 는 이 서비스가 직접 발행하는 토픽이므로
    # 구독 목록에서 제외 — 자기 발행 메시지를 자신이 소비하는 순환 방지
    # chatbot_message 는 자기 발행 토픽이나 Consumer가 로그만 출력(재발행 없음) → 루프 없음
    await _consumer.start(
        topics=[
            settings.kafka_topic_deposit_events,    # deposit-api 계약 이벤트
            settings.kafka_topic_chatbot_message,   # 챗봇 메시지 수신 이벤트
        ],
        group_id="consultation-service",
    )
    consume_task = asyncio.create_task(_kafka_consume_loop(_consumer))

    try:
        yield
    finally:
        consume_task.cancel()
        await _consumer.stop()
        await _events.stop()


app = FastAPI(title=settings.app_name, version=settings.app_version, lifespan=lifespan)

_origins = [
    o.strip()
    for o in os.getenv(
        "ALLOWED_ORIGINS",
        "http://localhost:3000,http://localhost:3001",
    ).split(",")
    if o.strip()
]
app.add_middleware(
    CORSMiddleware,
    allow_origins=_origins,
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)
Instrumentator(excluded_handlers=["/metrics", "/health"]).instrument(app).expose(app)

if static_dir.exists():
    app.mount("/static", StaticFiles(directory=static_dir), name="static")


# ── 의존성 ──────────────────────────────────────────────────────────────────

def get_chatbot_service(
    request: Request,
    db: Session = Depends(get_db),
) -> ChatbotService:
    return ChatbotService(db, request.app.state.events)


def get_chat_service(
    request: Request,
    db: Session = Depends(get_db),
) -> ChatService:
    return ChatService(db, request.app.state.events)


# ── 공통 ────────────────────────────────────────────────────────────────────

@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP"}


@app.get("/chat")
def chat_page() -> FileResponse:
    index = static_dir / "index.html"
    if not index.exists():
        raise HTTPException(status_code=404, detail="챗 UI를 찾을 수 없습니다.")
    return FileResponse(index)


# ── 챗봇 시나리오 ─────────────────────────────────────────────────────────────

@app.post("/chatbot/scenarios/default", response_model=ScenarioSeedResponse)
def seed_default_scenario(service: ChatbotService = Depends(get_chatbot_service)) -> ScenarioSeedResponse:
    scenario_id, first_node_id = service.seed_default_scenario()
    return ScenarioSeedResponse(scenario_id=scenario_id, first_node_id=first_node_id)


# ── 챗봇 기능 ─────────────────────────────────────────────────────────────────

@app.get("/chatbot/categories", response_model=list[ChatbotCategoryResponse])
def chatbot_categories(service: ChatbotService = Depends(get_chatbot_service)) -> list[ChatbotCategoryResponse]:
    return service.categories()


@app.get("/chatbot/features", response_model=list[ChatbotFeatureResponse])
def chatbot_features(service: ChatbotService = Depends(get_chatbot_service)) -> list[ChatbotFeatureResponse]:
    return service.features()


@app.get("/chatbot/features/{feature_code}", response_model=ChatbotFeatureResponse)
def chatbot_feature_detail(
    feature_code: str,
    service: ChatbotService = Depends(get_chatbot_service),
) -> ChatbotFeatureResponse:
    feature = service.feature_detail(feature_code)
    if not feature:
        raise HTTPException(status_code=404, detail="챗봇 기능을 찾을 수 없습니다.")
    return feature


@app.post("/chatbot/features/{feature_code}/execute", response_model=ChatbotFeatureExecuteResponse)
def execute_chatbot_feature(
    feature_code: str,
    request: ChatbotFeatureExecuteRequest,
    service: ChatbotService = Depends(get_chatbot_service),
) -> ChatbotFeatureExecuteResponse:
    # TODO: IDOR — JWT 미들웨어 도입 후 request.customer_no 가 인증된 사용자 본인인지 검증 필요.
    # TODO: STAFF 기능 — JWT 미들웨어 도입 후 Depends(require_staff_role) 로 교체.
    #       현재는 _validate_staff() 가 employees 테이블 DB 조회로 유효성을 1차 확인함.
    result = service.execute_feature(feature_code, request)
    if result.status == "NOT_FOUND":
        raise HTTPException(status_code=404, detail=result.message)
    return result


# ── 챗봇 상담 흐름 ────────────────────────────────────────────────────────────

@app.post("/chatbot/consultations/start", response_model=ChatbotStartResponse)
async def start_chatbot(
    request: ChatbotStartRequest,
    service: ChatbotService = Depends(get_chatbot_service),
) -> ChatbotStartResponse:
    # TODO: JWT 토큰 기반 인증 미들웨어 도입 후
    #       토큰에서 추출한 customer_no 와 request.customer_no 일치 여부를 검증해야 함.
    #       현재는 body 의 customer_no 를 그대로 사용하므로 IDOR 취약점 존재.
    #       ex) Depends(require_customer_matches(request.customer_no))
    try:
        response = await service.start(request.customer_no, request.entry_screen, request.app_version)
        chatbot_session_total.labels(entry_screen=request.entry_screen or "UNKNOWN").inc()
        chatbot_active_sessions.inc()
        return response
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@app.post("/chatbot/consultations/{chatbot_consultation_id}/messages", response_model=ChatbotMessageResponse)
async def send_chatbot_message(
    chatbot_consultation_id: int,
    request: ChatbotMessageRequest,
    service: ChatbotService = Depends(get_chatbot_service),
) -> ChatbotMessageResponse:
    try:
        response = await service.handle_message(
            chatbot_consultation_id,
            request.message,
            request.button_value,
        )
        chatbot_message_total.labels(process_method=response.process_method).inc()
        if response.agent_transfer_required:
            chatbot_handoff_total.inc()
        return response
    except ValueError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc


@app.post("/chatbot/transfer", response_model=ChatbotTransferResponse)
def chatbot_transfer(
    request: ChatbotTransferRequest,
    service: ChatbotService = Depends(get_chatbot_service),
) -> ChatbotTransferResponse:
    return service.execute_transfer(request)


# ── 상담사 채팅 ───────────────────────────────────────────────────────────────

def _to_chat_response(chat) -> ChatConsultationResponse:
    return ChatConsultationResponse(
        chat_consultation_id=chat.chat_consultation_id,
        consultation_id=chat.consultation_id,
        chatbot_consultation_id=chat.chatbot_consultation_id,
        status=_chat_status(chat),
        employee_id=chat.employee_id,
        agent_requested_at=chat.agent_requested_at,
        agent_connected_at=chat.agent_connected_at,
        chat_started_at=chat.chat_started_at,
        chat_ended_at=chat.chat_ended_at,
        active_yn=chat.active_yn,
        satisfaction_score=chat.satisfaction_score,
    )


@app.get(
    "/chat/queue",
    response_model=list[AgentQueueResponse],
    summary="상담사 대기열 조회",
    description="수락 대기 중인 채팅 상담 목록을 반환합니다. (직원 전용)",
)
def get_agent_queue(service: ChatService = Depends(get_chat_service)) -> list[AgentQueueResponse]:
    return service.get_waiting_queue()


@app.post(
    "/chat/consultations/{chat_consultation_id}/connect",
    response_model=ChatConsultationResponse,
    summary="상담사 연결 수락",
    description="상담사가 대기 중인 상담을 수락합니다. Kafka: AgentConnected",
)
async def connect_agent(
    chat_consultation_id: int,
    request: AgentConnectRequest,
    service: ChatService = Depends(get_chat_service),
) -> ChatConsultationResponse:
    try:
        chat = await service.connect_agent(chat_consultation_id, request.employee_id)
        return _to_chat_response(chat)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@app.post(
    "/chat/consultations/{chat_consultation_id}/messages",
    response_model=ChatMessageHistoryResponse,
    summary="채팅 메시지 전송",
    description="상담사(AGENT) 또는 고객(USER)이 메시지를 전송합니다. Kafka: ChatMessageSent",
)
async def send_chat_message(
    chat_consultation_id: int,
    request: ChatSendMessageRequest,
    service: ChatService = Depends(get_chat_service),
) -> ChatMessageHistoryResponse:
    sender_code = CODE_SENDER_AGENT if request.sender_type == "AGENT" else CODE_SENDER_USER
    try:
        msg = await service.send_message(chat_consultation_id, request.message, sender_code)
        return ChatMessageHistoryResponse(
            message_id=msg.chat_message_history_id,
            sender_type=request.sender_type,
            message=msg.message_content,
            sent_at=msg.created_at,
            read_yn=msg.read_yn,
        )
    except ValueError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc


@app.get(
    "/chat/consultations/{chat_consultation_id}/messages",
    response_model=list[ChatMessageHistoryResponse],
    summary="채팅 메시지 이력 조회",
    description="챗봇 메시지 + 상담사 메시지를 통합하여 시간 순으로 반환합니다.",
)
def get_chat_messages(
    chat_consultation_id: int,
    service: ChatService = Depends(get_chat_service),
) -> list[ChatMessageHistoryResponse]:
    try:
        messages = service.get_messages(chat_consultation_id)
        return [
            ChatMessageHistoryResponse(
                message_id=m.chat_message_history_id,
                sender_type=_SENDER_LABEL.get(m.sender_type_code_id or 0, "UNKNOWN"),
                message=m.message_content,
                sent_at=m.created_at,
                read_yn=m.read_yn,
            )
            for m in messages
        ]
    except ValueError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc


@app.post(
    "/chat/consultations/{chat_consultation_id}/end",
    response_model=ChatConsultationResponse,
    summary="상담 종료",
    description="상담을 종료하고 만족도를 기록합니다. Kafka: ChatEnded",
)
async def end_chat(
    chat_consultation_id: int,
    request: ChatEndRequest,
    service: ChatService = Depends(get_chat_service),
) -> ChatConsultationResponse:
    try:
        chat = await service.end_chat(chat_consultation_id, request.satisfaction_score)
        chatbot_active_sessions.dec()
        chatbot_session_ended_total.inc()
        if request.satisfaction_score is not None:
            chatbot_satisfaction_score.observe(request.satisfaction_score)
        return _to_chat_response(chat)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
