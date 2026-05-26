# loan-service 다음 구현 계획 — 도메인 기능 확장판

## Context

loan-service 는 현재 신청→가심사→CB→DSR→본심사→약정→실행→상환→연체→종결까지 전 주기 엔드포인트가 깔려있고 대부분에 `*FlowTest` 통합 테스트가 있다. 그러나 **상환 분배 정확성**, **종결 분기 실로직**, **자동이체 후속 처리**, **notification 채널 송신**, **고아 패키지 통합** 등 도메인 깊이가 얕은 영역이 다수 남아 있다.

사용자가 명시한 1순위 4영역(연체이자/수수료 분배, 신용정보 신고, 부분상환, 역분개)을 축으로 하고, 같은 흐름에서 자연스럽게 깊이를 더할 수 있는 영역을 함께 묶어 작업 단위를 키운다. 본 plan 범위 밖은 명시.

진척도를 먼저 정리:

| 영역 | 현황 | 갭 |
|---|---|---|
| 부분상환 `partialrepayment/*` | 풀구현 (`OverdueInterestCalculator` 사용 분배 포함) | 전용 테스트 없음 |
| 역분개 `reversal/*` | 풀구현 | 전용 테스트 없음 |
| 중도상환 `prepayment/*` | 풀구현 | `overdueInterestAmount=0L` 하드코딩, 연체이자·수수료 미분배 |
| 정확액 회차상환 `repayment.RepaymentService` | 풀구현 | `overdueInterestAmount=0L`, `feeAmount=0L` 하드코딩 |
| 신용정보 신고 `creditreport/*` | 풀구현 + `CreditInfoReportFlowTest` | — (회귀 점검만) |
| 종결 `closure/*` | NORMAL/EARLY/WRITE_OFF/SUBROGATION 분기 존재, 잔액 검증 분기 OK | WRITE_OFF·SUBROGATION 실로직 stub — 손실 회계·연체이자 소각·채권 감소 없음 |
| 자동이체 `autodebit/*` | 일배치 + 영업일 회피 OK | 출금 실패 시 warn 로그만, **재시도 정책·횟수·간격·실패 row 추적 없음** |
| notification `notification/*` | AFTER_COMMIT + @Async 리스너 5개 (Submitted/Approved/Disbursed/Signed/InstallmentPaid) | 모든 채널 stub(`simulate` + Thread.sleep), outbox·재시도 큐 없음 |
| 연대보증인 `guarantor/*` | 등록·서명·취소 API 존재 | 어느 흐름에도 필수 검증 없음 (`contract`/`execution` 에서 SIGNED 강제 X) |
| 신용점수 preview `creditscore/*` | `/api/credit-score/preview` 만 존재 | 신청 흐름과 단절 — orphan API |
| 신청 만료 `applicationexpiry/*` | 14일 상수 만료 | 상품별 차등 만료일 미구현 |
| 영업일 캘린더 `calendar/*` | `BusinessDayService.isBusinessDay()` autodebit 에서 호출 | `RepaymentSchedule.dueDate` / `Maturity.endDate` 산정에 휴일 보정 안 들어감 |
| 보증보험 `guaranteeinsurance*` | 정상 통합 | — |
| 만기 `maturity/*` | 정상 통합 | — |
| 상태이력 `statushistory/*` | 정상 통합 | — |

→ 작업의 무게중심:
1. **상환 분배 정확성** (A) — 사용자 명시 + 빈도 높은 거래
2. **부분상환·역분개 테스트 추가** (C, D) — 코드 있는데 회귀 안전망 없음
3. **종결 WRITE_OFF / SUBROGATION 실로직** (E) — 대손·대위변제 흐름
4. **자동이체 재시도 정책** (F) — 연체와 직결
5. **notification outbox** (G) — 운영 안정성
6. **고아 패키지 정착** (H, I) — guarantor 검증, creditscore 통합 또는 deprecate
7. **캘린더 휴일 보정** (J) — 스케줄 정확성

---

## A. 연체이자 / 수수료 분배 보강 (메인 작업)

**문제**: `flows §2.2` 분배 순서는 *연체이자 → 정상이자 → 원금 → 수수료*. `RepaymentService.repayInstallment` (L96-120) 와 `PrepaymentService` (L123 부근) 가 OVERDUE 회차조차 `overdueInterestAmount=0L`, `feeAmount=0L` 로 박아 둔다 — 부분상환만 정확히 분배.

**재사용할 기존 유틸**
- `repayment/service/OverdueInterestCalculator.java::compute(principal, rateBps, days)`
- `partialrepayment/service/PartialRepaymentService#computeOverdueInterest` (이미 정확한 분배)
- `LoanProduct.getOverdueRateBps()`
- `RepaymentTransaction.builder().overdueInterestAmount/feeAmount` (필드 이미 존재)

**변경**

1. **공통 헬퍼 신규** `repayment/service/PaymentAllocator.java`
   - 입력: 받은금액, scheduledPrincipal, scheduledInterest, overdueBase, overdueRateBps, days, requestedFee
   - 출력: `(overduePortion, interestPortion, principalPortion, feePortion, balanceAfter)`
   - 3개 서비스 (Repayment / Prepayment / PartialRepayment) 모두 호출 → 중복 제거
2. `RepaymentService.repayInstallment` — 회차 status=OVERDUE 일 때만 연체이자 산정 후 PaymentAllocator 호출
3. `PrepaymentService` — 잔여 회차 중 OVERDUE 가 있으면 그 회차 연체이자 우선 분배 + 상품·요청에서 prepayment fee 산출
4. `RepayInstallmentRequest` — `feeAmount` 옵션 필드 추가
5. 에러코드 `LOAN_092` 신규 (입력 금액 ≠ 정상회차+연체이자+수수료) — A안 채택 시
6. `PartialRepaymentService` — 새 헬퍼 호출 형태로 슬림화 (동작 변경 X)

**결정 필요 (A안 / B안)**
- A안: 입력 금액에 연체이자·수수료 포함을 강제 → API 입력 의미 변경
- B안: 받은 금액 그대로, 분배 결과만 컬럼 분리 표기 → 기존 API 호환

이 결정은 구현 시작 직전에 사용자에게 다시 확인.

---

## B. 신용정보 신고 — 회귀 점검만

코드·테스트 완성. 본 plan 변경이 `CreditInfoReportFlowTest` 를 깨지 않는지 마지막에 한 번 더 돌린다. 새 작업 없음.

---

## C. 부분상환 통합 테스트 추가

새 파일 `services/loan-service/src/test/java/com/bank/loan/PartialRepaymentFlowTest.java` — `AbstractLoanIntegrationTest` 기반, `RepaymentFlowTest` 의 helper 재사용.

케이스:
- 정상 부분상환 — `principalAmount` 만큼 잔액 감소, 회차 상태 유지
- OVERDUE 회차에 대한 부분상환 — 연체이자→정상이자→원금 순 분배
- 잔액 초과 입력 → 400
- 이미 PAID 회차 부분상환 → `LOAN_091`
- 미존재 cntrId → `LOAN_062`
- 분배 항등식 단언: `principal + interest + overdueInterest + fee == total`

---

## D. 역분개 통합 테스트 추가

새 파일 `services/loan-service/src/test/java/com/bank/loan/ReversalFlowTest.java`

케이스:
- 정상 회차상환 후 역분개 → 회차 `PAID → DUE` 롤백, 새 reversing tx `reversalYn=Y` 생성
- 이미 역분개된 거래 재역분개 → 409
- 미존재 rtxId → 404
- 다른 cntrId 의 rtxId → 거절 (보안)
- 부분상환 거래 역분개 가능 시 회차 잔액 복원 확인

---

## E. 종결 WRITE_OFF / SUBROGATION 실로직

**현재**: `LoanClosureService` 가 4종 분기는 하지만 WRITE_OFF/SUBROGATION 은 `finalPrincipalAmt/finalInterestAmt` 만 기록하는 stub.

**변경**

1. `closure/domain/LoanClosure` — `writeOffAmount`, `subrogationAmount`, `subrogationGuarantorRef`, `writeOffReasonCd` 컬럼 추가 (또는 기존 컬럼 활용)
2. `LoanClosureService.closeWriteOff(cntrId, req)`
   - 잔여 원금·이자·연체이자 합계 → `write_off_amount` 에 기록
   - 활성 회차(DUE/OVERDUE) 모두 `WRITTEN_OFF` 신규 상태로 전이 (또는 PAID + reason=WRITE_OFF)
   - `RepaymentTransaction` 에 손실 분개 1건 (`rtxTypeCd=WRITE_OFF`, status=SUCCESS, amount=0) — append-only 감사용
   - 활성 연체 정보 `Delinquency` 가 있으면 `RESOLVED` 로 전이 + reason=WRITE_OFF
3. `LoanClosureService.closeSubrogation(cntrId, req)`
   - 활성 보증보험(`guaranteeinsurance` ISSUED) 또는 연대보증인 약정 검증 — 없으면 `LOAN_125` 신규
   - 잔여 원금·이자를 `subrogation_amount` 로 기록, 보증기관·보증인 ID 를 `subrogation_party_ref` 에 기록
   - 회차·연체 처리는 WRITE_OFF 와 동일 패턴
4. 에러코드 `LOAN_125`(대위변제 사전조건 미충족), `LOAN_126`(이미 WRITE_OFF 된 계약) 추가
5. notification 새 이벤트 `LoanWrittenOffEvent`, `LoanSubrogatedEvent` (listener stub 등록)

새 테스트 `LoanClosureWriteOffFlowTest` — 두 분기 케이스 + 사전조건 실패 + 잔여 회차 일괄 전이 검증.

---

## F. 자동이체 OVERDUE 재시도 정책

**현재**: `AutoDebitBatchService.run(baseDate)` — 출금 실패 시 warn 로그 + `skipped++` 만. 재시도 정책 없음.

**변경**

1. 신규 도메인 `autodebit/domain/AutoDebitAttempt`
   - 컬럼: `cntrId, installmentNo, attemptNo, attemptedAt, status(SUCCESS/FAIL), failureReason`
   - 실패 row 가 적재돼 다음 배치에서 조회 가능
2. `AutoDebitBatchService.run` 흐름 변경
   - 1차 출금 실패 → `AutoDebitAttempt(attemptNo=1, FAIL)` 적재 + 회차 상태는 그대로
   - 2회·3회 재시도 — 기본 정책: D+1, D+3 (영업일 기준, `BusinessDayService` 활용)
   - 정책상 최종 실패(attemptNo=3 fail) → 회차를 OVERDUE 로 전이 + `DelinquencyRolloverService` 트리거
3. `application.yml` — `loan.auto-debit.retry.max-attempts`, `retry.intervals-days` 설정화
4. 새 엔드포인트 `GET /api/loan-contracts/{cntrId}/auto-debit-attempts` — 이력 조회

새 테스트 `AutoDebitRetryFlowTest`:
- 1차 실패 → attempt row 1건, 회차 DUE 유지
- 3차까지 모두 실패 → 회차 OVERDUE 전이, 연체 row 생성
- 2차에서 성공 → 회차 PAID, 후속 시도 X
- 메모리 룰: 테스트별 연도(2033/2034) 분리

---

## G. notification — outbox + 재시도 큐 (channel adapter 자리만 마련)

**현재**: 5개 listener 가 `simulate()` + Thread.sleep + 로그. 실패 시 재시도 없음. 외부 채널 SDK 없음.

**변경 — channel SDK 도입 *아님*, outbox 패턴만 깔아 둠**

1. 신규 테이블 `notification_outbox`
   - 컬럼: `id, eventTypeCd, payload(jsonb), channelCd, status(PENDING/SENT/FAILED), attemptNo, maxAttempt, nextAttemptAt, lastError, createdAt, sentAt`
2. 기존 listener 들이 `simulate()` 대신 `notificationOutboxRepository.save(...)` 호출 (같은 트랜잭션 — AFTER_COMMIT 이므로 별 트랜잭션 1개 추가)
3. 신규 batch `NotificationDispatchBatch` — `POST /api/internal/notification/dispatch` (PENDING 행을 채널별 어댑터에 전달, 어댑터는 여전히 stub 이지만 인터페이스가 분리됨)
4. `NotificationChannelAdapter` 인터페이스 + `StubSmsAdapter`, `StubEmailAdapter`, `StubKakaoAdapter` — 향후 실 SDK 교체 자리
5. 재시도 정책: max=5, exponential backoff (`nextAttemptAt = now + 2^attemptNo min`)
6. 메트릭: `notification_outbox_total{eventType, status}`

새 테스트 `NotificationOutboxFlowTest`:
- 이벤트 발행 → outbox PENDING 1건
- dispatch 호출 → SENT 전이
- 어댑터 강제 실패 stub → FAILED + attemptNo 증가 + nextAttemptAt 변경
- max 도달 시 더 이상 재시도 안 함

---

## H. 연대보증인 검증 통합

**현재**: `guarantor/*` 등록·서명·취소 API 존재. 어느 흐름에서도 SIGNED 약정을 필수로 검증하지 않음.

**변경**

1. `LoanProduct` 에 `guarantorRequiredYn` 필드는 이미 존재 (조회). 검증 hook 만 붙임:
   - `LoanReviewService.run / autoDecide` 사전조건에 `requireGuarantorSignedIfRequired(applId, product)` 추가 — `LOAN_038` 에 reason 분기
   - `LoanContractService.create` — 동일 검증
2. 활성 약정(`SIGNED` && not `CANCELED`) 0건 + `guarantorRequiredYn=Y` → 사전조건 실패
3. 에러코드 분기 reason 만 추가, 신규 코드 없음

새 테스트 — `LoanReviewFlowTest` 에 케이스 추가 / `LoanContractFlowTest` 에 케이스 추가.

---

## I. creditscore — 연결 결정

`/api/credit-score/preview` 가 신청 흐름과 단절. 선택:

- **선택 1 (추천)**: 신청 직후 `application.service` 에서 `CreditScoreEngine` 비동기 호출해 결과를 `LoanApplication.previewScore` 컬럼에 저장. 가심사 단계에서 활용.
- **선택 2**: orphan 으로 명시 deprecate — `@Deprecated` + Swagger 표기, 별 plan 으로 분리.

기본은 선택 2 — orphan 명시. 본 plan 작업량을 안 키운다. **사용자 확인 사항**.

---

## J. 영업일 캘린더 — 스케줄·만기 휴일 보정

**현재**: `BusinessDayService.isBusinessDay()` 가 `autodebit` 에서만 쓰임. `RepaymentSchedule.dueDate` 와 `Maturity.endDate` 산정 시 휴일을 그대로 둠.

**변경**

1. `schedule/service/RepaymentScheduleGenerator` (이름은 추정) 에서 `dueDate` 산정 시 휴일이면 `nextBusinessDay` 로 이동
2. `MaturityService.createOnContract` — `endDate` 가 휴일이면 후일로 이동 (또는 기록만, flag `holidayAdjusted=Y`)
3. 이미 생성된 schedule 은 변경 안 함 — 신규 약정부터 적용

새 테스트: `RepaymentScheduleFlowTest` 에 휴일 회피 케이스 추가, `MaturityFlowTest` 보강.

---

## K. 신청 만료 — 상품별 차등

**현재**: 14일 상수.

**변경**: `LoanProduct.applicationValidityDays` (또는 같은 의미 필드) 추가 → `ApplicationExpiryService.run(baseDate)` 가 상품 필드 우선, 미설정이면 default 14.

새 테스트: `ApplicationExpiryFlowTest` 신규 (있는지 확인 후, 없으면 추가).

---

## 작업 순서 (커밋 단위)

기능/테스트 분리(메모리 룰), AI 흔적 금지, 한 단계 끝낼 때마다 보고.

1. `refactor(loan): 상환 분배 공통 헬퍼 PaymentAllocator 추출` — 동작 무변경
2. `test(loan): 부분상환 통합 테스트 추가` (C)
3. `test(loan): 역분개 통합 테스트 추가` (D)
4. `feat(loan): 회차 정확액 상환에 연체이자 분배 적용` (A.2)
5. `test(loan): 연체이자 분배 회귀 테스트` (RepaymentFlowTest 보강)
6. `feat(loan): 중도상환에 연체이자·수수료 분배 적용` (A.3)
7. `test(loan): 중도상환 분배 회귀 테스트`
8. (A안 채택 시) `feat(loan): LOAN_092 분배 금액 불일치 에러코드 추가`
9. `feat(loan): 종결 WRITE_OFF/SUBROGATION 실로직 적용` (E)
10. `test(loan): WRITE_OFF/SUBROGATION 통합 테스트 추가`
11. `feat(loan): 자동이체 재시도 정책 (AutoDebitAttempt)` (F)
12. `test(loan): 자동이체 재시도 통합 테스트`
13. `feat(loan): notification outbox 패턴 도입` (G)
14. `test(loan): notification outbox 통합 테스트`
15. `feat(loan): 본심사/약정에 연대보증인 SIGNED 검증` (H)
16. `test(loan): 연대보증 필수 상품 사전조건 회귀`
17. `feat(loan): 영업일 캘린더로 dueDate/maturity 휴일 보정` (J)
18. `test(loan): 휴일 보정 회귀`
19. `feat(loan): 상품별 신청 만료 일수 적용` (K)
20. `test(loan): 신청 만료 통합 테스트`
21. `chore(loan): creditscore orphan 명시 (deprecate 또는 신청 연결)` (I, 결정 따라)

각 단계 끝나면 해당 FlowTest 만 우선 회귀, 마지막에 `:services:loan-service:test` 풀런.

---

## 영향 받는 파일 (요약)

**상환 분배 (A)**
- `services/loan-service/src/main/java/com/bank/loan/repayment/service/RepaymentService.java`
- `services/loan-service/src/main/java/com/bank/loan/repayment/service/PaymentAllocator.java` (신규)
- `services/loan-service/src/main/java/com/bank/loan/repayment/service/OverdueInterestCalculator.java`
- `services/loan-service/src/main/java/com/bank/loan/prepayment/service/PrepaymentService.java`
- `services/loan-service/src/main/java/com/bank/loan/partialrepayment/service/PartialRepaymentService.java`
- `services/loan-service/src/main/java/com/bank/loan/repayment/dto/RepayInstallmentRequest.java`
- `services/loan-service/src/main/java/com/bank/loan/support/LoanErrorCode.java`

**종결 (E)**
- `services/loan-service/src/main/java/com/bank/loan/closure/service/LoanClosureService.java`
- `services/loan-service/src/main/java/com/bank/loan/closure/domain/LoanClosure.java`
- `services/loan-service/src/main/java/com/bank/loan/closure/dto/*` (요청 DTO 신규/보강)
- `services/loan-service/src/main/java/com/bank/loan/notification/event/LoanWrittenOffEvent.java` (신규)
- `services/loan-service/src/main/java/com/bank/loan/notification/event/LoanSubrogatedEvent.java` (신규)

**자동이체 (F)**
- `services/loan-service/src/main/java/com/bank/loan/autodebit/service/AutoDebitBatchService.java`
- `services/loan-service/src/main/java/com/bank/loan/autodebit/domain/AutoDebitAttempt.java` (신규)
- `services/loan-service/src/main/java/com/bank/loan/autodebit/repository/AutoDebitAttemptRepository.java` (신규)
- `services/loan-service/src/main/java/com/bank/loan/autodebit/controller/AutoDebitController.java` (조회 엔드포인트 추가)
- `services/loan-service/src/main/resources/application.yml`

**notification (G)**
- `services/loan-service/src/main/java/com/bank/loan/notification/listener/*.java` (5개 — outbox 저장으로 변경)
- `services/loan-service/src/main/java/com/bank/loan/notification/outbox/*` (신규 패키지: 도메인/리포지토리/배치/어댑터)

**guarantor / creditscore / calendar / applicationexpiry (H/I/J/K)**
- `services/loan-service/src/main/java/com/bank/loan/review/service/LoanReviewService.java`
- `services/loan-service/src/main/java/com/bank/loan/contract/service/LoanContractService.java`
- `services/loan-service/src/main/java/com/bank/loan/schedule/service/*` (휴일 보정)
- `services/loan-service/src/main/java/com/bank/loan/maturity/service/MaturityService.java`
- `services/loan-service/src/main/java/com/bank/loan/applicationexpiry/service/ApplicationExpiryService.java`
- `services/loan-service/src/main/java/com/bank/loan/product/domain/LoanProduct.java` (`applicationValidityDays` 컬럼)

**테스트 (전 영역)**
- `PartialRepaymentFlowTest.java` (신규)
- `ReversalFlowTest.java` (신규)
- `PrepaymentFlowTest.java` (신규 또는 보강)
- `LoanClosureWriteOffFlowTest.java` (신규)
- `AutoDebitRetryFlowTest.java` (신규)
- `NotificationOutboxFlowTest.java` (신규)
- `ApplicationExpiryFlowTest.java` (신규)
- `RepaymentFlowTest.java` / `RepaymentScheduleFlowTest.java` / `MaturityFlowTest.java` / `LoanReviewFlowTest.java` / `LoanContractFlowTest.java` (보강)

---

## 검증

- 단위 회귀: 각 단계 커밋 직후 해당 `*FlowTest` 만 우선 실행 (메모리 룰).
- 분배 항등식: 새 상환 테스트 전부 `principal + interest + overdueInterest + fee == total` 단언.
- 전체 회귀: 단계별 묶음(A/E/F/G/H/J/K 각 끝) 마다 `:services:loan-service:test` 풀런.
- 날짜 격리(메모리 룰): 배치성 테스트(F, K, autodebit) 는 테스트별로 다른 연도 사용 — 2033/2034/2035/2036.
- 외부 채널 부재 검증: G 의 outbox dispatch 테스트는 stub 어댑터만으로 SENT 전이 확인. 실제 SDK 도입은 본 plan 범위 밖이라는 점을 테스트 주석에 명시.

---

## 비-목표

- 외부 결제 도메인 연동 (`LoanExecutionService` 의 `transaction_id` 채움, 실 송금) — 별 plan
- 본심사 LLM 도입 — `docs/ai/banking-review-llm.md`
- 실제 SMS/카카오/이메일 SDK 연동 — G 의 어댑터 인터페이스만 마련, SDK 도입은 후속
- 외부 감정평가기관 / 외부 IDV(PASS) 실 연동 — 후속
- 신청번호 시퀀스/Redis INCR 교체 — 운영 빚, 별 plan
- `common.CryptoService` 구현 (PII 컬럼 암호화) — 횡단 작업, 별 plan
- S3/MinIO 문서 스토리지 어댑터 교체 — 운영 빚, 별 plan
- Kafka 도입 — notification outbox 의 후속 단계, 본 plan 외
