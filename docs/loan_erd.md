# 🏛 LON 여신계 ERD

> 본 문서는 한국 은행권 **여신계(Loan)** 도메인의 데이터 모델을 정의한다.
> 표기·명명·코드 정책은 [data_dictionary.md](./data_dictionary.md) 및 STANDARDS.md 를 따른다.
>
> 본 ERD 는 **여신계 11개 테이블**(녹색)만 다루며, 공통계 테이블은 [부속물 1](#부속물-1-공통계-연계) 의 연계 컬럼만 표기한다.

## 컬럼 표기 규칙 (Mermaid 내부)

- 형식: `<TYPE> <물리명> <PK|FK|UK> "한글논리명"`
- 코드 컬럼: `VARCHAR xxx_cd FK "...코드(CODE)"` — 모두 `CODE_MASTER` FK
- 금액: `BIGINT` 또는 `NUMERIC(19,4)` · 금리/비율: `INT bps` 또는 `NUMERIC(5,2)`
- 감사 컬럼: 모든 등록계 테이블 말미에 `created_at / created_by / updated_at / updated_by / deleted_at / deleted_by` 포함

---

## 0. 전체 도메인 한눈에 보기

```mermaid
erDiagram
    LOAN_PRODUCT ||--o{ LOAN_APPLICATION : "상품 신청"
    LOAN_APPLICATION ||--|| LOAN_REVIEW : "심사"
    LOAN_REVIEW ||--|| LOAN_CONTRACT : "심사 후 약정"
    LOAN_APPLICATION ||--|| LOAN_CONTRACT : "신청-약정"
    LOAN_PRODUCT ||--o{ LOAN_REVIEW : "상품"
    LOAN_PRODUCT ||--o{ LOAN_CONTRACT : "상품"
    LOAN_CONTRACT ||--|| LOAN_ACCOUNT : "여신계좌"
    LOAN_CONTRACT ||--o{ REPAYMENT_SCHEDULE : "상환스케줄"
    LOAN_CONTRACT ||--o{ LOAN_EXECUTION : "대출실행"
    LOAN_CONTRACT ||--o{ LOAN_REPAYMENT : "대출상환"
    LOAN_CONTRACT ||--o{ LOAN_DELINQUENCY : "연체"
    LOAN_CONTRACT ||--|| LOAN_CLOSURE : "약정종료"
    LOAN_ACCOUNT ||--o{ LOAN_EXECUTION : "여신계좌"
    LOAN_ACCOUNT ||--o{ LOAN_REPAYMENT : "여신계좌"
    LOAN_ACCOUNT ||--o{ LOAN_DELINQUENCY : "여신계좌"
    LOAN_DELINQUENCY ||--o{ LOAN_CREDIT_REPORT : "신용정보신고"
```

---

# 단계별 상세 ERD

## STAGE 1. 여신상품 (LOAN_PRODUCT)

> 공통 `common_product.product_id` 와 동일 키. 여신 특화 속성만 보유.

```mermaid
erDiagram
    LOAN_PRODUCT {
        BIGINT product_id PK "상품ID"
        VARCHAR loan_purpose_cd "자금용도코드"
        VARCHAR collateral_type_cd "담보유형코드"
        VARCHAR repayment_method_cd "가능상환방식코드"
        VARCHAR guarantee_type_cd "보증유형코드"
        VARCHAR rate_type "금리유형코드"
        INT display_min_rate_bps "표시최저금리(bps)"
        INT display_max_rate_bps "표시최고금리(bps)"
        BOOLEAN collateral_required_yn "담보필수여부"
        BOOLEAN guarantee_required_yn "보증필수여부"
        BOOLEAN early_repay_fee_yn "중도상환수수료여부"
        BOOLEAN holiday_repay_target_yn "휴일상환대상여부"
        VARCHAR loan_product_status_cd "여신상품상태코드"
        TIMESTAMP created_at
        BIGINT created_by
        TIMESTAMP updated_at
        BIGINT updated_by
        TIMESTAMP deleted_at
        BIGINT deleted_by
    }
```

---

## STAGE 2. 신청·심사·계약

```mermaid
erDiagram
    LOAN_APPLICATION {
        BIGINT application_id PK "대출신청ID"
        VARCHAR application_no "대출신청번호"
        BIGINT customer_id "고객ID"
        VARCHAR apply_channel_cd "신청채널코드"
        VARCHAR application_branch_id "신청지점코드"
        BIGINT application_charge_id "담당자ID"
        BIGINT req_amt "대출신청금액"
        INT request_period_mo "대출신청기간"
        VARCHAR purpose_cd "자금용도코드"
        VARCHAR repayment_method_cd "상환방식코드"
        CHAR district_disbursement_date "대출모집실행일자"
        VARCHAR reject_reason "거절사유"
        VARCHAR reject_reason_cd "거절코드"
        TIMESTAMP applied_at "대출신청일"
        TIMESTAMP completed_at "완료일자"
        VARCHAR apply_status_cd "대출신청상태"
        TIMESTAMP created_at
        BIGINT created_by
        TIMESTAMP updated_at
        BIGINT updated_by
        TIMESTAMP deleted_at
        BIGINT deleted_by
        BIGINT product_id FK "상품ID"
    }

    LOAN_REVIEW {
        BIGINT loan_review_id PK "대출심사번호"
        BIGINT application_id FK "대출신청ID"
        VARCHAR review_target_cd "심사대상코드"
        INT review_round "심사회차"
        BIGINT approved_amount "승인금액"
        INT approved_rate_bps "승인금리(bps)"
        VARCHAR review_method_cd "심사방식코드"
        BIGINT reviewer_id "심사자ID"
        BIGINT approver_id "승인자ID"
        VARCHAR loan_review_round_cd "대출심사회차코드"
        VARCHAR review_opinion_cd "심사의견코드"
        VARCHAR review_opinion_reason_cd "심사의견사유코드"
        VARCHAR approval_decision_cd "결재결정코드"
        VARCHAR approval_decision_reason_cd "결재결정사유코드"
        CHAR assigned_at "심사배정일"
        CHAR started_at "심사시작일"
        CHAR decided_at "심사결정일"
        VARCHAR review_status_cd "대출심사상태"
        TIMESTAMP created_at
        BIGINT created_by
        TIMESTAMP updated_at
        BIGINT updated_by
        TIMESTAMP deleted_at
        BIGINT deleted_by
        BIGINT product_id FK "상품ID"
    }

    LOAN_CONTRACT {
        BIGINT contract_id PK "계약ID"
        BIGINT loan_review_id FK "대출심사ID"
        BIGINT loan_application_id FK "대출신청ID"
        BIGINT contractor_id "대출계약자ID"
        VARCHAR loan_product_name "대출상품명"
        VARCHAR contractor_name "계약자성명"
        VARCHAR facility_type_cd "대출시설유형코드"
        VARCHAR repayment_method_cd "상환방식코드"
        VARCHAR allocation_policy_cd "일부상환충당코드"
        VARCHAR prepayment_recalc_mode_cd "중도상환재산정모드코드"
        TIMESTAMP created_at
        BIGINT created_by
        TIMESTAMP updated_at
        BIGINT updated_by
        TIMESTAMP deleted_at
        BIGINT deleted_by
        BIGINT product_id FK "상품ID"
    }

    LOAN_PRODUCT ||--o{ LOAN_APPLICATION : "상품 신청"
    LOAN_PRODUCT ||--o{ LOAN_REVIEW : "상품"
    LOAN_PRODUCT ||--o{ LOAN_CONTRACT : "상품"
    LOAN_APPLICATION ||--|| LOAN_REVIEW : "심사"
    LOAN_REVIEW ||--|| LOAN_CONTRACT : "심사 후 약정"
    LOAN_APPLICATION ||--|| LOAN_CONTRACT : "신청-약정"
```

---

## STAGE 3. 여신계좌·상환스케줄

```mermaid
erDiagram
    LOAN_ACCOUNT {
        BIGINT account_id PK "계좌ID"
        BIGINT account_no "계좌번호"
        BIGINT customer_id "고객ID"
        VARCHAR loan_account_type_cd "여신계좌유형코드"
        VARCHAR purpose_cd "계좌용도코드"
        BIGINT unpaid_balance "미수합계"
        BIGINT principal_balance "원금잔액"
        BIGINT loan_balance "대출잔액"
        CHAR valid_from "유효시작일"
        VARCHAR loan_account_status "여신계좌상태"
        TIMESTAMP created_at
        BIGINT created_by
        TIMESTAMP updated_at
        BIGINT updated_by
        TIMESTAMP deleted_at
        BIGINT deleted_by
        BIGINT contract_id FK "계약ID"
        BIGINT product_id FK "상품ID"
    }

    REPAYMENT_SCHEDULE {
        BIGINT contract_id FK "계약ID"
        INT installment_no "상환회차"
        CHAR due_date "상환예정일자"
        BIGINT scheduled_principal "예정상환원금"
        BIGINT scheduled_interest "예정상환이자"
        BIGINT scheduled_total "예정상환합계"
        BIGINT remaining_balance "예정상환원금잔액"
        INT applied_rate_bps "상환스케줄적용금리(bps)"
        VARCHAR schedule_status_cd "상환스케줄상태코드"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        VARCHAR schedule_version "버전"
    }

    LOAN_CONTRACT ||--|| LOAN_ACCOUNT : "여신계좌"
    LOAN_CONTRACT ||--o{ REPAYMENT_SCHEDULE : "상환 스케줄"
```

---

## STAGE 4. 대출실행 (LOAN_EXECUTION)

```mermaid
erDiagram
    LOAN_EXECUTION {
        BIGINT execution_id PK "대출실행번호"
        BIGINT disbursement_account_id "대출입금계좌ID"
        BIGINT payment_tx_id FK "이체거래ID"
        BIGINT disbursement_amount "대출실행금액"
        VARCHAR masked_account_no "마스킹계좌번호"
        INT tranche_no "트랜치회차"
        TIMESTAMP value_date_at "자금기산일시"
        VARCHAR idempotency_key "멱등키"
        VARCHAR loan_exec_status_cd "대출실행상태코드"
        VARCHAR loan_exec_fail_reason_cd "대출실행실패사유코드"
        VARCHAR loan_exec_fail_reason_detail "대출실행실패사유내용"
        TIMESTAMP requested_at "요청일시"
        TIMESTAMP executed_at "실행일시"
        CHAR reversal_yn "역분개여부"
        BIGINT reversed_execution_id "역분개대상실행ID"
        VARCHAR reversal_reason_cd "역분개사유코드"
        TIMESTAMP reversed_at "역분개일시"
        TIMESTAMP created_at
        BIGINT created_by
        TIMESTAMP updated_at
        BIGINT updated_by
        TIMESTAMP deleted_at
        BIGINT deleted_by
        BIGINT contract_id FK "계약ID"
        BIGINT product_id FK "상품ID"
        BIGINT account_id FK "계좌ID"
    }

    LOAN_CONTRACT ||--o{ LOAN_EXECUTION : "대출실행"
    LOAN_ACCOUNT ||--o{ LOAN_EXECUTION : "여신계좌"
    LOAN_PRODUCT ||--o{ LOAN_EXECUTION : "상품"
```

> `payment_tx_id` 는 공통 `거래내역(common_transaction).거래ID` 를 참조한다.

---

## STAGE 5. 대출상환 (LOAN_REPAYMENT)

```mermaid
erDiagram
    LOAN_REPAYMENT {
        BIGINT repayment_id PK "상환번호"
        VARCHAR tx_type_cd "상환거래유형코드"
        VARCHAR payment_channel_cd "결제채널코드"
        VARCHAR idempotency_key "멱등키"
        BIGINT payment_tx_id FK "결제거래ID"
        NUMERIC paid_principal "원금납입액"
        NUMERIC paid_interest "이자납입액"
        NUMERIC paid_late_fee "연체이자납입액"
        NUMERIC total_paid "총납입액"
        BIGINT loan_account_id "출금계좌ID"
        BIGINT virtual_account_id "가상계좌ID"
        VARCHAR sender_name "송금자명"
        CHAR third_party_yn "제3자납부여부"
        VARCHAR repayment_status_cd "상환상태"
        VARCHAR loan_repayment_fail_reason_cd "상환실패사유코드"
        TEXT loan_repayment_fail_reason_detail "상환실패사유내용"
        TIMESTAMP requested_repayment_at "상환요청일시"
        BIGINT reverses_repayment_id "역분개대상상환ID"
        BOOLEAN reversal_yn "역분개여부"
        VARCHAR reversal_reason_cd "역분개사유코드"
        TIMESTAMP reversal_at "역분개일시"
        CHAR requested_at "요청일자"
        CHAR paid_at "처리일자"
        CHAR value_date "이자기산일자"
        TIMESTAMP created_at
        BIGINT created_by
        TIMESTAMP updated_at
        BIGINT updated_by
        TIMESTAMP deleted_at
        BIGINT deleted_by
        BIGINT party_id "당사자ID"
        BIGINT contract_id FK "계약ID"
        BIGINT product_id FK "상품ID"
        BIGINT account_id FK "계좌ID"
    }

    LOAN_CONTRACT ||--o{ LOAN_REPAYMENT : "대출상환"
    LOAN_ACCOUNT ||--o{ LOAN_REPAYMENT : "여신계좌"
    LOAN_PRODUCT ||--o{ LOAN_REPAYMENT : "상품"
```

> `payment_tx_id` 는 공통 `거래내역(common_transaction).거래ID` 를 참조한다.

---

## STAGE 6. 연체·신용정보신고

```mermaid
erDiagram
    LOAN_DELINQUENCY {
        BIGINT delinquency_id PK "연체ID"
        CHAR delinquency_start_date "연체시작일"
        CHAR delinquency_end_date "연체종료일"
        INT overdue_days "연체일수"
        BIGINT overdue_principal "연체원금"
        BIGINT overdue_interest "연체이자"
        VARCHAR delinquency_stage_cd "연체단계코드"
        NUMERIC penalty_rate_bps "연체이자율(bps)"
        VARCHAR delinquency_status_cd "연체상태코드"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        BIGINT party_id "당사자ID"
        BIGINT contract_id FK "계약ID"
        BIGINT account_id FK "계좌ID"
    }

    LOAN_CREDIT_REPORT {
        BIGINT credit_report_id PK "여신계신용정보신고ID"
        BIGINT customer_id "고객ID"
        BIGINT delinquency_id FK "연체ID"
        VARCHAR bureau_cd "신고기관코드"
        VARCHAR external_ref_no "신고기관번호"
        VARCHAR report_type_cd "신고유형코드"
        VARCHAR report_reason_cd "신고사유코드"
        CHAR report_date "신고등록일"
        CHAR release_date "신고해제일자"
        BIGINT report_amount "신고금액"
        VARCHAR credit_report_status "신고상태"
        BIGINT created_by
        TIMESTAMPTZ created_at
        BIGINT updated_by
        TIMESTAMPTZ updated_at
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        BIGINT contract_id FK "계약ID"
        BIGINT product_id FK "상품ID"
        BIGINT account_id FK "계좌ID"
    }

    LOAN_CONTRACT ||--o{ LOAN_DELINQUENCY : "연체"
    LOAN_ACCOUNT ||--o{ LOAN_DELINQUENCY : "여신계좌"
    LOAN_DELINQUENCY ||--o{ LOAN_CREDIT_REPORT : "신용정보신고"
```

---

## STAGE 7. 약정종료 (LOAN_CLOSURE)

```mermaid
erDiagram
    LOAN_CLOSURE {
        BIGINT closure_id PK "계약종료ID"
        BIGINT contract_id FK "계약ID"
        VARCHAR closure_reason_cd "종료사유코드"
        VARCHAR closure_reason "종료사유설명"
        CHAR closure_date "종료일자"
        BIGINT loan_principal "최종원금잔액"
        BIGINT loan_interest "최종이자합계"
        BIGINT loan_fee "최종수수료합계"
        BIGINT residual_amount "잔여채권금액"
        CHAR written_off_yn "미상환채권처리여부"
        BIGINT written_off_amount "미상환채권금액"
        TIMESTAMPTZ closed_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
    }

    LOAN_CONTRACT ||--|| LOAN_CLOSURE : "약정종료"
```

---

# 부속물 1. 공통계 연계

| LON 테이블 | 공통계 참조 | 연계 컬럼 | 비고 |
|---|---|---|---|
| LOAN_PRODUCT | common_product | product_id | 공통 상품 마스터 (1:1) |
| LOAN_CONTRACT | common_contract | contract_id | 공통 계약 마스터 |
| LOAN_ACCOUNT | common_account | account_id | 공통 계좌 마스터 |
| LOAN_EXECUTION | 거래내역(common_transaction) | payment_tx_id | 이체거래 |
| LOAN_REPAYMENT | 거래내역(common_transaction) | payment_tx_id | 결제거래 |
