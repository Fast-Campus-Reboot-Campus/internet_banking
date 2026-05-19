# 🔁 LON 여신계 도메인 흐름

> ERD([loan_erd.md](./loan_erd.md))만으로는 보이지 않는 **상태 전이**와 **핵심 처리 시퀀스**를 정리한다.
> 표기·코드·상태값은 [data_dictionary.md](./data_dictionary.md) 및 ERD와 동일 규칙을 따른다.

---

## 1. 대출신청 상태 전이도 (`LOAN_APPLICATION.appl_status_cd`)

```mermaid
stateDiagram-v2
    [*] --> DRAFT: 신청서 작성 시작
    DRAFT --> DRAFT: 임시저장
    DRAFT --> SUBMITTED: 신청 제출\n(필수항목 입력 + 약관동의)
    DRAFT --> CANCELLED: 사용자 취소

    SUBMITTED --> SCREENING: 가심사 자동 트리거\n(LOAN_PRESCREENING 생성)
    SCREENING --> SCREEN_REJECTED: 가심사 거절\n(reject_reason_cd 기록)
    SCREENING --> REVIEWING: 가심사 통과\n+ CB 조회 완료\n+ 본인확인 + 동의 완료

    REVIEWING --> ADDITIONAL_DOC: 서류 보완 요청\n(LOAN_DOCUMENT verify 실패)
    ADDITIONAL_DOC --> REVIEWING: 보완 서류 제출
    REVIEWING --> APPROVED: 본심사 승인\n(LOAN_REVIEW.rev_decision_cd=APPROVED)
    REVIEWING --> REJECTED: 본심사 거절
    REVIEWING --> WITHDRAWN: 고객 철회

    APPROVED --> CONTRACTED: 약정 체결\n(LOAN_CONTRACT signed_at)
    APPROVED --> EXPIRED: 승인 유효기간 경과(통상 14일)

    CONTRACTED --> DISBURSED: 대출 실행\n(LOAN_EXECUTION 성공)
    CONTRACTED --> CANCELLED: 실행 전 취소

    DISBURSED --> CLOSED: 약정 종료\n(LOAN_CLOSURE 정상/조기/대위변제 등)

    SCREEN_REJECTED --> [*]
    REJECTED --> [*]
    CANCELLED --> [*]
    WITHDRAWN --> [*]
    EXPIRED --> [*]
    CLOSED --> [*]
```

### 1.1 전이 조건·검증 룰

| 전이 | 트리거 | 사전 조건(검증 룰) | 부수 효과 |
|---|---|---|---|
| `DRAFT → SUBMITTED` | 사용자 "신청" 클릭 | 필수항목(상품·금액·기간·소득) 입력, 약관 동의 완료, `idempotency_key` 발급 | `applied_at` 기록 |
| `SUBMITTED → SCREENING` | 가심사 엔진 자동 호출 | 상품 판매기간 내(`LOAN_PRODUCT.sale_*_date`), 금액·기간이 상품 범위 내 | `LOAN_PRESCREENING` row 생성 |
| `SCREENING → SCREEN_REJECTED` | 가심사 엔진 결과 | `presc_result_cd=REJECT` (PD 임계 초과/내부 블랙리스트/한도 0원 등) | `LOAN_PRESCREENING.reject_reason_cd` 기록 |
| `SCREENING → REVIEWING` | 가심사 통과 + 후속 동의 완료 | `CREDIT_CONSENT.consent_yn=Y` (필수 동의 모두), `LOAN_IDENTITY_VERIFICATION.idv_result_cd=SUCCESS`, CB 조회(`CREDIT_EVALUATION` 생성) | DSR/LTV 산출 잡 트리거 |
| `REVIEWING → ADDITIONAL_DOC` | 심사관 또는 자동검증 | `LOAN_DOCUMENT.verify_result_cd=FAIL` 또는 누락 서류 존재 | 고객 알림 + 보완 채널 오픈 |
| `REVIEWING → APPROVED` | 본심사 결정 | `CREDIT_EVALUATION.ceval_decision_cd=APPROVE`, `DSR_CALCULATION.dsr_status_cd=PASS`, 담보필수상품이면 `LTV_CALCULATION.ltv_status_cd=PASS`, `LOAN_REVIEW.rev_decision_cd=APPROVED` | `approved_amount/rate/period` 확정, 승인 유효기간(D+14) 설정 |
| `REVIEWING → REJECTED` | 본심사 거절 | `LOAN_REVIEW.rev_decision_cd=REJECTED` | `reject_reason_cd` 기록, CB 미신고 |
| `APPROVED → CONTRACTED` | 전자약정 서명 완료 | 약정서(`cntr_doc_hash`) 무결성 확인, 보증·담보 약정 완료(`GUARANTOR_AGREEMENT.gagr_status_cd=SIGNED`, `COLLATERAL.col_status_cd=REGISTERED`) | `LOAN_CONTRACT.cntr_status_cd=ACTIVE` |
| `APPROVED → EXPIRED` | 일배치(승인일+14d) | `applied_at + 14d < now` 그리고 미약정 | 알림 후 자동 만료 |
| `CONTRACTED → DISBURSED` | 실행 트랜잭션 성공 | `REPAYMENT_ACCOUNT.racct_status_cd=VERIFIED`, 보증보험(필요시) `GUARANTEE_INSURANCE.gins_status_cd=ISSUED`, 회계전표 생성(`journal_entry_no`) | 상환스케줄 `REPAYMENT_SCHEDULE` 일괄 생성, `value_date` 기준 이자기산 |
| `DISBURSED → CLOSED` | 약정종료 | 잔액=0 (정상만기/조기상환) 또는 대위변제/대손 등 | `LOAN_CLOSURE` 생성, 증명서 발급 가능 상태 |

### 1.2 공통 규칙

- 모든 상태 변경은 `STATUS_HISTORY` 에 append-only 기록 (`target_table_cd=LOAN_APPLICATION`).
- 역행 전이는 `ADDITIONAL_DOC → REVIEWING` 한 곳만 허용. 그 외는 새 신청(`appl_id`) 필수.
- 종료 상태(`*_REJECTED, CANCELLED, WITHDRAWN, EXPIRED, CLOSED`)에서는 다른 상태로 전이 금지.

---

## 2. 핵심 시퀀스

### 2.1 대출 신청 → 가심사 → CB → 한도 → 본심사 → 계약 → 실행

```mermaid
sequenceDiagram
    autonumber
    actor C as 고객(앱)
    participant API as Loan API
    participant PS as 가심사엔진
    participant CB as 외부CB(KCB/NICE)
    participant IDV as 본인확인(통신3사)
    participant CE as 신용평가엔진
    participant DSR as DSR/LTV 산출
    participant REV as 본심사(자동+수동)
    participant CT as 약정/eForm
    participant EXEC as 실행/회계
    participant DB as LON DB

    C->>API: POST /applications (상품·금액·기간·소득)
    API->>DB: LOAN_APPLICATION (appl_status=SUBMITTED, idempotency_key)
    API->>PS: 가심사 요청
    PS->>DB: LOAN_PRESCREENING (estimated_limit/rate)
    PS-->>API: 결과(통과/거절)
    alt 가심사 거절
        API->>DB: appl_status=SCREEN_REJECTED + STATUS_HISTORY
        API-->>C: 거절 사유 안내
    else 가심사 통과
        API->>DB: appl_status=SCREENING
        C->>API: 약관 동의 + 본인확인 요청
        API->>DB: CREDIT_CONSENT (consent_yn=Y, signed_doc_hash)
        API->>IDV: 본인확인 호출(external_tx_no)
        IDV-->>API: CI/DI
        API->>DB: LOAN_IDENTITY_VERIFICATION (idv_result_cd=SUCCESS)
        API->>CB: 신용정보 조회(동의 token)
        CB-->>API: 점수·기존부채
        API->>CE: 신용평가 실행
        CE->>DB: CREDIT_EVALUATION (ceval_decision_cd)
        API->>DSR: DSR/LTV 산출
        DSR->>DB: DSR_CALCULATION + LTV_CALCULATION
        API->>DB: appl_status=REVIEWING
        API->>REV: 본심사 트리거
        REV->>DB: LOAN_REVIEW + REVIEW_CHECK_LOG (점검항목별)
        alt 승인
            REV-->>API: APPROVED(amount/rate/period)
            API->>DB: appl_status=APPROVED
            C->>API: 약정 전자서명
            API->>CT: 약정서 생성·서명
            CT->>DB: LOAN_CONTRACT (cntr_status=ACTIVE, signed_at)
            API->>DB: REPAYMENT_ACCOUNT (verified)
            API->>EXEC: 실행 요청(idempotency_key)
            EXEC->>DB: LOAN_EXECUTION (exec_status=SUCCESS, journal_entry_no)
            EXEC->>DB: REPAYMENT_SCHEDULE 회차별 생성
            API->>DB: appl_status=DISBURSED
            API-->>C: 실행 완료 + 스케줄 안내
        else 거절
            REV-->>API: REJECTED(reason)
            API->>DB: appl_status=REJECTED
            API-->>C: 거절 사유 안내
        end
    end
```

**포인트**
- 외부 호출은 모두 `idempotency_key` 또는 `external_tx_no` 로 재시도 안전성 확보.
- `LOAN_PRESCREENING` 은 1:1(최신본 갱신), `CREDIT_EVALUATION` 도 1:1 — 재평가 시 `version`/`evaluated_at` 으로 추적.
- 본심사 자동거절(시스템 룰 위배) 후 수동 심사관 오버라이드 가능 → `REVIEW_CHECK_LOG` 에 점검자/사유 기록.

---

### 2.2 자동이체 일배치 (스케줄 매칭 → 출금 → 결과 콜백 → 이자/원금 분배)

```mermaid
sequenceDiagram
    autonumber
    participant SCHED as 배치 스케줄러(매일 새벽)
    participant CAL as BUSINESS_CALENDAR
    participant BATCH as 자동이체 배치
    participant DB as LON DB
    participant FW as 출금펌뱅킹/공통계좌계
    participant LDG as 회계원장(common_transaction)
    participant NOTI as 알림

    SCHED->>CAL: 오늘 영업일 여부 조회
    CAL-->>SCHED: business_day_yn=Y
    SCHED->>BATCH: 일배치 시작(base_date)

    BATCH->>DB: REPAYMENT_SCHEDULE 조회\n  due_date=base_date AND rsch_status=DUE\n  + REPAYMENT_ACCOUNT.auto_debit_yn=Y
    DB-->>BATCH: 매칭된 회차 목록

    loop 회차별 처리(트랜잭션 단위)
        BATCH->>DB: REPAYMENT_TRANSACTION (rtx_status=REQUESTED, idempotency_key)
        BATCH->>FW: 출금 요청(계좌·금액)
        alt 출금 성공
            FW-->>BATCH: 성공(거래번호)
            BATCH->>DB: rtx_status=SUCCESS, paid_at, balance_after
            note right of BATCH: 분개 순서:\n1) 연체이자 → 2) 이자 → 3) 원금 → 4) 수수료
            BATCH->>DB: REPAYMENT_SCHEDULE.rsch_status=PAID
            BATCH->>LDG: common_transaction 회계전표 생성
            BATCH->>NOTI: 상환완료 알림
        else 잔액부족/실패
            FW-->>BATCH: 실패코드
            BATCH->>DB: rtx_status=FAILED + STATUS_HISTORY
            BATCH->>DB: REPAYMENT_SCHEDULE.rsch_status=OVERDUE\n   (D+1부터 DELINQUENCY 평가)
            BATCH->>NOTI: 미납 안내 + 재출금 일정
        end
    end

    BATCH->>DB: INTEREST_ACCRUAL 당일분 마감(전계약)\n   (영업일 기준 일이자 발생·누적)
    BATCH->>DB: DELINQUENCY 신규/연장,\n   DELINQUENCY_DAILY_SNAPSHOT 일별 적재
    BATCH-->>SCHED: 배치 종료(처리/성공/실패 건수)
```

**포인트**
- 휴일에는 `INTEREST_ACCRUAL` 만 발생, 출금은 익영업일로 이월(상품 정책에 따라).
- 실패 출금은 **새 `REPAYMENT_TRANSACTION` row** 로 재시도(역분개는 `reversal_yn=Y`로 별건 기록).
- 분배(allocation)는 **연체이자 → 정상이자 → 원금 → 수수료** 순서가 기본. 상품별 예외는 `LOAN_PRODUCT` 정책에서 분기.
- 연체 D+1 진입 시 `DELINQUENCY` 생성, 매일 `DELINQUENCY_DAILY_SNAPSHOT` append. `dlq_stage_cd` 는 5/30/90일 등 임계치 도달 시 갱신.

---

### 2.3 금리 변경 → 스케줄 재생성 → 다음 회차 적용

```mermaid
sequenceDiagram
    autonumber
    participant TRG as 트리거(기준금리/우대조건/연체해소 등)
    participant API as Loan API
    participant DB as LON DB
    participant SCHED_GEN as 스케줄 재생성기
    participant NOTI as 알림

    TRG->>API: 금리 변경 이벤트\n(reason: BASE_RATE_RESET / PREF_CHANGE / DELINQ_PENALTY)
    API->>DB: LOAN_CONTRACT 현재 금리 조회\n   (total/base/spread/preferential_rate_bps)
    API->>DB: RATE_CHANGE_HISTORY append\n   previous_rate_bps, new_rate_bps,\n   applied_start_date(=다음 이자기산일)

    note over API,DB: LOAN_CONTRACT.total_rate_bps 갱신은\n적용시작일 이전에는 예약/적용일에 반영.\n과거 회차는 절대 소급 안 함.

    API->>SCHED_GEN: 스케줄 재생성 요청\n   (from_installment_no = 다음 회차)

    SCHED_GEN->>DB: 기존 REPAYMENT_SCHEDULE 조회\n   (status=DUE AND due_date >= applied_start_date)
    SCHED_GEN->>DB: 해당 회차들 rsch_status=SUPERSEDED\n   (append-only 원칙: 행 삭제 금지)
    SCHED_GEN->>DB: 신규 REPAYMENT_SCHEDULE 생성\n   rsch_version_cd=V{n+1},\n   applied_rate_bps=new_rate_bps,\n   상환방식(원리금균등/원금균등) 룰 재계산

    SCHED_GEN-->>API: 신규 스케줄 요약(회차/금액)
    API->>DB: STATUS_HISTORY (LOAN_CONTRACT, RATE_CHANGED)
    API->>NOTI: 고객 통지(변경 사유·시작일·예시 회차 금액)

    note over DB: 다음 INTEREST_ACCRUAL 부터\napplied_rate_bps = new_rate_bps 자동 반영
```

**포인트**
- 금리 변경은 **시점(applied_start_date) 기준**으로만 발효. 과거 발생 이자(`INTEREST_ACCRUAL`)·기지급 회차는 불변.
- `REPAYMENT_SCHEDULE` 재생성은 **기존 행 삭제 금지** — `rsch_version_cd` 로 버전 분리, 구버전은 `SUPERSEDED` 처리.
- 변경 사유는 `rate_change_reason_cd` (CODE) 로 분류: 기준금리리셋 / 우대조건충족·실효 / 연체가산금리 부과·해소 / 상품정책변경.
- 연체가산금리(`DELINQUENCY.overdue_rate_bps`) 는 `RATE_CHANGE_HISTORY` 에는 기록하지 않고 연체 라이프사이클에서만 관리(상품정책에 따라 다름 — [검토필요]).

---

## 3. 운영 규칙 (흐름 관점)

1. **상태 변경 = STATUS_HISTORY append.** 코드 분기 없이 일관 처리.
2. **외부 호출 = idempotency_key 또는 external_tx_no.** 재시도/중복호출 무해성 보장.
3. **append-only 테이블** (`INTEREST_ACCRUAL`, `RATE_CHANGE_HISTORY`, `DELINQUENCY_DAILY_SNAPSHOT`, `REVIEW_CHECK_LOG`, `STATUS_HISTORY`) 은 수정·삭제 금지. 정정은 신규 row + 역분개(`reversal_yn`)로.
4. **스케줄 재생성은 버전 분리.** `rsch_version_cd` 로 비교/감사 가능하게.
5. **회계 분개 순서는 상품정책으로 통제.** 코드에 하드코딩 금지.
6. **일배치 멱등성.** `(cntr_id, due_date)` 기준 매칭, 이미 `PAID/SUCCESS` 회차는 스킵.

> [검토필요]
> - 승인 유효기간(현재 가이드 14일)을 상품별로 차등할지 (`LOAN_PRODUCT` 컬럼 추가 여부)
> - 연체가산금리 부과를 `RATE_CHANGE_HISTORY` 와 통합 관리할지, `DELINQUENCY` 전속으로 둘지
> - 자동이체 실패 시 재출금(retry) 규칙(횟수·간격)을 상품정책으로 분리할지, 운영 파라미터로 둘지
