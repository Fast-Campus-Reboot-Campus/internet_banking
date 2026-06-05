# loan-service ↔ payment-service 연동 명세

대출 서비스(여신계)가 이체 서비스(이체계)를 호출하는 모든 흐름을 정의한다.  
**deposit-service와 payment-service 코드는 건드리지 않는다.**

---

## 계좌 ID 관계

```
deposit-service.deposit_accounts.account_id (PK)
        ‖ 동일한 값 (cross-DB 참조, FK 없음)
loan-service.repayment_account.account_id
```

loan-service가 내부 복호화(`CryptoService.decrypt`)로 실제 계좌번호를 획득한 뒤  
payment-service에 이체를 요청한다. payment-service는 기존 deposit-service 연동 로직을 그대로 사용한다.

---

## 공통 — PaymentServiceClient

모든 흐름이 단일 Feign 클라이언트(`PaymentServiceClient`)를 통해 같은 엔드포인트를 호출한다.

```
POST /api/v1/payments
```

**공통 헤더**

| 헤더 | 값 |
|---|---|
| `X-Idempotency-Key` | 흐름별 멱등키 (각 섹션 참고) |
| `X-User-Id` | `SYSTEM` |
| `X-Auth-Token-Id` | `SYSTEM` |

**공통 응답**

| 필드 | 타입 | 설명 |
|---|---|---|
| `paymentInstructionId` | String | 결제지시 ID — 각 거래 row의 `pi_id`에 저장 |
| `transactionNo` | String | 거래번호 |
| `status` | String | `COMPLETED` / `FAILED` / `CLEARING` |
| `failureCategory` | String | 실패 원인 코드 (FAILED 시) |

> `CLEARING` 응답은 타행 청산 대기 상태. 흐름별 처리 방침은 각 섹션에 기술.

---

## 흐름 1 — 대출실행 (Disbursement)

**방향**: 은행 자금 집행 계좌 → 고객 수령 계좌

### 현황 및 변경

| | 내용 |
|---|---|
| **현재** | `LoanExecutionService.drawdown()`이 DB만 기록하고 실제 이체 없음. `transactionId=null` 플레이스홀더. |
| **변경** | payment-service 호출 → `COMPLETED`이면 `execStatusCd=DONE` 기록, `FAILED`이면 `FAILED` 기록. |

### 흐름

```
LoanExecutionService.drawdown(cntrId, req, idempotencyKey)
  1. 선행 검증 (계약·상환계좌·보증보험·보증인·한도)
  2. loan_execution INSERT (status=REQUESTED)
  3. PaymentServiceClient.pay() 호출
       COMPLETED → execStatusCd=DONE, journalEntryNo 채번, (최초 인출) 계약 ACTIVE + 스케줄 생성
       FAILED    → execStatusCd=FAILED, 예외 반환
       CLEARING  → 별도 협의 필요 (현재 미지원)
  4. 멱등키: "EXEC-{cntrId}-{idempotencyKey}"  (idempotencyKey 미제공 시 execId 사용)
```

### 요청 Body

| 필드 | 타입 | 값 |
|---|---|---|
| `senderAccountId` | String | `payment.disbursement-account-id` (application.yml 설정값) |
| `receiverBankCode` | String | `DrawdownRequest.disbursementBankCd` |
| `receiverAccountNo` | String | `DrawdownRequest.disbursementAccountNo` (요청 시 plain-text 수령) |
| `receiverHolderName` | String | 고객명 또는 `"대출실행"` |
| `transferAmount` | BigDecimal | `DrawdownRequest.executedAmount` |
| `senderMemo` | String | `"대출실행 {cntrNo}"` |
| `receiverMemo` | String | `"대출실행"` |
| `channel` | String | `"INBOUND"` |
| `receiverPassbookSenderDisplay` | String | 대출 계약번호 (`cntrNo`) |

---

## 흐름 2 — 자동이체 상환 (Auto-Debit)

**방향**: 고객 상환 계좌 → 은행 수납 계좌

### 흐름

```
AutoDebitBatchService.run(baseDate)
  1. 출금 대상 회차 조회 (DUE/OVERDUE, auto_debit_yn=Y, racct_status=VERIFIED)
  2. repayment_account.account_no_enc 복호화 → 실제 계좌번호
  3. PaymentServiceClient.pay() 호출
       COMPLETED → repaymentService.repayInstallment() → STATUS_SUCCESS
       FAILED    → RepaymentTransaction STATUS_FAILED 기록
       CLEARING  → pi_id 저장 후 콜백 대기 (흐름 2-1 참고)
  4. 멱등키: "AUTO-{cntrId}-{rschId}-{baseDate}"
```

### 요청 Body

| 필드 | 타입 | 값 |
|---|---|---|
| `senderAccountId` | String | `repayment_account.account_no` (복호화) |
| `receiverBankCode` | String | 은행 수납 계좌 은행코드 |
| `receiverAccountNo` | String | 은행 수납 계좌번호 |
| `receiverHolderName` | String | 은행명 또는 수납 계좌 예금주 |
| `transferAmount` | BigDecimal | 상환 회차 납부 금액 |
| `senderMemo` | String | `"대출상환 {installmentNo}회차"` |
| `receiverMemo` | String | `"대출상환"` |
| `channel` | String | `"INBOUND"` |
| `receiverPassbookSenderDisplay` | String | 대출 계약번호 |

### 흐름 2-1 — CLEARING 콜백

payment-service가 타행 청산 완료 후 `POST /api/internal/auto-debit/payment-result`를 콜백한다.

```
AutoDebitCallbackService.handleResult(req)
  1. 멱등키 파싱: "AUTO-{cntrId}-{rschId}-{baseDate}"
  2. RepaymentTransaction.idempotency_key UNIQUE — 중복 콜백 자동 차단
  3. COMPLETED → repaymentService.repayInstallment(piId=req.paymentInstructionId)
     FAILED    → STATUS_FAILED 기록
```

---

## 흐름 3 — 수동·창구 상환 (Manual Repayment)

**방향**: payment-service 호출 없음.

`POST /api/loan-contracts/{cntrId}/repayments` (및 `/partial`, `/prepayments`)는  
**창구·텔러가 이미 수납된 금액을 원장에 기록**하는 용도이므로 payment-service를 호출하지 않는다.

- `channel_cd`: 요청 Body의 `channelCd` 그대로 기록 (기본값 `MANUAL`)
- WEB·MOBILE 채널에서 고객이 직접 상환할 경우의 payment-service 연동 흐름은 **별도 협의 필요**.

---

## 흐름 4 — 역분개 후 환급 (Reversal Refund)

**방향**: 은행 수납 계좌 → 고객 계좌 (환급)

현재 `ReversalService`는 원장 역분개 row만 생성하고 실제 환급 이체는 수행하지 않는다.  
(`"회계 반대분개(common_transaction)는 본 단계 외"` 주석 참고)

- 환급 이체 자동화 여부 및 호출 시점은 **별도 협의 필요**.
- 자동화 시 멱등키 후보: `"REV-{cntrId}-{rtxId}"`

---

## 수정 파일 목록

| 파일 | 변경 내용 |
|---|---|
| `execution/service/LoanExecutionService` | payment-service 호출 추가 (흐름 1) |
| `autodebit/service/AutoDebitBatchService` | payment-service 호출 (흐름 2, 기존 구현) |
| `autodebit/service/AutoDebitCallbackService` | CLEARING 콜백 처리 (흐름 2-1, 기존 구현) |
| `payment/client/PaymentServiceClient` | Feign 클라이언트 (기존 구현) |
| `repayment/service/RepaymentService` | paymentStatus / piId 분기 (기존 구현) |
| `src/main/resources/application.yml` | payment.url, payment.disbursement-account-id 추가 |

---

## application.yml 추가

```yaml
payment:
  url: ${PAYMENT_SERVICE_URL:http://localhost:8080}
  disbursement-account-id: ${PAYMENT_DISBURSEMENT_ACCOUNT_ID:BANK_DISBURSE_001}
```

---

## 변경하지 않는 것

| 항목 | 이유 |
|---|---|
| `RepaymentAccountController` | 외부 노출 API 변경 없음 |
| `RepaymentAccountService` | 복호화는 AutoDebitBatchService 내부에서만 사용 |
| `RepaymentController` (수동 상환) | 창구 수납 기록 용도, payment-service 연동 불필요 |
| `ReversalService` | 환급 이체 자동화는 별도 협의 |
| DB 스키마 | 신규 테이블/컬럼 없음 (`loan_execution.pi_id` 컬럼 추가는 선택) |
| deposit-service | 변경 없음 |
| payment-service | 변경 없음 |

---

## 구현 계획 (Phase)

흐름 2(자동이체)·2-1(CLEARING 콜백)·`RepaymentService` 분기는 이미 구현됨.  
남은 작업은 흐름 1(대출실행)·4(역분개 환급)·3(WEB·MOBILE 상환)이며 아래 순서로 진행한다.

> 커밋 규칙: `feat(loan)` 와 `test(loan)` 은 항상 별도 커밋. 각 단계 완료 시 커밋 후 보고하고 멈춘다.

### Phase A — 대출실행 payment-service 연동 (흐름 1) · 최우선

구현 가능한 명확한 항목. 현재 `LoanExecutionService.drawdown()` 의 `transactionId=null` 플레이스홀더를 실제 이체로 대체.

| # | 작업 | 파일 |
|---|---|---|
| A-1 | `payment.disbursement-account-id` 설정 추가 | `application.yml` |
| A-2 | `loan_execution.pi_id` 컬럼 + 인덱스 마이그레이션 | `db/migration/V25__loan_execution_pi_id.sql` |
| A-3 | `LoanExecution` 도메인에 `piId` 필드 추가 | `execution/domain/LoanExecution.java` |
| A-4 | `drawdown()` 흐름 재구성 (아래 상세) | `execution/service/LoanExecutionService.java` |
| A-5 | 통합 테스트 (성공/실패/멱등성) — **별도 test 커밋** | `execution/.../LoanExecutionServiceTest` |

**A-4 흐름 재구성 (핵심)**

```
현재: 검증 → loan_execution INSERT(DONE) → (최초인출) ACTIVE 전이 + 스케줄 + outbox
변경: 검증 → loan_execution INSERT(REQUESTED)
       → PaymentServiceClient.pay(멱등키 "EXEC-{cntrId}-{idempotencyKey}")
         · COMPLETED → execStatusCd=DONE, pi_id 저장, journalEntryNo 채번,
                        (최초인출) ACTIVE 전이 + 스케줄 생성 + outbox 4건
         · FAILED    → execStatusCd=FAILED, pi_id 저장, BusinessException(신규 LOAN_xxx)
         · CLEARING  → 미지원, BusinessException
```

- **부수효과 이동**: 계약 ACTIVE 전이·스케줄 생성·outbox enqueue 는 반드시 `COMPLETED` 이후에만 실행 (현재는 INSERT 직후 실행 → 이동 필요).
- **신규 에러코드**: 출금 실패용 `LoanErrorCode` 1건 추가 (예: `LOAN_185 대출실행 출금 실패`).
- **멱등성**: 기존 `repository.findByIdempotencyKey` 선검사 유지 + payment 멱등키를 `EXEC-` 프리픽스로 연계.

### Phase B — 역분개 환급 자동화 (흐름 4) · 설계 선행

`ReversalService` 가 환급 이체를 호출하도록 확장. **단, 자동화 여부·시점이 미확정**이라 설계 합의 후 착수.

- 합의 필요: ① 역분개 시 즉시 환급 vs. 별도 배치 ② 환급 실패 시 reversal row 처리(롤백/보류)
- 합의 시: 멱등키 `"REV-{cntrId}-{rtxId}"`, `senderAccountId` = 은행 수납계좌, 환급 대상 = 원 거래 상환 계좌

### Phase C — WEB·MOBILE 상환 연동 (흐름 3) · 설계 선행

현재 `RepaymentController` 는 창구 수납 기록 전용(payment 미연동). 온라인 채널 직접 상환 흐름 미정.

- 합의 필요: ① loan-service 가 payment 를 호출(PULL)하는 orchestration 방식 vs. ② payment → loan 콜백 방식
- 결정 후 본 섹션과 흐름 3 을 구체화.

### 진행 순서

1. **Phase A** (A-1 → A-5) — 단계별 커밋·보고
2. Phase B·C 는 설계 합의 회의 후 별도 진행
