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
| `consultation-service` | Python 3.11 / FastAPI | 8090 | 챗봇·상담사 채팅, LLM 폴백 |
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
│       └── tests/                  # pytest 테스트 (325개)
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

### 계약 관리

- 계약 생성·조회·상태변경·해지·만기 처리
- 가입금액 검증: `minJoinAmount` 이상, `maxJoinAmount` 이하
- 계좌 자동 생성, 우대금리 이력, 특약 동의 관리

### 상품 추천 (RecommendAgentService)

고객의 최근 거래내역 현금흐름을 분석해 예금상품을 추천한다.

```
순현금흐름(netCashFlow) / periodMonth → estimatedSavingsAmount
→ minJoinAmount 이상 상품 필터 → bestRate 내림차순 최대 5개 반환
```

```
GET /api/products/recommend-agent?customerId={customerId}&periodMonth={periodMonth}
X-Customer-Id: {customerId}
```

### 계좌이체 실행 흐름

개인뱅킹 계좌이체 화면은 `deposit-service`의 거래 API를 최종 실행 지점으로 사용한다.

| 단계 | 프런트 라우트 | 역할 |
|---|---|---|
| STEP 1. 이체정보 입력 | `web/app/(personal)/transfer/account/page.tsx` | 출금 가능 계좌 조회, 입금 계좌/금액 입력, `pendingTransfer` 생성 |
| STEP 2. 이체정보 확인 | `web/app/(personal)/transfer/confirm/page.tsx` | 금융인증서 PIN 확인 후 결과 화면으로 이동 |
| STEP 3. 이체결과 | `web/app/(personal)/transfer/result/page.tsx` | `executeDepositTransfer()`를 호출해 실제 이체 실행 |

중복 이체를 막기 위해 실제 API 호출은 `result` 페이지 한 곳에서만 수행한다. `confirm` 페이지는 인증 및 화면 전환만 담당하며, `payment-service`의 `createInstantTransfer()`를 호출하지 않는다.

```
POST /api/transactions/transfer
X-Customer-Id: {customerId}
```

프런트의 `executeDepositTransfer()`는 다음 값을 deposit API로 전달한다.

- `fromAccountId`: 출금 계좌 ID
- `toAccountId`: 당행 내 계좌 이체인 경우 입금 계좌 ID
- `toAccountNo`: 입금 계좌번호
- `amount`: 이체금액
- `transferType`: `INTERNAL` 또는 `EXTERNAL`
- `counterpartyBankCode`, `counterpartyBankName`, `counterpartyName`
- `channelType`: `INTERNET`

출금 계좌 목록은 백엔드의 `isWithdrawable` 값을 우선 사용한다. 값이 없는 로컬 fallback 데이터는 계좌 유형명과 상품명에 `입출금` 또는 `통장`이 포함된 경우에만 출금 가능 계좌로 간주한다.

---

## consultation-service 주요 기능

- 16개 기능 코드 (PRODUCT_ADVICE / USER_FINANCE / STAFF_SUPPORT)
- 시나리오 기반 챗봇, 키워드 Intent 분류, OpenAI GPT-4o-mini 폴백
- 상담사 실시간 채팅, Kafka 이벤트 발행

```
GET  /chatbot/features
POST /chatbot/features/{feature_code}/execute
POST /chatbot/consultations/start
POST /chatbot/consultations/{id}/messages
POST /chat/consultations/{id}/connect
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

```powershell
cd services/consultation-service
.\.venv\Scripts\python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 8090
```

또는 스크립트 사용:

```powershell
.\scripts\start.ps1
```

기본 포트: `8090`  
Swagger UI: `http://localhost:8090/docs`  
헬스 체크: `http://localhost:8090/health`

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
> NEXT_PUBLIC_CONSULTATION_API_URL=http://localhost:8090
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

# consultation-service (325개)
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
