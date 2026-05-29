# Deposit Service — 수신계

인터넷뱅킹 플랫폼의 **수신(예·적금·청약) 도메인 백엔드 서비스**입니다.  
상품 등록·조회, 계약 체결, 계좌 관리, 거래 기록, 이자 계산, 수신 특약, 고객 맞춤 상품 추천,  
**납입 스케줄 생성·자동이체 실행·납입 지연 감지**까지 수신 업무 전 영역을 담당합니다.

---

## 프론트엔드 연동 변경 사항

### CorsConfig 추가

Next.js 프론트엔드(`localhost:3001`)에서 deposit-service를 직접 호출할 수 있도록 CORS를 허용했습니다.

```java
// src/main/java/com/bank/deposit/config/CorsConfig.java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    }
}
```

### ProductStatus.CLOSED 추가

DB에 `CLOSED` 상태의 상품이 존재하나 Java Enum에 정의되지 않아 발생하던 `IllegalArgumentException`을 해결했습니다.

```java
// Before
public enum ProductStatus { SELLING, SUSPENDED, EXPIRED }

// After
public enum ProductStatus { SELLING, SUSPENDED, EXPIRED, CLOSED }
```

### ContractService — 해지 시 계좌 상태 동기화

계약 해지(`terminate`) 처리 시 연결된 계좌 상태도 `CLOSED`로 변경되도록 수정했습니다.  
이전에는 계약만 `TERMINATED`로 변경되고 계좌는 `ACTIVE`로 남아 불일치가 발생했습니다.

---

## 2026-05-28 수정 파일 상세

이번 변경은 정기적금 자동이체와 수동 납입 연체 대응을 백엔드에 추가하고, 고객 화면에서 예금·적금·입출금 상품 가입, 해지, 잔액 반영 흐름을 보완한 작업입니다.

### 백엔드: 납입 스케줄·자동이체·연체

| 파일 | 변경 내용 |
|------|-----------|
| `src/main/java/com/bank/deposit/DepositServiceApplication.java` | `@EnableScheduling`을 추가해 납입 스케줄러가 애플리케이션 기동 시 활성화되도록 변경 |
| `src/main/resources/db/migration/V11__payment_schedules.sql` | `deposit_payment_schedules` 테이블 생성, 스케줄 조회 인덱스 추가, `contracts` 테이블에 `consecutive_miss_count`, `source_account_id` 컬럼 추가 |
| `src/main/java/com/bank/deposit/domain/enums/PaymentStatus.java` | 납입 상태 `PENDING`, `PAID`, `OVERDUE`, `FAILED`, `SUSPENDED` 정의 |
| `src/main/java/com/bank/deposit/domain/entity/PaymentSchedule.java` | 회차별 납입 예정일, 금액, 자동이체 여부, 출금 계좌, 납입 상태, 실제 납입 금액, 실패 사유를 저장하는 엔티티 추가 |
| `src/main/java/com/bank/deposit/repository/PaymentScheduleRepository.java` | 계약별 스케줄 조회, 상태별 조회, 자동이체 도래 대상 조회, 수동 납입 연체 대상 조회 쿼리 추가 |
| `src/main/java/com/bank/deposit/service/PaymentScheduleService.java` | 정기적금 계약 기간만큼 월별 납입 스케줄을 일괄 생성하고, 월말 보정 및 수동 납입 위임 처리 추가 |
| `src/main/java/com/bank/deposit/controller/PaymentScheduleController.java` | 납입 스케줄 생성, 계약별 조회, 상태별 조회, 고객 직접 납입 API 추가 |
| `src/main/java/com/bank/deposit/service/AutoTransferService.java` | 자동이체 실행, 실패 처리, 수동 납입 연체 처리, 고객 직접 납입, 3회 연속 실패 시 계약 정지, 납입 성공 시 미납 카운트 초기화 구현 |
| `src/main/java/com/bank/deposit/scheduler/PaymentSchedulerJob.java` | 매일 09:00에 자동이체 도래 스케줄과 수동 납입 지연 스케줄을 분리 처리하는 스케줄러 추가 |
| `src/main/java/com/bank/deposit/domain/entity/Contract.java` | 자동이체 출금 계좌 ID와 연속 미납/실패 횟수 필드 추가, 미납 카운트 증가·초기화, 3회 실패 계약 정지 메서드 추가 |
| `src/main/java/com/bank/deposit/dto/request/ContractCreateRequest.java` | 계약 생성 요청에 `sourceAccountId` 추가 |
| `src/main/java/com/bank/deposit/controller/ContractController.java` | 계약 생성 시 `sourceAccountId`를 서비스 계층으로 전달 |
| `src/main/java/com/bank/deposit/service/ContractService.java` | 계약 생성 시 자동이체 출금 계좌 ID를 `Contract`에 저장 |

### 백엔드: 테스트

| 파일 | 변경 내용 |
|------|-----------|
| `src/test/java/com/bank/deposit/service/AutoTransferServiceTest.java` | 자동이체 성공, 잔액 부족, 계좌 비활성, 출금 계좌 미설정, 3회 연속 실패 정지, 수동 납입 연체, 연체 납입 복구, 중복 납입 차단 테스트 추가 |
| `src/test/java/com/bank/deposit/controller/ContractControllerTest.java` | `sourceAccountId` 파라미터 추가에 맞춰 계약 생성 mock 검증 보정 |
| `src/test/java/com/bank/deposit/service/ContractServiceTest.java` | `ContractService.createContract` 시그니처 변경에 맞춰 테스트 인자 보정 |

### 프론트엔드: 예금·적금·입출금 상품 가입

| 파일 | 변경 내용 |
|------|-----------|
| `web/app/(personal)/products/deposit/list/page.tsx` | 가입 가능한 상품에 `canApply` 표시 추가, 정기적금 상품 3종(`AXful 직장인우대적금`, `AXful 꿈적금`, `AXful 함께적금`) 추가, 입출금/청약 상품 가입 버튼 노출 보강 |
| `web/app/(personal)/products/deposit/join/[id]/page.tsx` | 신규 정기적금·입출금 상품 ID 및 상품명 매핑 추가, 상품별 금리 표시 추가, 입출금 상품 가입 흐름 분리, 정기적금 자동이체 신청/미신청·이체일·출금계좌 입력 추가 |
| `web/app/(personal)/products/deposit/join/[id]/page.tsx` | 장병내일준비적금 전용 군종·군번·입대일·전역예정일 입력 및 검증 추가, 가입 확인 화면에 군 정보·자동이체 정보·상품별 금리 반영 |

### 프론트엔드: 계좌 조회·해지·챗봇

| 파일 | 변경 내용 |
|------|-----------|
| `web/app/(personal)/inquiry/accounts/page.tsx` | `accountOverrides`를 읽어 해지금 입금 등으로 변경된 mock 계좌 잔액을 계좌조회 화면에 반영 |
| `web/app/(personal)/products/deposit/inquiry/terminate/page.tsx` | 예금·적금 해지 시 해지 계좌를 `joinedAccounts`에서 제거하고, 해지금액을 선택한 입금 계좌 잔액에 합산 |
| `web/app/(personal)/products/deposit/inquiry/terminate/page.tsx` | 입금 계좌가 기본 mock 계좌인 경우 `accountOverrides`에 누적 금액을 저장해 계좌조회/챗봇에서 동일하게 보이도록 처리 |
| `web/components/chatbot/ChatbotWidget.tsx` | 챗봇 내 상품 조회 시 `accountOverrides`를 반영한 잔액 표시 추가 |
| `web/components/chatbot/ChatbotWidget.tsx` | 챗봇 해지 처리에서도 해지 계좌 제거, 입금 계좌 잔액 합산, mock 계좌 override 저장을 동일하게 적용 |

### 커밋 제외 대상

| 파일 | 사유 |
|------|------|
| `services/consultation-service/.env.bak2` | 환경 변수 백업 파일로 민감 정보 포함 가능성이 있어 커밋 제외 |
| `web/components/chatbot/ChatbotWidget.tsx.bak2` | 임시 백업 파일로 실제 소스 변경은 `ChatbotWidget.tsx`에 반영되어 커밋 제외 |

---

## 기술 스택

| 항목 | 내용 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Build | Gradle (멀티 모듈) |
| DB (운영) | PostgreSQL 16 |
| DB (로컬) | H2 In-Memory |
| 마이그레이션 | Flyway |
| ORM | Spring Data JPA / Hibernate |
| 스케줄러 | Spring `@Scheduled` (`@EnableScheduling`) |
| 문서 | Springdoc OpenAPI (Swagger UI) |

---

## 주요 기능

### 1. 수신 상품 관리
- 정기예금(DEPOSIT) · 입출금자유(DEMAND) · 적금(SAVINGS) · 청약(SUBSCRIPTION) 4종 상품 CRUD
- 상품 상태 관리 (`SELLING` / `SUSPENDED` / `CLOSED`)
- 가입 채널 · 대상 그룹 · 금리 조건 연결
- 고객 대면 시드 데이터 21개 상품 기 등록
  - 정기예금 4종, 입출금자유통장 10종, 적금 5종, 청약 2종

### 2. 계약 관리
- 상품별 계약 체결 / 조회 / 해지
- 계약 상태 이력 자동 기록 (`StatusHistory`)
- 계약 가입금액 범위 검증, 기간(periodMonth) 최솟값 제약
- 자동이체 출금 계좌(`sourceAccountId`) 및 연속 미납 횟수(`consecutiveMissCount`) 관리

### 3. 계좌 관리
- 계약과 1:1 연결된 수신 계좌 생성 및 상태 관리
- 계좌 상태: `ACTIVE` / `FROZEN` / `CLOSED`

### 4. 거래 기록
- 입금(IN) / 출금(OUT) 거래 기록
- 거래 상태(`SUCCESS` / `FAILED`) 및 실패 사유 코드 관리

### 5. 이자 계산 및 지급 이력
- 기본금리 · 우대금리 관리 (`BASE` / `PREFERENTIAL`)
- 이자 지급 이력 기록

### 6. 수신 특약 관리
- 특약 등록 및 상품·계약별 특약 동의 연결
- 공통 약관 참조 연동

### 7. 현금흐름 기반 상품 추천
- 고객 거래 이력을 분석해 최적 수신 상품 자동 추천
- `GET /products/recommend-agent?customerId={id}&periodMonth={n}` 으로 호출
- `X-Customer-Id` 헤더와 요청 `customerId`가 일치해야 조회 가능
- 분석 기간(periodMonth) 동안의 순입금액 기준 → 금리 최고 상품 매칭

### 8. 납입 스케줄 관리 (신규)
정기적금 계약 체결 시 회차별 납입 스케줄을 자동 생성하고, 매일 스케줄러가 납입 상태를 갱신합니다.

#### 납입 스케줄 생성
- 계약 체결 후 `POST /payment-schedules/contracts/{contractId}/generate` 호출
- 계약 기간(월) 만큼의 스케줄 행이 일괄 생성됨 (`paymentRound 1 ~ N`)
- 자동이체 신청 여부에 따라 `isAutoTransfer`가 구분됨

#### 자동이체 실행
- 매일 오전 9시 `PaymentSchedulerJob`이 실행
- 당일 포함 예정일 경과 + `PENDING` 상태인 자동이체 스케줄을 일괄 처리
- 출금 계좌 → 적금 계좌 내부 이체 후 거래 내역 2건 생성 (출금 `TRF`, 납입 `SAV`)
- 성공 시: `PAID` 처리, 연속 실패 횟수 초기화

#### 자동이체 실패 처리
| 연속 실패 횟수 | 처리 |
|---|---|
| 1~2회 | `FAILED` 상태, 계약은 `ACTIVE` 유지 |
| 3회 | `SUSPENDED` 상태, 계약 `SUSPENDED` + `autoTransferEnabled = false` |

| 실패 사유 | `FailureReasonCode` |
|---|---|
| 잔액 부족 | `INSUFFICIENT_BALANCE` |
| 계좌 비활성 / 미설정 | `INVALID_ACCOUNT` |
| 기타 예외 | `SYSTEM_ERROR` |

#### 수동 납입 지연 감지
- 자동이체 미신청 고객의 스케줄 중 예정일이 경과한 항목을 `OVERDUE`로 자동 변경
- 동일하게 연속 미납 횟수가 누적되며 3회 초과 시 계약 `SUSPENDED`

#### 수동 납입 처리 (고객 직접 납입)
- `POST /payment-schedules/{scheduleId}/pay?sourceAccountId={id}`
- `PENDING` 또는 `OVERDUE` 스케줄에 대해 실행 가능
- 납입 성공 시 연속 미납 횟수 초기화, `SUSPENDED` 상태였던 계약은 `ACTIVE`로 자동 복구

#### 납입 상태 흐름

```
PENDING ──(예정일 도래, 자동이체 성공)──▶ PAID
PENDING ──(예정일 도래, 자동이체 실패)──▶ FAILED
PENDING ──(예정일 경과, 수동 미납)    ──▶ OVERDUE
FAILED / OVERDUE ──(연속 3회)         ──▶ SUSPENDED
OVERDUE ──(고객 직접 납입)            ──▶ PAID
```

---

## API 엔드포인트 요약

### 상품 / 계약 / 계좌 / 거래

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/products` | 상품 목록 조회 (type·status 필터) |
| `POST` | `/products` | 상품 등록 |
| `GET` | `/products/{id}` | 상품 단건 조회 |
| `PUT` | `/products/{id}` | 상품 정보 수정 |
| `PATCH` | `/products/{id}` | 상품 상태 변경 |
| `GET` | `/products/{id}/deposit` | 예금 상세 조회 |
| `POST` | `/products/{id}/deposit` | 예금 상세 등록 |
| `GET` | `/products/recommend-agent` | 고객 맞춤 상품 추천 |
| `POST` | `/contracts` | 계약 체결 (`sourceAccountId` 포함) |
| `GET` | `/contracts` | 계약 목록 조회 |
| `GET` | `/contracts/{id}` | 계약 단건 조회 |
| `PATCH` | `/contracts/{id}/terminate` | 계약 해지 |
| `PATCH` | `/contracts/{id}/maturity` | 계약 만기 처리 |
| `PATCH` | `/contracts/{id}/auto-transfer-day` | 자동이체일 변경 |
| `POST` | `/accounts` | 계좌 생성 |
| `GET` | `/accounts/{id}` | 계좌 조회 |
| `POST` | `/transactions/deposit` | 입금 |
| `POST` | `/transactions/withdraw` | 출금 |
| `POST` | `/transactions/transfer` | 이체 |
| `GET` | `/transactions` | 거래 내역 조회 |
| `GET` | `/interests` | 이자 이력 조회 |
| `GET` | `/special-terms` | 특약 목록 조회 |
| `GET` | `/join-targets` | 가입 대상 그룹 조회 |
| `GET` | `/health` | 헬스 체크 |

### 납입 스케줄 (신규)

| Method | Path | 설명 |
|--------|------|------|
| `POST` | `/payment-schedules/contracts/{contractId}/generate` | 납입 스케줄 일괄 생성 |
| `GET` | `/payment-schedules/contracts/{contractId}` | 계약별 전체 스케줄 조회 |
| `GET` | `/payment-schedules/contracts/{contractId}/status/{status}` | 상태별 스케줄 조회 |
| `POST` | `/payment-schedules/{scheduleId}/pay` | 수동 납입 처리 |

> Swagger UI: `http://localhost:8082/swagger-ui.html`

#### `/payment-schedules/contracts/{contractId}/generate` 쿼리 파라미터

| 파라미터 | 필수 | 설명 |
|---|---|---|
| `accountId` | Y | 납입 대상 적금 계좌 ID |
| `contractPeriodMonth` | Y | 계약 기간 (개월 수) |
| `monthlyAmount` | Y | 월 납입 금액 |
| `isAutoTransfer` | N (기본 false) | 자동이체 여부 |
| `sourceAccountId` | N | 자동이체 출금 계좌 ID |
| `autoTransferDay` | N | 자동이체일 (1~31, 미지정 시 계약 시작일 기준) |
| `startedAt` | Y | 계약 시작일 (`yyyy-MM-dd`) |

---

## DB 마이그레이션 (Flyway)

| 버전 | 파일 | 내용 |
|------|------|------|
| V1 | `V1__initial_schema.sql` | 초기 스키마 |
| V2 | `V2__seed_postman_data.sql` | no-op |
| V3 | `V3__add_product_indexes.sql` | 상품 조회 인덱스 |
| V4 | `V4__add_product_rate_constraints.sql` | 상품 금리 제약 |
| V5 | `V5__full_erd_schema.sql` | 전체 ERD 반영 스키마 |
| V6 | `V6__term_application_management.sql` | 약관 신청 관리 테이블 |
| V7 | `V7__seed_regular_savings.sql` | no-op |
| V8 | `V8__seed_customer_frontend_products.sql` | no-op |
| V9 | `V9__add_account_version_column.sql` | 계좌 낙관락 버전 컬럼 |
| V10 | `V10__account_dates_and_number_sequence.sql` | 계좌/계약 날짜 DATE 변환, 계좌번호 sequence |
| V11 | `V11__payment_schedules.sql` | 납입 스케줄 테이블 생성, 계약에 연속 미납/출금 계좌 컬럼 추가 |

`deposit_payment_schedules` 테이블 주요 컬럼:

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `schedule_id` | BIGSERIAL PK | 스케줄 ID |
| `contract_id` | BIGINT FK | 연결 계약 |
| `account_id` | BIGINT FK | 납입 대상 적금 계좌 |
| `payment_round` | INT | 납입 회차 (1부터 시작) |
| `scheduled_date` | DATE | 납입 예정일 |
| `scheduled_amount` | NUMERIC | 예정 납입 금액 |
| `is_auto_transfer` | BOOLEAN | 자동이체 여부 |
| `source_account_id` | BIGINT | 자동이체 출금 계좌 ID |
| `status` | VARCHAR | `PENDING` / `PAID` / `OVERDUE` / `FAILED` / `SUSPENDED` |
| `paid_at` | TIMESTAMPTZ | 실제 납입 시각 |
| `actual_amount` | NUMERIC | 실제 납입 금액 |
| `transaction_id` | BIGINT | 연결 거래 ID |
| `failure_reason_code` | VARCHAR | 실패 사유 코드 |

---

## 도메인 모델 (주요 엔티티)

```
Product (수신 상품)
├── DepositProduct          (예금 상세)
├── SavingsProduct          (적금 상세)
├── SubscriptionProduct     (청약 상세)
├── ProductInterestRate     (금리 조건)
├── ProductJoinChannel      (가입 채널)
├── ProductSpecialTerm      (상품-특약 연결)
└── ProductTargetGroup      (대상 그룹 연결)

Contract (계약)
├── consecutiveMissCount    (연속 미납 횟수)
├── sourceAccountId         (자동이체 출금 계좌)
├── Account                 (계좌)
│   └── Transaction         (거래)
├── InterestHistory         (이자 이력)
├── ContractAppliedRate     (적용 금리)
├── ContractSpecialTermAgreement (특약 동의)
└── PaymentSchedule[]       (납입 스케줄 — 회차별)

Department (부서)
TargetGroup (대상 그룹)
SpecialTerm (수신 특약)
```

---

## 스케줄러 동작

`PaymentSchedulerJob`이 매일 오전 9시 자동 실행됩니다 (`@Scheduled(cron = "0 0 9 * * *")`).

```
09:00 매일 실행
  ├─ [자동이체] scheduledDate <= 오늘 && status = PENDING && isAutoTransfer = true
  │    ├─ 잔액 충분 → 출금 계좌 차감 + 적금 계좌 적립 + PAID
  │    └─ 잔액 부족/계좌 오류 → FAILED + consecutiveMissCount++
  │         └─ count >= 3 → 계약 SUSPENDED + autoTransferEnabled = false
  │
  └─ [수동 지연] scheduledDate < 오늘 && status = PENDING && isAutoTransfer = false
       └─ OVERDUE + consecutiveMissCount++
            └─ count >= 3 → 계약 SUSPENDED
```

납입 성공 또는 고객 직접 납입 시 `consecutiveMissCount`가 0으로 초기화됩니다.

---

## 로컬 실행 방법

### 사전 조건
- Java 21+
- Gradle (래퍼 사용 가능)
- PostgreSQL 16 (운영 프로파일) 또는 별도 설정 없이 H2 (로컬 프로파일)

### H2 인메모리로 빠른 실행 (local 프로파일)

```bash
# 프로젝트 루트에서
./gradlew :services:deposit-service:bootRun --args="--spring.profiles.active=local"
```

실행 시 `LocalDataSeeder`가 초기 상품 데이터를 자동 삽입합니다.

### PostgreSQL 연결 (default 프로파일)

1. `.env.sample`을 복사해 `.env` 작성
2. `docker compose up -d db` 로 DB 컨테이너 기동
3. Flyway 마이그레이션 자동 적용 (V11까지) 후 서비스 시작

```bash
./gradlew :services:deposit-service:bootRun
```

---

## 테스트 실행

```bash
./gradlew :services:deposit-service:test
```

### 주요 테스트 대상

| 테스트 클래스 | 대상 | 케이스 수 |
|---|---|---|
| `AutoTransferServiceTest` | 자동이체·수동납입·연체 시나리오 | 17 |
| `TransactionServiceTest` | 입금·출금·이체·적금납입·취소 | 다수 |
| `ContractServiceTest` | 계약 생성·해지·금리 계산 | 다수 |
| `AccountServiceTest` | 계좌 상태·한도 변경 | 다수 |
| `TransactionRepositoryTest` | JPA 쿼리 검증 | 다수 |

### 연체 시나리오 테스트 상세 (`AutoTransferServiceTest`)

| 시나리오 | 검증 내용 |
|---|---|
| 자동이체 성공 | PAID 전환, 잔액 이동, 연속 실패 0 초기화 |
| 잔액 부족 실패 | FAILED + `INSUFFICIENT_BALANCE`, 잔액 불변 |
| 계좌 비활성 실패 | FAILED + `INVALID_ACCOUNT` |
| sourceAccountId 미설정 | 즉시 FAILED, DB 접근 없음 |
| **3회 연속 실패** | 계약 SUSPENDED, `autoTransferEnabled = false` |
| 2회 실패 후 성공 | 연속 실패 초기화, 계약 ACTIVE 유지 |
| 수동 납입 지연 1·2회 | OVERDUE, 계약 ACTIVE 유지 |
| **수동 납입 지연 3회** | 계약 SUSPENDED |
| OVERDUE → 직접 납입 | PAID 전환, SUSPENDED 계약 ACTIVE 복구 |
| 이미 납입된 스케줄 재납입 | `INVALID_STATUS` 예외 |
| 납입 중 잔액 부족 | `INSUFFICIENT_BALANCE` 예외 전파 |

---

## 주요 설계 결정

| 결정 | 이유 |
|------|------|
| Flyway 마이그레이션 | 환경별 스키마 일관성 보장 |
| H2 로컬 프로파일 분리 | DB 없이 즉시 개발·검증 가능 |
| `StatusHistory` 자동 기록 | 계약·계좌 상태 변경 감사 추적 |
| 현금흐름 기반 추천 서비스 | LLM 호출 없는 규칙 기반 추천임을 명확히 분리 |
| 로컬 시드 분리 | 운영 DB에 테스트·프론트 데모 데이터가 섞이지 않도록 `LocalDataSeeder`로 제한 |
| `X-Customer-Id` 검증 | Gateway가 전달한 인증 고객과 요청 고객 불일치 시 IDOR 차단 |
| 납입 스케줄 분리 저장 | 회차별 납입 결과를 독립적으로 추적하고 연체 이력 보존 |
| 비관적 락 + 고정 순서 락 | 동시 자동이체 처리 시 데드락 방지 (낮은 ID 계좌 먼저 락 획득) |
| 3회 연속 실패 정지 | 잘못된 계좌 정보·지속 부족으로 인한 무한 재시도 방지 |
| 연속 실패 초기화 | 성공·직접 납입 시 `consecutiveMissCount = 0` 으로 계약 자동 복구 |

---

## 보안·정합성 보강 사항

| 항목 | 적용 내용 |
|------|------|
| 내부 이체 검증 | 수신 계좌 ID와 계좌번호를 함께 검증하고, 수신 계좌가 없으면 출금하지 않음 |
| 거래 동시성 | 계좌 조회에 `PESSIMISTIC_WRITE` 락을 적용하고 `@Version`으로 낙관락 컬럼도 유지 |
| 계좌 비밀번호 | 저장 전 BCrypt 해시 처리, 엔티티 저장 직전 평문 유입 차단 |
| 외부 이체 | 외부 이체는 출금 거래만 기록하고 내부 수신 거래를 생성하지 않음 |
| 날짜 타입 | 계좌·계약 주요 날짜를 `LocalDate` / SQL `DATE`로 관리 |
| 계좌번호 | DB sequence와 check digit 기반으로 발급 |
| 상품 추천 | 가입금액 필터를 SQL로 푸시다운하고 금리는 상품 ID 목록 기준으로 일괄 조회 |
| 시간 의존성 | 비즈니스 로직에서 `Clock`을 주입받아 테스트 결정성 확보 |
| 자동이체 정지 | 3회 연속 실패 시 자동으로 비활성화하여 고객 계좌 무단 출금 반복 차단 |
