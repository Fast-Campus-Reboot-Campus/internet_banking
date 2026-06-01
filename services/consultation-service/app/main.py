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
from app.llm import LlmAdapter, LlmHandoffAdapter
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
from app.rag import OpenAIEmbeddingProvider, ProductRagEngine
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
        logging.getLogger().addHandler(handler)
    except Exception as e:
        logging.getLogger(__name__).warning("[logging] 파일 핸들러 설정 실패: %s", e)


_setup_file_logging()


def _setup_phoenix() -> None:
    """Phoenix OTel 계측 초기화. PHOENIX_ENABLED=true 일 때만 활성화."""
    if os.getenv("PHOENIX_ENABLED", "false").lower() != "true":
        return
    try:
        from phoenix.otel import register
        from openinference.instrumentation.openai import OpenAIInstrumentor

        endpoint = os.getenv("PHOENIX_HTTP_ENDPOINT", "http://localhost:6006/v1/traces")
        tracer_provider = register(
            project_name="consultation-service",
            endpoint=endpoint,
            set_global_tracer_provider=True,
        )
        OpenAIInstrumentor().instrument(tracer_provider=tracer_provider)
        logger.info("[Phoenix] OTel 계측 활성화 → %s (project: consultation-service)", endpoint)
    except ImportError:
        logger.warning("[Phoenix] 패키지 미설치 — pip install arize-phoenix-otel openinference-instrumentation-openai")


_setup_phoenix()

# settings, static_dir 은 설정값이므로 모듈 수준에 유지
settings = get_settings()
static_dir = Path(__file__).resolve().parents[1] / "static"


def _setup_langfuse() -> None:
    """Langfuse LLM 추적 초기화. CONSULTATION_LANGFUSE_ENABLED=true 일 때만 활성화.

    키는 CONSULTATION_LANGFUSE_* 를 우선 사용하고, 없으면 LANGFUSE_* 를 그대로 사용한다.
    (Langfuse SDK 자체도 LANGFUSE_* 를 직접 읽으므로 이미 설정된 경우 추가 주입 불필요)
    """
    if not settings.langfuse_enabled:
        return
    secret = settings.langfuse_secret_key or os.getenv("LANGFUSE_SECRET_KEY", "")
    public = settings.langfuse_public_key or os.getenv("LANGFUSE_PUBLIC_KEY", "")
    host   = settings.langfuse_host if settings.langfuse_host != "http://localhost:3001" \
             else os.getenv("LANGFUSE_HOST", settings.langfuse_host)
    if not secret or not public:
        logger.warning("[Langfuse] secret/public 키 미설정 — 추적 비활성화")
        return
    os.environ["LANGFUSE_SECRET_KEY"] = secret
    os.environ["LANGFUSE_PUBLIC_KEY"] = public
    os.environ["LANGFUSE_HOST"] = host
    logger.info("[Langfuse] LLM 추적 활성화 → %s", host)


_setup_langfuse()


async def _handle_contract_created(payload: dict, rag: ProductRagEngine | None) -> None:
    """deposit-api 에서 ContractCreated 이벤트 수신 시 처리.

    고객이 상품에 가입했음을 기록하고, RAG 인덱스가 살아있으면 재빌드를 예약한다.
    (같은 DB를 공유하므로 신규 계약 데이터가 즉시 조회 가능)
    """
    customer_id  = payload.get("customerId", "")
    product_id   = payload.get("productId", "")
    contract_id  = payload.get("contractId", "")
    join_amount  = payload.get("joinAmount", 0)

    logger.info(
        "[Kafka] ContractCreated 처리 — customer=%s product=%s contract=%s amount=%s",
        customer_id, product_id, contract_id, join_amount,
    )

    # RAG 인덱스 재빌드: 신규 상품 데이터 반영
    if rag is not None:
        await _build_rag_index(rag)
        logger.info("[Kafka] RAG 인덱스 재빌드 완료 (ContractCreated 트리거)")


async def _kafka_consume_loop(
    consumer: KafkaEventConsumer,
    rag: ProductRagEngine | None,
) -> None:
    """카프카 이벤트를 수신해 비즈니스 로직을 처리하는 백그라운드 루프.

    처리 이벤트:
      - ContractCreated (deposit.contract.events): 고객 계약 완료 → RAG 재빌드
      - 그 외 consultation 이벤트: 구조화된 로그 출력
    """
    try:
        async for message in consumer:
            event_type = message.get("eventType", "UNKNOWN")
            payload    = message.get("payload", {})

            if event_type == "ContractCreated":
                await _handle_contract_created(payload, rag)
            else:
                logger.info("[Kafka] event=%s payload=%s", event_type, payload)

    except asyncio.CancelledError:
        pass
    except Exception as exc:
        logger.exception("[Kafka] consumer loop 오류: %s", exc)


async def _build_rag_index(rag: ProductRagEngine) -> None:
    """DB에서 상품 + 약관 데이터를 읽어 RAG 인덱스를 빌드한다."""
    from sqlalchemy import text as sa_text
    from app.database import SessionLocal

    db = SessionLocal()
    try:
        products = [
            dict(row._mapping)
            for row in db.execute(sa_text(
                """
                SELECT banking_product_id,
                       deposit_product_name,
                       deposit_product_type,
                       description,
                       base_interest_rate,
                       min_join_amount,
                       max_join_amount,
                       min_period_month,
                       max_period_month,
                       is_early_termination_allowed,
                       is_tax_benefit_available,
                       deposit_product_status
                  FROM deposit_banking_products
                 WHERE deposit_product_status = 'SELLING'
                """
            ))
        ]
        terms = [
            dict(row._mapping)
            for row in db.execute(sa_text(
                """
                SELECT special_term_id,
                       special_term_name,
                       special_term_content,
                       special_term_summary,
                       is_required,
                       status
                  FROM deposit_special_terms
                 WHERE status = 'ACTIVE'
                """
            ))
        ]
        rag.build_from_db(products, terms)
        logger.info("[RAG] 인덱스 빌드 완료: 상품 %d개, 약관 %d개", len(products), len(terms))
    except Exception as exc:
        logger.warning("[RAG] 인덱스 빌드 실패 (DB 미연결 등): %s", exc)
    finally:
        db.close()


@asynccontextmanager
async def lifespan(app: FastAPI):
    # ── 싱글턴 생성 및 app.state 등록 ────────────────────────────────────────
    _events = KafkaEventPublisher(settings)
    _consumer = KafkaEventConsumer(settings)
    _llm = LlmHandoffAdapter()
    _llm_adapter = (
        LlmAdapter(api_key=settings.openai_api_key, model=settings.openai_model)
        if settings.openai_api_key else None
    )
    _rag_engine: ProductRagEngine | None = (
        ProductRagEngine(OpenAIEmbeddingProvider(settings.openai_api_key))
        if settings.openai_api_key else None
    )

    app.state.events = _events
    app.state.consumer = _consumer
    app.state.llm = _llm
    app.state.llm_adapter = _llm_adapter
    app.state.rag_engine = _rag_engine

    # ── 기동 ─────────────────────────────────────────────────────────────────
    Base.metadata.create_all(bind=engine)
    await _events.start()
    # chatbot_events / chat_events 는 이 서비스가 직접 발행하는 토픽이므로
    # 구독 목록에서 제외 — 자기 발행 메시지를 자신이 소비하는 순환 방지
    await _consumer.start(
        topics=[
            settings.kafka_topic_deposit_events,   # deposit-api 계약 이벤트만 수신
        ],
        group_id="consultation-service",
    )
    consume_task = asyncio.create_task(_kafka_consume_loop(_consumer, _rag_engine))
    if _rag_engine is not None:
        await _build_rag_index(_rag_engine)
    else:
        logger.info("[RAG] OpenAI API 키 없음 → RAG 비활성화")

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
    state = request.app.state
    return ChatbotService(db, state.events, state.llm, state.llm_adapter, state.rag_engine)


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
