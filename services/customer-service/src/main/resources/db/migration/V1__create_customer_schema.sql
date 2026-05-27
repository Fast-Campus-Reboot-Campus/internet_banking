-- =============================================================================
-- V1: 고객계 스키마 생성
-- 테이블 13개 + 인덱스
-- 적용 순서: cust_code_master → party → (party_person, party_organization,
--            compliance_info, tax_residency_info, party_role, party_relation,
--            business_info) → foreigner_info → customer
--            → customer_status_history, customer_grade_history
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. cust_code_master  (전 도메인 공통 코드, FK 미설정 soft reference)
-- -----------------------------------------------------------------------------
CREATE TABLE cust_code_master (
    code_group_id        VARCHAR(30)    NOT NULL,
    code_value           VARCHAR(20)    NOT NULL,
    code_name            VARCHAR(100)   NOT NULL,
    description          VARCHAR(500),
    sort_order           INT,
    effective_start_date CHAR(8)        NOT NULL,
    effective_end_date   CHAR(8),
    created_at           TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by           BIGINT,
    updated_at           TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by           BIGINT,
    deleted_at           TIMESTAMPTZ(3),
    deleted_by           BIGINT,
    version              INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_cust_code_master PRIMARY KEY (code_group_id, code_value)
);

-- -----------------------------------------------------------------------------
-- 2. party  (관계자 최상위 엔티티)
-- -----------------------------------------------------------------------------
CREATE TABLE party (
    party_id           BIGINT         GENERATED ALWAYS AS IDENTITY,
    party_type_code    VARCHAR(20)    NOT NULL,
    party_name         VARCHAR(100)   NOT NULL,
    party_english_name VARCHAR(200),
    party_status_code  VARCHAR(20)    NOT NULL,
    created_at         TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by         BIGINT,
    updated_at         TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by         BIGINT,
    deleted_at         TIMESTAMPTZ(3),
    deleted_by         BIGINT,
    version            INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_party PRIMARY KEY (party_id)
);

-- -----------------------------------------------------------------------------
-- 3. party_person  (개인관계자)
-- -----------------------------------------------------------------------------
CREATE TABLE party_person (
    party_id                  BIGINT         NOT NULL,
    rrn_encrypted             VARCHAR(255),
    ci_value                  VARCHAR(88),
    nationality_type_code     VARCHAR(20),
    nationality_code          CHAR(3),
    birth_date                CHAR(8),
    gender_code               CHAR(1),
    marital_status_code       VARCHAR(10),
    dependent_count           INT,
    occupation_code           VARCHAR(10),
    occupation_name           VARCHAR(100),
    workplace_name            VARCHAR(200),
    annual_income_amount      BIGINT,
    income_proof_code         VARCHAR(10),
    capacity_limit_type_code  VARCHAR(20),
    is_pep_yn                 CHAR(1)        NOT NULL DEFAULT 'F',
    pep_type_code             VARCHAR(10),
    pep_country_code          CHAR(3),
    pep_position              VARCHAR(200),
    death_date                CHAR(8),
    created_at                TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                BIGINT,
    updated_at                TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by                BIGINT,
    deleted_at                TIMESTAMPTZ(3),
    deleted_by                BIGINT,
    version                   INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_party_person PRIMARY KEY (party_id),
    CONSTRAINT fk_party_person_party FOREIGN KEY (party_id) REFERENCES party (party_id),
    CONSTRAINT chk_party_person_pep CHECK (
        (is_pep_yn = 'T' AND pep_type_code IS NOT NULL)
        OR (is_pep_yn = 'F' AND pep_type_code IS NULL AND pep_country_code IS NULL)
    )
);

-- -----------------------------------------------------------------------------
-- 4. party_organization  (기업관계자)
-- -----------------------------------------------------------------------------
CREATE TABLE party_organization (
    party_id                      BIGINT         NOT NULL,
    org_subtype_code              VARCHAR(20)    NOT NULL,
    corp_reg_no                   CHAR(14),
    corp_formal_name              VARCHAR(200),
    corp_formal_english_name      VARCHAR(400),
    hq_country_code               CHAR(3),
    foreign_corp_reg_no_encrypted VARCHAR(255),
    corp_type_code                VARCHAR(20),
    non_corp_type_code            VARCHAR(10),
    ownership_type_code           VARCHAR(10),
    representative_type_code      VARCHAR(10),
    establishment_date            CHAR(8),
    dissolution_date              CHAR(8),
    capital_amount                BIGINT,
    fiscal_month                  SMALLINT,
    establishment_purpose         VARCHAR(500),
    member_count                  INT,
    charter_url                   VARCHAR(500),
    created_at                    TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                    BIGINT,
    updated_at                    TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by                    BIGINT,
    deleted_at                    TIMESTAMPTZ(3),
    deleted_by                    BIGINT,
    version                       INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_party_organization PRIMARY KEY (party_id),
    CONSTRAINT fk_party_organization_party FOREIGN KEY (party_id) REFERENCES party (party_id),
    CONSTRAINT chk_party_org_subtype CHECK (
        (org_subtype_code = 'CORPORATION'     AND corp_reg_no IS NOT NULL AND corp_type_code IS NOT NULL)
        OR (org_subtype_code = 'NON_CORPORATION' AND non_corp_type_code IS NOT NULL)
    ),
    CONSTRAINT chk_party_org_foreign_corp CHECK (
        (hq_country_code = 'KOR' AND foreign_corp_reg_no_encrypted IS NULL)
        OR (hq_country_code <> 'KOR' AND foreign_corp_reg_no_encrypted IS NOT NULL)
        OR hq_country_code IS NULL
    )
);

-- -----------------------------------------------------------------------------
-- 5. foreigner_info  (외국인정보, party_person 1:1)
-- -----------------------------------------------------------------------------
CREATE TABLE foreigner_info (
    party_id                BIGINT         NOT NULL,
    foreigner_no_encrypted  VARCHAR(255),
    passport_no             VARCHAR(20),
    passport_country_code   CHAR(3),
    passport_expiry_date    CHAR(8),
    stay_qualification_code VARCHAR(10),
    stay_expiry_date        CHAR(8),
    recent_entry_date       CHAR(8),
    stay_address            VARCHAR(500),
    created_at              TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ(3),
    deleted_by              BIGINT,
    version                 INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_foreigner_info PRIMARY KEY (party_id),
    CONSTRAINT fk_foreigner_info_party_person FOREIGN KEY (party_id) REFERENCES party_person (party_id)
);

-- -----------------------------------------------------------------------------
-- 6. compliance_info  (컴플라이언스정보, party 1:1)
--    is_sanctioned_yn: OFAC·UN·EU·KR 제재 여부 OR 합산 GENERATED STORED 컬럼
-- -----------------------------------------------------------------------------
CREATE TABLE compliance_info (
    party_id                          BIGINT         NOT NULL,
    aml_risk_level_code               VARCHAR(20)    NOT NULL,
    aml_last_assessed_at              TIMESTAMPTZ(3),
    aml_next_review_date              CHAR(8),
    is_ofac_sanctioned_yn             CHAR(1)        NOT NULL DEFAULT 'F',
    is_un_sanctioned_yn               CHAR(1)        NOT NULL DEFAULT 'F',
    is_eu_sanctioned_yn               CHAR(1)        NOT NULL DEFAULT 'F',
    is_kr_sanctioned_yn               CHAR(1)        NOT NULL DEFAULT 'F',
    is_sanctioned_yn                  CHAR(1)        GENERATED ALWAYS AS (
                                          CASE
                                              WHEN is_ofac_sanctioned_yn = 'T'
                                                OR is_un_sanctioned_yn   = 'T'
                                                OR is_eu_sanctioned_yn   = 'T'
                                                OR is_kr_sanctioned_yn   = 'T'
                                              THEN 'T' ELSE 'F'
                                          END
                                      ) STORED,
    sanction_last_screened_at         TIMESTAMPTZ(3),
    sanction_next_screen_date         CHAR(8),
    kyc_status_code                   VARCHAR(20)    NOT NULL,
    kyc_completed_at                  TIMESTAMPTZ(3),
    kyc_expiry_date                   CHAR(8),
    kyc_next_review_date              CHAR(8),
    identity_verification_method_code VARCHAR(10),
    cdd_level_code                    VARCHAR(20)    NOT NULL,
    cdd_last_reviewed_at              TIMESTAMPTZ(3),
    cdd_next_review_date              CHAR(8),
    edd_required_yn                   CHAR(1)        NOT NULL DEFAULT 'F',
    edd_last_reviewed_at              TIMESTAMPTZ(3),
    edd_next_review_date              CHAR(8),
    fatca_status_code                 VARCHAR(20)    NOT NULL,
    fatca_last_reviewed_at            TIMESTAMPTZ(3),
    fatca_next_review_date            CHAR(8),
    fatca_reportable_yn               CHAR(1)        NOT NULL DEFAULT 'F',
    crs_status_code                   VARCHAR(20)    NOT NULL,
    crs_last_reviewed_at              TIMESTAMPTZ(3),
    crs_next_review_date              CHAR(8),
    crs_reportable_yn                 CHAR(1)        NOT NULL DEFAULT 'F',
    created_at                        TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                        BIGINT,
    updated_at                        TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by                        BIGINT,
    deleted_at                        TIMESTAMPTZ(3),
    deleted_by                        BIGINT,
    version                           INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_compliance_info PRIMARY KEY (party_id),
    CONSTRAINT fk_compliance_info_party FOREIGN KEY (party_id) REFERENCES party (party_id)
);

-- -----------------------------------------------------------------------------
-- 7. tax_residency_info  (납세거주정보, party 1:N)
-- -----------------------------------------------------------------------------
CREATE TABLE tax_residency_info (
    tax_residency_id           BIGINT         GENERATED ALWAYS AS IDENTITY,
    party_id                   BIGINT         NOT NULL,
    resident_type_code         VARCHAR(20)    NOT NULL,
    tax_country_code           CHAR(3),
    foreign_tin                VARCHAR(50),
    withholding_rate_bps       INT,
    tax_residency_confirm_date CHAR(8)        NOT NULL,
    created_at                 TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                 BIGINT,
    updated_at                 TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by                 BIGINT,
    deleted_at                 TIMESTAMPTZ(3),
    deleted_by                 BIGINT,
    version                    INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_tax_residency_info PRIMARY KEY (tax_residency_id),
    CONSTRAINT fk_tax_residency_info_party FOREIGN KEY (party_id) REFERENCES party (party_id)
);

-- -----------------------------------------------------------------------------
-- 8. party_role  (관계자역할)
-- -----------------------------------------------------------------------------
CREATE TABLE party_role (
    role_id              BIGINT         GENERATED ALWAYS AS IDENTITY,
    party_id             BIGINT         NOT NULL,
    role_type_code       VARCHAR(20)    NOT NULL,
    role_status_code     VARCHAR(20)    NOT NULL,
    role_start_date      CHAR(8)        NOT NULL,
    role_end_date        CHAR(8),
    role_end_reason_code VARCHAR(20),
    created_at           TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by           BIGINT,
    updated_at           TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by           BIGINT,
    deleted_at           TIMESTAMPTZ(3),
    deleted_by           BIGINT,
    version              INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_party_role PRIMARY KEY (role_id),
    CONSTRAINT fk_party_role_party FOREIGN KEY (party_id) REFERENCES party (party_id),
    CONSTRAINT chk_party_role_end CHECK (
        (role_status_code = 'CLOSED' AND role_end_date IS NOT NULL AND role_end_reason_code IS NOT NULL)
        OR role_status_code <> 'CLOSED'
    )
);

-- -----------------------------------------------------------------------------
-- 9. party_relation  (관계자관계, N:M self-ref)
-- -----------------------------------------------------------------------------
CREATE TABLE party_relation (
    relation_id              BIGINT         GENERATED ALWAYS AS IDENTITY,
    from_party_id            BIGINT         NOT NULL,
    to_party_id              BIGINT         NOT NULL,
    relation_type_code       VARCHAR(10)    NOT NULL,
    relation_detail_code     VARCHAR(10),
    equity_ratio_bps         INT,
    representation_scope     VARCHAR(200),
    proof_url                VARCHAR(500),
    relation_start_date      CHAR(8)        NOT NULL,
    relation_end_date        CHAR(8),
    relation_end_reason_code VARCHAR(20),
    created_at               TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by               BIGINT,
    updated_at               TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by               BIGINT,
    deleted_at               TIMESTAMPTZ(3),
    deleted_by               BIGINT,
    version                  INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_party_relation PRIMARY KEY (relation_id),
    CONSTRAINT fk_party_relation_from FOREIGN KEY (from_party_id) REFERENCES party (party_id),
    CONSTRAINT fk_party_relation_to   FOREIGN KEY (to_party_id)   REFERENCES party (party_id),
    CONSTRAINT chk_party_relation_no_self CHECK (from_party_id <> to_party_id)
);

-- -----------------------------------------------------------------------------
-- 10. business_info  (사업자정보)
-- -----------------------------------------------------------------------------
CREATE TABLE business_info (
    business_info_id   BIGINT         GENERATED ALWAYS AS IDENTITY,
    party_id           BIGINT         NOT NULL,
    biz_reg_no         CHAR(12)       NOT NULL,
    biz_status_code    VARCHAR(20)    NOT NULL,
    trade_name         VARCHAR(200)   NOT NULL,
    english_trade_name VARCHAR(400),
    opening_date       CHAR(8)        NOT NULL,
    closing_date       CHAR(8),
    nts_industry_code  CHAR(6)        NOT NULL,
    ksic_code          CHAR(5)        NOT NULL,
    biz_type_code      VARCHAR(10),
    biz_item_code      VARCHAR(10)    NOT NULL,
    tax_type_code      VARCHAR(10)    NOT NULL,
    created_at         TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by         BIGINT,
    updated_at         TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by         BIGINT,
    deleted_at         TIMESTAMPTZ(3),
    deleted_by         BIGINT,
    version            INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_business_info PRIMARY KEY (business_info_id),
    CONSTRAINT fk_business_info_party FOREIGN KEY (party_id) REFERENCES party (party_id),
    CONSTRAINT uq_business_info_biz_reg_no UNIQUE (biz_reg_no)
);

-- -----------------------------------------------------------------------------
-- 11. customer  (고객)
-- -----------------------------------------------------------------------------
CREATE TABLE customer (
    customer_id              BIGINT         GENERATED ALWAYS AS IDENTITY,
    party_id                 BIGINT         NOT NULL,
    customer_grade_code      VARCHAR(10),
    customer_status_code     VARCHAR(20)    NOT NULL,
    main_customer_yn         CHAR(1)        NOT NULL DEFAULT 'F',
    credit_rating_code       VARCHAR(10),
    credit_evaluation_date   CHAR(8),
    credit_agency_code       VARCHAR(10),
    preferred_language_code  CHAR(2),
    sms_receive_yn           CHAR(1)        NOT NULL DEFAULT 'F',
    email_receive_yn         CHAR(1)        NOT NULL DEFAULT 'F',
    postal_receive_yn        CHAR(1)        NOT NULL DEFAULT 'F',
    notification_method_code VARCHAR(10),
    email                    VARCHAR(255),
    phone                    VARCHAR(20),
    zip_code                 VARCHAR(10),
    address                  VARCHAR(255),
    address_detail           VARCHAR(255),
    join_channel_code        VARCHAR(20),
    first_join_date          CHAR(8),
    joined_at                TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_transaction_at      TIMESTAMPTZ(3),
    dormant_at               TIMESTAMPTZ(3),
    closed_at                TIMESTAMPTZ(3),
    close_reason_code        VARCHAR(20),
    privacy_expiry_date      CHAR(8),
    created_at               TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by               BIGINT,
    updated_at               TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by               BIGINT,
    deleted_at               TIMESTAMPTZ(3),
    deleted_by               BIGINT,
    version                  INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_customer PRIMARY KEY (customer_id),
    CONSTRAINT fk_customer_party FOREIGN KEY (party_id) REFERENCES party (party_id),
    CONSTRAINT chk_customer_lifecycle CHECK (
        (customer_status_code = 'CLOSED'  AND closed_at IS NOT NULL AND close_reason_code IS NOT NULL)
        OR (customer_status_code = 'DORMANT' AND dormant_at IS NOT NULL)
        OR customer_status_code = 'ACTIVE'
    )
);

-- -----------------------------------------------------------------------------
-- 12. customer_status_history  (고객상태이력, 로그 테이블 — soft delete 미적용)
-- -----------------------------------------------------------------------------
CREATE TABLE customer_status_history (
    customer_status_history_id          BIGINT         GENERATED ALWAYS AS IDENTITY,
    previous_customer_status_history_id BIGINT,
    customer_id                         BIGINT         NOT NULL,
    customer_status_code                VARCHAR(20)    NOT NULL,
    previous_customer_status_code       VARCHAR(20),
    customer_status_change_reason_code  VARCHAR(20)    NOT NULL,
    customer_status_change_reason_detail VARCHAR(500),
    customer_status_effective_start_at  TIMESTAMPTZ(3) NOT NULL,
    customer_status_effective_end_at    TIMESTAMPTZ(3),
    system_auto_triggered_yn            CHAR(1)        NOT NULL DEFAULT 'F',
    created_at                          TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                          BIGINT,
    CONSTRAINT pk_customer_status_history PRIMARY KEY (customer_status_history_id),
    CONSTRAINT fk_customer_status_history_customer
        FOREIGN KEY (customer_id) REFERENCES customer (customer_id),
    CONSTRAINT fk_customer_status_history_self
        FOREIGN KEY (previous_customer_status_history_id)
        REFERENCES customer_status_history (customer_status_history_id)
);

-- -----------------------------------------------------------------------------
-- 13. customer_grade_history  (고객등급이력, 로그 테이블 — soft delete 미적용)
-- -----------------------------------------------------------------------------
CREATE TABLE customer_grade_history (
    customer_grade_history_id          BIGINT         GENERATED ALWAYS AS IDENTITY,
    previous_customer_grade_history_id BIGINT,
    customer_id                        BIGINT         NOT NULL,
    customer_grade_code                VARCHAR(10)    NOT NULL,
    previous_customer_grade_code       VARCHAR(10),
    customer_grade_change_reason_code  VARCHAR(20)    NOT NULL,
    customer_grade_change_reason_detail VARCHAR(500),
    customer_grade_effective_start_date CHAR(8)       NOT NULL,
    customer_grade_effective_end_date   CHAR(8),
    customer_grade_evaluated_at        TIMESTAMPTZ(3) NOT NULL,
    system_auto_triggered_yn           CHAR(1)        NOT NULL DEFAULT 'F',
    created_at                         TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                         BIGINT,
    CONSTRAINT pk_customer_grade_history PRIMARY KEY (customer_grade_history_id),
    CONSTRAINT fk_customer_grade_history_customer
        FOREIGN KEY (customer_id) REFERENCES customer (customer_id),
    CONSTRAINT fk_customer_grade_history_self
        FOREIGN KEY (previous_customer_grade_history_id)
        REFERENCES customer_grade_history (customer_grade_history_id)
);

-- =============================================================================
-- 인덱스
-- =============================================================================

-- party별 활성 고객 1건 제한 (CLOSED 제외)
CREATE UNIQUE INDEX uq_customer_active_per_party
    ON customer (party_id)
    WHERE customer_status_code <> 'CLOSED' AND deleted_at IS NULL;

-- 활성 관계자관계 중복 방지
CREATE UNIQUE INDEX uq_party_relation_active
    ON party_relation (from_party_id, to_party_id, relation_type_code)
    WHERE relation_end_date IS NULL AND deleted_at IS NULL;

CREATE INDEX idx_party_relation_from
    ON party_relation (from_party_id, relation_type_code);

CREATE INDEX idx_party_relation_to
    ON party_relation (to_party_id, relation_type_code);

CREATE INDEX idx_party_role_active
    ON party_role (party_id, role_status_code)
    WHERE role_status_code = 'ACTIVE' AND deleted_at IS NULL;

CREATE INDEX idx_business_info_party
    ON business_info (party_id)
    WHERE deleted_at IS NULL;
