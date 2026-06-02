# Consultation Service — 챗봇·상담 서비스

작성자: 정혜영

인터넷뱅킹 플랫폼의 **고객 챗봇, 현금흐름 기반 상품 추천, 상담사 채팅 도메인 백엔드 서비스**입니다.  
시나리오 기반 챗봇, 키워드 Intent 분류, OpenAI LLM 폴백, 현금흐름 분석·개인화 추천, 상담사 연결(Live Chat), Kafka 이벤트 발행까지 상담 업무 전 흐름을 담당합니다.

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

### 2. 현금흐름 기반 개인화 상품 추천 (신규)

고객의 최근 거래 패턴을 분석해 1~3위 맞춤 상품을 추천합니다.

#### 추천 흐름

```
POST /chatbot/features/CASH_FLOW_RECOMMEND/execute
  {"customer_no": "CUST001"}
```

```
1. 고객 보유 계좌 전체 잔액 합산 → total_balance
2. 최근 3개월 거래 조회 (direction_type 기준)
     direction_type = IN  → inflow 합산
     direction_type = OUT → outflow 합산
3. 월 잉여자금 = (inflow - outflow) / 3
4. 고객 유형 진단 (순서대로 적용, 조건 중복 없음)
     if 잉여자금 <= 0              → 지출 과다형 (예금만 허용)
     else if 잔액 >= 잉여자금 × 12 → 목돈 보유형 (예금 우선)
     else                          → 저축 성장형 (적금 우선)
5. 가입 불가 상품 제외
     예금: 잔액 < 최소가입금액          → 제외
     적금: 잉여자금 <= 0 또는
           잉여자금 < 최소납입액 × 2   → 제외 (납입 후 한 달치 여유 보장)
6. 개인고객 전용 상품만 대상 (target_group_id = 1)
     군인 전용(장병내일준비적금 등), 청년 전용 상품 자동 제외
7. 100점 체계로 점수 산정
8. 상위 3개 상품을 1~3위 카드로 반환
```

#### 100점 채점 기준

| 항목 | 배점 | 산식 |
|---|---|---|
| 재정 적합도 | 40점 | 예금: `min(잔액 / max(최소가입금액, 1), 5) / 5 × 40` |
| | | 적금: `min(잉여자금 / max(최소납입액×2, 1), 5) / 5 × 40` |
| | | 고객 유형 매칭 시 × 1.3 보너스 (최대 1.0 cap) |
| 예상 수익 | 30점 | 후보 전체 중 최고 수익을 1.0으로 정규화 후 × 30 |
| | | 예금: `최소가입금액 × 금리/100 × (최소기간/12)` |
| | | 적금: `최소납입액 × 최소기간 × 금리/100 / 2` (평균 잔액 기준) |
| 유동성 매칭 | 20점 | 월 10건↑ → 단기(≤12개월) 우선 / 5건↓ → 장기(≥24개월) 허용 |
| | | 6~9건 구간 → 기본값 0.7 |
| 혜택 | 10점 | 세금혜택 있으면 +7점 / 중도해지 가능하면 +3점 |

> 동점이면 금리 높은 순으로 정렬

#### 잉여자금 산정 상세

- `direction_type = 'IN'` → 입금 (급여, 적금 납입 수령, 이자 수취 등)
- `direction_type = 'OUT'` → 출금 (이체, 인출, 대출 상환, 적금 납입 등)
- 기간: **최근 3개월** 거래만 집계
- transaction_type이 아닌 **direction_type 기반**으로 계산하는 이유:
  - LOAN_PAYMENT 등 신규 거래 유형 추가 시 자동 처리
  - SAVINGS_PAYMENT 방향(납입 vs 만기 수령) 오분류 방지
  - 내부 계좌 간 이체는 OUT(출금계좌) + IN(입금계좌)로 자동 상쇄

#### 추천 이유 문구 예시

- 예금: `"잔액 6,200,000원 중 1,000,000원(16%) 예치 → 12개월 이자 약 30,000원 예상"`
- 적금: `"월 300,000원 납입(잉여자금의 20%), 납입 후 1,200,000원 여유 → 24개월 이자 약 144,000원 예상"`
- 청약: `"월 20,000원 납입 → 24개월 이자 약 14,400원, 주택청약 목적 상품"`

### 3. 저축 목표 기반 상품 추천 (SAVINGS_GOAL)

고객이 "1년 동안 500만원 모으고 싶어"처럼 저축 목표를 말하면, 멀티턴 대화로 추가 정보를 수집한 뒤 최적 상품을 계산·비교·추천합니다.

#### 대화 흐름

```
고객: "1년 동안 500만원 모으고 싶어"
  → 목표 금액(500만원)·기간(12개월) 파싱
  → 세션 저장 (stage: ASKED_MONTHLY)
챗봇: "1년 동안 500만원 모으는 목표군요! 매달 얼마 납입 가능하세요?"

고객: "월 30만원요" 또는 "목돈 300만원 있어요"
  → 납입 방식 파싱 (월납입 / 목돈)
  → deposit_banking_products 조회 (기간 조건 매칭)
  → 상품별 이자 계산 + 달성 가능 여부 판단
  → LLM 또는 룰 기반 추천 생성
챗봇: 상품 비교표 + 최종 추천 + 이자 계산 결과
```

#### 이자 계산식

| 상품 유형 | 계산식 |
|-----------|--------|
| 적금 | `월납입 × ((1 + r)^n - 1) / r × (1 + r)` (월복리, r = 연금리/12) |
| 예금 | `원금 × (1 + 연금리/100 × 개월수/12)` (단리) |

#### 세션 상태 관리

- 세션은 **프로세스 메모리(`_SESSION` dict)** 에 저장 (`chatbot_consultation_id` 키)
- **한계**: 서버 재시작 시 진행 중인 세션 초기화됨
- **한계**: `uvicorn --workers N` 멀티 워커 환경에서 워커 간 세션 공유 불가
- 프로덕션 전환 시 Redis 등 외부 저장소로 교체 필요

#### 추가 질문 트리거 키워드

| 키워드 예시 | 감지 의도 |
|-------------|-----------|
| "모으고 싶어", "모으려고" | 저축 목표 |
| "500만원 모으", "1억 모으" | 금액 포함 목표 |
| "저축 목표", "저축 계획" | 목표 설정 |
| "얼마 모을 수 있어" | 목표 달성 가능성 문의 |

### 4. 현금흐름 조회 (MY_CASH_FLOW)

```
POST /chatbot/features/MY_CASH_FLOW/execute
  {"customer_no": "CUST001"}
```

- 최근 3개월 거래 내역 최대 20건 반환
- `customer_no` 미제공 시 `AUTH_REQUIRED` 응답 (200)

### 4. 챗봇 시스템 (시나리오 기반)

- **시나리오 노드** 구조: `ChatbotNode` → `ChatbotNodeButton` → `ChatbotNodeFlow`
- 기본 시나리오 자동 시드: `POST /chatbot/scenarios/default`
- 챗봇 상담 전 과정 DB 기록 (`ChatbotConsultation`, `ChatbotIntent`)
- 버튼 선택 / 자유 텍스트 입력 모두 처리
- 종료된 상담에 메시지 전송 차단 (상태 검증)

### 5. LLM 폴백 (OpenAI)

시나리오·키워드로 처리되지 않는 자유 문의는 **OpenAI LLM**이 응답합니다.

```
처리 우선순위
1. 시나리오 버튼 선택 → 노드 흐름 응답
2. IntentClassifier 키워드 매칭 → DB 조회 후 포맷팅 응답
3. 미매칭 + OPENAI_API_KEY 설정 → gpt-4o-mini 응답 (LlmAdapter)
4. OPENAI_API_KEY 미설정 → 상담사 연결 안내 (LlmHandoffAdapter)
```

### 6. Kafka 이벤트

상담 흐름의 주요 이벤트를 Kafka로 발행합니다.

| 이벤트 | 토픽 | 발행 시점 |
|--------|------|-----------|
| `ChatbotStarted` | `chatbot-events` | 챗봇 상담 시작 |
| `ChatbotMessageSent` | `chatbot-events` | 챗봇 메시지 전송 |
| `AgentHandoffRequested` | `chatbot-events` | 상담사 연결 요청 |
| `AgentConnected` | `chat-events` | 상담사 수락 |
| `ChatMessageSent` | `chat-events` | 채팅 메시지 전송 |
| `ChatEnded` | `chat-events` | 상담 종료 |

> `CONSULTATION_KAFKA_ENABLED=false`(기본값) 시 발행 없이 서비스 정상 동작.

### 7. 상담사 채팅 (Live Chat)

- 상담사 대기열 조회: `GET /chat/queue`
- 상담사 수락 → 채팅 시작
- 챗봇 메시지 + 상담사 메시지 통합 이력 조회 (시간순)
- 만족도 점수 기록 후 상담 종료

---

## 지원 기능 코드 목록

| 카테고리 | 코드 | 설명 | 인증 필요 |
|----------|------|------|-----------|
| PRODUCT_ADVICE | `SAVINGS_GOAL` | 저축 목표 기반 멀티턴 상품 추천 | — |
| PRODUCT_ADVICE | `PRODUCT_GUIDE` | 수신 상품 추천 (RAG + 현금흐름) | — |
| PRODUCT_ADVICE | `RATE_GUIDE` | 상품 금리 안내 | — |
| PRODUCT_ADVICE | `JOIN_CONDITION` | 가입 조건 안내 | — |
| PRODUCT_ADVICE | `PRODUCT_COMPARE` | 상품 비교 | — |
| PRODUCT_ADVICE | `TERMS_RAG` | 약관 검색 | — |
| PRODUCT_ADVICE | `FAQ` | 자주 묻는 질문 | — |
| USER_FINANCE | `MY_ACCOUNTS` | 내 계좌 조회 | customer_no |
| USER_FINANCE | `MY_PRODUCTS` | 내 상품 조회 | customer_no |
| USER_FINANCE | `CONTRACT_STATUS` | 계약 상태 조회 | customer_no |
| USER_FINANCE | `MATURITY_SCHEDULE` | 만기 일정 조회 | customer_no |
| USER_FINANCE | `INTEREST_HISTORY` | 이자 이력 조회 | customer_no |
| USER_FINANCE | `MY_CASH_FLOW` | 현금 흐름 조회 (최근 3개월) | customer_no |
| USER_FINANCE | `CASH_FLOW_RECOMMEND` | 현금흐름 기반 상품 추천 1~3위 | customer_no |
| STAFF_SUPPORT | `STAFF_CUSTOMER` | 고객 정보 조회 (직원) | staff_id |
| STAFF_SUPPORT | `STAFF_CONTRACT` | 고객 계약 조회 (직원) | staff_id |
| STAFF_SUPPORT | `STAFF_ACCOUNT` | 고객 계좌 조회 (직원) | staff_id |
| STAFF_SUPPORT | `STAFF_TRANSFER_FLOW` | 이체 흐름 조회 (직원) | staff_id |
| STAFF_SUPPORT | `STAFF_CONSULTATION_HISTORY` | 상담 이력 조회 (직원) | staff_id |
| STAFF_SUPPORT | `STAFF_CASH_FLOW` | 고객 현금흐름 조회 (직원) | staff_id |

> 인증 미제공 시 `AUTH_REQUIRED` 또는 `STAFF_AUTH_REQUIRED` 응답 (HTTP 200)

---

## API 엔드포인트 요약

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/health` | 헬스 체크 |
| `GET` | `/chat` | 챗 UI (index.html) |
| `POST` | `/chatbot/scenarios/default` | 기본 시나리오 시드 |
| `GET` | `/chatbot/categories` | 챗봇 카테고리 목록 |
| `GET` | `/chatbot/features` | 챗봇 기능 목록 (20개) |
| `GET` | `/chatbot/features/{code}` | 챗봇 기능 상세 |
| `POST` | `/chatbot/features/{code}/execute` | 챗봇 기능 실행 |
| `POST` | `/chatbot/consultations/start` | 챗봇 상담 시작 |
| `POST` | `/chatbot/consultations/{id}/messages` | 챗봇 메시지 전송 |
| `GET` | `/chat/queue` | 상담사 대기열 조회 |
| `POST` | `/chat/consultations/{id}/connect` | 상담사 연결 수락 |
| `POST` | `/chat/consultations/{id}/messages` | 채팅 메시지 전송 |
| `GET` | `/chat/consultations/{id}/messages` | 채팅 메시지 이력 |
| `POST` | `/chat/consultations/{id}/end` | 상담 종료 |

> Swagger UI: `http://localhost:8090/docs`

---

## 환경 변수 설정

`.env` 파일 작성 (`.env.sample` 참고):

```env
# DB
CONSULTATION_DATABASE_URL=postgresql+psycopg://<user>:<password>@localhost:5432/<database>

# Kafka (비활성화 시 false)
CONSULTATION_KAFKA_ENABLED=false
CONSULTATION_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
CONSULTATION_KAFKA_TOPIC_CHATBOT_EVENTS=consultation.chatbot.events
CONSULTATION_KAFKA_TOPIC_CHAT_EVENTS=consultation.chat.events

# LLM (미설정 시 상담사 연결 안내로 폴백)
CONSULTATION_OPENAI_API_KEY=
CONSULTATION_OPENAI_MODEL=gpt-4o-mini
```

---

## 로컬 실행 방법

### 가상환경 설정

```powershell
cd services/consultation-service
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
```

### 서비스 시작

```powershell
uvicorn app.main:app --host 0.0.0.0 --reload --port 8087
```

또는 스크립트 사용 (Windows):

```powershell
.\scripts\start.ps1
```

### 기본 시나리오 시드

서비스 최초 실행 후 한 번 실행:

```powershell
curl -X POST http://localhost:8087/chatbot/scenarios/default
```

---

## 테스트 실행

```powershell
cd services/consultation-service
python -m pytest tests/ -q
```

| 테스트 파일 | 내용 |
|-------------|------|
| `test_basic.py` | 앱 임포트, 기본 기능 smoke test |
| `test_api_validation.py` | 입력 검증, 422 응답 |
| `test_chatbot_api.py` | 챗봇 상담 흐름 API |
| `test_chatbot_service.py` | ChatbotService 단위 테스트 |
| `test_features_api.py` | 기능 조회·실행 API |
| `test_features_product_advice.py` | PRODUCT_ADVICE 기능 실행 |
| `test_features_user_finance.py` | USER_FINANCE 기능 실행 |
| `test_features_staff_support.py` | STAFF_SUPPORT 기능 실행 |
| `test_chat_api.py` | 상담사 채팅 API |
| `test_chat_service.py` | ChatService 단위 테스트 |
| `test_runtime_contracts.py` | 런타임 계약 시나리오 |
| `test_savings_goal.py` | SAVINGS_GOAL 파서·세션·이자계산·빈 상품 처리 (19개) |

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

## 예외 처리

FastAPI 기본 처리 사용 (커스텀 핸들러 없음):

| 상황 | HTTP 상태 | 응답 형식 |
|------|-----------|-----------|
| 기능 코드 없음 | 404 | `{"detail": "챗봇 기능을 찾을 수 없습니다."}` |
| 지원하지 않는 기능 | 404 | `{"detail": "지원하지 않는 챗봇 기능입니다."}` |
| 인증 미제공 (USER) | 200 | `{"status": "AUTH_REQUIRED", "message": "..."}` |
| 인증 미제공 (직원) | 200 | `{"status": "STAFF_AUTH_REQUIRED", "message": "..."}` |
| Pydantic 검증 실패 | 422 | `{"detail": [{"loc": [...], "msg": "..."}]}` |
| 잘못된 요청 body | 422 | `{"detail": [{"type": "json_invalid", ...}]}` |

---

## 주요 설계 결정

| 결정 | 이유 |
|------|------|
| Intent 우선순위 분류 | 금리·가입조건 등 금융 특화 키워드를 먼저 처리해 LLM 호출 최소화 |
| LLM 폴백 선택적 적용 | API 키 미설정 시에도 서비스 정상 동작, 비용 제어 가능 |
| Kafka 비활성화 옵션 | 로컬·테스트 환경에서 Kafka 없이 즉시 실행 가능 |
| 챗봇·채팅 DB 분리 | 챗봇 이력과 상담사 채팅 이력을 독립적으로 관리 |
| 시나리오 자동 시드 | 첫 실행 시 기본 시나리오 자동 생성으로 빠른 시연 가능 |
| direction_type 기반 잉여자금 계산 | transaction_type 하드코딩 대신 방향(IN/OUT)으로 판단해 신규 거래 유형에 유연하게 대응 |
| 고객 유형 진단 if/else if/else | 잉여자금 음수일 때 조건 중복 방지 (surplus × 12 음수 → 조건 2 항상 참 버그 차단) |
| 적금 이자 계산에 상품 기간 반영 | 12개월 고정 대신 min_period_month 사용해 장기 상품 불이익 제거 |
| 개인고객 전용 필터 | target_group_id=1만 추천 대상으로 제한해 군인·청년 전용 상품 오추천 방지 |
| SAVINGS_GOAL 멀티턴 세션 메모리 저장 | DB 스키마 변경 없이 구현, 단일 워커 환경에서 충분. 한계(재시작 초기화·멀티 워커 미지원)는 README 및 코드 주석에 명시 |
| SAVINGS_GOAL 기간 조건 fallback | min/max_period_month 조건 일치 상품 없으면 기간 조건 제거 후 재조회해 빈 응답 방지 |
