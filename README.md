# Internet Banking MVP

작성자: 정혜영

---

## 프로젝트 개요

Internet Banking MVP는 고객, 예금, 상담, 결제 등 인터넷뱅킹의 핵심 기능을 서비스 단위로 분리한 멀티 모듈 프로젝트다.  
Spring Boot 기반 수신 서비스와 FastAPI 기반 챗봇·상담 서비스로 구성되며, 공통 인프라(PostgreSQL, Redis)를 Docker Compose로 관리한다.

---

## 서비스 구성

| 서비스 | 언어 / 프레임워크 | 포트 | 역할 |
|---|---|---|---|
| `services/deposit-service` | Java 17 / Spring Boot 3.x | 8082 | 예금상품, 계약, 계좌, 거래, 상품 추천 |
| `services/consultation-service` | Python 3.11 / FastAPI | 8090 | 챗봇, 현금흐름 분석, 상품 추천, 상담사 채팅 |
| `web` | Next.js 14 / TypeScript | 3001 | 인터넷뱅킹 프론트엔드 (AX풀뱅크) |
| `common` | Java | — | 서비스 공통 모듈 |
| `infra` | Docker Compose | — | PostgreSQL 16, Redis 7, Prometheus, Grafana |

---

## 기술 스택

| 구분 | 기술 |
|---|---|
| 백엔드 (수신계) | Java 17, Spring Boot 3.3.5, Gradle Multi-module |
| 백엔드 (챗봇) | Python 3.11, FastAPI, SQLAlchemy, Pydantic v2 |
| DB (수신계) | PostgreSQL 16 (운영), H2 (테스트) |
| DB (챗봇) | PostgreSQL 16 (운영), SQLite in-memory (테스트) |
| 캐시 | Redis 7 |
| 모니터링 | Prometheus, Grafana |
| 테스트 | JUnit 5, Mockito (Java) / pytest 8.3.4 (Python) |
| 패키지 루트 | `com.bank` |

---

## 모듈 구조

```
internet_banking/
├── common/                         # 공통 모듈 (Java)
├── services/
│   ├── deposit-service/            # Java Spring Boot — 수신계 핵심
│   │   ├── controller/             # REST 컨트롤러
│   │   ├── service/                # 비즈니스 로직
│   │   ├── domain/                 # 엔티티, Enum
│   │   ├── repository/             # JPA Repository
│   │   ├── dto/                    # 요청/응답 DTO
│   │   └── exception/              # 예외 처리 (GlobalExceptionHandler)
│   └── consultation-service/       # Python FastAPI — 챗봇·상담·추천
│       ├── app/
│       │   ├── main.py             # FastAPI 앱, 라우터, CORS
│       │   ├── services.py         # ChatbotService (추천 포함), ChatService
│       │   ├── features/           # 기능별 Feature 모듈
│       │   ├── models.py           # SQLAlchemy 모델
│       │   ├── schemas.py          # Pydantic 스키마
│       │   ├── database.py         # DB 연결
│       │   └── kafka.py            # Kafka 이벤트 발행
│       └── tests/                  # pytest 테스트 (325개)
├── web/                            # Next.js 14 프론트엔드
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
├── infra/                          # Docker Compose 인프라
└── docs/                           # 문서
```

---

## deposit-service 주요 기능

### 계약 관리 (ContractService)

- 계약 생성 / 조회 / 상태 변경 / 해지 / 만기 처리
- 가입금액 검증: `minJoinAmount` 이상, `maxJoinAmount` 이하 (위반 시 400 반환)
- 계좌 자동 생성 (계약 생성 시 연동)
- 우대금리 적용 이력 관리
- 특약 동의 관리

### 상품 추천 (RecommendAgentService)

고객의 최근 거래내역을 분석해 현금흐름을 계산하고, 월 예상 저축 가능 금액에 맞는 예금상품을 추천한다.

**추천 로직**

```
1. 고객의 활성 계좌 조회
2. 지정 기간(periodMonth) 내 성공 거래내역 조회
3. 입금 합계 / 출금 합계 / 순현금흐름 계산
4. estimatedSavingsAmount = netCashFlow / periodMonth (RoundingMode.DOWN)
5. estimatedSavingsAmount > 0 이고 상품 minJoinAmount 이상인 상품 필터
6. 최고금리(bestRate) 내림차순 정렬 후 최대 5개 추천 반환
```

**API**

```
GET /api/products/recommend-agent?customerId={customerId}&periodMonth={periodMonth}
X-Customer-Id: {customerId}
```

| 파라미터 | 필수 | 기본값 | 제약 | 설명 |
|---|---|---|---|---|
| `customerId` | ✅ | — | — | 추천 대상 고객 ID |
| `periodMonth` | ❌ | 3 | 1 이상 (미만 시 400) | 현금흐름 분석 기간 (개월) |

**응답 예시**

```json
{
  "customerId": "CUST001",
  "analysisPeriodMonth": 3,
  "cashFlow": {
    "totalInflow": 3000000,
    "totalOutflow": 2000000,
    "netCashFlow": 1000000,
    "estimatedSavingsAmount": 333333
  },
  "recommendations": [
    {
      "productId": 1,
      "productName": "자유적금",
      "productType": "SAVINGS",
      "baseRate": 3.20,
      "bestRate": 3.50,
      "minJoinAmount": 10000,
      "maxJoinAmount": 1000000,
      "minPeriodMonth": 6,
      "maxPeriodMonth": 36,
      "reason": "월 평균 저축 가능 금액(333,333원) 기반 추천. 연 3.50% 금리 적용."
    }
  ]
}
```

**엣지 케이스 처리**

| 상황 | 처리 결과 |
|---|---|
| 계좌 없음 | cashFlow 전부 0, recommendations=[] |
| 거래내역 없음 | cashFlow 전부 0, recommendations=[] |
| 출금 > 입금 | netCashFlow 음수, recommendations=[] |
| 판매 상품 없음 | recommendations=[] |
| estimatedSavings < minJoinAmount | 해당 상품 추천 제외 |
| periodMonth < 1 | 400 Bad Request |

### 예외 처리 (GlobalExceptionHandler)

| 예외 | HTTP 상태 | 설명 |
|---|---|---|
| `BusinessException` | 각 ErrorCode 지정값 | 비즈니스 규칙 위반 |
| `MethodArgumentNotValidException` | 400 | `@Valid` DTO 검증 실패 |
| `ConstraintViolationException` | 400 | `@Validated` + `@Min` 등 파라미터 검증 실패 |
| `HttpMessageNotReadableException` | 400 | 요청 body 파싱 불가 |
| `MissingServletRequestParameterException` | 400 | 필수 파라미터 누락 |
| `NoResourceFoundException` | 404 | 경로 없음 |
| `Exception` (catch-all) | 500 | 미처리 예외 |

---

## consultation-service 주요 기능

### 챗봇 기능 실행

16개 기능 코드 지원. 카테고리별 분류:

| 카테고리 | 기능 코드 |
|---|---|
| PRODUCT_ADVICE | PRODUCT_GUIDE, RATE_GUIDE, JOIN_CONDITION, PRODUCT_COMPARE, TERMS_RAG, FAQ |
| USER_FINANCE | MY_ACCOUNTS, MY_PRODUCTS, CONTRACT_STATUS, MATURITY_SCHEDULE, INTEREST_HISTORY |
| STAFF_SUPPORT | STAFF_CUSTOMER, STAFF_CONTRACT, STAFF_ACCOUNT, STAFF_TRANSFER_FLOW, STAFF_CONSULTATION_HISTORY |

**API**

```
GET  /chatbot/categories                           # 카테고리 목록
GET  /chatbot/features                             # 기능 목록 (16개)
GET  /chatbot/features/{feature_code}              # 기능 상세
POST /chatbot/features/{feature_code}/execute      # 기능 실행
```

**인증 처리**

| 요청 상황 | 응답 status |
|---|---|
| USER_FINANCE 기능, customer_no 없음 | AUTH_REQUIRED (200) |
| STAFF_SUPPORT 기능, staff_id 없음 | STAFF_AUTH_REQUIRED (200) |
| 존재하지 않는 feature_code | 404 |

### 챗봇 상담 흐름

```
POST /chatbot/consultations/start                          # 상담 시작
POST /chatbot/consultations/{id}/messages                  # 메시지 전송
```

### 상담사 채팅

```
GET  /chat/queue                                           # 대기열 조회
POST /chat/consultations/{id}/connect                      # 상담 수락
POST /chat/consultations/{id}/messages                     # 메시지 전송
GET  /chat/consultations/{id}/messages                     # 이력 조회
POST /chat/consultations/{id}/end                          # 상담 종료
```

### 예외 처리

FastAPI 기본 처리 사용 (커스텀 핸들러 없음):

| 상황 | HTTP 상태 | 응답 형식 |
|---|---|---|
| 기능 없음 | 404 | `{"detail": "챗봇 기능을 찾을 수 없습니다."}` |
| 실행 기능 없음 | 404 | `{"detail": "지원하지 않는 챗봇 기능입니다."}` |
| Pydantic 검증 실패 | 422 | `{"detail": [{"loc": [...], "msg": "..."}]}` |
| 잘못된 요청 body | 422 | `{"detail": [{"type": "json_invalid", ...}]}` |

---

## 테스트 현황

### deposit-service

```powershell
.\gradlew :services:deposit-service:test
```

| 항목 | 수치 |
|---|---|
| 전체 테스트 수 | **236개** |
| 통과 | 236개 |
| 실패 | 0개 |
| 에러 | 0개 |
| 스킵 | 0개 |

#### 서비스 / 컨트롤러 테스트 파일

| 도메인 | Service 테스트 | Controller 테스트 |
|---|---|---|
| Account | AccountServiceTest | AccountControllerTest |
| Contract | ContractServiceTest | ContractControllerTest |
| Department | DepartmentServiceTest | DepartmentControllerTest |
| Interest | InterestServiceTest | InterestControllerTest |
| Product | ProductServiceTest | ProductControllerTest |
| RecommendAgent | RecommendAgentServiceTest | RecommendAgentControllerTest |
| SpecialTerm | SpecialTermServiceTest | SpecialTermControllerTest |
| TargetGroup | TargetGroupServiceTest | TargetGroupControllerTest |
| Transaction | TransactionServiceTest | TransactionControllerTest |
| TermApplicationManagement | TermApplicationManagementServiceTest | TermApplicationManagementControllerTest |
| SubscriptionPaymentRecognitionHistory | SubscriptionPaymentRecognitionHistoryServiceTest | SubscriptionPaymentRecognitionHistoryControllerTest |

#### Repository 테스트 파일

| Repository | 테스트 파일 | 테스트 방식 |
|---|---|---|
| TransactionRepository | TransactionRepositoryTest | @DataJpaTest + H2 in-memory |

#### 안정화에서 추가된 테스트 (5개)

| 테스트 | 내용 |
|---|---|
| `joinAmount가 minJoinAmount보다 작으면 예외가 발생한다` | 최소 가입금액 미만 → 400 |
| `joinAmount가 maxJoinAmount보다 크면 예외가 발생한다` | 최대 가입금액 초과 → 400 |
| `joinAmount가 minJoinAmount와 같으면 계약이 정상 생성된다` | 경계값 정상 처리 |
| `periodMonth=0 요청 시 400을 반환한다` | @Min(1) 검증 → 400 |
| `periodMonth=-1 요청 시 400을 반환한다` | @Min(1) 검증 → 400 |

#### TransactionRepository @DataJpaTest 검증 항목 (5개)

대상 메서드: `findByAccountIdInAndTransactionAtBetweenAndStatus`

| 테스트 | 검증 내용 |
|---|---|
| `여러 accountId를 넘기면 해당 계좌들의 거래를 함께 조회한다` | accountId IN 조건 — 지정한 계좌만 포함 |
| `transactionAt 기간 필터가 적용된다 — 기간 내 거래만 반환한다` | BETWEEN 조건 — 기간 내 거래만 반환 |
| `status=SUCCESS인 거래만 조회된다 — FAILED·PENDING은 제외된다` | status = SUCCESS 필터 |
| `기간 밖 거래는 결과에서 제외된다 — 경계 직전·직후 모두 제외` | 기간 경계값 제외 확인 |
| `accountId 목록에 없는 계좌의 거래는 제외된다` | accountId NOT IN 목록 제외 확인 |

### consultation-service

```powershell
cd services/consultation-service
python -m pytest tests/ -q
```

| 항목 | 수치 |
|---|---|
| 전체 테스트 수 | **325개** |
| 통과 | 323개 |
| 실패 | 1개 (정적 파일 미존재 — 발표 대상 외) |
| 스킵 | 1개 (deposit-service 영역 — 의도적) |

#### 테스트 파일 목록

| 파일 | 대상 |
|---|---|
| test_basic.py | 앱 임포트, 기본 기능 smoke test |
| test_api_validation.py | 입력 검증, 422 응답 |
| test_chatbot_api.py | 챗봇 상담 흐름 API |
| test_chatbot_service.py | ChatbotService 단위 테스트 |
| test_features_api.py | 기능 조회·실행 API (카테고리/피처/실행) |
| test_features_product_advice.py | PRODUCT_ADVICE 기능 실행 |
| test_features_user_finance.py | USER_FINANCE 기능 실행 |
| test_features_staff_support.py | STAFF_SUPPORT 기능 실행 |
| test_chat_api.py | 상담사 채팅 API |
| test_chat_service.py | ChatService 단위 테스트 |
| test_runtime_contracts.py | 런타임 계약 시나리오 |

---

## 로컬 실행

### 사전 조건

- Java 17+
- Docker Desktop (PostgreSQL, Redis)
- Python 3.11+
- Node.js 18+

### 인프라 실행

```powershell
docker compose -f infra/docker-compose.yml up -d
```

### deposit-service 실행

```powershell
# PostgreSQL(5432) 연결, Flyway 검증 비활성화(로컬 스키마 불일치 방지)
$env:DEPOSIT_DB_PORT = "5432"
$env:DEPOSIT_DB_PASSWORD = "deposit"
cd services/deposit-service
java -jar build/libs/deposit-service-0.0.1-SNAPSHOT.jar `
  --spring.profiles.active=postgres-local `
  --spring.flyway.validate-on-migrate=false
```

기본 포트: `8082`  
Swagger UI: `http://localhost:8082/swagger-ui/index.html`  
Actuator 헬스: `http://localhost:8082/actuator/health`

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

### 프론트엔드 실행

```powershell
cd web
npm install
npm run dev
```

기본 포트: `3001`  
접속: `http://localhost:3001`

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

## 발표 시연 순서

### 1. 서비스 실행 확인

```powershell
# deposit-service 헬스 체크
curl http://localhost:8082/actuator/health

# consultation-service 헬스 체크
curl http://localhost:8001/health
```

### 2. 상품 조회

```
GET http://localhost:8082/api/products
GET http://localhost:8082/api/products/{product_id}
```

### 3. 계약 생성

```
POST http://localhost:8082/api/contracts
{
  "customerId": "CUST001",
  "productId": 1,
  "joinAmount": 3000000,
  "contractPeriodMonth": 12,
  "accountPassword": "1234"
}
```

### 4. 계좌 / 거래내역 조회

```
GET http://localhost:8082/api/accounts?customerId=CUST001
GET http://localhost:8082/api/transactions?accountId={id}
```

### 5. recommend-agent 호출

```
GET http://localhost:8082/api/products/recommend-agent?customerId=CUST001&periodMonth=3
X-Customer-Id: CUST001
```

확인 항목:
- `cashFlow.totalInflow` / `totalOutflow` / `netCashFlow` / `estimatedSavingsAmount`
- `recommendations` 목록 (상품명, bestRate, reason)

### 6. 챗봇 기능 조회 및 실행

```
GET  http://localhost:8001/chatbot/features
GET  http://localhost:8001/chatbot/features/MY_ACCOUNTS
POST http://localhost:8001/chatbot/features/MY_ACCOUNTS/execute
     {"customer_no": "CUST001"}

POST http://localhost:8001/chatbot/features/STAFF_TRANSFER_FLOW/execute
     {"customer_no": "CUST001", "staff_id": "EMP001"}
```

### 7. 챗봇 상담 시작

```
POST http://localhost:8001/chatbot/scenarios/default
POST http://localhost:8001/chatbot/consultations/start
     {"customer_no": "CUST001"}
```

---

## 발표 전 체크리스트

```
[ ] docker compose up -d                  — PostgreSQL, Redis 정상 기동 확인
[ ] deposit-service bootRun               — 포트 8082 정상 응답 확인
[ ] consultation-service uvicorn          — 포트 8001 정상 응답 확인
[ ] GET /actuator/health                  — {"status":"UP"} 확인
[ ] GET /health (consultation)            — {"status":"UP"} 확인
[ ] GET /api/products                     — 상품 목록 1건 이상 확인
[ ] GET /recommend-agent (CUST001, X-Customer-Id 포함) — recommendations 1건 이상 확인
[ ] cashFlow.estimatedSavingsAmount > 0   — 수치 정상 확인
[ ] recommendations[0].reason 표시        — reason 문자열 포함 확인
[ ] GET /chatbot/features                 — 16개 기능 코드 확인
[ ] POST MY_ACCOUNTS/execute              — status="OK", data 반환 확인
[ ] POST STAFF_TRANSFER_FLOW/execute      — TX-001 포함 확인
[ ] periodMonth=0 요청                    — 400 Bad Request 반환 확인
[ ] 잘못된 feature_code 요청              — 404 반환 확인
```

---

## 안정화 내역 (발표 버전 기준)

### deposit-service

| 항목 | 수정 내용 |
|---|---|
| ContractService | 계약 생성 시 `minJoinAmount` / `maxJoinAmount` 검증 추가 |
| RecommendAgentController | `periodMonth` 파라미터에 `@Validated` + `@Min(1)` 적용 |
| GlobalExceptionHandler | `ConstraintViolationException` 핸들러 추가 (400 반환) |
| ContractServiceTest | joinAmount 경계값 테스트 3개 추가 |
| RecommendAgentControllerTest | periodMonth=0/-1 거부 테스트 2개 추가 |

### consultation-service

| 항목 | 판정 |
|---|---|
| 404 body 확인 | `{"detail": "..."}` 정상 포함 — 수정 불필요 |
| 422 body 확인 | FastAPI 표준 형식 정상 포함 — 수정 불필요 |

---

## 브랜치 / 커밋 정보

- **브랜치**: `deposit`
- **작성자**: 정혜영
- **주요 커밋**:
  - `feat(deposit): implement cash-flow based recommendation agent`
  - `test(deposit): 수신계 기능 테스트 보강`
  - `feat(consultation): 챗봇·상담 서비스 구현`
  - `fix(deposit): 계약 가입금액 범위 검증 및 periodMonth 최솟값 제약 추가`

---

## 주의사항

- `main` 브랜치 직접 커밋/푸시 금지
- `.env` 파일 커밋 금지
- 운영/스테이징 DB 접속 명령 금지
- 커밋 메시지에 AI 모델명·서명 삽입 금지 (공통 가이드 §5)
