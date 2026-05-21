# 🏛 LON 여신계 ERD

> 본 문서는 한국 은행권 **여신계(Loan)** 도메인의 데이터 모델을 정의한다.
> 표기·명명·코드 정책은 [data_dictionary.md](./data_dictionary.md) 및 STANDARDS.md 를 따른다.

## 컬럼 표기 규칙 (Mermaid 내부)

- 형식: `<TYPE> <물리명> <PK|FK|UK|없음> "한글논리명"`
- 코드 컬럼: `VARCHAR xxx_cd FK "...코드(CODE)"` — 모두 `CODE_MASTER` FK
- 암호화 컬럼: `BYTEA xxx_enc "...(암호화)"`
- 해시/마스킹: `_hash` / `_masked` 접미사
- 금액: `BIGINT`(원 단위) · 금리/비율: `INT bps` 또는 `DECIMAL(5,4)`
- 공통 감사 컬럼: 모든 테이블 말미에 `created_at / created_by / updated_at / updated_by / deleted_at / deleted_by / version` 순서로 포함
- 이력·스냅샷 테이블은 **append-only** (Soft Delete 대상 아님 — 감사 컬럼은 등록계 4개만)

---

## 0. 전체 도메인 한눈에 보기 (Big Picture)

```mermaid
erDiagram
    CODE_MASTER ||--o{ STATUS_HISTORY : "상태값 참조"
    LOAN_PRODUCT ||--o{ LOAN_APPLICATION : "상품 신청"
    LOAN_APPLICATION ||--|| LOAN_PRESCREENING : "가심사"
    LOAN_APPLICATION ||--o{ CREDIT_CONSENT : "신용정보동의"
    LOAN_APPLICATION ||--o{ LOAN_IDENTITY_VERIFICATION : "본인확인"
    LOAN_APPLICATION ||--o{ LOAN_DOCUMENT : "서류제출"
    LOAN_DOCUMENT ||--o{ LOAN_DOCUMENT_OCR : "OCR추출"
    LOAN_APPLICATION ||--o{ GUARANTOR_AGREEMENT : "보증약정"
    GUARANTOR_MASTER ||--o{ GUARANTOR_AGREEMENT : "보증인약정"
    LOAN_APPLICATION ||--o{ COLLATERAL : "담보등록"
    COLLATERAL ||--o{ COLLATERAL_EVALUATION : "담보평가"
    COLLATERAL ||--o{ LTV_CALCULATION : "LTV산출"
    LOAN_APPLICATION ||--|| CREDIT_EVALUATION : "신용평가"
    LOAN_APPLICATION ||--|| DSR_CALCULATION : "DSR산출"
    LOAN_APPLICATION ||--|| LOAN_REVIEW : "본심사"
    LOAN_REVIEW ||--o{ REVIEW_CHECK_LOG : "심사점검"
    LOAN_REVIEW ||--|| LOAN_CONTRACT : "약정체결"
    LOAN_CONTRACT ||--|| REPAYMENT_ACCOUNT : "상환계좌"
    LOAN_CONTRACT ||--|| LOAN_EXECUTION : "대출실행"
    LOAN_CONTRACT ||--o{ GUARANTEE_INSURANCE : "보증보험"
    LOAN_CONTRACT ||--o{ REPAYMENT_SCHEDULE : "상환스케줄"
    LOAN_CONTRACT ||--o{ INTEREST_ACCRUAL : "이자발생"
    LOAN_CONTRACT ||--o{ REPAYMENT_TRANSACTION : "상환거래"
    LOAN_CONTRACT ||--o{ RATE_CHANGE_HISTORY : "금리변경"
    LOAN_CONTRACT ||--|| MATURITY : "만기관리"
    LOAN_CONTRACT ||--o{ DELINQUENCY : "연체발생"
    DELINQUENCY ||--o{ DELINQUENCY_DAILY_SNAPSHOT : "일별스냅샷"
    LOAN_CONTRACT ||--o{ CREDIT_INFO_REPORT : "신용정보신고"
    LOAN_CONTRACT ||--|| LOAN_CLOSURE : "약정종료"
    LOAN_CONTRACT ||--o{ LOAN_CERTIFICATE : "증명서발급"
    BUSINESS_CALENDAR ||--o{ INTEREST_ACCRUAL : "영업일적용"
```

> 박스 다이어그램(컬럼 포함)은 도메인 양이 매우 크므로 아래 **단계별 ERD** 에서 상세히 정의한다.

---

# 단계별 상세 ERD

## STAGE 1. 공통·상품 그룹

> CODE_MASTER, STATUS_HISTORY, BUSINESS_CALENDAR, LOAN_PRODUCT

```mermaid
erDiagram
    CODE_MASTER {
        BIGINT code_id PK "코드ID"
        VARCHAR code_group_cd UK "코드그룹코드"
        VARCHAR code_cd UK "코드값"
        VARCHAR code_name "코드명"
        VARCHAR code_desc "코드설명"
        INT sort_no "정렬순서"
        CHAR active_yn "활성여부"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        INT version
    }

    STATUS_HISTORY {
        BIGINT sthist_id PK "상태이력ID"
        VARCHAR target_table_cd FK "대상테이블코드(CODE)"
        BIGINT target_id "대상식별번호"
        VARCHAR before_status_cd FK "이전상태코드(CODE)"
        VARCHAR after_status_cd FK "변경후상태코드(CODE)"
        VARCHAR change_reason_cd FK "변경사유코드(CODE)"
        VARCHAR change_remark "상태변경비고"
        TIMESTAMPTZ changed_at "상태변경일시"
        BIGINT changed_by "상태변경자ID"
        TIMESTAMPTZ created_at
        BIGINT created_by
    }

    BUSINESS_CALENDAR {
        BIGINT cal_id PK "영업일ID"
        VARCHAR cal_date UK "일자(YYYYMMDD)"
        CHAR business_day_yn "영업일여부"
        VARCHAR holiday_type_cd FK "휴일유형코드(CODE)"
        VARCHAR holiday_name "휴일명"
        VARCHAR base_country_cd FK "기준국가코드(CODE)"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        INT version
    }

    LOAN_PRODUCT {
        BIGINT prod_id PK "상품ID"
        BIGINT product_id FK "공통상품ID"
        VARCHAR prod_cd UK "상품번호"
        VARCHAR prod_name "상품명"
        VARCHAR loan_type_cd FK "대출유형코드(CODE)"
        VARCHAR target_customer_cd FK "대상고객코드(CODE)"
        VARCHAR repayment_method_cd FK "상환방식코드(CODE)"
        VARCHAR rate_type_cd FK "금리유형코드(CODE)"
        INT base_rate_bps "기본금리(bps)"
        INT min_rate_bps "최저금리(bps)"
        INT max_rate_bps "최고금리(bps)"
        BIGINT min_amount "최소대출금액"
        BIGINT max_amount "최대대출금액"
        INT min_period_mo "최소대출기간"
        INT max_period_mo "최대대출기간"
        CHAR collateral_required_yn "담보필수여부"
        CHAR guarantor_required_yn "보증인필수여부"
        VARCHAR sale_start_date "상품판매시작일자"
        VARCHAR sale_end_date "상품판매종료일자"
        VARCHAR prod_status_cd FK "상품상태코드(CODE)"
        VARCHAR prod_terms_url "상품설명서URL"
        VARCHAR prod_terms_hash "상품설명서해시"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        INT version
    }

    CODE_MASTER ||--o{ STATUS_HISTORY : "상태값 참조"
    CODE_MASTER ||--o{ BUSINESS_CALENDAR : "휴일유형 참조"
    CODE_MASTER ||--o{ LOAN_PRODUCT : "코드 참조"
```

---

## STAGE 2. 신청·가심사·동의·본인확인

```mermaid
erDiagram
    LOAN_APPLICATION {
        BIGINT appl_id PK "대출신청ID"
        VARCHAR appl_no UK "대출신청노출번호"
        BIGINT customer_id FK "고객ID"
        BIGINT prod_id FK "상품ID"
        VARCHAR channel_cd FK "신청채널코드(CODE)"
        BIGINT requested_amount "희망대출금액"
        INT requested_period_mo "희망대출기간"
        VARCHAR loan_purpose_cd FK "대출목적코드(CODE)"
        VARCHAR repayment_method_cd FK "상환방식코드(CODE)"
        BIGINT estimated_income_amt "추정연소득금액"
        VARCHAR employment_type_cd FK "직업유형코드(CODE)"
        VARCHAR appl_status_cd FK "대출신청상태코드(CODE)"
        TIMESTAMPTZ applied_at "대출신청일시"
        VARCHAR client_ip "신청IP"
        VARCHAR device "신청디바이스"
        VARCHAR idempotency_key UK "멱등성키"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        INT version
    }

    LOAN_PRESCREENING {
        BIGINT presc_id PK "가심사ID"
        BIGINT appl_id FK "대출신청ID"
        VARCHAR presc_result_cd FK "가심사결과코드(CODE)"
        BIGINT estimated_limit_amt "예상대출한도금액"
        INT estimated_rate_bps "예상적용금리(bps)"
        VARCHAR estimated_grade "추정신용등급"
        INT estimated_score "추정신용점수"
        VARCHAR reject_reason_cd FK "거절사유코드(CODE)"
        VARCHAR presc_remark "가심사비고"
        TIMESTAMPTZ prescreened_at "가심사일시"
        VARCHAR presc_engine_version "가심사엔진버전"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        INT version
    }

    CREDIT_CONSENT {
        BIGINT csnt_id PK "신용정보동의ID"
        BIGINT appl_id FK "대출신청ID"
        BIGINT customer_id FK "고객ID"
        VARCHAR consent_type_cd FK "동의유형코드(CODE)"
        VARCHAR consent_scope_cd FK "동의권한코드(CODE)"
        VARCHAR consent_target_cd FK "동의대상기관코드(CODE)"
        CHAR consent_yn "동의여부"
        TIMESTAMPTZ consented_at "동의일시"
        VARCHAR consent_method_cd FK "동의방식코드(CODE)"
        VARCHAR consent_token "멱등성토큰"
        VARCHAR signed_doc_url "서명동의서URL"
        VARCHAR signed_doc_hash "서명동의서해시"
        VARCHAR client_ip "동의IP"
        VARCHAR device "동의디바이스"
        VARCHAR retention_until "보관만료일자"
        CHAR withdrawn_yn "철회여부"
        TIMESTAMPTZ withdrawn_at "동의철회일시"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        INT version
    }

    LOAN_IDENTITY_VERIFICATION {
        BIGINT idv_id PK "본인확인ID"
        BIGINT appl_id FK "대출신청ID"
        BIGINT customer_id FK "고객ID"
        VARCHAR idv_method_cd FK "본인확인방식코드(CODE)"
        VARCHAR idv_status_cd FK "본인확인상태코드(CODE)"
        VARCHAR idv_result_cd FK "본인확인결과코드(CODE)"
        VARCHAR idv_target_cd FK "본인확인대상코드(CODE)"
        VARCHAR ci_hash "CI해시"
        VARCHAR di_hash "DI해시"
        BYTEA mobile_no_enc "휴대전화번호(암호화)"
        VARCHAR mobile_no_masked "휴대전화번호마스킹"
        TIMESTAMPTZ verified_at "본인확인일시"
        VARCHAR client_ip "본인확인IP"
        VARCHAR device "본인확인디바이스"
        VARCHAR external_tx_no "외부거래번호"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        INT version
    }

    LOAN_APPLICATION ||--|| LOAN_PRESCREENING : "1:1 가심사"
    LOAN_APPLICATION ||--o{ CREDIT_CONSENT : "동의이력"
    LOAN_APPLICATION ||--o{ LOAN_IDENTITY_VERIFICATION : "본인확인"
    LOAN_PRODUCT ||--o{ LOAN_APPLICATION : "상품 신청"
```

---

## STAGE 3. 서류·OCR

```mermaid
erDiagram
    LOAN_DOCUMENT {
        BIGINT doc_id PK "서류ID"
        BIGINT appl_id FK "대출신청ID"
        VARCHAR doc_type_cd FK "서류유형코드(CODE)"
        VARCHAR doc_status_cd FK "서류상태코드(CODE)"
        VARCHAR doc_source_cd FK "서류출처코드(CODE)"
        VARCHAR doc_name "서류명"
        VARCHAR doc_url "서류저장경로"
        VARCHAR doc_hash "서류콘텐츠해시"
        VARCHAR mime_type "MIME유형"
        BIGINT file_size_bytes "파일크기(B)"
        TIMESTAMPTZ submitted_at "서류제출일시"
        TIMESTAMPTZ verified_at "서류검증일시"
        VARCHAR verify_result_cd FK "서류검증결과코드(CODE)"
        VARCHAR retention_until "보관만료일자"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        INT version
    }

    LOAN_DOCUMENT_OCR {
        BIGINT ocr_id PK "OCRID"
        BIGINT doc_id FK "서류ID"
        VARCHAR ocr_engine "OCR엔진"
        VARCHAR ocr_engine_version "OCR엔진버전"
        VARCHAR ocr_status_cd FK "OCR상태코드(CODE)"
        DECIMAL ocr_confidence "OCR신뢰도"
        JSONB extracted_fields "추출필드JSON"
        TEXT extracted_text "추출원문"
        TIMESTAMPTZ ocr_at "OCR실행일시"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        INT version
    }

    LOAN_APPLICATION ||--o{ LOAN_DOCUMENT : "서류제출"
    LOAN_DOCUMENT ||--o{ LOAN_DOCUMENT_OCR : "OCR추출"
```

---

## STAGE 4. 보증·담보·LTV

```mermaid
erDiagram
    GUARANTOR_MASTER {
        BIGINT gmst_id PK "보증인ID"
        BYTEA guarantor_name_enc "보증인성명(암호화)"
        VARCHAR guarantor_name_masked "보증인성명마스킹"
        VARCHAR guarantor_ci_hash "보증인CI해시"
        VARCHAR relation_type_cd FK "보증인관계유형코드(CODE)"
        BYTEA mobile_no_enc "휴대전화번호(암호화)"
        VARCHAR mobile_no_masked "휴대전화번호마스킹"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        INT version
    }

    GUARANTOR_AGREEMENT {
        BIGINT gagr_id PK "보증계약ID"
        BIGINT appl_id FK "대출신청ID"
        BIGINT gmst_id FK "보증인ID"
        VARCHAR gagr_type_cd FK "보증유형코드(CODE)"
        BIGINT guarantee_amount "보증금액"
        INT guarantee_ratio_bps "보증비율(bps)"
        VARCHAR gagr_status_cd FK "보증약정상태코드(CODE)"
        TIMESTAMPTZ consented_at "보증동의일시"
        VARCHAR signed_doc_url "보증서명동의서URL"
        VARCHAR signed_doc_hash "보증서명동의서해시"
        VARCHAR client_ip "보증동의IP"
        VARCHAR device "보증동의디바이스"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        INT version
    }

    COLLATERAL {
        BIGINT col_id PK "담보ID"
        BIGINT appl_id FK "대출신청ID"
        VARCHAR col_type_cd FK "담보유형코드(CODE)"
        VARCHAR col_status_cd FK "담보상태코드(CODE)"
        VARCHAR col_no UK "담보노출번호"
        VARCHAR col_name "담보명"
        VARCHAR col_address "담보소재지"
        VARCHAR col_registry_no "담보등기번호"
        BIGINT declared_value "신고담보가액"
        VARCHAR currency_cd FK "통화코드(CODE)"
        VARCHAR ownership_type_cd FK "소유유형코드(CODE)"
        VARCHAR senior_lien_yn "선순위권리여부"
        BIGINT senior_lien_amount "선순위권리금액"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        INT version
    }

    COLLATERAL_EVALUATION {
        BIGINT ceval_col_id PK "담보평가ID"
        BIGINT col_id FK "담보ID"
        VARCHAR eval_method_cd FK "평가방식코드(CODE)"
        VARCHAR eval_agency_cd FK "평가기관코드(CODE)"
        BIGINT appraised_value "감정평가금액"
        BIGINT applied_value "적용담보가액"
        VARCHAR eval_status_cd FK "담보평가상태코드(CODE)"
        VARCHAR eval_report_url "감정평가서URL"
        VARCHAR eval_report_hash "감정평가서해시"
        TIMESTAMPTZ evaluated_at "담보평가일시"
        VARCHAR applied_start_date "적용시작일자"
        VARCHAR applied_end_date "적용종료일자"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        INT version
    }

    LTV_CALCULATION {
        BIGINT ltv_id PK "LTVID"
        BIGINT appl_id FK "대출신청ID"
        BIGINT col_id FK "담보ID"
        BIGINT applied_col_value "적용담보가액"
        BIGINT senior_lien_amount "선순위권리금액"
        BIGINT requested_amount "희망대출금액"
        INT ltv_ratio_bps "LTV비율(bps)"
        INT ltv_limit_bps "LTV한도비율(bps)"
        BIGINT max_loan_amount "최대대출가능금액"
        VARCHAR ltv_status_cd FK "LTV판정상태코드(CODE)"
        TIMESTAMPTZ calculated_at "LTV산출일시"
        VARCHAR calc_engine_version "산출엔진버전"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        INT version
    }

    GUARANTOR_MASTER ||--o{ GUARANTOR_AGREEMENT : "보증인약정"
    LOAN_APPLICATION ||--o{ GUARANTOR_AGREEMENT : "보증약정"
    LOAN_APPLICATION ||--o{ COLLATERAL : "담보등록"
    COLLATERAL ||--o{ COLLATERAL_EVALUATION : "담보평가"
    COLLATERAL ||--o{ LTV_CALCULATION : "LTV산출"
```

---

## STAGE 5. 신용평가·DSR·심사

```mermaid
erDiagram
    CREDIT_EVALUATION {
        BIGINT ceval_id PK "신용평가ID"
        BIGINT appl_id FK "대출신청ID"
        BIGINT customer_id FK "고객ID"
        VARCHAR ceval_engine "신용평가엔진"
        VARCHAR ceval_engine_version "신용평가엔진버전"
        VARCHAR ceval_grade "신용등급"
        INT ceval_score "신용점수"
        INT pd_bps "부도확률(bps)"
        VARCHAR ceval_decision_cd FK "신용평가결정코드(CODE)"
        BIGINT eval_limit_amount "신용평가한도금액"
        INT eval_rate_bps "신용평가기준금리(bps)"
        VARCHAR ceval_status_cd FK "신용평가상태코드(CODE)"
        JSONB ceval_factors "평가인자JSON"
        TIMESTAMPTZ evaluated_at "신용평가일시"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        INT version
    }

    DSR_CALCULATION {
        BIGINT dsr_id PK "DSRID"
        BIGINT appl_id FK "대출신청ID"
        BIGINT customer_id FK "고객ID"
        BIGINT annual_income_amt "연소득금액"
        BIGINT existing_principal_total "기존대출원금합계"
        BIGINT existing_annual_repay_amt "기존연간상환금액"
        BIGINT new_annual_repay_amt "신규연간상환금액"
        BIGINT total_annual_repay_amt "총연간상환금액"
        INT dsr_ratio_bps "DSR비율(bps)"
        INT dsr_limit_bps "DSR한도비율(bps)"
        VARCHAR dsr_status_cd FK "DSR판정상태코드(CODE)"
        VARCHAR dsr_reg_type_cd FK "DSR규제유형코드(CODE)"
        TIMESTAMPTZ calculated_at "DSR산출일시"
        VARCHAR calc_engine_version "산출엔진버전"
        JSONB dsr_detail "DSR산출명세JSON"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        INT version
    }

    LOAN_REVIEW {
        BIGINT rev_id PK "심사ID"
        BIGINT appl_id FK "대출신청ID"
        VARCHAR rev_type_cd FK "심사유형코드(CODE)"
        VARCHAR rev_status_cd FK "심사상태코드(CODE)"
        VARCHAR rev_decision_cd FK "심사결정코드(CODE)"
        BIGINT approved_amount "승인대출금액"
        INT approved_rate_bps "승인적용금리(bps)"
        INT approved_period_mo "승인대출기간"
        VARCHAR reject_reason_cd FK "거절사유코드(CODE)"
        VARCHAR rev_remark "심사비고"
        BIGINT reviewer_id "심사자ID"
        TIMESTAMPTZ reviewed_at "심사일시"
        TIMESTAMPTZ approved_at "승인일시"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        INT version
    }

    REVIEW_CHECK_LOG {
        BIGINT rchk_id PK "심사점검ID"
        BIGINT rev_id FK "심사ID"
        VARCHAR check_item_cd FK "심사점검항목코드(CODE)"
        VARCHAR check_result_cd FK "심사점검결과코드(CODE)"
        VARCHAR check_remark "심사점검비고"
        BIGINT checker_id "점검자ID"
        TIMESTAMPTZ checked_at "점검일시"
        TIMESTAMPTZ created_at
        BIGINT created_by
    }

    LOAN_APPLICATION ||--|| CREDIT_EVALUATION : "신용평가"
    LOAN_APPLICATION ||--|| DSR_CALCULATION : "DSR산출"
    LOAN_APPLICATION ||--|| LOAN_REVIEW : "본심사"
    LOAN_REVIEW ||--o{ REVIEW_CHECK_LOG : "점검항목"
```

---

## STAGE 6. 계약·상환계좌·실행·보증보험

```mermaid
erDiagram
    LOAN_CONTRACT {
        BIGINT cntr_id PK "계약ID"
        VARCHAR cntr_no UK "계약번호"
        BIGINT contract_id FK "공통계약ID"
        BIGINT appl_id FK "대출신청ID"
        BIGINT rev_id FK "심사ID"
        BIGINT customer_id FK "고객ID"
        BIGINT prod_id FK "상품ID"
        BIGINT contracted_amount "약정대출금액"
        VARCHAR currency_cd FK "통화코드(CODE)"
        INT contracted_period_mo "약정대출기간"
        INT total_rate_bps "약정적용금리(bps)"
        INT base_rate_bps "기준금리(bps)"
        INT spread_bps "가산금리(bps)"
        INT preferential_rate_bps "우대금리(bps)"
        VARCHAR rate_type_cd FK "금리유형코드(CODE)"
        VARCHAR repayment_method_cd FK "상환방식코드(CODE)"
        VARCHAR cntr_status_cd FK "계약상태코드(CODE)"
        VARCHAR cntr_start_date "약정시작일자"
        VARCHAR cntr_end_date "약정종료일자"
        VARCHAR cntr_doc_url "약정서URL"
        VARCHAR cntr_doc_hash "약정서해시"
        TIMESTAMPTZ signed_at "약정체결일시"
        VARCHAR client_ip "약정IP"
        VARCHAR device "약정디바이스"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        INT version
    }

    REPAYMENT_ACCOUNT {
        BIGINT racct_id PK "상환계좌ID"
        BIGINT cntr_id FK "계약ID"
        BIGINT account_id FK "공통계좌ID"
        VARCHAR account_no_masked "계좌번호마스킹"
        BYTEA account_no_enc "계좌번호(암호화)"
        VARCHAR bank_cd FK "은행코드(CODE)"
        VARCHAR holder_name_masked "예금주마스킹"
        VARCHAR racct_status_cd FK "상환계좌상태코드(CODE)"
        CHAR auto_debit_yn "자동이체여부"
        INT debit_day "이체기준일"
        TIMESTAMPTZ verified_at "계좌검증일시"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        INT version
    }

    LOAN_EXECUTION {
        BIGINT exec_id PK "대출실행ID"
        BIGINT cntr_id FK "계약ID"
        BIGINT transaction_id FK "공통거래ID"
        BIGINT executed_amount "실행대출금액"
        VARCHAR currency_cd FK "통화코드(CODE)"
        VARCHAR exec_status_cd FK "대출실행상태코드(CODE)"
        VARCHAR disbursement_bank_cd FK "지급은행코드(CODE)"
        BYTEA disbursement_account_enc "지급계좌(암호화)"
        VARCHAR disbursement_account_masked "지급계좌마스킹"
        TIMESTAMPTZ executed_at "대출실행일시"
        VARCHAR value_date "이자기산일자"
        BIGINT fee_amount "실행수수료금액"
        VARCHAR idempotency_key UK "멱등성키"
        VARCHAR journal_entry_no "회계전표번호"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        INT version
    }

    GUARANTEE_INSURANCE {
        BIGINT gins_id PK "보증보험ID"
        BIGINT cntr_id FK "계약ID"
        VARCHAR gins_agency_cd FK "보증보험기관코드(CODE)"
        VARCHAR gins_policy_no UK "보증서노출번호"
        BIGINT guarantee_amount "보증금액"
        INT guarantee_ratio_bps "보증비율(bps)"
        BIGINT premium_amount "보증보험료금액"
        VARCHAR gins_status_cd FK "보증보험상태코드(CODE)"
        VARCHAR gins_start_date "보증시작일자"
        VARCHAR gins_end_date "보증종료일자"
        VARCHAR gins_doc_url "보증서URL"
        VARCHAR gins_doc_hash "보증서해시"
        TIMESTAMPTZ issued_at "보증서발급일시"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        INT version
    }

    LOAN_REVIEW ||--|| LOAN_CONTRACT : "심사 확정 후 약정"
    LOAN_CONTRACT ||--|| REPAYMENT_ACCOUNT : "상환계좌 등록"
    LOAN_CONTRACT ||--|| LOAN_EXECUTION : "대출실행"
    LOAN_CONTRACT ||--o{ GUARANTEE_INSURANCE : "보증보험"
```

---

## STAGE 7. 상환스케줄·이자발생·상환거래·금리변경

```mermaid
erDiagram
    REPAYMENT_SCHEDULE {
        BIGINT rsch_id PK "상환스케줄ID"
        BIGINT cntr_id FK "계약ID"
        INT installment_no "회차"
        VARCHAR due_date "상환예정일자"
        BIGINT scheduled_principal "예정원금금액"
        BIGINT scheduled_interest "예정이자금액"
        BIGINT scheduled_total "예정상환합계금액"
        BIGINT remaining_balance "예정원금잔액"
        INT applied_rate_bps "적용금리(bps)"
        VARCHAR rsch_status_cd FK "상환스케줄상태코드(CODE)"
        VARCHAR rsch_version_cd FK "스케줄버전코드(CODE)"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        INT version
    }

    INTEREST_ACCRUAL {
        BIGINT iacc_id PK "이자발생ID"
        BIGINT cntr_id FK "계약ID"
        VARCHAR accrual_date UK "이자발생일자"
        BIGINT principal_balance "원금잔액"
        INT applied_rate_bps "적용금리(bps)"
        VARCHAR day_count_basis_cd FK "이자계산기준코드(CODE)"
        BIGINT daily_interest_amt "일이자금액"
        BIGINT cumulative_interest_amt "누적이자금액"
        VARCHAR iacc_status_cd FK "이자발생상태코드(CODE)"
        TIMESTAMPTZ accrued_at "이자발생일시"
        TIMESTAMPTZ created_at
        BIGINT created_by
    }

    REPAYMENT_TRANSACTION {
        BIGINT rtx_id PK "상환거래ID"
        BIGINT cntr_id FK "계약ID"
        BIGINT rsch_id FK "상환스케줄ID"
        BIGINT transaction_id FK "공통거래ID"
        VARCHAR rtx_type_cd FK "상환거래유형코드(CODE)"
        BIGINT total_amount "상환합계금액"
        BIGINT principal_amount "상환원금금액"
        BIGINT interest_amount "상환이자금액"
        BIGINT overdue_interest_amount "연체이자금액"
        BIGINT fee_amount "상환수수료금액"
        VARCHAR currency_cd FK "통화코드(CODE)"
        VARCHAR channel_cd FK "상환채널코드(CODE)"
        VARCHAR rtx_status_cd FK "상환거래상태코드(CODE)"
        TIMESTAMPTZ paid_at "상환일시"
        VARCHAR value_date "이자기산일자"
        BIGINT balance_after "상환후원금잔액"
        VARCHAR idempotency_key UK "멱등성키"
        CHAR reversal_yn "역분개여부"
        BIGINT reversal_target_rtx_id "역분개대상ID"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        INT version
    }

    RATE_CHANGE_HISTORY {
        BIGINT rchg_id PK "금리변경ID"
        BIGINT cntr_id FK "계약ID"
        VARCHAR rate_change_reason_cd FK "금리변경사유코드(CODE)"
        INT previous_rate_bps "이전금리(bps)"
        INT new_rate_bps "신규금리(bps)"
        INT base_rate_bps "기준금리(bps)"
        INT spread_bps "가산금리(bps)"
        INT preferential_rate_bps "우대금리(bps)"
        VARCHAR applied_start_date "적용시작일자"
        VARCHAR applied_end_date "적용종료일자"
        TIMESTAMPTZ changed_at "금리변경일시"
        BIGINT changed_by "금리변경자ID"
        TIMESTAMPTZ created_at
        BIGINT created_by
    }

    LOAN_CONTRACT ||--o{ REPAYMENT_SCHEDULE : "상환스케줄"
    LOAN_CONTRACT ||--o{ INTEREST_ACCRUAL : "이자일발생"
    LOAN_CONTRACT ||--o{ REPAYMENT_TRANSACTION : "상환거래"
    REPAYMENT_SCHEDULE ||--o{ REPAYMENT_TRANSACTION : "회차별 상환"
    LOAN_CONTRACT ||--o{ RATE_CHANGE_HISTORY : "금리변경이력"
```

---

## STAGE 8. 만기·연체·신용정보신고

```mermaid
erDiagram
    MATURITY {
        BIGINT mat_id PK "만기ID"
        BIGINT cntr_id FK "계약ID"
        VARCHAR original_maturity_date "최초만기일자"
        VARCHAR current_maturity_date "현재만기일자"
        VARCHAR mat_status_cd FK "만기상태코드(CODE)"
        VARCHAR extension_type_cd FK "기한연장유형코드(CODE)"
        INT extension_count "기한연장횟수"
        VARCHAR last_extended_date "최종기한연장일자"
        INT extended_period_mo "연장기간"
        VARCHAR notice_status_cd FK "만기안내상태코드(CODE)"
        TIMESTAMPTZ last_notice_at "최종만기안내일시"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        INT version
    }

    DELINQUENCY {
        BIGINT dlq_id PK "연체ID"
        BIGINT cntr_id FK "계약ID"
        VARCHAR dlq_status_cd FK "연체상태코드(CODE)"
        VARCHAR dlq_start_date "연체시작일자"
        VARCHAR dlq_end_date "연체해소일자"
        INT dlq_days "연체일수"
        BIGINT dlq_principal_amt "연체원금금액"
        BIGINT dlq_interest_amt "연체이자금액"
        BIGINT dlq_total_amt "연체합계금액"
        INT overdue_rate_bps "연체가산금리(bps)"
        VARCHAR dlq_stage_cd FK "연체단계코드(CODE)"
        TIMESTAMPTZ resolved_at "연체해소일시"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        INT version
    }

    DELINQUENCY_DAILY_SNAPSHOT {
        BIGINT dlqs_id PK "연체일별스냅샷ID"
        BIGINT dlq_id FK "연체ID"
        BIGINT cntr_id FK "계약ID"
        VARCHAR snapshot_date UK "스냅샷일자"
        INT dlq_days "연체일수"
        BIGINT dlq_principal_amt "연체원금금액"
        BIGINT dlq_interest_amt "연체이자금액"
        BIGINT dlq_total_amt "연체합계금액"
        INT overdue_rate_bps "연체가산금리(bps)"
        VARCHAR dlq_stage_cd FK "연체단계코드(CODE)"
        TIMESTAMPTZ snapshotted_at "스냅샷일시"
        TIMESTAMPTZ created_at
        BIGINT created_by
    }

    CREDIT_INFO_REPORT {
        BIGINT crpt_id PK "신용정보신고ID"
        BIGINT cntr_id FK "계약ID"
        BIGINT customer_id FK "고객ID"
        VARCHAR crpt_type_cd FK "신용정보신고유형코드(CODE)"
        VARCHAR crpt_agency_cd FK "신고기관코드(CODE)"
        VARCHAR crpt_status_cd FK "신고상태코드(CODE)"
        VARCHAR report_target_cd FK "신고대상코드(CODE)"
        VARCHAR report_reason_cd FK "신고사유코드(CODE)"
        JSONB report_payload "신고전문JSON"
        VARCHAR external_tx_no "외부거래번호"
        TIMESTAMPTZ reported_at "신고일시"
        TIMESTAMPTZ ack_at "신고접수확인일시"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        INT version
    }

    LOAN_CONTRACT ||--|| MATURITY : "만기관리"
    LOAN_CONTRACT ||--o{ DELINQUENCY : "연체발생"
    DELINQUENCY ||--o{ DELINQUENCY_DAILY_SNAPSHOT : "일별 스냅샷"
    LOAN_CONTRACT ||--o{ CREDIT_INFO_REPORT : "신용정보신고"
```

---

## STAGE 9. 약정종료·증명서

```mermaid
erDiagram
    LOAN_CLOSURE {
        BIGINT clos_id PK "약정종료ID"
        BIGINT cntr_id FK "계약ID"
        VARCHAR clos_type_cd FK "약정종료유형코드(CODE)"
        VARCHAR clos_reason_cd FK "약정종료사유코드(CODE)"
        VARCHAR clos_status_cd FK "약정종료상태코드(CODE)"
        BIGINT final_principal_amt "최종원금금액"
        BIGINT final_interest_amt "최종이자금액"
        BIGINT final_fee_amt "최종수수료금액"
        BIGINT prepayment_fee_amt "중도상환수수료금액"
        BIGINT total_settled_amt "최종정산금액"
        VARCHAR clos_date "약정종료일자"
        TIMESTAMPTZ closed_at "약정종료일시"
        VARCHAR clos_doc_url "약정종료증서URL"
        VARCHAR clos_doc_hash "약정종료증서해시"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        INT version
    }

    LOAN_CERTIFICATE {
        BIGINT cert_id PK "증명서ID"
        BIGINT cntr_id FK "계약ID"
        BIGINT customer_id FK "고객ID"
        VARCHAR cert_type_cd FK "증명서유형코드(CODE)"
        VARCHAR cert_no UK "증명서노출번호"
        VARCHAR cert_status_cd FK "증명서상태코드(CODE)"
        VARCHAR cert_purpose_cd FK "증명서용도코드(CODE)"
        VARCHAR cert_doc_url "증명서URL"
        VARCHAR cert_doc_hash "증명서해시"
        VARCHAR issue_channel_cd FK "발급채널코드(CODE)"
        TIMESTAMPTZ issued_at "증명서발급일시"
        VARCHAR retention_until "보관만료일자"
        TIMESTAMPTZ created_at
        BIGINT created_by
        TIMESTAMPTZ updated_at
        BIGINT updated_by
        TIMESTAMPTZ deleted_at
        BIGINT deleted_by
        INT version
    }

    LOAN_CONTRACT ||--|| LOAN_CLOSURE : "약정종료"
    LOAN_CONTRACT ||--o{ LOAN_CERTIFICATE : "증명서발급"
```

---

# 부속물 1. 그룹 간 관계 다이어그램

```mermaid
flowchart LR
    subgraph G1["공통·상품"]
        CODE_MASTER
        STATUS_HISTORY
        BUSINESS_CALENDAR
        LOAN_PRODUCT
    end
    subgraph G2["신청·동의·본인확인"]
        LOAN_APPLICATION
        LOAN_PRESCREENING
        CREDIT_CONSENT
        LOAN_IDENTITY_VERIFICATION
    end
    subgraph G3["서류·OCR"]
        LOAN_DOCUMENT
        LOAN_DOCUMENT_OCR
    end
    subgraph G4["보증·담보·LTV"]
        GUARANTOR_MASTER
        GUARANTOR_AGREEMENT
        COLLATERAL
        COLLATERAL_EVALUATION
        LTV_CALCULATION
    end
    subgraph G5["평가·심사"]
        CREDIT_EVALUATION
        DSR_CALCULATION
        LOAN_REVIEW
        REVIEW_CHECK_LOG
    end
    subgraph G6["계약·실행"]
        LOAN_CONTRACT
        REPAYMENT_ACCOUNT
        LOAN_EXECUTION
        GUARANTEE_INSURANCE
    end
    subgraph G7["상환·이자·금리"]
        REPAYMENT_SCHEDULE
        INTEREST_ACCRUAL
        REPAYMENT_TRANSACTION
        RATE_CHANGE_HISTORY
    end
    subgraph G8["만기·연체·신고"]
        MATURITY
        DELINQUENCY
        DELINQUENCY_DAILY_SNAPSHOT
        CREDIT_INFO_REPORT
    end
    subgraph G9["종료·증명서"]
        LOAN_CLOSURE
        LOAN_CERTIFICATE
    end

    G1 --> G2
    G2 --> G3
    G2 --> G4
    G2 --> G5
    G4 --> G5
    G3 --> G5
    G5 --> G6
    G6 --> G7
    G6 --> G8
    G7 --> G8
    G6 --> G9
    G8 --> G9
    G1 -. 상태이력/코드 참조 .-> G2
    G1 -. 상태이력/코드 참조 .-> G5
    G1 -. 상태이력/코드 참조 .-> G6
    G1 -. 상태이력/코드 참조 .-> G7
    G1 -. 상태이력/코드 참조 .-> G8
```

---

# 부속물 2. 공통계(共通系) 연계 매핑

| LON 테이블 | 공통계 참조 | 연계 컬럼 | 비고 |
|---|---|---|---|
| LOAN_PRODUCT | common_product | product_id | 마케팅·상품 카탈로그 공통화 |
| LOAN_CONTRACT | common_contract | contract_id | 약정 마스터 공통화 |
| LOAN_CONTRACT | common_contract_party | (간접) | 보증인·공동차주 등 당사자 |
| REPAYMENT_ACCOUNT | common_account | account_id | 자동이체 출금계좌 |
| LOAN_EXECUTION / REPAYMENT_TRANSACTION | common_transaction | transaction_id | 회계·거래 원장 |
| LOAN_PRODUCT 금리 | common_rate_policy / pref_rate_policy | product_id | 기본금리·우대금리 정책 |
| CREDIT_CONSENT | common_terms_consent / _version / _template | (간접) | 약관 동의 트레이서빌리티 |

---

# 부속물 3. 운영 규칙 요약

1. **모든 코드 컬럼**(`*_cd`, `*_status_cd`, `*_type_cd` 등)은 `CODE_MASTER`를 참조한다. DB enum 금지.
2. **상태 변경**은 반드시 `STATUS_HISTORY` 에 append-only 로 기록한다.
3. **개인정보**(성명·주민번호·연락처·계좌번호 등)는 `BYTEA *_enc` 암호화 + `*_masked` 마스킹 컬럼을 함께 운영한다.
4. **금액**은 `BIGINT` 원 단위, **금리·비율**은 `INT bps` 또는 `DECIMAL(5,4)` 로 통일한다.
5. **이력·스냅샷 테이블**(`STATUS_HISTORY`, `INTEREST_ACCRUAL`, `RATE_CHANGE_HISTORY`, `DELINQUENCY_DAILY_SNAPSHOT`, `REVIEW_CHECK_LOG`)은 **append-only**, Soft Delete 미적용.
6. **외부 연계 트랜잭션**(본인확인·신용정보신고·대출실행 등)은 `idempotency_key` 또는 `external_tx_no` 로 중복호출을 차단한다.
7. **물리 삭제 금지** — 모든 등록계 테이블은 `deleted_at IS NULL` 활성 행 조회 패턴을 따른다.

> [검토필요]
> - `LOAN_PRODUCT` 의 채널별 판매 정책 (모바일/창구/제휴) 분리 여부
> - `COLLATERAL` 의 동산/유가증권 등 비부동산 담보 서브타입 필요 여부
> - `CREDIT_INFO_REPORT` 신고기관 분기(KCB/NICE/한국신용정보원) 별 별도 테이블 분리 여부
