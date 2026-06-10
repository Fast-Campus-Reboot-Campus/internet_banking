# Internet Banking MVP

MSA 구조 기반 인터넷뱅킹 플랫폼. 수신·여신·결제·고객·상담 도메인을 독립 서비스로 분리하고, AI/RAG 기반 자동심사·유사사례 검색·편향검증을 통합한다.

---

## 서비스 구성

| 서비스 | 언어 / 프레임워크 | 포트 | 역할 |
|---|---|---|---|
| `gateway-service` | Java 17 / Spring Cloud Gateway | 8080 | API 게이트웨이, 라우팅, Rate-limit |
| `customer-service` | Java 17 / Spring Boot 3.x | 8081 | 고객 정보, 인증(ID·금융인증서) |
| `deposit-service` | Java 17 / Spring Boot 3.x | 8082 | 예금상품·계약·계좌·거래·상품추천 |
| `deposit-api` | Python 3.11 / FastAPI | 8082 (proxy) | deposit-service 앞단 경량 레이어, 이체 실행·소유주 검증 |
| `loan-service` | Java 17 / Spring Boot 3.x | 8083 | 대출 신청~상환 전 생애주기 + RAG |
| `master-service` | Java 17 / Spring Boot 3.x | 8085 | 공통 코드·마스터 데이터 |
| `ai-service` | Java 17 / Spring Boot 3.x | 8086 | AI 모델 서빙, 임베딩, RAG 벡터검색 |
| `auto-loan-review` | Java 17 / Spring Boot 3.x | 8089 | 자동심사 에이전트, 편향검증, 4-eye 승인 |
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
├── docker-compose.yml
├── common/
├── services/
│   ├── customer-service/
│   │   └── config/SecurityConfig.java   # CORS 허용 (localhost:3001)
│   ├── deposit-service/
│   │   ├── service/TransactionService.java          # 이체 실행, 한도 검증
│   │   ├── service/IdempotentTransactionSaver.java  # 멱등성 키 중복 방지
│   │   └── resources/db/migration/                  # Flyway V1~V14
│   ├── deposit-api/
│   │   └── app/main.py                  # 이체 엔드포인트, 소유주 검증
│   ├── loan-service/
│   ├── consultation-service/
│   │   └── app/
│   │       ├── main.py                  # FastAPI 앱, 라우터, CORS
│   │       ├── services.py              # ChatbotService, ChatService
│   │       ├── features/                # 기능별 Feature 모듈
│   │       └── kafka.py
│   └── (기타 서비스)
├── web/
│   ├── app/
│   │   ├── (personal)/
│   │   │   ├── login/page.tsx           # ID·금융인증서 통합 세션 저장
│   │   │   ├── logout/page.tsx          # sessionStorage 전체 초기화
│   │   │   └── products/deposit/join/[id]/page.tsx
│   │   └── api/
│   │       ├── customer/cert-login/route.ts  # 금융인증서 SSR 프록시
│   │       └── v1/auth/cert-login/route.ts   # 구 경로 호환
│   └── components/
│       ├── layout/Header.tsx            # pathname 변경 시 세션 재평가
│       └── chatbot/ChatbotWidget.tsx    # 챗봇 위젯
├── infra/
└── docs/
```

---

## deposit-service 주요 기능

### 상품 관리

- 예금·적금·청약·입출금 상품 조회 (유형별 필터링, 판매 상태 기준)
- 상품별 기본금리·우대금리·가입기간·가입금액 조건 관리
- 가입 대상 그룹(연령·직군 등) 연동 — `GET /products` 응답에 `targetGroups[]` 포함 (`targetGroupId`, `targetGroupName`, `minAge`, `maxAge`)
- 상품 `savingType` 필드(`REGULAR` / `FREE`)로 정기적금·자유적금 탭 분류
- 상품 slug는 항상 `product-{productId}` 패턴으로 생성 — 상품명 하드코딩 맵(`PRODUCT_NAME_TO_SLUG`) 제거, DB 상품명 변경 시 조용한 오동작 방지
- `getDepositSlugByProductId(productId)` — `productName` 파라미터 제거됨. 상세 페이지 라우팅이 필요한 경우 `productId`를 직접 쿼리 파라미터로 전달할 것

### 계약 관리

- 계약 생성·조회·상태변경·해지·만기 처리
- 가입금액 검증: `minJoinAmount` 이상, `maxJoinAmount` 이하
- 계좌 자동 생성, 우대금리 이력, 특약 동의 관리

### 계좌 관리

- 계좌 목록 조회 (고객 ID 기준, CLOSED 계좌 제외)
- 계좌 유형별 분류: 예금 / 적금 / 청약 / 입출금
- `isWithdrawable` 플래그 기반 출금 가능 계좌 구분 (값이 없는 경우 계좌명으로 fallback)

### 거래 관리

- 입금·출금·이체 거래 원장(Transaction) 기록
- 당행이체(`INTERNAL`): 수취 계좌 실존 여부 및 계좌번호 일치 검증
- 타행이체(`EXTERNAL`): 상대 은행 코드·이름·예금주명 기록
- 이체 실행 지점 단일화 — result 페이지에서만 호출, 중복 실행 방지

### 이체 멱등성 (idempotency key)

동일한 요청이 네트워크 재시도 등으로 중복 실행되는 것을 방지한다.

- deposit-api가 `idempotency_key`(UUID v4)를 생성해 이체 요청에 포함
- `IdempotentTransactionSaver`가 `REQUIRES_NEW` 트랜잭션으로 키 유일성 보장
  - 이미 처리된 키: 기존 Transaction 반환 (중복 실행 없음)
  - 신규 키: 정상 저장 후 반환
- DB: `deposit_transactions.idempotency_key` 컬럼 + 부분 유니크 인덱스 (NULL 제외)

```sql
-- V13 마이그레이션 (자동 적용)
ALTER TABLE deposit_transactions ADD COLUMN idempotency_key VARCHAR(64) NULL;
CREATE UNIQUE INDEX uq_deposit_transactions_idempotency_key
  ON deposit_transactions (idempotency_key) WHERE idempotency_key IS NOT NULL;
```

> **주의**: deposit-service가 V13 마이그레이션 적용 전에 기동 중이었다면 컬럼이 없어 이체·거래 조회가 500으로 실패한다. 위 SQL을 직접 실행하거나 서비스를 재기동한다.

### 이체 일일 한도 검증

이체 요청마다 당일(KST 00:00~23:59) 누적 금액·횟수를 검증한다.

- 초과 시: `DAILY_TRANSFER_AMOUNT_EXCEEDED` / `DAILY_TRANSFER_COUNT_EXCEEDED` 에러 코드 반환
- 한도 미설정 시 검증 건너뜀 (`null` 허용)
- KST 기준 자정 리셋 (`ZoneId.of("Asia/Seoul")`)

### 상품 추천 (CashflowBasedRecommendService)

고객의 최근 거래내역 현금흐름을 분석해 예금·적금 상품을 최대 5개 추천한다. 외부 AI/LLM 호출 없이 100점 채점 모델로 동작한다.

#### 추천 흐름

```
순현금흐름(netCashFlow) / periodMonth → monthlySavings
→ 필터링 → 100점 채점 → 총점 내림차순 상위 5개 반환
```

#### 필터링 규칙 (점수 산정 전 제외)

| 조건 | 제외 상품 |
|---|---|
| `ProductType.SUBSCRIPTION` | 청약 상품 전체 (사용자 요청 시) |
| 상품명·설명에 "군인", "장병", "군무원", "사병", "병사" 포함 | 특수 대상 상품 |
| **1순위** 상품 `targetGroups[].maxAge < 고객 만 나이` | 나이 상한 초과 (DB 기준) |
| **2순위** `targetGroups` 데이터 없을 때 "청년도약", "청년우대" 등 키워드 포함 + 고객 나이 > 34 | 청년 전용 상품 (키워드 fallback) |
| DEPOSIT — `currentBalance` < minJoinAmount | 잔액 부족 |
| DEPOSIT — `currentBalance` > maxJoinAmount | 한도 초과 |
| SAVINGS — `monthlySavings` < minJoinAmount | 월 저축 여력 부족 |

나이 조건 판단 우선순위:
- **1순위**: `deposit_target_groups.min_age` / `max_age` 컬럼 기준 (DB). 나이 제한이 있는 그룹 중 고객이 하나라도 충족하면 통과.
- **2순위**: `targetGroups` 데이터가 없거나 나이 필드가 null이면 상품명·설명의 "청년" 키워드 보조 필터링.
- **3순위**: 고객 생년월일 조회 실패 시 키워드 보조 필터링만 적용.

#### 100점 채점 모델

| 항목 | 배점 | 산정 방식 |
|---|---|---|
| 재정 적합도 | 40점 | 예금: `currentBalance / minJoinAmount` 비율 평가. 적금(자유적금 포함): `monthlySavings / (minJoinAmount×2)` 비율 평가. 저축 성장형(연간 저축 가능액 > 현재 잔액) 적금에 ×1.30 가중치 부여 |
| 예상 수익 | 30점 | 실제 이자액 계산 후 후보 풀 내 상대 정규화(1위=30점). 예금: `잔액×금리×기간/12`, 적금(자유적금 포함): `monthlySavings × Σ가중개월 × 금리/12` |
| 유동성 매칭 | 20점 | 거래 빈도와 상품 만기를 기준으로 고객의 자금 고정 가능성을 평가합니다. 입출금 거래가 적은 고객은 자금이 일정 기간 묶이는 중장기 상품에 더 높은 점수를 부여하고, 거래가 많은 고객은 짧은 만기 또는 유동성이 높은 상품에 상대적으로 높은 점수를 부여합니다. 조기해지 허용 시 +2점 |
| 부가 혜택 | 10점 | 비과세 혜택 +6점, 중도해지 가능 +4점 |

#### 월 잉여자금 부족(monthlySavings ≤ 0) 예외 처리

월 잉여자금이 0 이하인 경우 신규 저축 여력이 부족한 것으로 판단하여 현재 보유 잔액을 기준으로 예금 중심 fallback 추천을 제공합니다. 적금 상품은 추천 후보에서 제외되며, 응답의 `fallbackReason` 필드에 사유 문구가 포함됩니다.

#### bestRate 계산

```
bestRate = BASE 금리 최댓값 + PREFERENTIAL 금리 전체 합산
```

> 기간별 BASE 구간이 여럿이면 최댓값만 취하고, 우대금리는 조건 무관 전부 합산한다.

```
GET /api/products/recommend-agent?customerId={customerId}&periodMonth={periodMonth}
X-Customer-Id: {customerId}
```

---

## 계좌이체 전체 흐름

### 개인뱅킹 이체 (웹 → deposit-api → deposit-service)

| 단계 | 프런트 라우트 | 역할 |
|---|---|---|
| STEP 1. 이체정보 입력 | `transfer/account/page.tsx` | 출금 계좌 선택, 당행/타행 탭 전환, 입금 계좌·금액 입력 |
| STEP 2. 이체정보 확인 | `transfer/confirm/page.tsx` | 금융인증서 PIN 확인 후 result로 이동 |
| STEP 3. 이체결과 | `transfer/result/page.tsx` | `executeDepositTransfer()` → 실제 이체 실행 |

#### deposit-api 이체 요청 파라미터

```
POST /api/transactions/transfer
X-Customer-Id: {customerId}
```

| 파라미터 | 설명 |
|---|---|
| `fromAccountId` | 출금 계좌 ID |
| `toAccountId` | 당행이체 시 입금 계좌 ID (미전달 시 `toAccountNo`로 자동 조회) |
| `toAccountNo` | 입금 계좌번호 |
| `amount` | 이체금액 |
| `transferType` | `INTERNAL`(당행) 또는 `EXTERNAL`(타행) |
| `counterpartyBankCode` | 타행 은행 코드 |
| `counterpartyBankName` | 타행 은행명 |
| `counterpartyName` | 수취인 성명 |
| `channelType` | 항상 `INTERNET` |

#### deposit-api 소유주 검증 (보안)

- `X-Customer-Id` 헤더 값과 출금 계좌의 `customer_id` 불일치 시 `403 Forbidden` 반환
- `counterparty_account_id`를 이체 기록에 저장해 거래 추적 가능

### 챗봇 이체 (ChatbotWidget → consultation-service → deposit-api)

```
ChatbotWidget (웹)
  └─ POST /chatbot/transfer (consultation-service:8087)
       └─ POST /api/transactions/transfer (deposit-api:8082)
            └─ deposit-service TransactionService
```

- 비로그인 상태에서 이체 버튼 클릭 시: 로그인 안내 → 로그인 완료 시 이체 재시작
- 챗봇 위젯 열기(`onOpen`) 시 `/api/v1/customers/me` 서버 토큰 검증 → 만료·없음 시 localStorage 초기화 후 로그인 유도

---

## 예금 상품 가입 흐름

| 단계 | 라우트 | 설명 |
|---|---|---|
| 상품 목록 | `/products/deposit/list` | 탭별 상품 목록, 가입하기 버튼 |
| 상품 상세 | `/products/deposit/[id]` | 상품 안내·금리·약관 상세 |
| 가입 | `/products/deposit/join/[id]` | 약관동의 → 정보입력 → 정보확인 3단계 |

지원 상품 유형: `예금` / `정기적금` / `자유적금` / `입출금자유` / `주택청약`

| 유형 | 약관 | 가입기간 | 이자지급방법 |
|---|---|---|---|
| 예금 | 거치식예금약관 포함 | O | O |
| 적금 | 적립식예금약관 포함 | O | O |
| 입출금자유 | 보통예금약관 포함 | X | X |
| 주택청약 | 주택청약종합저축약관 | O | O |

---

## 계좌 해지 흐름

| 단계 | 설명 |
|---|---|
| 계좌 선택 | 계좌조회 페이지의 각 계좌 카드에서 **해지** 버튼 클릭 → `accountId` 쿼리 파라미터와 함께 해지 페이지로 이동 |
| 해지 확인 | `accountId`가 전달되면 계좌 선택 단계를 건너뛰고 2단계(해지계좌 확인)로 자동 진입 |
| 비밀번호 입력 | 마우스 숫자패드 또는 키보드 입력 |
| 지급 방법 선택 | **당행 계좌**(내 입출금 계좌 중 선택) / **타행 계좌**(은행 선택 + 계좌번호 직접 입력) / **현금** |
| 완료 | 해지 API 호출 → 잔액 `accountOverrides` localStorage에 반영 |

```
GET /products/deposit/inquiry/terminate?accountId={accountId}
```

---

## 거래조회 개선

- 계좌 목록 로드 후 각 계좌별로 거래내역을 병렬 조회(`Promise.all`) → 전체 거래를 한 번에 로드
- 거래 필터링 기준: `accountNumber` 일치 **또는** `accountId` 일치 — 계좌번호가 없는 거래도 누락 없이 표시
- 선택 계좌·캘린더 계좌 초기값을 첫 번째 계좌로 자동 설정

---

## 인증 (customer-service)

### 로그인 방식

| 방식 | 엔드포인트 | 설명 |
|---|---|---|
| ID/비밀번호 | `POST /api/v1/auth/login` | 일반 로그인 |
| 금융인증서 (FinCert) | `POST /api/v1/auth/cert-login` | 인증서 PIN 기반 로그인 |

### 보안카드 에러코드 (CUST_140~144)

| 코드 | HTTP | 설명 |
|---|---|---|
| `CUST_140` | 404 | 활성 보안카드 없음 |
| `CUST_141` | 409 | 이미 활성 보안카드 존재 |
| `CUST_142` | 401 | 보안카드 코드 불일치 |
| `CUST_143` | 410 | 보안카드 챌린지 만료·없음 |
| `CUST_144` | 400 | 챌린지에 없는 위치 코드 포함 |

### 금융인증서 로그인 SSR 프록시

브라우저에서 `customer-service:8081`을 직접 호출하면 CORS 오류가 발생하므로, Next.js 서버에서 프록시한다.

```
웹 브라우저 → POST /api/customer/cert-login (Next.js SSR)
               └─ POST http://localhost:8081/api/v1/auth/cert-login (customer-service)
```

- 파일: `web/app/api/customer/cert-login/route.ts`
- 8초 AbortController 타임아웃, customer-service 미기동 시 503 반환
- PIN 패드: 고정 순서 (`1 2 3 … 0`), 키보드 입력 지원

### 세션 저장

로그인 성공 시 `sessionStorage`에 저장:

| 키 | 값 |
|---|---|
| `accessToken` | JWT 액세스 토큰 |
| `customerId` | 고객 ID |
| `user` | `{ name, customerId }` JSON |

`persistLoginState()` 헬퍼(`login/page.tsx`)가 ID·인증서 두 경로 모두에서 동일 항목을 저장한다.

### 로그아웃

`logout/page.tsx`에서 `sessionStorage.clear()` + `setUser(null)` 실행 → 헤더 즉시 비로그인 상태 전환.  
`Header.tsx`는 `useEffect([pathname])`으로 경로 변경마다 세션 재평가, `/logout` 진입 시 즉시 `setUser(null)`.

---

## consultation-service 주요 기능

### 챗봇 상담

- Python · FastAPI 기반 시나리오형 챗봇
- 시나리오 노드·버튼·플로우 DB 구조, 멀티턴 대화 상태 관리
- 키워드 기반 Intent 분류 → 기능 코드 실행 라우팅

#### 챗봇 상품 추천 — 백엔드 단일 채점

`CASH_FLOW_RECOMMEND` 기능 실행 시 채점은 **consultation-service 백엔드(`_rank_products()`)에서만** 수행한다. 프런트엔드(`ChatbotWidget.tsx`)는 백엔드 결과를 표시하는 역할에 집중하며, 브라우저에서 거래 데이터를 직접 집계하거나 채점하지 않는다.

| 항목 | 배점 | 계산 기준 |
|---|---|---|
| 재정 적합도 | 40점 | DB에서 직접 집계한 잔액·월 잉여금 기준. 저축 성장형 진단 시 적금에 1.3× 가중치 |
| 예상 수익 (ROI) | 30점 | 금리×기간 기반 세전 이자 상대 평가, 후보 풀 내 정규화 |
| 유동성 매칭 | 20점 | 거래 빈도 vs 상품 만기 적합도 |
| 부가 혜택 | 10점 | 비과세·중도해지·우대금리 여부 합산 |

**예금·적금 적합 판단 (`answerDepositSavingsFit`)**

"예금이랑 적금 중 나한테 맞는 거" 질문 시 deposit-service 추천 에이전트(`fetchDepositRecommendAgent`)를 호출해 판단한다. `GET /api/v1/customers/me`의 `birthDate`로 고객 만 나이를 계산해 백엔드에 전달하며, 연령 기반 상품 필터링은 deposit-service에서 처리된다.

**fallback 추천 (monthlySavings ≤ 0)**

월 잉여금이 0 이하인 경우 점수 기반 추천 대신 현재 보유 잔액 기준 예금 중심 fallback 추천을 제공하며, 응답의 `fallbackReason` 필드에 사유 문구가 포함된다.

| 카테고리 | 기능 코드 | 설명 |
|---|---|---|
| PRODUCT_ADVICE | PRODUCT_GUIDE | 예금·적금·청약 상품 안내 (인증 필요) |
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

**인증 필요 기능 코드**: `MY_ACCOUNTS`, `MY_PRODUCTS`, `CONTRACT_STATUS`, `MATURITY_SCHEDULE`, `INTEREST_HISTORY`, `MY_CASH_FLOW`, `CASH_FLOW_RECOMMEND`, `SAVINGS_GOAL`, `PRODUCT_GUIDE`

비로그인 상태에서 위 기능 실행 시 로그인 안내 메시지를 표시하고 로그인 완료 후 재실행한다.

### 상품 추천 채점 — 백엔드 단일화 원칙

`CASH_FLOW_RECOMMEND` 실행 시 채점 로직은 **consultation-service 백엔드(`_rank_products()`)에서만** 수행한다.

- 채점 모델: 재정 적합도 40점·ROI 30점·유동성 20점·부가혜택 10점 (100점 만점)
- DB에서 직접 집계한 거래 데이터 기반으로 점수를 계산해 프론트에 최종 순위만 반환
- 프론트(ChatbotWidget)는 백엔드 결과를 **표시**하는 역할에 집중하며, 브라우저에서 거래 데이터를 재집계하거나 직접 채점하지 않음
- 채점 기준 변경은 백엔드 한 곳에서만 관리 → 프론트·백 결과 불일치 방지

### 챗봇 토큰 검증

챗봇 위젯 열기(`onOpen`) 시 `/api/v1/customers/me`로 토큰 유효성을 검증한다.

- 유효: 정상 진입
- 만료/없음: localStorage의 `accessToken`, `access_token`, `customerId`, `user` 일괄 초기화 → 로그인 유도

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

Kafka 활성화: `.env`에서 `CONSULTATION_KAFKA_ENABLED=true` 설정

### 주요 API

```
GET  /health
GET  /chatbot/features
POST /chatbot/features/{feature_code}/execute
POST /chatbot/consultations/start
POST /chatbot/consultations/{id}/messages
POST /chatbot/transfer
GET  /chat/queue
POST /chat/consultations/{id}/connect
POST /chat/consultations/{id}/messages
GET  /chat/consultations/{id}/messages
POST /chat/consultations/{id}/end
GET  /metrics
```

---

## loan-service 주요 기능

### 대출 심사 흐름

```
신청 → 가심사 → DSR 계산 → 신용평가 → 신용점수 → 본심사
→ 담보/LTV → 보증인/보증보험 → 서류 → 전자서명 → 본인확인 → 약정실행
```

### 대출 관리 흐름

```
계약 → 상환 / 부분상환 / 선납 → 연체 → 금리변경 → 만기 → 해지
```

### Agentic RAG (유사사례 검색)

| 컴포넌트 | 역할 |
|---|---|
| `SimilarCaseExporter` | 심사 완료 건을 청크로 변환, 임베딩·저장 |
| `ai-service` | pgvector 기반 유사도 검색 |

### 자동심사·편향검증

`auto-loan-review` 서비스가 규칙 기반 + LLM 심사 의견을 생성하고, 편향검증 에이전트가 성별·나이·지역 편향 여부를 검사한다. 임계치 초과 시 4-eye 승인 프로세스로 에스컬레이션한다.

---

## 로컬 실행

### 사전 조건

- Java 17+, Docker Desktop, Python 3.11, Node.js 20+

### 인프라 실행

```powershell
docker compose up -d
```

컨테이너: PostgreSQL×6 (5432~5437), pgvector, Kafka (9092), Schema Registry (8081), Redis (6379), Prometheus (9090), Grafana (3000), Gateway (8080)

### 백엔드 서비스 실행

```powershell
.\gradlew :services:customer-service:bootRun   # 8081
.\gradlew :services:deposit-service:bootRun    # 8082
.\gradlew :services:loan-service:bootRun       # 8083
.\gradlew :services:master-service:bootRun     # 8085
.\gradlew :services:ai-service:bootRun         # 8086
```

### deposit-api 실행

```powershell
cd services/deposit-api
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8082
```

### consultation-service 실행

```powershell
# Docker 권장
cd services/consultation-service
docker compose up -d

# 직접 실행
.\.venv\Scripts\python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 8087
```

> consultation-service 독립 docker-compose는 PostgreSQL을 포트 `5439`에 노출한다. 메인 compose와 포트 충돌 없음.

### customer-service 환경변수

```powershell
$env:CRYPTO_KEY_BASE64="bG9hbi1zZXJ2aWNlLWRldi1hZXMta2V5LTMyYnl0ZXM="
.\gradlew :services:customer-service:bootRun
```

> 운영 환경에서는 반드시 별도 32바이트 AES-256 키(Base64)를 사용한다.

### 프런트엔드 실행

```powershell
cd web
npm install
npm run dev   # http://localhost:3001
```

`.env.local` 예시:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_DEPOSIT_API_URL=http://localhost:8082/api
NEXT_PUBLIC_CONSULTATION_API_URL=http://localhost:8087
NEXT_PUBLIC_LOAN_API_URL=http://localhost:8083
NEXT_PUBLIC_ADVISORY_API_URL=http://localhost:8084
NEXT_PUBLIC_MASTER_API_URL=http://localhost:8085
NEXT_PUBLIC_AI_API_URL=http://localhost:8086
NEXT_PUBLIC_PAYMENT_API_URL=http://localhost:8087
CUSTOMER_API_URL=http://localhost:8081
```

`CUSTOMER_API_URL`은 SSR 전용 변수 (서버 → customer-service 직접 프록시용, `NEXT_PUBLIC_` 없음).

---

## DB 마이그레이션 현황 (deposit-service)

Flyway로 스키마를 관리하며 서비스 기동 시 자동 적용된다.

| 버전 | 내용 |
|---|---|
| V1~V3 | 초기 스키마 (상품·계약·계좌·거래 테이블) |
| V4 | 공통 약관 연결 테이블 |
| V5~V8 | 마스터 코드·그룹·시드 데이터 |
| V12 | 이체 일일 한도 컬럼 추가 |
| V13 | `deposit_transactions.idempotency_key` 컬럼 + 부분 유니크 인덱스 |
| V14 | `deposit_target_groups.min_age` / `max_age` 컬럼 추가. 청년고객 19~34세, 국군장병 18~27세로 초기값 설정 |
| V15 | employee01 테스트 계좌 시드 데이터 추가 |

> 서비스 재기동 없이 컬럼이 추가된 경우 Hibernate가 SELECT 시 해당 컬럼을 포함해 500 오류가 발생한다. DB에 직접 DDL을 실행하거나 서비스를 재기동한다.

---

## 테스트 실행

```powershell
# deposit-service
.\gradlew :services:deposit-service:test

# loan-service
.\gradlew :services:loan-service:test

# consultation-service (1614개)
cd services/consultation-service
python -m pytest tests/ -q
```

### deposit-service 테스트 주요 커버리지

| 테스트 클래스 | 주요 시나리오 |
|---|---|
| `TransactionServiceTest` | 이체 성공, 잔액 부족, 일일 금액 한도 초과, 일일 횟수 한도 초과, 한도 미설정 건너뜀, KST 자정 경계값 |
| `TransactionRepositoryTest` | 거래 유형별 조회, 기간별 합산 |
| `AccountServiceTest` | 계좌 목록, 출금 가능 여부 |
| `ContractServiceTest` | 계약 생성·해지·만기, 가입금액 범위 검증 |
| `ProductServiceTest` | 상품 조회·필터, 추천 에이전트 |

---

## 모니터링

| 도구 | 주소 | 설명 |
|---|---|---|
| Prometheus | `http://localhost:9090` | 메트릭 수집 |
| Grafana | `http://localhost:3000` | 대시보드 |

- loan-service 메트릭: [`docs/loan-service-metrics.md`](docs/loan-service-metrics.md)
- 챗봇 모니터링: [`docs/monitoring/CHATBOT_GUIDE.md`](docs/monitoring/CHATBOT_GUIDE.md)
- LLM/RAG 모니터링: [`docs/monitoring/LLM_RAG_MONITORING_GUIDE.md`](docs/monitoring/LLM_RAG_MONITORING_GUIDE.md)

---

## 주의사항

- `main` 브랜치 직접 커밋/푸시 금지
- `.env` 파일 커밋 금지
- 운영/스테이징 DB 접속 명령 금지
- AI 모델명·세션 링크를 커밋 메시지·PR에 삽입 금지

공통 AI 작업 가이드: [`docs/AI_GUIDELINES.md`](docs/AI_GUIDELINES.md)  
Claude Code 추가 설정: [`CLAUDE.md`](CLAUDE.md)
