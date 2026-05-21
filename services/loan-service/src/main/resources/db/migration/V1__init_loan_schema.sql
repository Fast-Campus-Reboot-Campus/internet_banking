-- ============================================================
-- Loan domain schema (PostgreSQL 16)
-- Notes:
--   - CODE_MASTER owned by master-service → no FK
--   - Status history stored per domain DB
--   - Soft delete only
-- ============================================================

SET TIME ZONE 'Asia/Seoul';

-- ============================================================
-- Common
-- ============================================================

CREATE TABLE status_history (
    sthist_id         BIGSERIAL    PRIMARY KEY,
    target_domain_cd  VARCHAR(30)  NOT NULL,
    target_table_cd   VARCHAR(50)  NOT NULL,
    target_id         BIGINT       NOT NULL,
    before_status_cd  VARCHAR(50),
    after_status_cd   VARCHAR(50)  NOT NULL,
    change_reason_cd  VARCHAR(50),
    change_remark     VARCHAR(500),
    changed_at        TIMESTAMPTZ  NOT NULL,
    changed_by        BIGINT       NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by        BIGINT       NOT NULL
);
CREATE INDEX idx_status_history_target
    ON status_history (target_domain_cd, target_table_cd, target_id, changed_at);

-- ============================================================
-- STAGE 1. 영업일 캘린더 · 상품
-- ============================================================

CREATE TABLE business_calendar (
    cal_id           BIGSERIAL    PRIMARY KEY,
    cal_date         VARCHAR(8)   NOT NULL UNIQUE,
    business_day_yn  CHAR(1)      NOT NULL,
    holiday_type_cd  VARCHAR(50),
    holiday_name     VARCHAR(100),
    base_country_cd  VARCHAR(10),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by       BIGINT       NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by       BIGINT       NOT NULL,
    deleted_at       TIMESTAMPTZ,
    deleted_by       BIGINT,
    version          INT          NOT NULL DEFAULT 0
);

CREATE TABLE loan_product (
    prod_id                  BIGSERIAL    PRIMARY KEY,
    product_id               BIGINT,
    prod_cd                  VARCHAR(30)  NOT NULL UNIQUE,
    prod_name                VARCHAR(200) NOT NULL,
    loan_type_cd             VARCHAR(50)  NOT NULL,
    target_customer_cd       VARCHAR(50),
    repayment_method_cd      VARCHAR(50)  NOT NULL,
    rate_type_cd             VARCHAR(50)  NOT NULL,
    base_rate_bps            INT          NOT NULL,
    min_rate_bps             INT,
    max_rate_bps             INT,
    min_amount               BIGINT       NOT NULL,
    max_amount               BIGINT       NOT NULL,
    min_period_mo            INT          NOT NULL,
    max_period_mo            INT          NOT NULL,
    collateral_required_yn   CHAR(1)      NOT NULL DEFAULT 'N',
    guarantor_required_yn    CHAR(1)      NOT NULL DEFAULT 'N',
    sale_start_date          VARCHAR(8),
    sale_end_date            VARCHAR(8),
    prod_status_cd           VARCHAR(50)  NOT NULL,
    prod_terms_url           VARCHAR(500),
    prod_terms_hash          VARCHAR(128),
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by               BIGINT       NOT NULL,
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by               BIGINT       NOT NULL,
    deleted_at               TIMESTAMPTZ,
    deleted_by               BIGINT,
    version                  INT          NOT NULL DEFAULT 0
);

CREATE TABLE preferential_rate_policy (
    policy_id              BIGSERIAL    PRIMARY KEY,
    prod_id                BIGINT       NOT NULL REFERENCES loan_product(prod_id),
    policy_name            VARCHAR(200) NOT NULL,
    condition_cd           VARCHAR(50)  NOT NULL,
    preferential_rate_bps  INT          NOT NULL,
    max_stack_bps          INT,
    active_yn              CHAR(1)      NOT NULL DEFAULT 'Y',
    effective_start_date   VARCHAR(8),
    effective_end_date     VARCHAR(8),
    policy_remark          VARCHAR(500),
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by             BIGINT       NOT NULL,
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by             BIGINT       NOT NULL,
    deleted_at             TIMESTAMPTZ,
    deleted_by             BIGINT,
    version                INT          NOT NULL DEFAULT 0
);
CREATE INDEX idx_pref_rate_policy_prod
    ON preferential_rate_policy (prod_id, active_yn);
CREATE UNIQUE INDEX uk_pref_rate_policy_prod_condition_active
    ON preferential_rate_policy (prod_id, condition_cd)
    WHERE deleted_at IS NULL AND active_yn = 'Y';

-- ============================================================
-- STAGE 2. 신청 · 가심사 · 동의 · 본인확인
-- ============================================================

CREATE TABLE loan_application (
    appl_id              BIGSERIAL     PRIMARY KEY,
    appl_no              VARCHAR(30)   NOT NULL UNIQUE,
    customer_id          BIGINT        NOT NULL,
    prod_id              BIGINT        NOT NULL REFERENCES loan_product(prod_id),
    channel_cd           VARCHAR(50)   NOT NULL,
    requested_amount     BIGINT        NOT NULL,
    requested_period_mo  INT           NOT NULL,
    loan_purpose_cd      VARCHAR(50),
    repayment_method_cd  VARCHAR(50)   NOT NULL,
    estimated_income_amt BIGINT,
    employment_type_cd   VARCHAR(50),
    appl_status_cd       VARCHAR(50)   NOT NULL,
    applied_at           TIMESTAMPTZ   NOT NULL,
    client_ip            VARCHAR(64),
    device               VARCHAR(200),
    idempotency_key      VARCHAR(100)  UNIQUE,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by           BIGINT        NOT NULL,
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by           BIGINT        NOT NULL,
    deleted_at           TIMESTAMPTZ,
    deleted_by           BIGINT,
    version              INT           NOT NULL DEFAULT 0
);
CREATE INDEX idx_loan_application_customer ON loan_application (customer_id);

CREATE TABLE loan_prescreening (
    presc_id              BIGSERIAL    PRIMARY KEY,
    appl_id               BIGINT       NOT NULL UNIQUE REFERENCES loan_application(appl_id),
    presc_result_cd       VARCHAR(50)  NOT NULL,
    estimated_limit_amt   BIGINT,
    estimated_rate_bps    INT,
    estimated_grade       VARCHAR(10),
    estimated_score       INT,
    reject_reason_cd      VARCHAR(50),
    presc_remark          VARCHAR(500),
    prescreened_at        TIMESTAMPTZ  NOT NULL,
    presc_engine_version  VARCHAR(50),
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by            BIGINT       NOT NULL,
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by            BIGINT       NOT NULL,
    deleted_at            TIMESTAMPTZ,
    deleted_by            BIGINT,
    version               INT          NOT NULL DEFAULT 0
);

CREATE TABLE credit_consent (
    csnt_id            BIGSERIAL     PRIMARY KEY,
    appl_id            BIGINT        NOT NULL REFERENCES loan_application(appl_id),
    customer_id        BIGINT        NOT NULL,
    consent_type_cd    VARCHAR(50)   NOT NULL,
    consent_scope_cd   VARCHAR(50)   NOT NULL,
    consent_target_cd  VARCHAR(50)   NOT NULL,
    consent_yn         CHAR(1)       NOT NULL,
    consented_at       TIMESTAMPTZ   NOT NULL,
    consent_method_cd  VARCHAR(50),
    consent_token      VARCHAR(100),
    signed_doc_url     VARCHAR(500),
    signed_doc_hash    VARCHAR(128),
    client_ip          VARCHAR(64),
    device             VARCHAR(200),
    retention_until    VARCHAR(8),
    withdrawn_yn       CHAR(1)       NOT NULL DEFAULT 'N',
    withdrawn_at       TIMESTAMPTZ,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by         BIGINT        NOT NULL,
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by         BIGINT        NOT NULL,
    deleted_at         TIMESTAMPTZ,
    deleted_by         BIGINT,
    version            INT           NOT NULL DEFAULT 0
);

CREATE TABLE loan_identity_verification (
    idv_id            BIGSERIAL     PRIMARY KEY,
    appl_id           BIGINT        NOT NULL REFERENCES loan_application(appl_id),
    customer_id       BIGINT        NOT NULL,
    idv_method_cd     VARCHAR(50)   NOT NULL,
    idv_status_cd     VARCHAR(50)   NOT NULL,
    idv_result_cd     VARCHAR(50),
    idv_target_cd     VARCHAR(50)   NOT NULL,
    ci_hash           VARCHAR(128),
    di_hash           VARCHAR(128),
    mobile_no_enc     BYTEA,
    mobile_no_masked  VARCHAR(20),
    verified_at       TIMESTAMPTZ,
    client_ip         VARCHAR(64),
    device            VARCHAR(200),
    external_tx_no    VARCHAR(100),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by        BIGINT        NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by        BIGINT        NOT NULL,
    deleted_at        TIMESTAMPTZ,
    deleted_by        BIGINT,
    version           INT           NOT NULL DEFAULT 0
);

-- ============================================================
-- STAGE 3. 서류 · OCR
-- ============================================================

CREATE TABLE loan_document (
    doc_id            BIGSERIAL     PRIMARY KEY,
    appl_id           BIGINT        NOT NULL REFERENCES loan_application(appl_id),
    doc_type_cd       VARCHAR(50)   NOT NULL,
    doc_status_cd     VARCHAR(50)   NOT NULL,
    doc_source_cd     VARCHAR(50),
    doc_name          VARCHAR(200),
    doc_url           VARCHAR(500),
    doc_hash          VARCHAR(128),
    mime_type         VARCHAR(100),
    file_size_bytes   BIGINT,
    submitted_at      TIMESTAMPTZ,
    verified_at       TIMESTAMPTZ,
    verify_result_cd  VARCHAR(50),
    retention_until   VARCHAR(8),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by        BIGINT        NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by        BIGINT        NOT NULL,
    deleted_at        TIMESTAMPTZ,
    deleted_by        BIGINT,
    version           INT           NOT NULL DEFAULT 0
);

CREATE TABLE loan_document_ocr (
    ocr_id              BIGSERIAL     PRIMARY KEY,
    doc_id              BIGINT        NOT NULL REFERENCES loan_document(doc_id),
    ocr_engine          VARCHAR(50)   NOT NULL,
    ocr_engine_version  VARCHAR(50),
    ocr_status_cd       VARCHAR(50)   NOT NULL,
    ocr_confidence      DECIMAL(5,4),
    extracted_fields    JSONB,
    extracted_text      TEXT,
    ocr_at              TIMESTAMPTZ,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by          BIGINT        NOT NULL,
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by          BIGINT        NOT NULL,
    deleted_at          TIMESTAMPTZ,
    deleted_by          BIGINT,
    version             INT           NOT NULL DEFAULT 0
);

-- ============================================================
-- STAGE 4. 보증 · 담보 · LTV
-- ============================================================

CREATE TABLE guarantor_master (
    gmst_id                BIGSERIAL     PRIMARY KEY,
    guarantor_name_enc     BYTEA         NOT NULL,
    guarantor_name_masked  VARCHAR(50),
    guarantor_ci_hash      VARCHAR(128)  NOT NULL,
    relation_type_cd       VARCHAR(50),
    mobile_no_enc          BYTEA,
    mobile_no_masked       VARCHAR(20),
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by             BIGINT        NOT NULL,
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by             BIGINT        NOT NULL,
    deleted_at             TIMESTAMPTZ,
    deleted_by             BIGINT,
    version                INT           NOT NULL DEFAULT 0
);

CREATE TABLE guarantor_agreement (
    gagr_id             BIGSERIAL     PRIMARY KEY,
    appl_id             BIGINT        NOT NULL REFERENCES loan_application(appl_id),
    gmst_id             BIGINT        NOT NULL REFERENCES guarantor_master(gmst_id),
    gagr_type_cd        VARCHAR(50)   NOT NULL,
    guarantee_amount    BIGINT        NOT NULL,
    guarantee_ratio_bps INT,
    gagr_status_cd      VARCHAR(50)   NOT NULL,
    consented_at        TIMESTAMPTZ,
    signed_doc_url      VARCHAR(500),
    signed_doc_hash     VARCHAR(128),
    client_ip           VARCHAR(64),
    device              VARCHAR(200),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by          BIGINT        NOT NULL,
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by          BIGINT        NOT NULL,
    deleted_at          TIMESTAMPTZ,
    deleted_by          BIGINT,
    version             INT           NOT NULL DEFAULT 0
);

CREATE TABLE collateral (
    col_id              BIGSERIAL     PRIMARY KEY,
    appl_id             BIGINT        NOT NULL REFERENCES loan_application(appl_id),
    col_type_cd         VARCHAR(50)   NOT NULL,
    col_status_cd       VARCHAR(50)   NOT NULL,
    col_no              VARCHAR(30)   NOT NULL UNIQUE,
    col_name            VARCHAR(200),
    col_address         VARCHAR(500),
    col_registry_no     VARCHAR(100),
    declared_value      BIGINT,
    currency_cd         VARCHAR(10)   NOT NULL DEFAULT 'KRW',
    ownership_type_cd   VARCHAR(50),
    senior_lien_yn      CHAR(1)       NOT NULL DEFAULT 'N',
    senior_lien_amount  BIGINT,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by          BIGINT        NOT NULL,
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by          BIGINT        NOT NULL,
    deleted_at          TIMESTAMPTZ,
    deleted_by          BIGINT,
    version             INT           NOT NULL DEFAULT 0
);

CREATE TABLE collateral_evaluation (
    ceval_col_id       BIGSERIAL     PRIMARY KEY,
    col_id             BIGINT        NOT NULL REFERENCES collateral(col_id),
    eval_method_cd     VARCHAR(50)   NOT NULL,
    eval_agency_cd     VARCHAR(50),
    appraised_value    BIGINT        NOT NULL,
    applied_value      BIGINT        NOT NULL,
    eval_status_cd     VARCHAR(50)   NOT NULL,
    eval_report_url    VARCHAR(500),
    eval_report_hash   VARCHAR(128),
    evaluated_at       TIMESTAMPTZ,
    applied_start_date VARCHAR(8),
    applied_end_date   VARCHAR(8),
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by         BIGINT        NOT NULL,
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by         BIGINT        NOT NULL,
    deleted_at         TIMESTAMPTZ,
    deleted_by         BIGINT,
    version            INT           NOT NULL DEFAULT 0
);

CREATE TABLE ltv_calculation (
    ltv_id              BIGSERIAL     PRIMARY KEY,
    appl_id             BIGINT        NOT NULL REFERENCES loan_application(appl_id),
    col_id              BIGINT        NOT NULL REFERENCES collateral(col_id),
    applied_col_value   BIGINT        NOT NULL,
    senior_lien_amount  BIGINT,
    requested_amount    BIGINT        NOT NULL,
    ltv_ratio_bps       INT           NOT NULL,
    ltv_limit_bps       INT           NOT NULL,
    max_loan_amount     BIGINT        NOT NULL,
    ltv_status_cd       VARCHAR(50)   NOT NULL,
    calculated_at       TIMESTAMPTZ   NOT NULL,
    calc_engine_version VARCHAR(50),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by          BIGINT        NOT NULL,
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by          BIGINT        NOT NULL,
    deleted_at          TIMESTAMPTZ,
    deleted_by          BIGINT,
    version             INT           NOT NULL DEFAULT 0
);

-- ============================================================
-- STAGE 5. 신용평가 · DSR · 본심사
-- ============================================================

CREATE TABLE credit_evaluation (
    ceval_id             BIGSERIAL     PRIMARY KEY,
    appl_id              BIGINT        NOT NULL UNIQUE REFERENCES loan_application(appl_id),
    customer_id          BIGINT        NOT NULL,
    ceval_engine         VARCHAR(50)   NOT NULL,
    ceval_engine_version VARCHAR(50),
    ceval_grade          VARCHAR(10),
    ceval_score          INT,
    pd_bps               INT,
    ceval_decision_cd    VARCHAR(50)   NOT NULL,
    eval_limit_amount    BIGINT,
    eval_rate_bps        INT,
    ceval_status_cd      VARCHAR(50)   NOT NULL,
    ceval_factors        JSONB,
    evaluated_at         TIMESTAMPTZ   NOT NULL,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by           BIGINT        NOT NULL,
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by           BIGINT        NOT NULL,
    deleted_at           TIMESTAMPTZ,
    deleted_by           BIGINT,
    version              INT           NOT NULL DEFAULT 0
);

CREATE TABLE dsr_calculation (
    dsr_id                      BIGSERIAL     PRIMARY KEY,
    appl_id                     BIGINT        NOT NULL UNIQUE REFERENCES loan_application(appl_id),
    customer_id                 BIGINT        NOT NULL,
    annual_income_amt           BIGINT        NOT NULL,
    existing_principal_total    BIGINT        NOT NULL DEFAULT 0,
    existing_annual_repay_amt   BIGINT        NOT NULL DEFAULT 0,
    new_annual_repay_amt        BIGINT        NOT NULL,
    total_annual_repay_amt      BIGINT        NOT NULL,
    dsr_ratio_bps               INT           NOT NULL,
    dsr_limit_bps               INT           NOT NULL,
    dsr_status_cd               VARCHAR(50)   NOT NULL,
    dsr_reg_type_cd             VARCHAR(50),
    calculated_at               TIMESTAMPTZ   NOT NULL,
    calc_engine_version         VARCHAR(50),
    dsr_detail                  JSONB,
    created_at                  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by                  BIGINT        NOT NULL,
    updated_at                  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by                  BIGINT        NOT NULL,
    deleted_at                  TIMESTAMPTZ,
    deleted_by                  BIGINT,
    version                     INT           NOT NULL DEFAULT 0
);

CREATE TABLE loan_review (
    rev_id              BIGSERIAL     PRIMARY KEY,
    appl_id             BIGINT        NOT NULL UNIQUE REFERENCES loan_application(appl_id),
    rev_type_cd         VARCHAR(50)   NOT NULL,
    rev_status_cd       VARCHAR(50)   NOT NULL,
    rev_decision_cd     VARCHAR(50),
    approved_amount     BIGINT,
    approved_rate_bps   INT,
    approved_period_mo  INT,
    reject_reason_cd    VARCHAR(50),
    rev_remark          VARCHAR(500),
    reviewer_id         BIGINT,
    reviewed_at         TIMESTAMPTZ,
    approved_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by          BIGINT        NOT NULL,
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by          BIGINT        NOT NULL,
    deleted_at          TIMESTAMPTZ,
    deleted_by          BIGINT,
    version             INT           NOT NULL DEFAULT 0
);

CREATE TABLE review_check_log (
    rchk_id          BIGSERIAL     PRIMARY KEY,
    rev_id           BIGINT        NOT NULL REFERENCES loan_review(rev_id),
    check_item_cd    VARCHAR(50)   NOT NULL,
    check_result_cd  VARCHAR(50)   NOT NULL,
    check_remark     VARCHAR(500),
    checker_id       BIGINT,
    checked_at       TIMESTAMPTZ   NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by       BIGINT        NOT NULL
);

-- ============================================================
-- STAGE 6. 계약 · 상환계좌 · 실행 · 보증보험
-- ============================================================

CREATE TABLE loan_contract (
    cntr_id                BIGSERIAL     PRIMARY KEY,
    cntr_no                VARCHAR(30)   NOT NULL UNIQUE,
    contract_id            BIGINT,
    appl_id                BIGINT        NOT NULL REFERENCES loan_application(appl_id),
    rev_id                 BIGINT        REFERENCES loan_review(rev_id), -- nullable: 본심사 API 도입 전 임시

    customer_id            BIGINT        NOT NULL,
    prod_id                BIGINT        NOT NULL REFERENCES loan_product(prod_id),
    contracted_amount      BIGINT        NOT NULL,
    currency_cd            VARCHAR(10)   NOT NULL DEFAULT 'KRW',
    contracted_period_mo   INT           NOT NULL,
    total_rate_bps         INT           NOT NULL,
    base_rate_bps          INT           NOT NULL,
    spread_bps             INT           NOT NULL DEFAULT 0,
    preferential_rate_bps  INT           NOT NULL DEFAULT 0,
    rate_type_cd           VARCHAR(50)   NOT NULL,
    repayment_method_cd    VARCHAR(50)   NOT NULL,
    cntr_status_cd         VARCHAR(50)   NOT NULL,
    cntr_start_date        VARCHAR(8)    NOT NULL,
    cntr_end_date          VARCHAR(8)    NOT NULL,
    cntr_doc_url           VARCHAR(500),
    cntr_doc_hash          VARCHAR(128),
    signed_at              TIMESTAMPTZ,
    client_ip              VARCHAR(64),
    device                 VARCHAR(200),
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by             BIGINT        NOT NULL,
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by             BIGINT        NOT NULL,
    deleted_at             TIMESTAMPTZ,
    deleted_by             BIGINT,
    version                INT           NOT NULL DEFAULT 0
);
CREATE INDEX idx_loan_contract_customer ON loan_contract (customer_id);

CREATE TABLE repayment_account (
    racct_id           BIGSERIAL     PRIMARY KEY,
    cntr_id            BIGINT        NOT NULL UNIQUE REFERENCES loan_contract(cntr_id),
    account_id         BIGINT,
    account_no_masked  VARCHAR(50),
    account_no_enc     BYTEA,
    bank_cd            VARCHAR(10)   NOT NULL,
    holder_name_masked VARCHAR(50),
    racct_status_cd    VARCHAR(50)   NOT NULL,
    auto_debit_yn      CHAR(1)       NOT NULL DEFAULT 'N',
    debit_day          INT,
    verified_at        TIMESTAMPTZ,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by         BIGINT        NOT NULL,
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by         BIGINT        NOT NULL,
    deleted_at         TIMESTAMPTZ,
    deleted_by         BIGINT,
    version            INT           NOT NULL DEFAULT 0
);

CREATE TABLE loan_execution (
    exec_id                       BIGSERIAL     PRIMARY KEY,
    cntr_id                       BIGINT        NOT NULL REFERENCES loan_contract(cntr_id),
    transaction_id                BIGINT,
    executed_amount               BIGINT        NOT NULL,
    currency_cd                   VARCHAR(10)   NOT NULL DEFAULT 'KRW',
    exec_status_cd                VARCHAR(50)   NOT NULL,
    disbursement_bank_cd          VARCHAR(10),
    disbursement_account_enc      BYTEA,
    disbursement_account_masked   VARCHAR(50),
    executed_at                   TIMESTAMPTZ,
    value_date                    VARCHAR(8),
    fee_amount                    BIGINT        NOT NULL DEFAULT 0,
    idempotency_key               VARCHAR(100)  UNIQUE,
    journal_entry_no              VARCHAR(50),
    created_at                    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by                    BIGINT        NOT NULL,
    updated_at                    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by                    BIGINT        NOT NULL,
    deleted_at                    TIMESTAMPTZ,
    deleted_by                    BIGINT,
    version                       INT           NOT NULL DEFAULT 0
);

CREATE TABLE guarantee_insurance (
    gins_id            BIGSERIAL     PRIMARY KEY,
    cntr_id            BIGINT        NOT NULL REFERENCES loan_contract(cntr_id),
    gins_agency_cd     VARCHAR(50)   NOT NULL,
    gins_policy_no     VARCHAR(50)   NOT NULL UNIQUE,
    guarantee_amount   BIGINT        NOT NULL,
    guarantee_ratio_bps INT          NOT NULL,
    premium_amount     BIGINT        NOT NULL,
    gins_status_cd     VARCHAR(50)   NOT NULL,
    gins_start_date    VARCHAR(8)    NOT NULL,
    gins_end_date      VARCHAR(8)    NOT NULL,
    gins_doc_url       VARCHAR(500),
    gins_doc_hash      VARCHAR(128),
    issued_at          TIMESTAMPTZ,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by         BIGINT        NOT NULL,
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by         BIGINT        NOT NULL,
    deleted_at         TIMESTAMPTZ,
    deleted_by         BIGINT,
    version            INT           NOT NULL DEFAULT 0
);

-- ============================================================
-- STAGE 7. 상환 스케줄 · 이자발생 · 상환거래 · 금리변경이력
-- ============================================================

CREATE TABLE repayment_schedule (
    rsch_id             BIGSERIAL     PRIMARY KEY,
    cntr_id             BIGINT        NOT NULL REFERENCES loan_contract(cntr_id),
    installment_no      INT           NOT NULL,
    due_date            VARCHAR(8)    NOT NULL,
    scheduled_principal BIGINT        NOT NULL,
    scheduled_interest  BIGINT        NOT NULL,
    scheduled_total     BIGINT        NOT NULL,
    remaining_balance   BIGINT        NOT NULL,
    applied_rate_bps    INT           NOT NULL,
    rsch_status_cd      VARCHAR(50)   NOT NULL,
    rsch_version_cd     VARCHAR(50)   NOT NULL,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by          BIGINT        NOT NULL,
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by          BIGINT        NOT NULL,
    deleted_at          TIMESTAMPTZ,
    deleted_by          BIGINT,
    version             INT           NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_repayment_schedule_cntr_inst_ver
    ON repayment_schedule (cntr_id, installment_no, rsch_version_cd);

CREATE TABLE interest_accrual (
    iacc_id                BIGSERIAL     PRIMARY KEY,
    cntr_id                BIGINT        NOT NULL REFERENCES loan_contract(cntr_id),
    accrual_date           VARCHAR(8)    NOT NULL,
    principal_balance      BIGINT        NOT NULL,
    applied_rate_bps       INT           NOT NULL,
    day_count_basis_cd     VARCHAR(50)   NOT NULL,
    daily_interest_amt     BIGINT        NOT NULL,
    cumulative_interest_amt BIGINT       NOT NULL,
    iacc_status_cd         VARCHAR(50)   NOT NULL,
    accrued_at             TIMESTAMPTZ   NOT NULL,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by             BIGINT        NOT NULL
);
CREATE UNIQUE INDEX uk_interest_accrual_cntr_date
    ON interest_accrual (cntr_id, accrual_date);

CREATE TABLE repayment_transaction (
    rtx_id                 BIGSERIAL     PRIMARY KEY,
    cntr_id                BIGINT        NOT NULL REFERENCES loan_contract(cntr_id),
    rsch_id                BIGINT        REFERENCES repayment_schedule(rsch_id),
    transaction_id         BIGINT,
    rtx_type_cd            VARCHAR(50)   NOT NULL,
    total_amount           BIGINT        NOT NULL,
    principal_amount       BIGINT        NOT NULL DEFAULT 0,
    interest_amount        BIGINT        NOT NULL DEFAULT 0,
    overdue_interest_amount BIGINT       NOT NULL DEFAULT 0,
    fee_amount             BIGINT        NOT NULL DEFAULT 0,
    currency_cd            VARCHAR(10)   NOT NULL DEFAULT 'KRW',
    channel_cd             VARCHAR(50)   NOT NULL,
    rtx_status_cd          VARCHAR(50)   NOT NULL,
    paid_at                TIMESTAMPTZ,
    value_date             VARCHAR(8),
    balance_after          BIGINT,
    idempotency_key        VARCHAR(100)  UNIQUE,
    reversal_yn            CHAR(1)       NOT NULL DEFAULT 'N',
    reversal_target_rtx_id BIGINT,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by             BIGINT        NOT NULL,
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by             BIGINT        NOT NULL,
    deleted_at             TIMESTAMPTZ,
    deleted_by             BIGINT,
    version                INT           NOT NULL DEFAULT 0,
    CONSTRAINT fk_rtx_reversal_target FOREIGN KEY (reversal_target_rtx_id)
        REFERENCES repayment_transaction(rtx_id)
);

CREATE TABLE rate_change_history (
    rchg_id                BIGSERIAL     PRIMARY KEY,
    cntr_id                BIGINT        NOT NULL REFERENCES loan_contract(cntr_id),
    rate_change_reason_cd  VARCHAR(50)   NOT NULL,
    previous_rate_bps      INT           NOT NULL,
    new_rate_bps           INT           NOT NULL,
    base_rate_bps          INT           NOT NULL,
    spread_bps             INT           NOT NULL DEFAULT 0,
    preferential_rate_bps  INT           NOT NULL DEFAULT 0,
    applied_start_date     VARCHAR(8)    NOT NULL,
    applied_end_date       VARCHAR(8),
    changed_at             TIMESTAMPTZ   NOT NULL,
    changed_by             BIGINT        NOT NULL,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by             BIGINT        NOT NULL
);

-- ============================================================
-- STAGE 8. 만기 · 연체 · 신용정보신고
-- ============================================================

CREATE TABLE maturity (
    mat_id                 BIGSERIAL     PRIMARY KEY,
    cntr_id                BIGINT        NOT NULL UNIQUE REFERENCES loan_contract(cntr_id),
    original_maturity_date VARCHAR(8)    NOT NULL,
    current_maturity_date  VARCHAR(8)    NOT NULL,
    mat_status_cd          VARCHAR(50)   NOT NULL,
    extension_type_cd      VARCHAR(50),
    extension_count        INT           NOT NULL DEFAULT 0,
    last_extended_date     VARCHAR(8),
    extended_period_mo     INT,
    notice_status_cd       VARCHAR(50),
    last_notice_at         TIMESTAMPTZ,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by             BIGINT        NOT NULL,
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by             BIGINT        NOT NULL,
    deleted_at             TIMESTAMPTZ,
    deleted_by             BIGINT,
    version                INT           NOT NULL DEFAULT 0
);

CREATE TABLE delinquency (
    dlq_id              BIGSERIAL     PRIMARY KEY,
    cntr_id             BIGINT        NOT NULL REFERENCES loan_contract(cntr_id),
    dlq_status_cd      VARCHAR(50)    NOT NULL,
    dlq_start_date     VARCHAR(8)     NOT NULL,
    dlq_end_date       VARCHAR(8),
    dlq_days           INT            NOT NULL DEFAULT 0,
    dlq_principal_amt  BIGINT         NOT NULL DEFAULT 0,
    dlq_interest_amt   BIGINT         NOT NULL DEFAULT 0,
    dlq_total_amt      BIGINT         NOT NULL DEFAULT 0,
    overdue_rate_bps   INT            NOT NULL DEFAULT 0,
    dlq_stage_cd       VARCHAR(50)    NOT NULL,
    resolved_at        TIMESTAMPTZ,
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by         BIGINT         NOT NULL,
    updated_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_by         BIGINT         NOT NULL,
    deleted_at         TIMESTAMPTZ,
    deleted_by         BIGINT,
    version            INT            NOT NULL DEFAULT 0
);

CREATE TABLE delinquency_daily_snapshot (
    dlqs_id            BIGSERIAL     PRIMARY KEY,
    dlq_id             BIGINT        NOT NULL REFERENCES delinquency(dlq_id),
    cntr_id            BIGINT        NOT NULL REFERENCES loan_contract(cntr_id),
    snapshot_date      VARCHAR(8)    NOT NULL,
    dlq_days           INT           NOT NULL,
    dlq_principal_amt  BIGINT        NOT NULL DEFAULT 0,
    dlq_interest_amt   BIGINT        NOT NULL DEFAULT 0,
    dlq_total_amt      BIGINT        NOT NULL DEFAULT 0,
    overdue_rate_bps   INT           NOT NULL DEFAULT 0,
    dlq_stage_cd       VARCHAR(50)   NOT NULL,
    snapshotted_at     TIMESTAMPTZ   NOT NULL,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by         BIGINT        NOT NULL
);
CREATE UNIQUE INDEX uk_dlq_snapshot_dlq_date
    ON delinquency_daily_snapshot (dlq_id, snapshot_date);

CREATE TABLE credit_info_report (
    crpt_id           BIGSERIAL     PRIMARY KEY,
    cntr_id           BIGINT        NOT NULL REFERENCES loan_contract(cntr_id),
    customer_id       BIGINT        NOT NULL,
    crpt_type_cd      VARCHAR(50)   NOT NULL,
    crpt_agency_cd    VARCHAR(50)   NOT NULL,
    crpt_status_cd    VARCHAR(50)   NOT NULL,
    report_target_cd  VARCHAR(50)   NOT NULL,
    report_reason_cd  VARCHAR(50),
    report_payload    JSONB,
    external_tx_no    VARCHAR(100),
    reported_at       TIMESTAMPTZ,
    ack_at            TIMESTAMPTZ,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by        BIGINT        NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by        BIGINT        NOT NULL,
    deleted_at        TIMESTAMPTZ,
    deleted_by        BIGINT,
    version           INT           NOT NULL DEFAULT 0
);

-- ============================================================
-- STAGE 9. 약정종료 · 증명서
-- ============================================================

CREATE TABLE loan_closure (
    clos_id             BIGSERIAL     PRIMARY KEY,
    cntr_id             BIGINT        NOT NULL UNIQUE REFERENCES loan_contract(cntr_id),
    clos_type_cd        VARCHAR(50)   NOT NULL,
    clos_reason_cd      VARCHAR(50),
    clos_status_cd      VARCHAR(50)   NOT NULL,
    final_principal_amt BIGINT        NOT NULL DEFAULT 0,
    final_interest_amt  BIGINT        NOT NULL DEFAULT 0,
    final_fee_amt       BIGINT        NOT NULL DEFAULT 0,
    prepayment_fee_amt  BIGINT        NOT NULL DEFAULT 0,
    total_settled_amt   BIGINT        NOT NULL DEFAULT 0,
    clos_date           VARCHAR(8)    NOT NULL,
    closed_at           TIMESTAMPTZ,
    clos_doc_url        VARCHAR(500),
    clos_doc_hash       VARCHAR(128),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by          BIGINT        NOT NULL,
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by          BIGINT        NOT NULL,
    deleted_at          TIMESTAMPTZ,
    deleted_by          BIGINT,
    version             INT           NOT NULL DEFAULT 0
);

CREATE TABLE loan_certificate (
    cert_id           BIGSERIAL     PRIMARY KEY,
    cntr_id           BIGINT        NOT NULL REFERENCES loan_contract(cntr_id),
    customer_id       BIGINT        NOT NULL,
    cert_type_cd      VARCHAR(50)   NOT NULL,
    cert_no           VARCHAR(50)   NOT NULL UNIQUE,
    cert_status_cd    VARCHAR(50)   NOT NULL,
    cert_purpose_cd   VARCHAR(50),
    cert_doc_url      VARCHAR(500),
    cert_doc_hash     VARCHAR(128),
    issue_channel_cd  VARCHAR(50),
    issued_at         TIMESTAMPTZ,
    retention_until   VARCHAR(8),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by        BIGINT        NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by        BIGINT        NOT NULL,
    deleted_at        TIMESTAMPTZ,
    deleted_by        BIGINT,
    version           INT           NOT NULL DEFAULT 0
);

-- ============================================================
-- 끝.
--   - 운영 환경에서는 sale_*_date 등 VARCHAR(8) 날짜 컬럼을 DATE 로 승격 검토.
--   - JSONB 컬럼(ceval_factors, dsr_detail, extracted_fields, report_payload) 은
--     필요 시 GIN 인덱스 추가.
-- ============================================================
