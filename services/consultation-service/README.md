# Consultation Service — 챗봇·상담 서비스

인터넷뱅킹 플랫폼의 **고객 챗봇 및 상담사 채팅 도메인 백엔드 서비스**입니다.  
시나리오 기반 챗봇, 키워드 Intent 분류, OpenAI LLM 폴백, 상담사 연결(Live Chat), Kafka 이벤트 발행까지 상담 업무 전 흐름을 담당합니다.

---

## 기술 스택

| 항목 | 내용 |
|------|------|
| Language | Python 3.11+ |
| Framework | FastAPI |
| DB | PostgreSQL (운영) / SQLite (테스트) |
| ORM | SQLAlchemy |
| 메시지 큐 | Apache Kafka (aiokafka) |
| LLM | OpenAI API (`gpt-4o-mini`) |
| 테스트 | pytest + pytest-asyncio |
| 컨테이너 | Docker / Docker Compose |

---

## 서비스 아키텍처

```
고객 요청
    │
    ▼
[챗봇 상담 시작]  POST /chatbot/consultations/start
    │
    ▼
[메시지 처리]     POST /chatbot/consultations/{id}/messages
    │
    ├─ 시나리오 버튼 → 정해진 노드 흐름으로 응답
    │
    ├─ 자유 텍스트
    │     ├─ IntentClassifier (키워드 기반)
    │     │       ↓ 매칭된 경우
    │     │   Feature 실행 (DB 조회 → 포맷팅 응답)
    │     │
    │     └─ 미매칭 → LLM Adapter (OpenAI) 폴백 응답
    │
    └─ '상담사 연결' 선택 → ChatConsultation 생성
            │
            ▼
    [상담사 채팅]  /chat/consultations/*
            │
            ├─ 상담사 수락: POST /chat/consultations/{id}/connect
            ├─ 메시지 전송: POST /chat/consultations/{id}/messages
            ├─ 이력 조회:   GET  /chat/consultations/{id}/messages
            └─ 상담 종료:   POST /chat/consultations/{id}/end
```

---

## 주요 기능

### 1. 수신 상품 안내 (상품 연계)
챗봇은 **deposit-service DB를 직접 조회**하여 고객에게 실시간 상품 정보를 안내합니다.

| Intent 코드 | 키워드 예시 | 응답 내용 |
|-------------|------------|-----------|
| `PRODUCT_GUIDE` | 상품 추천, 어떤 예금, 적금 알려줘 | 수신 상품 목록 (최대 5개) |
| `RATE_GUIDE` | 금리, 이자율, 이율 | 상품별 금리 안내 |
| `JOIN_CONDITION` | 가입 조건, 가입 가능 | 가입 금액·기간·혜택 |
| `PRODUCT_COMPARE` | 비교, 차이, 뭐가 나아 | 상품 비교표 |
| `TERMS_RAG` | 약관, 중도해지, 수수료 | 특약 검색 결과 |
| `FAQ` | 자주 묻는, FAQ | 자주 묻는 질문 |

### 2. 챗봇 시스템 (시나리오 기반)
- **시나리오 노드** 구조: 노드(`ChatbotNode`) → 버튼(`ChatbotNodeButton`) → 다음 노드 흐름(`ChatbotNodeFlow`)
- 기본 시나리오 자동 시드: `POST /chatbot/scenarios/default`
- 챗봇 상담 전 과정 DB 기록 (`ChatbotConsultation`, `ChatbotIntent`)
- 버튼 선택 / 자유 텍스트 입력 모두 처리
- 종료된 상담에 메시지 전송 차단 (상태 검증)

### 3. LLM 폴백 (OpenAI)
시나리오·키워드로 처리되지 않는 자유 문의는 **OpenAI LLM**이 응답합니다.

```
처리 우선순위
1. 시나리오 버튼 선택 → 노드 흐름 응답
2. IntentClassifier 키워드 매칭 → DB 조회 후 포맷팅 응답
3. 미매칭 + OPENAI_API_KEY 설정 → gpt-4o-mini 응답 (LlmAdapter)
4. OPENAI_API_KEY 미설정 → 상담사 연결 안내 (LlmHandoffAdapter)
```

**LLM 시스템 프롬프트 요약:**
> 인터넷뱅킹 고객 상담 챗봇으로서 수신 금융상품(예금·적금·청약) 질문에 친절하게 답변.  
> 인증이 필요한 내용은 '상담사 연결'로 안내. 답변은 한국어, 간결하게.

### 4. Kafka 이벤트
상담 흐름의 주요 이벤트를 Kafka로 발행합니다.

| 이벤트 | 토픽 | 발행 시점 |
|--------|------|-----------|
| `ChatbotStarted` | `chatbot-events` | 챗봇 상담 시작 |
| `ChatbotMessageSent` | `chatbot-events` | 챗봇 메시지 전송 |
| `AgentHandoffRequested` | `chatbot-events` | 상담사 연결 요청 |
| `AgentConnected` | `chat-events` | 상담사 수락 |
| `ChatMessageSent` | `chat-events` | 채팅 메시지 전송 |
| `ChatEnded` | `chat-events` | 상담 종료 |

> `KAFKA_ENABLED=false`(기본값) 시 발행 없이 서비스 정상 동작. 테스트 환경에서도 비활성화.

### 5. 상담사 채팅 (Live Chat)
- 상담사 대기열 조회: `GET /chat/queue`
- 상담사 수락 → 채팅 시작
- 챗봇 메시지 + 상담사 메시지 통합 이력 조회 (시간순)
- 만족도 점수 기록 후 상담 종료

---

## API 엔드포인트 요약

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/health` | 헬스 체크 |
| `GET` | `/chat` | 챗 UI (index.html) |
| `POST` | `/chatbot/scenarios/default` | 기본 시나리오 시드 |
| `GET` | `/chatbot/categories` | 챗봇 카테고리 목록 |
| `GET` | `/chatbot/features` | 챗봇 기능 목록 |
| `GET` | `/chatbot/features/{code}` | 챗봇 기능 상세 |
| `POST` | `/chatbot/features/{code}/execute` | 챗봇 기능 실행 |
| `POST` | `/chatbot/consultations/start` | 챗봇 상담 시작 |
| `POST` | `/chatbot/consultations/{id}/messages` | 챗봇 메시지 전송 |
| `GET` | `/chat/queue` | 상담사 대기열 조회 |
| `POST` | `/chat/consultations/{id}/connect` | 상담사 연결 수락 |
| `POST` | `/chat/consultations/{id}/messages` | 채팅 메시지 전송 |
| `GET` | `/chat/consultations/{id}/messages` | 채팅 메시지 이력 |
| `POST` | `/chat/consultations/{id}/end` | 상담 종료 |

> Swagger UI: `http://localhost:8087/docs`

---

## 환경 변수 설정

`.env.sample`을 복사해 `.env` 작성:

```env
# DB
DATABASE_URL=postgresql://user:pass@localhost:5432/consultation_db

# Kafka (비활성화 시 KAFKA_ENABLED=false)
KAFKA_ENABLED=false
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_TOPIC_CHATBOT_EVENTS=chatbot-events
KAFKA_TOPIC_CHAT_EVENTS=chat-events

# LLM (미설정 시 상담사 연결 안내로 폴백)
OPENAI_API_KEY=sk-...
OPENAI_MODEL=gpt-4o-mini
```

---

## 로컬 실행 방법

### 가상환경 설정

```bash
cd services/consultation-service
python -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
```

### 서비스 시작

```bash
uvicorn app.main:app --reload --port 8087
```

또는 스크립트 사용:

```powershell
.\scripts\start.ps1
```

### 기본 시나리오 시드

서비스 시작 후 한 번 실행:

```bash
curl -X POST http://localhost:8087/chatbot/scenarios/default
```

---

## 테스트 실행

```bash
pytest
```

| 테스트 파일 | 내용 |
|-------------|------|
| `test_chatbot_api.py` | 챗봇 API 엔드포인트 |
| `test_chatbot_service.py` | 챗봇 서비스 유닛 테스트 |
| `test_chat_api.py` | 상담사 채팅 API |
| `test_chat_service.py` | 채팅 서비스 유닛 테스트 |
| `test_features_product_advice.py` | 상품 안내 Feature 테스트 |
| `test_features_staff_support.py` | 상담사 지원 Feature 테스트 |
| `test_features_user_finance.py` | 사용자 금융 Feature 테스트 |
| `test_runtime_contracts.py` | 런타임 계약 연동 테스트 |
| `test_scenario_flow.py` | 시나리오 전체 흐름 테스트 |
| `test_api_validation.py` | 입력 유효성 검증 테스트 |

---

## 도메인 모델 (주요 엔티티)

```
Consultation (상담)
├── ChatbotConsultation   (챗봇 상담)
│   ├── ChatbotIntent     (Intent 기록)
│   └── ChatbotNode 참조 → ChatbotScenario
└── ChatConsultation      (상담사 채팅)
    └── ChatMessageHistory (채팅 메시지 이력)

ChatbotScenario (챗봇 시나리오)
└── ChatbotNode           (노드)
    ├── ChatbotNodeButton (버튼 선택지)
    └── ChatbotNodeFlow   (다음 노드 연결)
```

---

## 주요 설계 결정

| 결정 | 이유 |
|------|------|
| Intent 우선순위 분류 | 금리·가입조건 등 금융 특화 키워드를 먼저 처리해 LLM 호출 최소화 |
| LLM 폴백 선택적 적용 | API 키 미설정 시에도 서비스 정상 동작, 비용 제어 가능 |
| Kafka 비활성화 옵션 | 로컬·테스트 환경에서 Kafka 없이 즉시 실행 가능 |
| 챗봇·채팅 DB 분리 | 챗봇 이력과 상담사 채팅 이력을 독립적으로 관리 |
| 시나리오 자동 시드 | 첫 실행 시 기본 시나리오 자동 생성으로 빠른 시연 가능 |
