# consultation-service

인터넷뱅킹 MVP의 **챗봇·상담사 채팅** 서비스입니다.  
고객은 챗봇과 대화하고, 필요 시 실시간 상담사 채팅으로 이관됩니다.

---

## 목차

1. [서비스 개요](#서비스-개요)
2. [아키텍처](#아키텍처)
3. [기술 스택](#기술-스택)
4. [디렉터리 구조](#디렉터리-구조)
5. [도메인 모델](#도메인-모델)
6. [API 엔드포인트](#api-엔드포인트)
7. [챗봇 기능 목록](#챗봇-기능-목록)
8. [상담 흐름](#상담-흐름)
9. [환경 변수](#환경-변수)
10. [로컬 실행](#로컬-실행)
11. [테스트](#테스트)
12. [Kafka 이벤트](#kafka-이벤트)
13. [변경 이력](#변경-이력)

---

## 서비스 개요

| 항목 | 내용 |
|------|------|
| 역할 | 챗봇 상담 + 상담사 이관 채팅 |
| 포트 | `8002` (기본값) |
| DB | PostgreSQL 16 (`deposit_db`) |
| 메시지 브로커 | Kafka (비활성화 가능) |
| 런타임 | Python 3.11 / FastAPI 0.115 |

---

## 아키텍처

```
고객 (앱/웹)
    │
    ▼
FastAPI (consultation-service)
    ├── ChatbotService   ─── 시나리오 기반 응답 + LLM fallback
    └── ChatService      ─── 상담사 채팅 관리
            │
            ├── PostgreSQL  (consultation, chatbot_consultation,
            │                chat_consultation, chat_message_history …)
            └── Kafka       (chatbot.events / chat.events)
```

### 상담 흐름 요약

```
[챗봇 시작]
    │  POST /chatbot/consultations/start
    ▼
[시나리오 응답]
    │  POST /chatbot/consultations/{id}/messages
    │  button_value 매핑 → 다음 노드 응답
    │  매핑 없음         → LLM fallback + agent_transfer_required=true
    ▼
[상담사 이관 → 대기열]
    │  GET  /chat/queue
    ▼
[상담사 연결]
    │  POST /chat/consultations/{id}/connect
    ▼
[메시지 교환]
    │  POST /chat/consultations/{id}/messages
    │  GET  /chat/consultations/{id}/messages
    ▼
[상담 종료]
       POST /chat/consultations/{id}/end
```

---

## 기술 스택

| 분류 | 라이브러리 | 버전 |
|------|-----------|------|
| 웹 프레임워크 | FastAPI | 0.115.6 |
| ASGI 서버 | Uvicorn | 0.34.0 |
| ORM | SQLAlchemy | 2.0.36 |
| DB 드라이버 | psycopg (v3) | 3.2.3 |
| 설정 | pydantic-settings | 2.7.1 |
| Kafka 클라이언트 | aiokafka | 0.12.0 |
| 테스트 | pytest | 8.3.4 |
| HTTP 클라이언트 | httpx | 0.28.1 |

---

## 디렉터리 구조

```
consultation-service/
├── app/
│   ├── __init__.py
│   ├── config.py       # 환경 변수 설정 (pydantic-settings)
│   ├── database.py     # SQLAlchemy 엔진·세션
│   ├── kafka.py        # KafkaEventPublisher (aiokafka)
│   ├── llm.py          # LlmHandoffAdapter (BP002 fallback)
│   ├── main.py         # FastAPI 라우터
│   ├── models.py       # SQLAlchemy ORM 모델
│   ├── schemas.py      # Pydantic 요청/응답 스키마
│   └── services.py     # ChatbotService / ChatService 비즈니스 로직
├── tests/
│   ├── conftest.py                       # pytest fixture (in-memory SQLite)
│   ├── test_basic.py                     # 기본 동작·헬스체크
│   ├── test_api_validation.py            # 입력값 유효성 검증
│   ├── test_chat_api.py                  # 상담사 채팅 HTTP API
│   ├── test_chat_service.py              # ChatService 단위 테스트
│   ├── test_chatbot_api.py               # 챗봇 HTTP API
│   ├── test_chatbot_service.py           # ChatbotService 단위 테스트
│   ├── test_features_product_advice.py   # PRODUCT_ADVICE 기능 상세
│   ├── test_features_staff_support.py    # STAFF_SUPPORT 기능 상세
│   ├── test_features_user_finance.py     # USER_FINANCE 기능 상세
│   └── test_runtime_contracts.py        # DB 스키마·Kafka 계약 검증
├── static/                              # 챗 UI (선택, 없으면 /chat → 404)
├── pytest.ini
└── requirements.txt
```

---

## 도메인 모델

### 주요 테이블

| 테이블 | 설명 |
|--------|------|
| `consultation` | 상담 마스터 (챗봇·채팅 공통 부모) |
| `chatbot_consultation` | 챗봇 상담 세션 |
| `chat_consultation` | 상담사 채팅 세션 |
| `chatbot_scenario` | 시나리오 정의 |
| `chatbot_node` | 시나리오 노드 (메시지 + 버튼) |
| `chatbot_node_button` | 노드 버튼 정의 |
| `chatbot_node_flow` | 노드 간 이동 규칙 |
| `chat_message_history` | 챗봇·상담사 메시지 통합 이력 |

### 상태값

**ChatConsultation.active_yn**

| 값 | 의미 |
|----|------|
| `"Y"` | 진행 중 |
| `"N"` | 종료됨 |

> 종료된 상담(`active_yn="N"`)에는 메시지 전송이 차단됩니다.

---

## API 엔드포인트

### 공통

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/health` | 헬스체크 |
| GET | `/chat` | 챗 UI (static/index.html) |

### 챗봇

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/chatbot/scenarios/default` | 기본 시나리오 시드 |
| GET | `/chatbot/categories` | 기능 카테고리 목록 |
| GET | `/chatbot/features` | 전체 기능 목록 |
| GET | `/chatbot/features/{code}` | 기능 상세 |
| POST | `/chatbot/features/{code}/execute` | 기능 실행 |
| POST | `/chatbot/consultations/start` | 챗봇 상담 시작 |
| POST | `/chatbot/consultations/{id}/messages` | 챗봇 메시지 전송 |

### 상담사 채팅

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/chat/queue` | 대기 중인 상담 목록 |
| POST | `/chat/consultations/{id}/connect` | 상담사 연결 수락 |
| POST | `/chat/consultations/{id}/messages` | 메시지 전송 |
| GET | `/chat/consultations/{id}/messages` | 메시지 이력 조회 |
| POST | `/chat/consultations/{id}/end` | 상담 종료 |

### 에러 코드

| HTTP | 상황 |
|------|------|
| 400 | 이미 연결/종료된 상담에 재요청 |
| 404 | 존재하지 않는 상담 ID / 종료된 상담에 메시지 전송 |
| 422 | 필수 필드 누락 또는 유효성 실패 |

---

## 챗봇 기능 목록

### PRODUCT_ADVICE — 금융상품 상담

| 코드 | 기능 | 인증 |
|------|------|------|
| `PRODUCT_GUIDE` | 예금·적금·청약 상품 안내 | 불필요 |
| `RATE_GUIDE` | 금리·우대금리 설명 | 불필요 |
| `JOIN_CONDITION` | 가입 조건 안내 | 불필요 |
| `PRODUCT_COMPARE` | 상품 비교 | 불필요 |
| `TERMS_RAG` | 약관 키워드 검색 | 불필요 |
| `FAQ` | FAQ 응답 | 불필요 |

### USER_FINANCE — 사용자 금융정보 조회

| 코드 | 기능 | 인증 |
|------|------|------|
| `MY_ACCOUNTS` | 내 계좌 목록·잔액 | 고객 인증 |
| `MY_PRODUCTS` | 가입 상품 조회 | 고객 인증 |
| `CONTRACT_STATUS` | 계약 상태 조회 | 고객 인증 |
| `MATURITY_SCHEDULE` | 만기 예정 조회 | 고객 인증 |
| `INTEREST_HISTORY` | 이자 내역 조회 | 고객 인증 |

### STAFF_SUPPORT — 직원 업무 지원

| 코드 | 기능 | 인증 |
|------|------|------|
| `STAFF_CUSTOMER` | 고객 계좌 조회 | 직원 인증 |
| `STAFF_CONTRACT` | 고객 계약 조회 | 직원 인증 |
| `STAFF_ACCOUNT` | 고객 계좌 상세 | 직원 인증 |
| `STAFF_TRANSFER_FLOW` | 이체 흐름 조회 | 직원 인증 |
| `STAFF_CONSULTATION_HISTORY` | 상담 이력 조회 | 직원 인증 |

> **인증 응답 status**  
> - `AUTH_REQUIRED` : `customer_no` 없이 USER_FINANCE 기능 호출  
> - `STAFF_AUTH_REQUIRED` : `customer_no` 또는 `staff_id` 누락 시 STAFF_SUPPORT 기능 호출  
> - `EMPTY` : 인증은 통과했으나 조회 결과 없음

---

## 환경 변수

모든 변수는 `CONSULTATION_` 접두사를 사용합니다.  
`.env.example`을 복사하여 `.env`로 사용합니다 (`.env`는 gitignore 대상).

```bash
cp services/consultation-service/.env.example services/consultation-service/.env
```

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `CONSULTATION_DATABASE_URL` | `postgresql+psycopg://deposit:deposit@localhost:5432/deposit_db` | DB 연결 URL |
| `CONSULTATION_KAFKA_ENABLED` | `false` | Kafka 활성화 여부 (`true` 시 브로커 필수) |
| `CONSULTATION_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka 브로커 주소 |
| `CONSULTATION_KAFKA_TOPIC_CHATBOT_EVENTS` | `consultation.chatbot.events` | 챗봇 이벤트 토픽 |
| `CONSULTATION_KAFKA_TOPIC_CHAT_EVENTS` | `consultation.chat.events` | 채팅 이벤트 토픽 |
| `CONSULTATION_LLM_CONFIDENCE_THRESHOLD` | `70` | LLM 신뢰도 임계값 |

> **주의**: `CONSULTATION_KAFKA_ENABLED=true` 상태에서 Kafka 브로커가 실행 중이지 않으면  
> 서비스 기동 시 `KafkaConnectionError`가 발생하여 시작이 실패합니다.

---

## 로컬 실행

### 1. 가상환경 및 의존성 설치

```bash
cd services/consultation-service
python -m venv .venv
# Windows
.venv\Scripts\activate
# macOS/Linux
source .venv/bin/activate

pip install -r requirements.txt
```

### 2. 환경 변수 설정

```bash
cp .env.example .env
# 필요 시 .env 내용 수정
```

### 3. Kafka 브로커 실행 (Kafka 연동 시)

Docker Compose를 사용하는 경우 프로젝트 루트에서:

```bash
docker compose up kafka -d
```

Kafka 없이 로컬 개발만 하는 경우 `.env`에서 비활성화:

```dotenv
CONSULTATION_KAFKA_ENABLED=false
```

### 4. 서버 실행

```bash
uvicorn app.main:app --reload --port 8002
```

### 5. API 문서 확인

서버 실행 후 브라우저에서 접속:

- Swagger UI : http://localhost:8002/docs
- ReDoc      : http://localhost:8002/redoc

### 6. Docker Compose (전체 스택)

프로젝트 루트에서:

```bash
docker compose up consultation-service
```

---

## 테스트

### 테스트 실행

```bash
cd services/consultation-service

# 전체 테스트
pytest

# 상세 출력
pytest -v

# 특정 파일만
pytest tests/test_chat_service.py -v
pytest tests/test_chat_api.py -v
```

### 테스트 환경

- DB : SQLite in-memory (PostgreSQL 불필요)
- Kafka : `AsyncMock` 으로 대체 (실제 브로커 불필요)
- 픽스처 : `conftest.py` — `db`, `service`, `chat_service`, `empty_service`, `rich_service`

### 테스트 현황 (2026-05-24 기준)

| 파일 | 테스트 수 | 내용 |
|------|----------|------|
| test_basic.py | 6 | 기본 동작·상품·계좌·거래 조회 |
| test_api_validation.py | 6 | 입력값 유효성 (필수 필드·범위) |
| test_chatbot_api.py | 7 | 챗봇 HTTP API |
| test_chatbot_service.py | 5 + 17 = 22 | ChatbotService 단위·파라미터화 |
| test_chat_api.py | 15 | 상담사 채팅 HTTP API |
| test_chat_service.py | 17 | ChatService 단위 |
| test_features_product_advice.py | 53 | PRODUCT_ADVICE 기능 상세 |
| test_features_user_finance.py | 54 | USER_FINANCE 기능 상세 |
| test_features_staff_support.py | 62 | STAFF_SUPPORT 기능 상세 |
| test_runtime_contracts.py | 8 | DB 스키마·Kafka 계약 |
| **합계** | **328** | |

> 전체 328개 중 326개 통과 / 1개 스킵 / 1개 기존 실패  
> (기존 실패: `test_chat_page_returns_static_html` — 테스트 환경에 static 디렉터리 없음)

---

## Kafka 이벤트

### 동작 방식

`CONSULTATION_KAFKA_ENABLED=false`(기본값)이면 모든 `publish()` 호출이 **조용히 무시**됩니다.  
`CONSULTATION_KAFKA_ENABLED=true`이면 서비스 기동 시 브로커에 연결하고, 각 API 처리 시점에 이벤트를 발행합니다.  
토픽은 Kafka `auto.create.topics.enable=true` 설정으로 **자동 생성**됩니다.

### Producer

| 클래스 | 파일 | 설명 |
|--------|------|------|
| `KafkaEventPublisher` | `app/kafka.py` | 챗봇·채팅 이벤트 발행 |

### Consumer

| 클래스 | 파일 | 설명 |
|--------|------|------|
| `KafkaEventConsumer` | `app/kafka.py` | 비동기 이터레이터 인터페이스 제공 (향후 WebSocket 알림 연동용) |

### 챗봇 이벤트 (`consultation.chatbot.events`)

| 이벤트 | 발행 시점 | 주요 필드 |
|--------|----------|----------|
| `ChatbotConsultationStarted` | `POST /chatbot/consultations/start` | `consultationId`, `chatbotConsultationId`, `customerNo` |
| `ChatbotMessageHandled` | `POST /chatbot/consultations/{id}/messages` | `chatbotConsultationId`, `message`, `processMethod`, `agentTransferRequired` |
| `ChatbotAgentTransferRequested` | 위 API에서 `agentTransferRequired=true`일 때 추가 발행 | `chatbotConsultationId`, `consultationId` |

**메시지 예시**

```json
{"eventType": "ChatbotConsultationStarted",
 "payload": {"consultationId": 1, "chatbotConsultationId": 1, "customerNo": "CUST001"}}

{"eventType": "ChatbotMessageHandled",
 "payload": {"chatbotConsultationId": 1, "message": "상품 안내해줘",
             "processMethod": "SCENARIO", "agentTransferRequired": false}}

{"eventType": "ChatbotAgentTransferRequested",
 "payload": {"chatbotConsultationId": 1, "consultationId": 1}}
```

### 채팅 이벤트 (`consultation.chat.events`)

| 이벤트 | 발행 시점 | 주요 필드 |
|--------|----------|----------|
| `AgentConnected` | `POST /chat/consultations/{id}/connect` | `chatConsultationId`, `consultationId`, `employeeId`, `customerNo` |
| `ChatMessageSent` | `POST /chat/consultations/{id}/messages` | `chatConsultationId`, `senderType`, `message` |
| `ChatEnded` | `POST /chat/consultations/{id}/end` | `chatConsultationId`, `consultationId`, `satisfactionScore` |

**메시지 예시**

```json
{"eventType": "AgentConnected",
 "payload": {"chatConsultationId": 1, "consultationId": 1, "employeeId": 1, "customerNo": "CUST001"}}

{"eventType": "ChatMessageSent",
 "payload": {"chatConsultationId": 1, "senderType": "AGENT", "message": "안녕하세요 상담사입니다"}}

{"eventType": "ChatEnded",
 "payload": {"chatConsultationId": 1, "consultationId": 1, "satisfactionScore": 5}}
```

### Kafka Consumer 수동 확인 (개발용)

```bash
# 챗봇 이벤트 실시간 확인
kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic consultation.chatbot.events \
  --from-beginning

# 채팅 이벤트 실시간 확인
kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic consultation.chat.events \
  --from-beginning
```

---

## 변경 이력

| 날짜 | 내용 |
|------|------|
| 2026-05-24 | Kafka 이벤트 발행 활성화 — `.env.example` 추가, README Kafka 섹션 상세화 |
| 2026-05-24 | `ChatService.send_message()` 종료 상담 가드 추가 — 종료된 상담(`active_yn="N"`)에 메시지 전송 시 `ValueError` 발생 → HTTP 404 반환. 관련 테스트 3개 추가 |
| 2026-05-24 | 챗봇·상담 서비스 초기 구현 |
