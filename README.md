# Internet Banking MVP

MSA 구조 기반 인터넷뱅킹 플랫폼. 수신·여신·결제·고객·상담 도메인을 독립 서비스로 분리하고, AI/RAG 기반 자동심사·유사사례 검색·편향검증을 통합한다.

---

## 서비스 구성

| 서비스 | 언어 / 프레임워크 | 포트 | 역할 |
|---|---|---|---|
| `gateway-service` | Java 17 / Spring Cloud Gateway | 8080 | API 게이트웨이, 라우팅, Rate-limit |
| `customer-service` | Java 17 / Spring Boot 3.x | 8081 | 고객 정보, 인증 |
| `deposit-service` | Java 17 / Spring Boot 3.x | 8082 | 예금상품·계약·계좌·거래·상품추천 |
| `loan-service` | Java 17 / Spring Boot 3.x | 8083 | 대출 신청~상환 전 생애주기 + RAG |
| `master-service` | Java 17 / Spring Boot 3.x | 8085 | 공통 코드·마스터 데이터 |
| `ai-service` | Java 17 / Spring Boot 3.x | 8086 | AI 모델 서빙, 임베딩, RAG 벡터검색 |
| `auto-loan-review` | Java 17 / Spring Boot 3.x | 8086 | 자동심사 에이전트, 편향검증, 4-eye 승인 |
| `review-ai-gateway` | Java 17 / Spring Boot 3.x | 8088 | 심사 AI 라우팅 게이트웨이 |
| `payment-service` | Java 17 / Spring Boot 3.x | — | 결제·이체 처리, Kafka 이벤트 |
| `api-gateway` | Java 17 / Spring Cloud Gateway | — | (보조 게이트웨이) |
| `consultation-service` | Python 3.11 / FastAPI | 8087 | 챗봇·상담사 채팅, LLM 폴백, 챗봇 이체 실행 |
| `web` | Next.js 15 / TypeScript | 3001 | 고객·어드민 통합 프런트엔드 |
| `common` | Java | — | 서비스 공통 모듈 |
| `infra` | Docker Compose | — | PostgreSQL 16, Redis 7, Prometheus, Grafana |

---

## 기술 스택

| 구분 | 기술 |
|---|---|
| 백엔드 | Java 17, Spring Boot 3.x, Gradle Multi-module |
| 챗봇 | Python 3.11, FastAPI, SQLAlchemy, Pydantic v2 |
| 프런트엔드 | Next.js 15, TypeScript, TanStack Query, Tailwind CSS |
| DB | PostgreSQL 16 (서비스별 독립 DB), pgvector (AI/RAG) |
| 캐시 | Redis 7 |
| 메시지 큐 | Apache Kafka 3.8, Confluent Schema Registry |
| AI/LLM | Spring AI, OpenAI GPT-4o-mini |
| 모니터링 | Prometheus, Grafana, Loki, Promtail |
| 테스트 | JUnit 5, Mockito, @SpringBootTest / pytest 8 |
| 패키지 루트 | `com.bank` |

---

## 모듈 구조

```
internet_banking/
├── docker-compose.yml              # 전체 인프라 (DB×6, Kafka, Redis, Prometheus, Grafana, Gateway)
├── common/                         # 공통 모듈 (Java)
├── services/
│   ├── gateway-service/            # Spring Cloud Gateway
│   ├── customer-service/           # 고객 도메인
│   ├── deposit-service/            # 수신 도메인
│   │   ├── controller/             # REST 컨트롤러
│   │   ├── service/                # 비즈니스 로직
│   │   ├── domain/                 # 엔티티, Enum
│   │   ├── repository/             # JPA Repository
│   │   ├── dto/                    # 요청/응답 DTO
│   │   └── exception/              # 예외 처리
│   ├── loan-service/               # 여신 도메인 (대출 전 생애주기 + RAG)
│   ├── master-service/             # 공통 마스터 코드
│   ├── ai-service/                 # AI 서빙 + 벡터 DB
│   ├── auto-loan-review/           # 자동심사 에이전트
│   ├── review-ai-gateway/          # 심사 AI 라우팅
│   ├── payment-service/            # 결제·이체
│   ├── api-gateway/                # (보조 게이트웨이)
│   └── consultation-service/       # Python FastAPI — 챗봇·상담
│       ├── app/
│       │   ├── main.py             # FastAPI 앱, 라우터, CORS
│       │   ├── services.py         # ChatbotService (추천 포함), ChatService
│       │   ├── features/           # 기능별 Feature 모듈
│       │   ├── models.py           # SQLAlchemy 모델
│       │   ├── schemas.py          # Pydantic 스키마
│       │   ├── database.py         # DB 연결
│       │   └── kafka.py            # Kafka 이벤트 발행
│       └── tests/                  # pytest 테스트 (1614개)
├── web/                            # Next.js 프런트엔드
│   ├── app/
│   │   ├── (personal)/             # 개인뱅킹 라우트 그룹
│   │   └── (admin)/                # 어드민 라우트 그룹
│   ├── components/
│   │   └── chatbot/ChatbotWidget.tsx  # 챗봇 위젯 (consultation-service 연동)
│   └── lib/
│       ├── deposit-api.ts          # deposit-service 클라이언트
│       ├── consultation-api.ts     # consultation-service 클라이언트
│       ├── loan-api.ts             # loan-service 클라이언트
│       ├── advisory-api.ts         # advisory-service 클라이언트
│       ├── master-api.ts           # master-service 클라이언트
│       ├── payment-api.ts          # payment-service 클라이언트
│       └── ai-api.ts               # ai-service 클라이언트
├── infra/                          # Prometheus·Grafana·Loki 설정
└── docs/                           # 가이드 문서
```

---

## loan-service 주요 기능

대출 신청부터 상환·만기까지 전 생애주기를 처리한다.

### 대출 심사 흐름

```
신청(application) → 가심사(prescreening) → DSR 계산 → 신용평가(creditevaluation)
→ 신용점수(creditscore) → 본심사(review) → 담보(collateral) / LTV 계산
→ 보증인(guarantor) / 보증보험(guaranteeinsurance) → 서류(document)
→ 전자서명(consent) → 본인확인(IDV) → 약정실행(execution)
```

### 대출 관리 흐름

```
계약(contract) → 상환(repayment) / 부분상환(partialrepayment) / 선납(prepayment)
→ 연체(delinquency) → 금리변경(ratechange) → 만기(maturity) → 해지(closure)
```

### Agentic RAG (유사사례 검색)

대출심사 시 유사한 과거 심사 사례를 벡터 검색으로 조회해 심사위원 참고 자료로 제공한다.

| 컴포넌트 | 위치 | 역할 |
|---|---|---|
| `SimilarCaseExporter` | `loan-service/rag/` | 심사 완료 건을 청크로 변환, 임베딩·저장 |
| `SimilarCaseChunkTemplate` | `loan-service/rag/` | 청크 텍스트 템플릿 |
| `ai-service` | `services/ai-service/` | pgvector 기반 유사도 검색 |

### 자동심사·편향검증

`auto-loan-review` 서비스가 규칙 기반 + LLM 심사 의견을 생성하고, 편향검증 에이전트가 성별·나이·지역 편향 여부를 검사한다. 임계치 초과 시 4-eye 승인 프로세스로 에스컬레이션한다.

---

## deposit-service 주요 기능

### 상품 관리

- 예금·적금·청약·입출금 상품 조회 (유형별 필터링, 판매 상태 기준)
- 상품별 기본금리·우대금리·가입기간·가입금액 조건 관리
- 가입 대상 그룹(연령·직군 등) 연동
- `deposit-api` (FastAPI, 포트 8082): deposit-service 앞단 경량 API 레이어. `/transactions/transfer` 이체 엔드포인트 포함

### 계약 관리

- 계약 생성·조회·상태변경·해지·만기 처리
- 가입금액 검증: `minJoinAmount` 이상, `maxJoinAmount` 이하
- 계좌 자동 생성, 우대금리 이력, 특약 동의 관리
- 자동이체 설정 및 만기 처리

### 계좌 관리

- 계좌 목록 조회 (고객 ID 기준, CLOSED 계좌 제외)
- 계좌 유형별 분류: 예금 / 적금 / 청약 / 입출금
- `isWithdrawable` 플래그 기반 출금 가능 계좌 구분
- 잔액·거래내역 조회

### 거래 관리

- 입금·출금·이체 거래 원장(Transaction) 기록
- 당행이체 (`INTERNAL`): 수취 계좌 실존 여부 및 계좌번호 일치 검증
- 타행이체 (`EXTERNAL`): 상대 은행 코드·이름·예금주명 기록
- 이체 실행 지점 단일화 — result 페이지에서만 호출, 중복 실행 방지

### 상품 추천 (RecommendAgentService)

고객의 최근 거래내역 현금흐름을 분석해 예금상품을 추천한다. 추천 결과는 외부 AI/LLM 호출 없이
입출금 내역, 월평균 저축 가능 금액, 가입금액 조건, 판매 상태, 금리 기준을 점수화해 산출한다.

```
순현금흐름(netCashFlow) / periodMonth → estimatedSavingsAmount
→ 가입금액 조건 필터 → 판매 중 상품 필터 → bestRate 내림차순 최대 5개 반환
```

```
GET /api/products/recommend-agent?customerId={customerId}&periodMonth={periodMonth}
X-Customer-Id: {customerId}
```

### 계좌이체 실행 흐름

개인뱅킹 계좌이체 화면은 `deposit-service`의 거래 API를 최종 실행 지점으로 사용한다.

| 단계 | 프런트 라우트 | 역할 |
|---|---|---|
| STEP 1. 이체정보 입력 | `web/app/(personal)/transfer/account/page.tsx` | 출금 계좌 선택, 당행/타행 탭 전환, 입금 계좌·금액 입력, `pendingTransfer` 생성 |
| STEP 2. 이체정보 확인 | `web/app/(personal)/transfer/confirm/page.tsx` | 금융인증서 PIN 확인 후 결과 화면으로 이동 |
| STEP 3. 이체결과 | `web/app/(personal)/transfer/result/page.tsx` | `executeDepositTransfer()`를 호출해 실제 이체 실행 |

이체 인증 흐름은 금융인증서(AXful) 기반 3단계로 구성된다.

| 단계 | 설명 |
|---|---|
| 1. 전자서명 원문 확인 | 이체 금액·계좌 정보 표시 및 확인 |
| 2. 보안카드 번호 입력 | 랜덤 2개 위치의 보안카드 앞 2자리 입력 |
| 3. 인증서 PIN 6자리 입력 | 핀 6자리 입력 완료 후 `consultation-service /chatbot/transfer` 호출 |

실제 이체 실행은 PIN 입력 완료 시점에 `consultation-service`를 통해 수행된다.  
결과는 `sessionStorage('paymentResult')`에 저장되고 result 페이지에서 표시한다.

#### 당행/타행 이체 구분

- **당행 탭**: 내 계좌 드롭다운으로 수취 계좌 선택 → `transferType: INTERNAL`
- **타행 탭**: 은행 선택 모달 + 계좌번호 직접 입력 → `transferType: EXTERNAL`

```
POST /api/transactions/transfer
X-Customer-Id: {customerId}
```

프런트의 `executeDepositTransfer()`는 다음 값을 deposit API로 전달한다.

| 파라미터 | 설명 |
|---|---|
| `fromAccountId` | 출금 계좌 ID |
| `toAccountId` | 당행이체 시 입금 계좌 ID |
| `toAccountNo` | 입금 계좌번호 |
| `amount` | 이체금액 |
| `transferType` | `INTERNAL`(당행) 또는 `EXTERNAL`(타행) |
| `counterpartyBankCode` | 타행 은행 코드 |
| `counterpartyBankName` | 타행 은행명 |
| `counterpartyName` | 수취인 성명 |
| `channelType` | 항상 `INTERNET` |

출금 계좌 목록은 백엔드의 `isWithdrawable` 값을 우선 사용한다. 값이 없는 로컬 fallback 데이터는 계좌 유형명과 상품명에 `입출금` 또는 `통장`이 포함된 경우에만 출금 가능 계좌로 간주한다.

### 예금 상품 가입 흐름

| 단계 | 라우트 | 설명 |
|---|---|---|
| 상품 목록 | `/products/deposit/list` | 탭별 상품 목록, 가입하기 버튼 |
| 상품 상세 | `/products/deposit/[id]` | 상품 안내·금리·약관 상세 |
| 가입 | `/products/deposit/join/[id]` | 약관동의 → 정보입력 → 정보확인 3단계 |

지원 상품 유형: `예금` / `정기적금` / `자유적금` / `입출금자유` / `주택청약`

각 유형별로 약관 목록·가입기간 필드·금액 입력 범위·이자지급방법 표시 여부가 다르게 렌더링된다.

| 유형 | 약관 | 가입기간 | 이자지급방법 | LMS |
|---|---|---|---|---|
| 예금 | 거치식예금약관 포함 | O | O | O |
| 적금 | 적립식예금약관 포함 | O | O | O |
| 입출금자유 | 보통예금약관 포함 | X | X | X |
| 주택청약 | 주택청약종합저축약관 | O | O | O |

---

## consultation-service 주요 기능

### 챗봇 상담

- Python · FastAPI 기반 시나리오형 챗봇
- 시나리오 노드·버튼·플로우 DB 구조 설계, 멀티턴 대화 상태 관리
- 키워드 기반 Intent 분류 → 16개 기능 코드 실행 라우팅
- **챗봇 이체 API**: 인증 완료 후 챗봇 경로(`/chatbot/transfer`)로 이체 실행

| 카테고리 | 기능 코드 | 설명 |
|---|---|---|
| PRODUCT_ADVICE | PRODUCT_GUIDE | 예금·적금·청약 상품 안내 |
| | RATE_GUIDE | 금리·우대금리 안내 |
| | JOIN_CONDITION | 가입 조건 안내 |
| | PRODUCT_COMPARE | 상품 비교 |
| | TERMS_RAG | 약관 검색 |
| | FAQ | 자주 묻는 질문 |
| USER_FINANCE | MY_ACCOUNTS | 내 계좌 조회 |
| | MY_PRODUCTS | 가입 상품 조회 |
| | CONTRACT_STATUS | 계약 상태 조회 |
| | MATURITY_SCHEDULE | 만기 예정 조회 |
| | INTEREST_HISTORY | 이자 내역 조회 |
| | MY_CASH_FLOW | 현금 흐름 조회 |
| | CASH_FLOW_RECOMMEND | 현금흐름 기반 상품 추천 |
| | SAVINGS_GOAL | 저축 목표 멀티턴 추천 |
| STAFF_SUPPORT | STAFF_CUSTOMER | 직원용 고객 정보 조회 |
| | STAFF_CONTRACT | 직원용 고객 계약 조회 |
| | STAFF_ACCOUNT | 직원용 고객 계좌 조회 |
| | STAFF_TRANSFER_FLOW | 직원용 이체 흐름 조회 |
| | STAFF_CONSULTATION_HISTORY | 상담 이력 조회 |
| | STAFF_CASH_FLOW | 직원용 현금 흐름 조회 |

### 상담사 실시간 채팅

- 챗봇 → 상담사 핸드오프 흐름 (상담 접수·메시지 이력·채팅 종료)
- 상담사 대기열 조회 및 연결 수락
- 만족도 점수 기록

### Kafka 연동

| Topic | 이벤트 | 방향 |
|---|---|---|
| `consultation.chatbot.events` | ChatbotConsultationStarted, ChatbotMessageHandled, ChatbotAgentTransferRequested | Producer |
| `consultation.chatbot.message` | ChatbotMessageReceived | Producer + Consumer |
| `consultation.chat.events` | AgentConnected, ChatMessageSent, ChatEnded | Producer |
| `deposit.contract.events` | ContractCreated | Consumer |

- `consultation.chatbot.message` 수신 시 `chat_message_history` 테이블 자동 저장
- Kafka 활성화: `.env`에서 `CONSULTATION_KAFKA_ENABLED=true` 설정

### 주요 API

```
GET  /health
GET  /chatbot/features
GET  /chatbot/features/{feature_code}
POST /chatbot/features/{feature_code}/execute
POST /chatbot/consultations/start
POST /chatbot/consultations/{id}/messages
POST /chatbot/transfer
GET  /chat/queue
POST /chat/consultations/{id}/connect
POST /chatbot/transfer                         # 챗봇 경로 이체 실행
POST /chat/consultations/{id}/messages
GET  /chat/consultations/{id}/messages
POST /chat/consultations/{id}/end
GET  /metrics
```

---

## 로컬 실행

### 사전 조건

- Java 17+
- Docker Desktop
- Python 3.11 (consultation-service)
- Node.js 20+ (web)

### 인프라 실행

```powershell
docker compose up -d
```

컨테이너: PostgreSQL×6 (5432~5437), pgvector, Kafka (9092), Schema Registry (8081), Redis (6379), Prometheus (9090), Grafana (3000), Gateway (8080)

### 백엔드 서비스 실행

```powershell
# 각 서비스 개별 실행
.\gradlew :services:customer-service:bootRun   # 8081
.\gradlew :services:deposit-service:bootRun    # 8082
.\gradlew :services:loan-service:bootRun       # 8083
.\gradlew :services:master-service:bootRun     # 8085
.\gradlew :services:ai-service:bootRun         # 8086
```

Swagger UI: `http://localhost:{port}/swagger-ui/index.html`

### consultation-service 실행

Docker Compose 사용 (권장):

```powershell
cd services/consultation-service
docker compose up -d
```

직접 실행:

```powershell
cd services/consultation-service
.\.venv\Scripts\python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 8087
```

기본 포트: `8087`  
Swagger UI: `http://localhost:8087/docs`  
헬스 체크: `http://localhost:8087/health`

> **주의**: consultation-service의 독립 docker-compose는 PostgreSQL을 포트 `5439`에 노출한다. 메인 `docker-compose.yml`과 동시에 실행해도 포트 충돌이 없다.

### customer-service 실행 시 환경변수

customer-service는 `CRYPTO_KEY_BASE64` 환경변수가 없으면 기동 실패한다.

```powershell
$env:CRYPTO_KEY_BASE64="bG9hbi1zZXJ2aWNlLWRldi1hZXMta2V5LTMyYnl0ZXM="
.\gradlew :services:customer-service:bootRun
```

> 운영 환경에서는 반드시 별도의 32바이트 AES-256 키(Base64)를 발급해 사용한다.

### 프런트엔드 실행

```powershell
cd web
npm install
npm run dev   # http://localhost:3001
```

> `.env.local` 파일이 없으면 아래 내용으로 생성:
>
> ```env
> NEXT_PUBLIC_API_URL=http://localhost:8080
> NEXT_PUBLIC_DEPOSIT_API_URL=http://localhost:8082/api
> NEXT_PUBLIC_CONSULTATION_API_URL=http://localhost:8087
> NEXT_PUBLIC_LOAN_API_URL=http://localhost:8083
> NEXT_PUBLIC_ADVISORY_API_URL=http://localhost:8084
> NEXT_PUBLIC_MASTER_API_URL=http://localhost:8085
> NEXT_PUBLIC_AI_API_URL=http://localhost:8086
> NEXT_PUBLIC_PAYMENT_API_URL=http://localhost:8087
> ```

---

## 테스트 실행

```powershell
# deposit-service (236개)
.\gradlew :services:deposit-service:test

# loan-service
.\gradlew :services:loan-service:test

# consultation-service (1614개)
cd services/consultation-service
python -m pytest tests/ -q
```

---

## 모니터링

| 도구 | 주소 | 설명 |
|---|---|---|
| Prometheus | `http://localhost:9090` | 메트릭 수집 |
| Grafana | `http://localhost:3000` | 대시보드 |

loan-service 메트릭 명세·PromQL·SLO: [`docs/loan-service-metrics.md`](docs/loan-service-metrics.md)

---

## 주의사항

- `main` 브랜치 직접 커밋/푸시 금지
- `.env` 파일 커밋 금지
- 운영/스테이징 DB 접속 명령 금지
- AI 모델명·세션 링크를 커밋 메시지·PR에 삽입 금지

공통 AI 작업 가이드: [`docs/AI_GUIDELINES.md`](docs/AI_GUIDELINES.md)  
Claude Code 추가 설정: [`CLAUDE.md`](CLAUDE.md)
