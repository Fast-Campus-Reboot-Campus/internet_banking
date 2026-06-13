-- ============================================================
-- SERVICE: customer-service  (DB: customer_db)
-- ============================================================

-- ---- V1__create_customer_schema.sql ----
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

-- ---- V2__create_auth_security_schema.sql ----
-- =============================================================================
-- V2: 인증보안계 스키마 생성
-- 테이블 15개 + 인덱스
-- 전제: V1 (고객계) 적용 완료 후 실행 — customer.customer_id FK 기준점
--
-- 순환 참조 처리 (login_session ↔ api_token):
--   login_session.token_id  → api_token.token_id   DEFERRABLE INITIALLY DEFERRED
--   api_token.session_id    → login_session.session_id  DEFERRABLE INITIALLY DEFERRED
--   → 동일 트랜잭션 내 세션 INSERT → 토큰 INSERT → 세션 UPDATE token_id → COMMIT 순서 적용
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. fds_rule  (FDS탐지룰)
-- -----------------------------------------------------------------------------
CREATE TABLE fds_rule (
    fds_rule_id                BIGINT         GENERATED ALWAYS AS IDENTITY,
    fds_rule_code              VARCHAR(30)    NOT NULL,
    fds_rule_name              VARCHAR(100)   NOT NULL,
    fds_rule_category_code     VARCHAR(30)    NOT NULL,
    fds_rule_target_event_code VARCHAR(50)    NOT NULL,
    fds_rule_condition_json    JSON           NOT NULL,
    fds_rule_risk_weight       INT            NOT NULL DEFAULT 50,
    fds_rule_action_type_code  VARCHAR(20)    NOT NULL,
    fds_rule_active_yn         CHAR(1)        NOT NULL DEFAULT 'F',
    fds_rule_effective_date    CHAR(8)        NOT NULL,
    fds_rule_expiry_date       CHAR(8),
    created_at                 TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                 BIGINT,
    updated_at                 TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by                 BIGINT,
    deleted_at                 TIMESTAMPTZ(3),
    deleted_by                 BIGINT,
    version                    INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_fds_rule PRIMARY KEY (fds_rule_id),
    CONSTRAINT chk_fds_rule_action_type CHECK (fds_rule_action_type_code IN ('BLOCK','CHALLENGE','MONITOR')),
    CONSTRAINT chk_fds_rule_active      CHECK (fds_rule_active_yn IN ('T','F')),
    CONSTRAINT chk_fds_rule_risk_weight CHECK (fds_rule_risk_weight BETWEEN 0 AND 100)
);

-- -----------------------------------------------------------------------------
-- 2. credential  (계정자격증명)
-- -----------------------------------------------------------------------------
CREATE TABLE credential (
    credential_id                    BIGINT         GENERATED ALWAYS AS IDENTITY,
    customer_id                      BIGINT         NOT NULL,
    login_id                         VARCHAR(50)    NOT NULL,
    password_hash                    VARCHAR(255)   NOT NULL,
    password_changed_at              TIMESTAMPTZ(3) NOT NULL,
    password_expiry_at               TIMESTAMPTZ(3),
    account_status_code              VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    password_login_failure_count     INT            NOT NULL DEFAULT 0,
    max_password_login_failure_count INT            NOT NULL DEFAULT 5,
    password_login_locked_at         TIMESTAMPTZ(3),
    password_login_unlocked_at       TIMESTAMPTZ(3),
    password_last_login_at           TIMESTAMPTZ(3),
    created_at                       TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                       BIGINT,
    updated_at                       TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by                       BIGINT,
    deleted_at                       TIMESTAMPTZ(3),
    deleted_by                       BIGINT,
    version                          INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_credential PRIMARY KEY (credential_id),
    CONSTRAINT fk_credential_customer FOREIGN KEY (customer_id) REFERENCES customer (customer_id),
    CONSTRAINT chk_credential_account_status
        CHECK (account_status_code IN ('ACTIVE','LOCKED','DORMANT','CLOSED'))
);

-- -----------------------------------------------------------------------------
-- 3. registered_device  (등록기기)
-- -----------------------------------------------------------------------------
CREATE TABLE registered_device (
    device_id               BIGINT         GENERATED ALWAYS AS IDENTITY,
    customer_id             BIGINT         NOT NULL,
    device_name             VARCHAR(100),
    device_type_code        VARCHAR(20)    NOT NULL,
    device_os_name          VARCHAR(50),
    device_os_version       VARCHAR(50),
    device_fingerprint_hash VARCHAR(255)   NOT NULL,
    trusted_device_yn       CHAR(1)        NOT NULL DEFAULT 'F',
    designated_pc_yn        CHAR(1)        NOT NULL DEFAULT 'F',
    device_registered_ip    VARCHAR(45)    NOT NULL,
    device_last_used_at     TIMESTAMPTZ(3),
    device_status_code      VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at              TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ(3),
    deleted_by              BIGINT,
    version                 INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_registered_device PRIMARY KEY (device_id),
    CONSTRAINT fk_registered_device_customer
        FOREIGN KEY (customer_id) REFERENCES customer (customer_id),
    CONSTRAINT chk_registered_device_type
        CHECK (device_type_code IN ('MOBILE','PC','TABLET')),
    CONSTRAINT chk_registered_device_status
        CHECK (device_status_code IN ('ACTIVE','SUSPENDED','REVOKED')),
    CONSTRAINT chk_registered_device_trusted
        CHECK (trusted_device_yn IN ('T','F')),
    CONSTRAINT chk_registered_device_designated_pc
        CHECK (designated_pc_yn IN ('T','F'))
);

-- -----------------------------------------------------------------------------
-- 4. auth_method  (인증수단)
-- -----------------------------------------------------------------------------
CREATE TABLE auth_method (
    auth_method_id              BIGINT         GENERATED ALWAYS AS IDENTITY,
    customer_id                 BIGINT         NOT NULL,
    auth_method_type_code       VARCHAR(20)    NOT NULL,
    auth_method_alias_name      VARCHAR(50),
    auth_method_status_code     VARCHAR(20)    NOT NULL,
    primary_auth_method_yn      CHAR(1)        NOT NULL DEFAULT 'F',
    auth_method_registered_date CHAR(8)        NOT NULL,
    auth_method_expiry_date     CHAR(8),
    auth_method_last_used_at    TIMESTAMPTZ(3),
    created_at                  TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                  BIGINT,
    updated_at                  TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by                  BIGINT,
    deleted_at                  TIMESTAMPTZ(3),
    deleted_by                  BIGINT,
    version                     INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_auth_method PRIMARY KEY (auth_method_id),
    CONSTRAINT fk_auth_method_customer
        FOREIGN KEY (customer_id) REFERENCES customer (customer_id),
    CONSTRAINT chk_auth_method_type
        CHECK (auth_method_type_code IN ('SMS','PASS','CERT_FIN','CERT_COMMON','PIN','BIO_FACE','BIO_FINGER')),
    CONSTRAINT chk_auth_method_primary
        CHECK (primary_auth_method_yn IN ('T','F'))
);

-- -----------------------------------------------------------------------------
-- 5. certificate  (금융인증서)
--    cert_login_failure_count / max_cert_login_failure_count: nullable (ERD 확정)
-- -----------------------------------------------------------------------------
CREATE TABLE certificate (
    certificate_id                     BIGINT         GENERATED ALWAYS AS IDENTITY,
    customer_id                        BIGINT         NOT NULL,
    auth_method_id                     BIGINT         NOT NULL,
    certificate_type_code              VARCHAR(20)    NOT NULL,
    certificate_serial_number          VARCHAR(100)   NOT NULL,
    certificate_issuer_name            VARCHAR(50)    NOT NULL,
    certificate_subject_dn             TEXT           NOT NULL,
    certificate_issuer_dn              TEXT           NOT NULL,
    certificate_public_key             TEXT           NOT NULL,
    certificate_purpose_code           VARCHAR(50)    NOT NULL,
    certificate_issued_date            CHAR(8)        NOT NULL,
    certificate_expiry_date            CHAR(8)        NOT NULL,
    certificate_renewal_scheduled_date CHAR(8),
    certificate_status_code            VARCHAR(20)    NOT NULL,
    certificate_revoke_reason_code     VARCHAR(200),
    certificate_revoked_at             TIMESTAMPTZ(3),
    cert_login_failure_count           INT,
    max_cert_login_failure_count       INT,
    last_cert_login_failure_at         TIMESTAMPTZ(3),
    cert_login_locked_at               TIMESTAMPTZ(3),
    cert_login_unlocked_at             TIMESTAMPTZ(3),
    created_at                         TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                         BIGINT,
    updated_at                         TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by                         BIGINT,
    deleted_at                         TIMESTAMPTZ(3),
    deleted_by                         BIGINT,
    version                            INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_certificate PRIMARY KEY (certificate_id),
    CONSTRAINT uq_certificate_serial_number UNIQUE (certificate_serial_number),
    CONSTRAINT fk_certificate_customer
        FOREIGN KEY (customer_id) REFERENCES customer (customer_id),
    CONSTRAINT fk_certificate_auth_method
        FOREIGN KEY (auth_method_id) REFERENCES auth_method (auth_method_id),
    CONSTRAINT chk_certificate_status
        CHECK (certificate_status_code IN ('ACTIVE','EXPIRED','REVOKED','SUSPENDED'))
);

-- -----------------------------------------------------------------------------
-- 6. mobile_auth  (휴대폰인증요청, 이력 테이블 — soft delete 적용)
--    customer_id: 가입 전 본인확인 허용 → nullable
-- -----------------------------------------------------------------------------
CREATE TABLE mobile_auth (
    mobile_auth_id                     BIGINT         GENERATED ALWAYS AS IDENTITY,
    customer_id                        BIGINT,
    mobile_auth_method_type_code       VARCHAR(20)    NOT NULL,
    mobile_auth_telecom_carrier_code   VARCHAR(20)    NOT NULL,
    mobile_auth_recipient_phone_number VARCHAR(20)    NOT NULL,
    mobile_auth_code_hash              VARCHAR(255)   NOT NULL,
    mobile_auth_purpose_code           VARCHAR(30)    NOT NULL,
    mobile_auth_request_ip             VARCHAR(45)    NOT NULL,
    mobile_auth_request_channel_code   VARCHAR(20)    NOT NULL,
    mobile_auth_sent_at                TIMESTAMPTZ(3) NOT NULL,
    mobile_auth_expiry_at              TIMESTAMPTZ(3) NOT NULL,
    mobile_auth_verified_at            TIMESTAMPTZ(3),
    mobile_auth_verified_yn            CHAR(1)        NOT NULL DEFAULT 'F',
    mobile_auth_attempt_count          INT            NOT NULL DEFAULT 0,
    mobile_auth_failure_reason_code    VARCHAR(200),
    created_at                         TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                         BIGINT,
    updated_at                         TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by                         BIGINT,
    deleted_at                         TIMESTAMPTZ(3),
    deleted_by                         BIGINT,
    version                            INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_mobile_auth PRIMARY KEY (mobile_auth_id),
    CONSTRAINT fk_mobile_auth_customer
        FOREIGN KEY (customer_id) REFERENCES customer (customer_id),
    CONSTRAINT chk_mobile_auth_verified
        CHECK (mobile_auth_verified_yn IN ('T','F'))
);

-- -----------------------------------------------------------------------------
-- 7. login_attempt  (로그인시도이력, 이력 테이블 — soft delete 적용)
--    customer_id: 미존재 ID로 시도 허용 → nullable
--    device_id:   미등록 기기 허용 → nullable
-- -----------------------------------------------------------------------------
CREATE TABLE login_attempt (
    login_attempt_id                      BIGINT         GENERATED ALWAYS AS IDENTITY,
    customer_id                           BIGINT,
    device_id                             BIGINT,
    attempted_login_id                    VARCHAR(50)    NOT NULL,
    login_attempt_channel_code            VARCHAR(20)    NOT NULL,
    login_attempt_ip                      VARCHAR(45)    NOT NULL,
    login_attempt_ip_country_code         CHAR(3),
    login_attempt_user_agent              TEXT,
    login_attempt_device_fingerprint_hash VARCHAR(255),
    login_attempt_success_yn              CHAR(1)        NOT NULL DEFAULT 'F',
    login_attempt_failure_reason_code     VARCHAR(20),
    login_attempted_at                    TIMESTAMPTZ(3) NOT NULL,
    created_at                            TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                            BIGINT,
    updated_at                            TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by                            BIGINT,
    deleted_at                            TIMESTAMPTZ(3),
    deleted_by                            BIGINT,
    version                               INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_login_attempt PRIMARY KEY (login_attempt_id),
    CONSTRAINT fk_login_attempt_customer
        FOREIGN KEY (customer_id) REFERENCES customer (customer_id),
    CONSTRAINT fk_login_attempt_device
        FOREIGN KEY (device_id) REFERENCES registered_device (device_id),
    CONSTRAINT chk_login_attempt_success
        CHECK (login_attempt_success_yn IN ('T','F'))
);

-- -----------------------------------------------------------------------------
-- 8. login_session  (로그인세션)
--    token_id FK(→ api_token)는 순환 참조로 인해 api_token 생성 후 아래서 추가
--    fk_login_session_token: DEFERRABLE INITIALLY DEFERRED
-- -----------------------------------------------------------------------------
CREATE TABLE login_session (
    session_id               VARCHAR(64)    NOT NULL,
    customer_id              BIGINT         NOT NULL,
    login_attempt_id         BIGINT         NOT NULL,
    device_id                BIGINT,
    token_id                 BIGINT         NOT NULL,
    session_issued_ip        VARCHAR(45)    NOT NULL,
    session_channel_code     VARCHAR(20)    NOT NULL,
    session_status_code      VARCHAR(20)    NOT NULL,
    session_mfa_completed_yn CHAR(1)        NOT NULL DEFAULT 'F',
    session_expiry_at        TIMESTAMPTZ(3) NOT NULL,
    session_ended_at         TIMESTAMPTZ(3),
    session_end_reason_code  VARCHAR(20),
    created_at               TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by               BIGINT,
    updated_at               TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by               BIGINT,
    deleted_at               TIMESTAMPTZ(3),
    deleted_by               BIGINT,
    version                  INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_login_session PRIMARY KEY (session_id),
    CONSTRAINT fk_login_session_customer
        FOREIGN KEY (customer_id) REFERENCES customer (customer_id),
    CONSTRAINT fk_login_session_login_attempt
        FOREIGN KEY (login_attempt_id) REFERENCES login_attempt (login_attempt_id),
    CONSTRAINT fk_login_session_device
        FOREIGN KEY (device_id) REFERENCES registered_device (device_id),
    CONSTRAINT chk_login_session_status
        CHECK (session_status_code IN ('ACTIVE','EXPIRED','LOGGED_OUT','FORCED_OUT')),
    CONSTRAINT chk_login_session_mfa
        CHECK (session_mfa_completed_yn IN ('T','F'))
    -- fk_login_session_token 은 api_token 생성 후 ALTER TABLE로 추가 (아래 참조)
);

-- -----------------------------------------------------------------------------
-- 9. api_token  (API토큰, 이력 테이블 — soft delete 적용)
--    created_at / updated_at / updated_by: ERD isAllowNull=true → nullable 유지
--    (미결정 사항 §3.9: NOT NULL 통일 여부 추후 결정)
--    session_id FK: 순환 참조 → DEFERRABLE INITIALLY DEFERRED
-- -----------------------------------------------------------------------------
CREATE TABLE api_token (
    token_id                  BIGINT         GENERATED ALWAYS AS IDENTITY,
    customer_id               BIGINT         NOT NULL,
    session_id                VARCHAR(64)    NOT NULL,
    token_type_code           VARCHAR(20)    NOT NULL,
    token_hash                VARCHAR(255)   NOT NULL,
    token_issued_channel_code VARCHAR(20)    NOT NULL,
    token_scope               VARCHAR(500),
    token_client_id           VARCHAR(50),
    token_issued_at           TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    token_expiry_at           TIMESTAMPTZ(3) NOT NULL,
    token_revoked_at          TIMESTAMPTZ(3),
    token_revoke_reason_code  VARCHAR(20),
    created_at                TIMESTAMPTZ(3) DEFAULT CURRENT_TIMESTAMP(3),
    created_by                BIGINT,
    updated_at                TIMESTAMPTZ(3),
    updated_by                BIGINT,
    deleted_at                TIMESTAMPTZ(3),
    deleted_by                BIGINT,
    version                   INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_api_token PRIMARY KEY (token_id),
    CONSTRAINT fk_api_token_customer
        FOREIGN KEY (customer_id) REFERENCES customer (customer_id),
    CONSTRAINT fk_api_token_session
        FOREIGN KEY (session_id) REFERENCES login_session (session_id)
        DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT chk_api_token_type
        CHECK (token_type_code IN ('ACCESS','REFRESH','OAUTH'))
);

-- 순환 참조 FK 추가: login_session.token_id → api_token.token_id
ALTER TABLE login_session
    ADD CONSTRAINT fk_login_session_token
        FOREIGN KEY (token_id) REFERENCES api_token (token_id)
        DEFERRABLE INITIALLY DEFERRED;

-- -----------------------------------------------------------------------------
-- 10. pin  (간편비밀번호)
-- -----------------------------------------------------------------------------
CREATE TABLE pin (
    pin_id                      BIGINT         GENERATED ALWAYS AS IDENTITY,
    customer_id                 BIGINT         NOT NULL,
    auth_method_id              BIGINT         NOT NULL,
    device_id                   BIGINT         NOT NULL,
    pin_hash                    VARCHAR(255)   NOT NULL,
    pin_length                  INT            NOT NULL,
    pin_login_failure_count     INT            NOT NULL DEFAULT 0,
    max_pin_login_failure_count INT            NOT NULL DEFAULT 5,
    pin_login_locked_at         TIMESTAMPTZ(3),
    pin_login_unlocked_at       TIMESTAMPTZ(3),
    pin_last_login_at           TIMESTAMPTZ(3),
    pin_status_code             VARCHAR(20)    NOT NULL,
    created_at                  TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                  BIGINT,
    updated_at                  TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by                  BIGINT,
    deleted_at                  TIMESTAMPTZ(3),
    deleted_by                  BIGINT,
    version                     INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_pin PRIMARY KEY (pin_id),
    CONSTRAINT fk_pin_customer
        FOREIGN KEY (customer_id) REFERENCES customer (customer_id),
    CONSTRAINT fk_pin_auth_method
        FOREIGN KEY (auth_method_id) REFERENCES auth_method (auth_method_id),
    CONSTRAINT fk_pin_device
        FOREIGN KEY (device_id) REFERENCES registered_device (device_id)
);

-- -----------------------------------------------------------------------------
-- 11. password_history  (비밀번호이력, 이력 테이블 — soft delete 적용)
-- -----------------------------------------------------------------------------
CREATE TABLE password_history (
    password_history_id           BIGINT         GENERATED ALWAYS AS IDENTITY,
    credential_id                 BIGINT         NOT NULL,
    customer_id                   BIGINT         NOT NULL,
    password_hash                 VARCHAR(255)   NOT NULL,
    password_change_channel_code  VARCHAR(20)    NOT NULL,
    password_change_reason_code   VARCHAR(200),
    password_change_ip            VARCHAR(45),
    created_at                    TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                    BIGINT,
    updated_at                    TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by                    BIGINT,
    deleted_at                    TIMESTAMPTZ(3),
    deleted_by                    BIGINT,
    version                       INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_password_history PRIMARY KEY (password_history_id),
    CONSTRAINT fk_password_history_credential
        FOREIGN KEY (credential_id) REFERENCES credential (credential_id),
    CONSTRAINT fk_password_history_customer
        FOREIGN KEY (customer_id) REFERENCES customer (customer_id)
);

-- -----------------------------------------------------------------------------
-- 12. fds_detection  (FDS탐지결과, 이력 테이블 — soft delete 적용)
--    fds_detection_status_code DEFAULT 'PENDING'
--    ERD 도구 오류: customer_id에 DEFAULT 'PENDING' 태기 → 무시, BIGINT NOT NULL 처리
-- -----------------------------------------------------------------------------
CREATE TABLE fds_detection (
    fds_detection_id                  BIGINT         GENERATED ALWAYS AS IDENTITY,
    customer_id                       BIGINT         NOT NULL,
    fds_rule_id                       BIGINT         NOT NULL,
    fds_detection_event_type_code     VARCHAR(30)    NOT NULL,
    fds_detection_event_reference_id  BIGINT         NOT NULL,
    fds_detected_at                   TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    fds_detection_status_code         VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    created_at                        TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                        BIGINT,
    updated_at                        TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by                        BIGINT,
    deleted_at                        TIMESTAMPTZ(3),
    deleted_by                        BIGINT,
    version                           INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_fds_detection PRIMARY KEY (fds_detection_id),
    CONSTRAINT fk_fds_detection_customer
        FOREIGN KEY (customer_id) REFERENCES customer (customer_id),
    CONSTRAINT fk_fds_detection_rule
        FOREIGN KEY (fds_rule_id) REFERENCES fds_rule (fds_rule_id),
    CONSTRAINT chk_fds_detection_status
        CHECK (fds_detection_status_code IN ('PENDING','CONFIRMED','FALSE_POSITIVE'))
);

-- -----------------------------------------------------------------------------
-- 13. fds_incident  (FDS사고처리, 이력 테이블 — soft delete 적용)
-- -----------------------------------------------------------------------------
CREATE TABLE fds_incident (
    fds_incident_id                  BIGINT         GENERATED ALWAYS AS IDENTITY,
    fds_detection_id                 BIGINT         NOT NULL,
    fds_incident_handler_employee_id BIGINT,
    fds_incident_type_code           VARCHAR(20)    NOT NULL,
    fds_incident_process_status_code VARCHAR(20)    NOT NULL,
    fds_incident_fss_reported_yn     CHAR(1)        NOT NULL DEFAULT 'F',
    fds_incident_reported_at         TIMESTAMPTZ(3),
    fds_incident_closed_at           TIMESTAMPTZ(3),
    created_at                       TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                       BIGINT,
    updated_at                       TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by                       BIGINT,
    deleted_at                       TIMESTAMPTZ(3),
    deleted_by                       BIGINT,
    version                          INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_fds_incident PRIMARY KEY (fds_incident_id),
    CONSTRAINT fk_fds_incident_detection
        FOREIGN KEY (fds_detection_id) REFERENCES fds_detection (fds_detection_id),
    CONSTRAINT chk_fds_incident_fss_reported
        CHECK (fds_incident_fss_reported_yn IN ('T','F'))
);

-- -----------------------------------------------------------------------------
-- 14. identity_verification  (본인확인이력, 이력 테이블 — soft delete 적용)
--    customer_id: SIGNUP 목적 시 customer 미생성 상태 허용 → nullable
-- -----------------------------------------------------------------------------
CREATE TABLE identity_verification (
    identity_verification_id                    BIGINT         GENERATED ALWAYS AS IDENTITY,
    customer_id                                 BIGINT,
    mobile_auth_id                              BIGINT         NOT NULL,
    identity_verification_agency_code           VARCHAR(30)    NOT NULL,
    identity_verification_purpose_code          VARCHAR(30)    NOT NULL,
    identity_verification_ci_value              VARCHAR(88)    NOT NULL,
    identity_verification_name                  VARCHAR(50)    NOT NULL,
    identity_verification_birth_date            CHAR(8)        NOT NULL,
    identity_verification_gender_code           CHAR(1)        NOT NULL,
    identity_verification_nationality_type_code VARCHAR(20)    NOT NULL,
    identity_verification_telecom_carrier_code  VARCHAR(20),
    identity_verification_phone_number          VARCHAR(20),
    identity_verified_at                        TIMESTAMPTZ(3) NOT NULL,
    created_at                                  TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                                  BIGINT,
    updated_at                                  TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by                                  BIGINT,
    deleted_at                                  TIMESTAMPTZ(3),
    deleted_by                                  BIGINT,
    version                                     INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_identity_verification PRIMARY KEY (identity_verification_id),
    CONSTRAINT fk_identity_verification_customer
        FOREIGN KEY (customer_id) REFERENCES customer (customer_id),
    CONSTRAINT fk_identity_verification_mobile_auth
        FOREIGN KEY (mobile_auth_id) REFERENCES mobile_auth (mobile_auth_id),
    CONSTRAINT chk_identity_verification_agency
        CHECK (identity_verification_agency_code IN ('NICE','KCB','SCI','PASS'))
);

-- -----------------------------------------------------------------------------
-- 15. certificate_use  (인증서사용이력, 이력 테이블 — soft delete 적용)
--    certificate_use_target_transaction_id: 거래계 미구축 → VARCHAR(50) 임시
--    거래계 구축 후 PK 타입 확인하여 타입 통일 필요 (§7 미래 연결 사항)
-- -----------------------------------------------------------------------------
CREATE TABLE certificate_use (
    certificate_use_id                       BIGINT         GENERATED ALWAYS AS IDENTITY,
    certificate_id                           BIGINT         NOT NULL,
    customer_id                              BIGINT         NOT NULL,
    purpose_code                             VARCHAR(30)    NOT NULL,
    certificate_use_target_transaction_id    VARCHAR(50),
    certificate_use_target_system_code       VARCHAR(20),
    certificate_use_signed_data_hash         VARCHAR(255)   NOT NULL,
    certificate_use_signature_value          TEXT           NOT NULL,
    certificate_use_verification_result_code VARCHAR(20)    NOT NULL,
    certificate_use_failure_reason_code      VARCHAR(200),
    certificate_use_request_ip               VARCHAR(45)    NOT NULL,
    certificate_use_request_channel_code     VARCHAR(20)    NOT NULL,
    certificate_used_at                      TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_at                               TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                               BIGINT,
    updated_at                               TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by                               BIGINT,
    deleted_at                               TIMESTAMPTZ(3),
    deleted_by                               BIGINT,
    version                                  INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_certificate_use PRIMARY KEY (certificate_use_id),
    CONSTRAINT fk_certificate_use_certificate
        FOREIGN KEY (certificate_id) REFERENCES certificate (certificate_id),
    CONSTRAINT fk_certificate_use_customer
        FOREIGN KEY (customer_id) REFERENCES customer (customer_id)
);

-- =============================================================================
-- 인덱스
-- =============================================================================

-- 활성 계정 내 로그인ID 중복 방지
CREATE UNIQUE INDEX uq_credential_active_login_id
    ON credential (login_id)
    WHERE deleted_at IS NULL;

-- 고객 기준 활성 기기 조회
CREATE INDEX idx_registered_device_customer
    ON registered_device (customer_id)
    WHERE deleted_at IS NULL;

-- 고객별 로그인 시도 이력 시간순 조회
CREATE INDEX idx_login_attempt_customer_at
    ON login_attempt (customer_id, login_attempted_at DESC);

-- 고객 활성 세션 만료 일정 조회
CREATE INDEX idx_login_session_customer_expiry
    ON login_session (customer_id, session_expiry_at)
    WHERE deleted_at IS NULL;

-- 자격증명별 비밀번호 이력 시간순 조회
CREATE INDEX idx_password_history_credential
    ON password_history (credential_id, created_at DESC);

-- 고객별 FDS 탐지 이력 시간순 조회
CREATE INDEX idx_fds_detection_customer_at
    ON fds_detection (customer_id, fds_detected_at DESC);

-- 인증서별 사용 이력 시간순 조회
CREATE INDEX idx_certificate_use_cert_at
    ON certificate_use (certificate_id, certificate_used_at DESC);

-- ---- V3__seed_employee_accounts.sql ----
-- =============================================================================
-- V3: 테스트용 직원 계정 시드
-- customer_id 9001 (지점장), 9002 (부지점장)
-- loginId: employee01 / employee02
-- password: Employee1234!
--   BCrypt hash: $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG
--   (= BCryptPasswordEncoder(10).encode("Employee1234!") 결과로 교체 필요 시 갱신)
-- =============================================================================

-- party
INSERT INTO party (party_id, party_type_code, party_name, party_status_code, version)
OVERRIDING SYSTEM VALUE VALUES
    (9001, 'PERSON', '지점장테스트', 'ACTIVE', 0),
    (9002, 'PERSON', '부지점장테스트', 'ACTIVE', 0);

-- party_person
INSERT INTO party_person (party_id, is_pep_yn, version)
VALUES
    (9001, 'F', 0),
    (9002, 'F', 0);

-- customer
INSERT INTO customer (customer_id, party_id, customer_status_code, main_customer_yn,
                      sms_receive_yn, email_receive_yn, postal_receive_yn,
                      joined_at, created_at, updated_at, version)
OVERRIDING SYSTEM VALUE VALUES
    (9001, 9001, 'ACTIVE', 'F', 'F', 'F', 'F', NOW(), NOW(), NOW(), 0),
    (9002, 9002, 'ACTIVE', 'F', 'F', 'F', 'F', NOW(), NOW(), NOW(), 0);

-- credential
-- password: Employee1234!  (BCrypt strength 10)
INSERT INTO credential (customer_id, login_id, password_hash,
                        password_changed_at, account_status_code,
                        created_at, updated_at, version)
VALUES
    (9001, 'employee01', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
     NOW(), 'ACTIVE', NOW(), NOW(), 0),
    (9002, 'employee02', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
     NOW(), 'ACTIVE', NOW(), NOW(), 0);

-- ---- V4__add_axful_cert_and_qr_login.sql ----
-- =============================================================================
-- V3: AXful 인증서 타입 추가 + QR코드 로그인 테이블 생성
-- 전제: V2 (인증보안계) 적용 완료 후 실행
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. auth_method 인증수단 타입에 CERT_AXFUL 추가
-- -----------------------------------------------------------------------------
ALTER TABLE auth_method
    DROP CONSTRAINT chk_auth_method_type;

ALTER TABLE auth_method
    ADD CONSTRAINT chk_auth_method_type
        CHECK (auth_method_type_code IN (
            'SMS', 'PASS', 'CERT_FIN', 'CERT_COMMON', 'CERT_AXFUL',
            'PIN', 'BIO_FACE', 'BIO_FINGER'
        ));

-- -----------------------------------------------------------------------------
-- 2. qr_login_token  (QR코드 로그인 토큰)
--    플로우: PC에서 QR 생성(PENDING) → 모바일 앱 스캔(SCANNED)
--            → 모바일에서 승인(APPROVED) → PC 세션 발급
--    session_id: 승인 완료 후 login_session과 연결
--    customer_id: 모바일 스캔 후 확인된 고객 (스캔 전 nullable)
-- -----------------------------------------------------------------------------
CREATE TABLE qr_login_token (
    qr_token_id      BIGINT         GENERATED ALWAYS AS IDENTITY,
    qr_token_hash    VARCHAR(255)   NOT NULL,
    customer_id      BIGINT,
    session_id       VARCHAR(64),
    qr_status_code   VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    request_ip       VARCHAR(45)    NOT NULL,
    request_channel_code VARCHAR(20) NOT NULL DEFAULT 'WEB',
    issued_at        TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    expiry_at        TIMESTAMPTZ(3) NOT NULL,
    scanned_at       TIMESTAMPTZ(3),
    approved_at      TIMESTAMPTZ(3),
    created_at       TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by       BIGINT,
    updated_at       TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by       BIGINT,
    deleted_at       TIMESTAMPTZ(3),
    deleted_by       BIGINT,
    version          INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_qr_login_token PRIMARY KEY (qr_token_id),
    CONSTRAINT uq_qr_login_token_hash UNIQUE (qr_token_hash),
    CONSTRAINT fk_qr_login_token_customer
        FOREIGN KEY (customer_id) REFERENCES customer (customer_id),
    CONSTRAINT fk_qr_login_token_session
        FOREIGN KEY (session_id) REFERENCES login_session (session_id),
    CONSTRAINT chk_qr_login_token_status
        CHECK (qr_status_code IN ('PENDING', 'SCANNED', 'APPROVED', 'EXPIRED', 'CANCELLED'))
);

-- -----------------------------------------------------------------------------
-- 인덱스
-- -----------------------------------------------------------------------------

-- QR 토큰 해시 조회 (PC 폴링용)
CREATE INDEX idx_qr_login_token_hash_status
    ON qr_login_token (qr_token_hash, qr_status_code)
    WHERE deleted_at IS NULL;

-- 만료된 토큰 정리 배치용
CREATE INDEX idx_qr_login_token_expiry
    ON qr_login_token (expiry_at)
    WHERE qr_status_code = 'PENDING' AND deleted_at IS NULL;

-- =============================================================================
-- 테스트 시드: auth_method + certificate (customer_id=1 기준)
-- PIN = 계정 비밀번호와 동일 (MVP 정책)
-- =============================================================================

-- auth_method 3건 (공동·금융·AXful)
INSERT INTO auth_method (customer_id, auth_method_type_code, auth_method_status_code,
                         primary_auth_method_yn, auth_method_registered_date,
                         created_at, updated_at, version)
VALUES
    (9001, 'CERT_COMMON', 'ACTIVE', 'F', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), NOW(), NOW(), 0),
    (9001, 'CERT_FIN',    'ACTIVE', 'T', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), NOW(), NOW(), 0),
    (9001, 'CERT_AXFUL',  'ACTIVE', 'F', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), NOW(), NOW(), 0);

-- certificate 3건
INSERT INTO certificate (customer_id, auth_method_id,
                         certificate_type_code, certificate_serial_number,
                         certificate_issuer_name, certificate_subject_dn, certificate_issuer_dn,
                         certificate_public_key, certificate_purpose_code,
                         certificate_issued_date, certificate_expiry_date,
                         certificate_status_code,
                         cert_login_failure_count, max_cert_login_failure_count,
                         created_at, updated_at, version)
SELECT
    9001,
    am.auth_method_id,
    am.auth_method_type_code,
    CASE am.auth_method_type_code
        WHEN 'CERT_COMMON' THEN 'COMMON-TEST-2024-000001'
        WHEN 'CERT_FIN'    THEN 'FINCERT-TEST-2024-000001'
        WHEN 'CERT_AXFUL'  THEN 'AXFUL-TEST-2024-000001'
    END,
    CASE am.auth_method_type_code
        WHEN 'CERT_COMMON' THEN '한국전자인증'
        WHEN 'CERT_FIN'    THEN '금융결제원'
        WHEN 'CERT_AXFUL'  THEN 'AXful Bank'
    END,
    'CN=홍길동, OU=Personal, O=AXful Bank, C=KR',
    CASE am.auth_method_type_code
        WHEN 'CERT_COMMON' THEN 'CN=한국전자인증CA, O=KECA, C=KR'
        WHEN 'CERT_FIN'    THEN 'CN=금융결제원CA, O=KFTC, C=KR'
        WHEN 'CERT_AXFUL'  THEN 'CN=AXful Bank CA, O=AXful Bank, C=KR'
    END,
    'RSA-PUBLIC-KEY-PLACEHOLDER',
    'LOGIN',
    TO_CHAR(CURRENT_DATE, 'YYYYMMDD'),
    TO_CHAR(CURRENT_DATE + INTERVAL '3 years', 'YYYYMMDD'),
    'ACTIVE',
    0, 5,
    NOW(), NOW(), 0
FROM auth_method am
WHERE am.customer_id = 9001
  AND am.auth_method_type_code IN ('CERT_COMMON', 'CERT_FIN', 'CERT_AXFUL')
  AND am.deleted_at IS NULL;

-- ---- V5__add_cert_pin_hash.sql ----
-- 인증서 암호(PIN)를 로그인 비밀번호와 분리
ALTER TABLE certificate
    ADD COLUMN cert_pin_hash VARCHAR(255);

-- ---- V6__add_withdrawal_account.sql ----
-- ============================================================
-- V6: 출금계좌 등록/삭제/순위변경 (인증보안계)
-- ============================================================

CREATE TABLE withdrawal_account (
    withdrawal_account_id BIGINT          GENERATED ALWAYS AS IDENTITY,
    customer_id           BIGINT          NOT NULL,
    account_number        VARCHAR(50)     NOT NULL,
    bank_code             VARCHAR(10)     NOT NULL,
    bank_name             VARCHAR(50)     NOT NULL,
    account_holder_name   VARCHAR(100),
    account_alias         VARCHAR(100),
    registration_type     VARCHAR(20)     NOT NULL DEFAULT 'ONLINE',
    priority_order        SMALLINT        NOT NULL DEFAULT 0,
    registered_at         TIMESTAMPTZ(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_at            TIMESTAMPTZ(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by            BIGINT,
    updated_at            TIMESTAMPTZ(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by            BIGINT,
    deleted_at            TIMESTAMPTZ(3),
    deleted_by            BIGINT,
    version               INT             NOT NULL DEFAULT 0,

    CONSTRAINT pk_withdrawal_account PRIMARY KEY (withdrawal_account_id),
    CONSTRAINT fk_withdrawal_account_customer
        FOREIGN KEY (customer_id) REFERENCES customer(customer_id)
);

-- 동일 고객의 활성(미삭제) 계좌는 계좌번호 중복 금지
CREATE UNIQUE INDEX uq_withdrawal_account_active
    ON withdrawal_account (customer_id, account_number)
    WHERE deleted_at IS NULL;

-- ---- V7__add_otp_security_card_auth_token.sql ----
-- =============================================================================
-- V5: OTP 기기, 보안카드, 인증토큰 테이블 추가
-- =============================================================================

-- auth_method 인증수단 타입에 OTP, SECURITY_CARD 추가
ALTER TABLE auth_method
    DROP CONSTRAINT IF EXISTS chk_auth_method_type;

ALTER TABLE auth_method
    ADD CONSTRAINT chk_auth_method_type
        CHECK (auth_method_type_code IN (
            'SMS', 'PASS', 'CERT_FIN', 'CERT_COMMON', 'CERT_AXFUL',
            'OTP', 'SECURITY_CARD'
        ));

-- =============================================================================
-- 1. otp_device (OTP 기기)
-- =============================================================================
CREATE TABLE otp_device (
    otp_device_id               BIGINT          GENERATED ALWAYS AS IDENTITY,
    customer_id                 BIGINT          NOT NULL,
    auth_method_id              BIGINT,
    otp_serial_number           VARCHAR(50)     NOT NULL,
    otp_seed_encrypted          BYTEA           NOT NULL,
    otp_type_code               VARCHAR(20)     NOT NULL DEFAULT 'TOTP',
    otp_status_code             VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    otp_issued_date             CHAR(8)         NOT NULL,
    otp_expiry_date             CHAR(8),
    otp_failure_count           INT             NOT NULL DEFAULT 0,
    max_otp_failure_count       INT             NOT NULL DEFAULT 5,
    created_at                  TIMESTAMPTZ(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                  BIGINT,
    updated_at                  TIMESTAMPTZ(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by                  BIGINT,
    deleted_at                  TIMESTAMPTZ(3),
    deleted_by                  BIGINT,
    version                     INT             NOT NULL DEFAULT 0,
    CONSTRAINT pk_otp_device PRIMARY KEY (otp_device_id),
    CONSTRAINT uq_otp_serial UNIQUE (otp_serial_number),
    CONSTRAINT fk_otp_device_customer    FOREIGN KEY (customer_id)   REFERENCES customer(customer_id),
    CONSTRAINT fk_otp_device_auth_method FOREIGN KEY (auth_method_id) REFERENCES auth_method(auth_method_id),
    CONSTRAINT chk_otp_type    CHECK (otp_type_code    IN ('TOTP', 'HOTP')),
    CONSTRAINT chk_otp_status  CHECK (otp_status_code  IN ('ACTIVE', 'INACTIVE', 'LOCKED'))
);

CREATE INDEX idx_otp_device_customer ON otp_device (customer_id) WHERE deleted_at IS NULL;

-- =============================================================================
-- 2. security_card (보안카드)
-- =============================================================================
CREATE TABLE security_card (
    security_card_id            BIGINT          GENERATED ALWAYS AS IDENTITY,
    customer_id                 BIGINT          NOT NULL,
    auth_method_id              BIGINT,
    security_card_number        VARCHAR(20)     NOT NULL,
    security_card_status_code   VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    security_card_issued_date   CHAR(8)         NOT NULL,
    security_card_expiry_date   CHAR(8),
    created_at                  TIMESTAMPTZ(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                  BIGINT,
    updated_at                  TIMESTAMPTZ(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by                  BIGINT,
    deleted_at                  TIMESTAMPTZ(3),
    deleted_by                  BIGINT,
    version                     INT             NOT NULL DEFAULT 0,
    CONSTRAINT pk_security_card PRIMARY KEY (security_card_id),
    CONSTRAINT uq_security_card_number UNIQUE (security_card_number),
    CONSTRAINT fk_security_card_customer    FOREIGN KEY (customer_id)    REFERENCES customer(customer_id),
    CONSTRAINT fk_security_card_auth_method FOREIGN KEY (auth_method_id) REFERENCES auth_method(auth_method_id),
    CONSTRAINT chk_security_card_status CHECK (security_card_status_code IN ('ACTIVE', 'INACTIVE', 'LOST'))
);

-- =============================================================================
-- 3. security_card_code (보안카드 격자 코드)
-- =============================================================================
CREATE TABLE security_card_code (
    security_card_code_id       BIGINT          GENERATED ALWAYS AS IDENTITY,
    security_card_id            BIGINT          NOT NULL,
    position_code               VARCHAR(4)      NOT NULL,
    code_hash                   VARCHAR(255)    NOT NULL,
    created_at                  TIMESTAMPTZ(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    version                     INT             NOT NULL DEFAULT 0,
    CONSTRAINT pk_security_card_code PRIMARY KEY (security_card_code_id),
    CONSTRAINT fk_security_card_code_card FOREIGN KEY (security_card_id) REFERENCES security_card(security_card_id),
    CONSTRAINT uq_security_card_position UNIQUE (security_card_id, position_code)
);

-- =============================================================================
-- 4. auth_token (인증토큰 — 결제계 연동)
-- =============================================================================
CREATE TABLE auth_token (
    auth_token_id               BIGINT          GENERATED ALWAYS AS IDENTITY,
    customer_id                 BIGINT          NOT NULL,
    auth_token_hash             VARCHAR(255)    NOT NULL,
    auth_method_type_code       VARCHAR(20)     NOT NULL,
    auth_token_purpose_code     VARCHAR(30)     NOT NULL,
    auth_token_status_code      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    auth_token_issued_at        TIMESTAMPTZ(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    auth_token_expiry_at        TIMESTAMPTZ(3)  NOT NULL,
    auth_token_used_at          TIMESTAMPTZ(3),
    created_at                  TIMESTAMPTZ(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                  BIGINT,
    updated_at                  TIMESTAMPTZ(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by                  BIGINT,
    deleted_at                  TIMESTAMPTZ(3),
    deleted_by                  BIGINT,
    version                     INT             NOT NULL DEFAULT 0,
    CONSTRAINT pk_auth_token PRIMARY KEY (auth_token_id),
    CONSTRAINT uq_auth_token_hash UNIQUE (auth_token_hash),
    CONSTRAINT fk_auth_token_customer FOREIGN KEY (customer_id) REFERENCES customer(customer_id),
    CONSTRAINT chk_auth_token_status CHECK (auth_token_status_code IN ('ACTIVE', 'USED', 'EXPIRED', 'REVOKED'))
);

CREATE INDEX idx_auth_token_customer  ON auth_token (customer_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_auth_token_hash      ON auth_token (auth_token_hash) WHERE deleted_at IS NULL;

-- ---- V8__seed_fds_rules.sql ----
-- =============================================================================
-- V8: 기본 FDS 탐지 룰 시드 데이터
-- 3종 룰: 로그인 실패 연속(BLOCK) / 인증서 실패 연속(BLOCK) / 비밀번호 잦은 변경(MONITOR)
-- =============================================================================

INSERT INTO fds_rule (
    fds_rule_code,
    fds_rule_name,
    fds_rule_category_code,
    fds_rule_target_event_code,
    fds_rule_condition_json,
    fds_rule_risk_weight,
    fds_rule_action_type_code,
    fds_rule_active_yn,
    fds_rule_effective_date,
    created_by,
    updated_by
) VALUES
-- 30분 내 로그인 실패 10회 → 차단
(
    'LOGIN_FAIL_BLOCK_10',
    '로그인 실패 10회 차단',
    'LOGIN_FAILURE_COUNT',
    'LOGIN_ATTEMPT',
    '{"window_minutes": 30, "threshold": 10}',
    90,
    'BLOCK',
    'T',
    TO_CHAR(CURRENT_DATE, 'YYYYMMDD'),
    0,
    0
),
-- 10분 내 인증서 로그인 실패 5회 → 차단
(
    'CERT_FAIL_BLOCK_5',
    '인증서 로그인 실패 5회 차단',
    'CERT_FAILURE_COUNT',
    'CERT_LOGIN',
    '{"window_minutes": 10, "threshold": 5}',
    85,
    'BLOCK',
    'T',
    TO_CHAR(CURRENT_DATE, 'YYYYMMDD'),
    0,
    0
),
-- 1일 내 비밀번호 변경 3회 이상 → 모니터링
(
    'PWD_CHANGE_MONITOR_3',
    '비밀번호 잦은 변경 모니터링',
    'PASSWORD_CHANGE_FREQ',
    'PASSWORD_CHANGE',
    '{"window_days": 1, "threshold": 3}',
    50,
    'MONITOR',
    'T',
    TO_CHAR(CURRENT_DATE, 'YYYYMMDD'),
    0,
    0
);

-- ---- V9__drop_unused_auth_methods_restore_pin_type.sql ----
-- =============================================================================
-- V9: 미사용 인증수단/토큰 정리 + auth_method 타입 복구
--
-- 배경
--  - V7이 추가한 otp_device(OTP기기)·security_card(보안카드)·security_card_code·
--    auth_token(인증토큰)은 설계문서(docs/auth_security_ddl_design.md)에 없고
--    참조 코드도 0건인 선반영 스키마다 → 제거하여 문서·코드와 일치시킨다.
--  - V7이 chk_auth_method_type을 재정의하며 'PIN'·생체(BIO_*)를 누락시켰다.
--    PIN 등록은 'PIN' 타입 인증수단을 생성하므로, 미복구 시 CHECK 위반으로
--    런타임 실패한다 → V4(=설계문서) 기준 타입 집합으로 복구한다.
--
--  ※ api_token(API토큰, 설계문서 #9)은 정상 정의이며 본 정리 대상이 아니다.
-- =============================================================================

-- 1. 미사용 테이블 제거 (자식 → 부모 순서)
DROP TABLE IF EXISTS security_card_code;
DROP TABLE IF EXISTS security_card;
DROP TABLE IF EXISTS otp_device;
DROP TABLE IF EXISTS auth_token;

-- 2. auth_method 인증수단 타입 CHECK 복구 (설계문서 기준 = V4 집합)
ALTER TABLE auth_method
    DROP CONSTRAINT IF EXISTS chk_auth_method_type;

ALTER TABLE auth_method
    ADD CONSTRAINT chk_auth_method_type
        CHECK (auth_method_type_code IN (
            'SMS', 'PASS', 'CERT_FIN', 'CERT_COMMON', 'CERT_AXFUL',
            'PIN', 'BIO_FACE', 'BIO_FINGER'
        ));

-- ---- V10__reset_seed_certificate_pin.sql ----
-- Reset seed certificate PINs so the web demo financial certificate login works.
-- V5 added cert_pin_hash but never backfilled the V4 seed certs, leaving them NULL.
-- A NULL hash makes CertLoginService fall back to the account password (Employee1234!),
-- which can never match the 6-digit PIN the web cert login pad sends.
-- PIN: 123456
--
-- 적용 범위: 아래 WHERE의 *-TEST-2024-000001 시리얼은 V4가 심는 데모 시드 전용이다.
-- 운영/스테이징에는 해당 시리얼이 없으므로 이 UPDATE는 0건 처리(no-op)된다.
-- (Flyway 마이그레이션은 프로파일 게이팅이 불가하므로, 공개 PIN 해시를 쓰는 본 UPDATE의
--  영향 범위를 시드 시리얼로 한정해 둔다. 기동 중 데모 DB 보정은 local 전용 CertificatePinSeedBackfill 담당.)
UPDATE certificate
SET cert_pin_hash = '$2a$10$D53MtwzNYduF8dFtg9rfxuTlv5rfN7nWWX72Lu1KyWC2gZ9ep6wwC',
    cert_login_failure_count = 0,
    last_cert_login_failure_at = NULL,
    cert_login_locked_at = NULL,
    cert_login_unlocked_at = NULL,
    updated_at = NOW()
WHERE certificate_serial_number IN (
    'COMMON-TEST-2024-000001',
    'FINCERT-TEST-2024-000001',
    'AXFUL-TEST-2024-000001'
)
  AND deleted_at IS NULL;

-- ---- V11__employee_directory_party_role.sql ----
-- =============================================================================
-- V11: 직원 디렉토리를 party_role 기반 DB 모델로 이관
--
--  기존: 직원 판정(branch/grade/roles)을 application.yml `employee-directory` 가 담당.
--  변경: party 패턴 정석대로 직원 party 에 EMPLOYEE 역할(party_role)을 부여하고,
--        직급·지점 등 직원 전용 속성은 신규 employee 테이블(party_person 과 같은
--        서브타입 디테일 테이블)에 저장한다. JWT roles 는 grade_code(=BankRole 값)에서 파생.
--
--  grade_code 는 common BankRole enum 의 이름을 그대로 쓴다 (예: HQ_RISK, BRANCH_MANAGER).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. employee  (직원, party_person 1:1 서브타입과 같은 위치)
-- -----------------------------------------------------------------------------
CREATE TABLE employee (
    employee_id  BIGINT         GENERATED ALWAYS AS IDENTITY,
    party_id     BIGINT         NOT NULL,
    branch_code  VARCHAR(10)    NOT NULL,
    grade_code   VARCHAR(30)    NOT NULL,
    status_code  VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by   BIGINT,
    updated_at   TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by   BIGINT,
    deleted_at   TIMESTAMPTZ(3),
    deleted_by   BIGINT,
    version      INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_employee PRIMARY KEY (employee_id),
    CONSTRAINT fk_employee_party FOREIGN KEY (party_id) REFERENCES party (party_id),
    CONSTRAINT uq_employee_party UNIQUE (party_id),
    CONSTRAINT chk_employee_status CHECK (status_code IN ('ACTIVE','CLOSED'))
);
CREATE INDEX idx_employee_party ON employee (party_id);

-- -----------------------------------------------------------------------------
-- 2. 관리자 콘솔 직원 7계정 시드 (party / party_person / customer / credential)
--    customer_id 9003~9009. password: Employee1234! (V3 와 동일 BCrypt 해시)
-- -----------------------------------------------------------------------------
INSERT INTO party (party_id, party_type_code, party_name, party_status_code, version)
OVERRIDING SYSTEM VALUE VALUES
    (9003, 'PERSON', '김감사',   'ACTIVE', 0),
    (9004, 'PERSON', '이심사',   'ACTIVE', 0),
    (9005, 'PERSON', '박리스크', 'ACTIVE', 0),
    (9006, 'PERSON', '최마케팅', 'ACTIVE', 0),
    (9007, 'PERSON', '정담당',   'ACTIVE', 0),
    (9008, 'PERSON', '한직원',   'ACTIVE', 0),
    (9009, 'PERSON', '오타지점', 'ACTIVE', 0);

INSERT INTO party_person (party_id, is_pep_yn, version)
VALUES (9003,'F',0),(9004,'F',0),(9005,'F',0),(9006,'F',0),(9007,'F',0),(9008,'F',0),(9009,'F',0);

INSERT INTO customer (customer_id, party_id, customer_status_code, main_customer_yn,
                      sms_receive_yn, email_receive_yn, postal_receive_yn,
                      joined_at, created_at, updated_at, version)
OVERRIDING SYSTEM VALUE VALUES
    (9003, 9003, 'ACTIVE', 'F', 'F', 'F', 'F', NOW(), NOW(), NOW(), 0),
    (9004, 9004, 'ACTIVE', 'F', 'F', 'F', 'F', NOW(), NOW(), NOW(), 0),
    (9005, 9005, 'ACTIVE', 'F', 'F', 'F', 'F', NOW(), NOW(), NOW(), 0),
    (9006, 9006, 'ACTIVE', 'F', 'F', 'F', 'F', NOW(), NOW(), NOW(), 0),
    (9007, 9007, 'ACTIVE', 'F', 'F', 'F', 'F', NOW(), NOW(), NOW(), 0),
    (9008, 9008, 'ACTIVE', 'F', 'F', 'F', 'F', NOW(), NOW(), NOW(), 0),
    (9009, 9009, 'ACTIVE', 'F', 'F', 'F', 'F', NOW(), NOW(), NOW(), 0);

-- password: Employee1234!  (BCrypt strength 10) — V3 와 동일 해시
INSERT INTO credential (customer_id, login_id, password_hash,
                        password_changed_at, account_status_code,
                        created_at, updated_at, version)
VALUES
    (9003, 'audit01',  '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', NOW(), 'ACTIVE', NOW(), NOW(), 0),
    (9004, 'review01', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', NOW(), 'ACTIVE', NOW(), NOW(), 0),
    (9005, 'risk01',   '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', NOW(), 'ACTIVE', NOW(), NOW(), 0),
    (9006, 'mkt01',    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', NOW(), 'ACTIVE', NOW(), NOW(), 0),
    (9007, 'owner01',  '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', NOW(), 'ACTIVE', NOW(), NOW(), 0),
    (9008, 'staff01',  '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', NOW(), 'ACTIVE', NOW(), NOW(), 0),
    (9009, 'other01',  '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', NOW(), 'ACTIVE', NOW(), NOW(), 0);

-- -----------------------------------------------------------------------------
-- 3. EMPLOYEE party_role — 기존 직원(9001/9002) + 신규 7계정(9003~9009)
--    "이 party 는 직원 역할을 한다"는 정식 표시. 인증 디렉토리의 게이트.
-- -----------------------------------------------------------------------------
INSERT INTO party_role (party_id, role_type_code, role_status_code, role_start_date, version)
VALUES
    (9001, 'EMPLOYEE', 'ACTIVE', '20260101', 0),
    (9002, 'EMPLOYEE', 'ACTIVE', '20260101', 0),
    (9003, 'EMPLOYEE', 'ACTIVE', '20260101', 0),
    (9004, 'EMPLOYEE', 'ACTIVE', '20260101', 0),
    (9005, 'EMPLOYEE', 'ACTIVE', '20260101', 0),
    (9006, 'EMPLOYEE', 'ACTIVE', '20260101', 0),
    (9007, 'EMPLOYEE', 'ACTIVE', '20260101', 0),
    (9008, 'EMPLOYEE', 'ACTIVE', '20260101', 0),
    (9009, 'EMPLOYEE', 'ACTIVE', '20260101', 0);

-- -----------------------------------------------------------------------------
-- 4. employee 디테일 — branch_code / grade_code(=BankRole)
--    9001/9002 는 yaml employee-directory 에서 이관, 9003~9009 는 관리자 7역할 매핑.
--    관리자 7역할 → BankRole:
--      감사→COMPLIANCE, 심사→HQ_REVIEWER, 리스크→HQ_RISK, 마케팅→HQ_MARKETING,
--      담당→BRANCH_MANAGER, 지점직원→TELLER.
--    (PRIMARY_OWNER/OTHER_BRANCH 는 정적 직급이 아니라 담당·지점 비교로 판정하는 동적 개념)
-- -----------------------------------------------------------------------------
INSERT INTO employee (party_id, branch_code, grade_code, status_code, version)
VALUES
    (9001, '0001', 'BRANCH_MANAGER', 'ACTIVE', 0),
    (9002, '0001', 'DEPUTY_MANAGER', 'ACTIVE', 0),
    (9003, '0000', 'COMPLIANCE',     'ACTIVE', 0),
    (9004, '0000', 'HQ_REVIEWER',    'ACTIVE', 0),
    (9005, '0000', 'HQ_RISK',        'ACTIVE', 0),
    (9006, '0000', 'HQ_MARKETING',   'ACTIVE', 0),
    (9007, '0001', 'BRANCH_MANAGER', 'ACTIVE', 0),
    (9008, '0001', 'TELLER',         'ACTIVE', 0),
    (9009, '0002', 'TELLER',         'ACTIVE', 0);

-- 시퀀스 동기화: OVERRIDING SYSTEM VALUE 로 명시 id 시드 시 IDENTITY 시퀀스가 올라가지 않아
-- 앱 생성 id 와 충돌한다. beforeEachMigrate 콜백도 맞추지만 본 마이그레이션에서도 보정한다.
SELECT setval(pg_get_serial_sequence('party', 'party_id'),
              COALESCE((SELECT MAX(party_id) FROM party), 1), true);
SELECT setval(pg_get_serial_sequence('customer', 'customer_id'),
              COALESCE((SELECT MAX(customer_id) FROM customer), 1), true);

-- ---- V12__seed_review_ops_employee_accounts.sql ----
-- =============================================================================
-- V12: 대출 본심사 매트릭스 시연용 직원 계정 보강
--
--  V11 이 시드한 관리자 7계정에는 DEPUTY_MANAGER(심사역)·OPS(운영팀) 직급이 없어
--  '수동 심사 실행·확정', '자동 심사', 'EOD' 등 매트릭스 액션을 로그인으로 시연할 수 없다.
--  본 마이그레이션은 두 직급의 직원 계정을 추가한다. (V11 은 이미 적용된 DB 의 체크섬
--  보호를 위해 수정하지 않고 새 버전으로 분리한다.)
--
--  customer_id 9010(deputy01)·9011(ops01). password: Employee1234! (V3/V11 과 동일 해시)
-- =============================================================================

INSERT INTO party (party_id, party_type_code, party_name, party_status_code, version)
OVERRIDING SYSTEM VALUE VALUES
    (9010, 'PERSON', '심사대리', 'ACTIVE', 0),
    (9011, 'PERSON', '운영담당', 'ACTIVE', 0);

INSERT INTO party_person (party_id, is_pep_yn, version)
VALUES (9010,'F',0),(9011,'F',0);

INSERT INTO customer (customer_id, party_id, customer_status_code, main_customer_yn,
                      sms_receive_yn, email_receive_yn, postal_receive_yn,
                      joined_at, created_at, updated_at, version)
OVERRIDING SYSTEM VALUE VALUES
    (9010, 9010, 'ACTIVE', 'F', 'F', 'F', 'F', NOW(), NOW(), NOW(), 0),
    (9011, 9011, 'ACTIVE', 'F', 'F', 'F', 'F', NOW(), NOW(), NOW(), 0);

-- password: Employee1234!  (BCrypt strength 10)
INSERT INTO credential (customer_id, login_id, password_hash,
                        password_changed_at, account_status_code,
                        created_at, updated_at, version)
VALUES
    (9010, 'deputy01', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', NOW(), 'ACTIVE', NOW(), NOW(), 0),
    (9011, 'ops01',    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', NOW(), 'ACTIVE', NOW(), NOW(), 0);

-- EMPLOYEE party_role (직원 게이트)
INSERT INTO party_role (party_id, role_type_code, role_status_code, role_start_date, version)
VALUES
    (9010, 'EMPLOYEE', 'ACTIVE', '20260101', 0),
    (9011, 'EMPLOYEE', 'ACTIVE', '20260101', 0);

-- employee 디테일 — grade_code = BankRole 이름
--   9010 → DEPUTY_MANAGER (수동 심사 실행·확정·편향확인)
--   9011 → OPS            (자동 심사·운영·EOD)
INSERT INTO employee (party_id, branch_code, grade_code, status_code, version)
VALUES
    (9010, '0000', 'DEPUTY_MANAGER', 'ACTIVE', 0),
    (9011, '0000', 'OPS',            'ACTIVE', 0);

-- 시퀀스 동기화 (OVERRIDING SYSTEM VALUE 시드 후 IDENTITY 충돌 방지)
SELECT setval(pg_get_serial_sequence('party', 'party_id'),
              COALESCE((SELECT MAX(party_id) FROM party), 1), true);
SELECT setval(pg_get_serial_sequence('customer', 'customer_id'),
              COALESCE((SELECT MAX(customer_id) FROM customer), 1), true);

-- ---- V13__add_customer_suspended_status.sql ----
-- =============================================================================
-- V13: 회원 정지(SUSPENDED) 상태 도입
-- =============================================================================
-- 기존 chk_customer_lifecycle 는 ACTIVE/DORMANT/CLOSED 만 허용한다. 직원용 '회원 상태 관리'
-- 화면의 정지/해제 기능을 위해 Customer 생애주기에 SUSPENDED 를 추가한다.
-- 정지 시점 기록용 suspended_at 컬럼을 두고, CLOSED·DORMANT 와 동일하게 CHECK 불변식에 포함한다.
--
-- 활성 고객 1건 제한 인덱스(uq_customer_active_per_party, WHERE customer_status_code <> 'CLOSED')는
-- SUSPENDED 를 비-CLOSED 로 간주하므로 정지 고객도 활성 슬롯을 점유한다(정지 중 재가입 불가) — 의도된 동작.

ALTER TABLE customer ADD COLUMN suspended_at TIMESTAMPTZ(3);

ALTER TABLE customer DROP CONSTRAINT chk_customer_lifecycle;

ALTER TABLE customer ADD CONSTRAINT chk_customer_lifecycle CHECK (
    (customer_status_code = 'CLOSED'    AND closed_at IS NOT NULL AND close_reason_code IS NOT NULL)
    OR (customer_status_code = 'DORMANT'   AND dormant_at IS NOT NULL)
    OR (customer_status_code = 'SUSPENDED' AND suspended_at IS NOT NULL)
    OR customer_status_code = 'ACTIVE'
);

-- ---- V14__add_party_relation_review_status.sql ----
-- =============================================================================
-- V14: 관계자관계 검토상태(대리인 위임장 검토) 도입
-- =============================================================================
-- 대리인 위임장 검토 화면(/admin/agent)은 신규 등록된 관계(대리인 위임 등)를 직원이 검토해
-- 승인/거절하는 큐가 필요하다. 기존 party_relation에는 검토상태 컬럼이 없어 대기목록을 만들 수 없었다.
-- relation_review_status_code(PENDING/APPROVED/REJECTED)를 추가한다.
--
-- nullable로 둔다: 기존 관계(주주·대표이사 등 시드 데이터)는 NULL로 남아 검토 큐(='PENDING')에서 제외된다.
-- 신규 등록 관계는 서비스 레이어에서 PENDING으로 생성한다.

ALTER TABLE party_relation ADD COLUMN relation_review_status_code VARCHAR(20);

-- ---- V15__add_sanction_screening_hit.sql ----
-- =============================================================================
-- V15: 제재 스크리닝 Hit 검토 워크플로
-- =============================================================================
-- compliance_info의 제재 플래그(is_*_sanctioned_yn)는 "현재 제재대상 여부" 상태일 뿐,
-- 스크리닝 탐지 건별(일치율·Hit유형·검토상태·검토자) 정보를 담지 못한다.
-- 제재대상 Hit 검토 화면(/admin/screening)의 검토 큐를 위해 hit 단위 테이블을 신설한다.

CREATE TABLE sanction_screening_hit (
    sanction_screening_hit_id  BIGINT         GENERATED ALWAYS AS IDENTITY,
    party_id                   BIGINT         NOT NULL,
    hit_type_code              VARCHAR(30)    NOT NULL,   -- OFAC_SDN / KR_PEP / UN / EU
    match_rate                 INT            NOT NULL,   -- 0~100 유사도(%)
    screening_status_code      VARCHAR(20)    NOT NULL,   -- PENDING / CLEARED(동명이인) / CONFIRMED(제재확정)
    reviewer_employee_id       BIGINT,
    review_comment             VARCHAR(500),
    detected_at                TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    reviewed_at                TIMESTAMPTZ(3),
    created_at                 TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                 BIGINT,
    updated_at                 TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by                 BIGINT,
    deleted_at                 TIMESTAMPTZ(3),
    deleted_by                 BIGINT,
    version                    INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_sanction_screening_hit PRIMARY KEY (sanction_screening_hit_id),
    CONSTRAINT fk_sanction_screening_hit_party FOREIGN KEY (party_id) REFERENCES party (party_id),
    CONSTRAINT chk_sanction_screening_hit_rate CHECK (match_rate BETWEEN 0 AND 100)
);

-- 검토 대기 큐 조회
CREATE INDEX idx_sanction_screening_hit_status
    ON sanction_screening_hit (screening_status_code)
    WHERE deleted_at IS NULL;

-- ---- V16__add_duplicate_review_case.sql ----
-- =============================================================================
-- V16: 중복고객 검토 케이스
-- =============================================================================
-- party_person.ci_value/이름+생년월일로 중복 후보를 탐지할 수는 있으나, 검토 결과(복본 확정/별개 확정)를
-- 담을 곳이 없다. 중복고객 검토 화면(/admin/duplicates)의 검토 큐를 위해 케이스 테이블을 신설한다.
-- 탐지 로직(배치/가입 시)이 신규 party와 기존 party를 묶어 PENDING 케이스로 적재하고, 직원이 복본/별개를 판정한다.

CREATE TABLE duplicate_review_case (
    duplicate_review_case_id  BIGINT         GENERATED ALWAYS AS IDENTITY,
    new_party_id              BIGINT         NOT NULL,   -- 신규(또는 후보) party
    existing_party_id         BIGINT         NOT NULL,   -- 기존 party
    match_type_code           VARCHAR(20)    NOT NULL,   -- CI / NAME_BIRTH
    review_status_code        VARCHAR(20)    NOT NULL,   -- PENDING / DUPLICATE(복본확정) / DISTINCT(별개확정)
    reviewer_employee_id      BIGINT,
    review_comment            VARCHAR(500),
    detected_at               TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    reviewed_at               TIMESTAMPTZ(3),
    created_at                TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                BIGINT,
    updated_at                TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by                BIGINT,
    deleted_at                TIMESTAMPTZ(3),
    deleted_by                BIGINT,
    version                   INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_duplicate_review_case PRIMARY KEY (duplicate_review_case_id),
    CONSTRAINT fk_duplicate_review_case_new      FOREIGN KEY (new_party_id)      REFERENCES party (party_id),
    CONSTRAINT fk_duplicate_review_case_existing FOREIGN KEY (existing_party_id) REFERENCES party (party_id),
    CONSTRAINT chk_duplicate_review_case_distinct_parties CHECK (new_party_id <> existing_party_id)
);

-- 검토 대기 큐 조회
CREATE INDEX idx_duplicate_review_case_status
    ON duplicate_review_case (review_status_code)
    WHERE deleted_at IS NULL;

-- ---- V17__unique_party_person_ci.sql ----
-- =============================================================================
-- V17: party_person.ci_value 부분 유니크 인덱스
--
--  party 패턴 정석 — "한 사람(본인확인 CI) = 한 party" 를 DB 레벨에서 보장한다.
--  본인확인을 거친 가입은 CI 로 기존 party 를 찾아 역할만 추가하므로(RegisterService),
--  동일 CI 로 party 가 둘로 쪼개지는 신원 분리를 인덱스가 최종 방어한다.
--
--  CI 가 없는 기존/레거시 row(직원 시드 등)는 대상에서 제외(부분 인덱스).
-- =============================================================================

CREATE UNIQUE INDEX uq_party_person_ci
    ON party_person (ci_value)
    WHERE ci_value IS NOT NULL AND deleted_at IS NULL;

-- ---- V18__identity_verification_rrn.sql ----
-- =============================================================================
-- V18: identity_verification 에 주민번호 암호문·소비 컬럼 추가
--
--  주민번호 기반 본인확인 강화. 검증 시점에 주민번호를 AES-256 암호문으로 박제하고
--  (평문 미보관), 가입(RegisterService)이 이 검증 1건을 1회 소비해 party 로 이전한다.
--  - rrn_encrypted        : 주민등록번호 AES-256 암호문 (가입 시 party_person.rrn_encrypted 로 복사)
--  - consumed_yn/customer : 가입에 소비됐는지 + 연결된 고객 (검증 1건 = 가입 1건)
-- =============================================================================

ALTER TABLE identity_verification
    ADD COLUMN rrn_encrypted        VARCHAR(255),
    ADD COLUMN consumed_yn          CHAR(1)        NOT NULL DEFAULT 'F',
    ADD COLUMN consumed_customer_id BIGINT,
    ADD COLUMN consumed_at          TIMESTAMPTZ(3);

ALTER TABLE identity_verification
    ADD CONSTRAINT chk_identity_verification_consumed CHECK (consumed_yn IN ('T','F'));

-- ---- V19__add_history_actor_and_access_log.sql ----
-- =============================================================================
-- V19: 직원 행위자 기록 — 변경 이력 actor + 조회 접근 감사로그
-- =============================================================================
-- 그동안 직원 admin API(상태/등급 변경, 고객 조회)의 "행위자"가 어디에도 남지 않았다.
-- (customer-service 는 CurrentActorProvider 를 오버라이드하지 않아 created_by 는 항상 0=SYSTEM)
-- 게이트웨이가 JWT 에서 주입하는 X-Employee-Id(검증된 직원 employee_id)를 받아 기록한다.

-- 1) 상태/등급 변경 이력에 변경자(직원) 컬럼 추가. 시스템 자동 전환은 NULL.
ALTER TABLE customer_status_history
    ADD COLUMN changed_by_employee_id BIGINT;

ALTER TABLE customer_grade_history
    ADD COLUMN changed_by_employee_id BIGINT;

-- 2) 조회 접근 감사로그 — "누가(직원) 누구를(고객) 무엇을(행위) 왜(사유) 언제" 조회했는가.
--    append-only 로그. FK 는 두지 않는다(감사 적재가 FK 로 막히면 안 됨, reviewer_employee_id 선례와 동일).
--    직원명·역할·지점·고객명은 조회 시점 스냅샷으로 적재한다 — 조회는 조인 없는 단순 SELECT 가 되고
--    이후 직원 전보·개명이 있어도 "그 시점의 사실"이 보존돼 감사상 정확하다.
CREATE TABLE customer_access_log (
    customer_access_log_id  BIGINT         GENERATED ALWAYS AS IDENTITY,
    accessor_employee_id    BIGINT         NOT NULL,   -- 조회한 직원 employee_id (X-Employee-Id)
    accessor_name           VARCHAR(100),              -- 직원명 스냅샷
    accessor_role           VARCHAR(40),               -- 직원 역할(BankRole) 스냅샷
    accessor_branch_code    VARCHAR(10),               -- 직원 지점 스냅샷(지점 범위 필터용)
    target_customer_id      BIGINT         NOT NULL,   -- 조회 대상 고객 customer_id
    target_customer_name    VARCHAR(100),              -- 고객명 스냅샷
    access_action_code      VARCHAR(40)    NOT NULL,   -- CUSTOMER_DETAIL / CONTACT_VIEW 등
    access_reason           VARCHAR(500),              -- 조회 사유(필요 역할만 입력)
    accessed_at             TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_customer_access_log PRIMARY KEY (customer_access_log_id)
);

-- 대상 고객별 접근 이력 조회
CREATE INDEX idx_customer_access_log_target   ON customer_access_log (target_customer_id);
-- 직원별 접근 이력 조회
CREATE INDEX idx_customer_access_log_accessor ON customer_access_log (accessor_employee_id);
-- 최근순 정렬(감사 화면 기본)
CREATE INDEX idx_customer_access_log_at       ON customer_access_log (accessed_at DESC);

-- ---- V20__fix_employee_password_hash.sql ----
-- =============================================================================
-- V20: 직원 계정 비밀번호 해시 교정
--
--  V3/V11/V12 가 시드한 직원 11계정의 password_hash 가 placeholder 였고
--  실제 'Employee1234!' 와 BCrypt 매칭되지 않아 관리자 로그인이 모두 401 로 거부됐다.
--  (시드 주석의 "교체 필요 시 갱신" 이 반영되지 않은 채 머지됨)
--
--  V3/V11/V12 는 이미 적용된 DB 의 체크섬 보호를 위해 수정하지 않고 본 버전에서 교정한다.
--  새 해시는 BCryptPasswordEncoder(10).encode("Employee1234!") 검증 완료(strength 10, $2a).
-- =============================================================================

UPDATE credential
   SET password_hash = '$2a$10$cLdYthdLyRkMgrSGeVwSBOrLmExEFpFvgwXqt.SFAovzo34fDvWRS',
       updated_at    = NOW()
 WHERE password_hash = '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG';

-- ---- V21__fix_employee_roles_and_test_names.sql ----
-- =============================================================================
-- V21: 직원 역할 의미 정합 + 테스트 계정 실명화
--
--  1) owner01(9007 정담당): grade BRANCH_MANAGER → TELLER 로 정정.
--     "담당 직원(PRIMARY_OWNER)"은 정적 직급이 아니라 동적 관계(담당 고객 보유)로
--     판정해야 하는 개념이라, 직급 자체는 창구직원(TELLER)이 맞다. 이로써 0001 지점에
--     지점장이 둘(9001 + 9007)이던 모순도 해소된다. (대출 최종결재 데모는 9001 지점장이 담당)
--  2) 9001/9002 placeholder 이름(지점장테스트/부지점장테스트)을 실명으로 교체.
--     직급(BRANCH_MANAGER/DEPUTY_MANAGER)·지점(0001)은 유지.
--
--  V3/V11 은 이미 적용된 DB 의 체크섬 보호를 위해 수정하지 않고 본 버전에서 정정한다.
-- =============================================================================

-- 1) 담당직원 직급 정정 (지점장 → 창구직원)
UPDATE employee
   SET grade_code = 'TELLER',
       updated_at = NOW()
 WHERE party_id = 9007
   AND grade_code = 'BRANCH_MANAGER';

-- 2) 테스트 계정 실명화
UPDATE party SET party_name = '박상우' WHERE party_id = 9001 AND party_name = '지점장테스트';
UPDATE party SET party_name = '김다은' WHERE party_id = 9002 AND party_name = '부지점장테스트';

-- ---- V22__add_compliance_info_actor.sql ----
-- =============================================================================
-- V22: compliance_info 행위자 기록 — AML 위험도 평가 / KYC 완료 처리자
-- =============================================================================
-- compliance_info 에는 "언제"(aml_last_assessed_at, kyc_completed_at)는 있으나
-- "누가" 처리했는지가 없었다. 게이트웨이가 JWT 에서 주입하는 X-Employee-Id
-- (검증된 직원 employee_id)를 받아 마지막 처리 직원을 기록한다.
-- 시스템 자동 처리는 NULL. (감사 적재가 FK 로 막히면 안 되므로 FK 는 두지 않는다 —
-- customer_status_history.changed_by_employee_id 선례와 동일)

ALTER TABLE compliance_info
    ADD COLUMN aml_last_assessed_by_employee_id BIGINT;

ALTER TABLE compliance_info
    ADD COLUMN kyc_completed_by_employee_id BIGINT;

-- ---- V23__seed_demo_customer_accounts.sql ----
-- =============================================================================
-- V23: 데모용 고객(차주) 로그인 계정 시드 — user01 / user02 / user03
--
--  배경: 기존 고객 계정(user0N, customer_id 9111+)은 레포 밖 일회성 스크립트로
--        로컬 DB 에만 생성돼 있어, 신규 클론에서는 고객으로 로그인할 수 없었다.
--        (직원 계정은 V3/V11/V12 로 시드되지만 고객 계정 시드는 없었음)
--
--  본 마이그레이션은 고객 로그인 흐름(대출 신청 등) 재현을 위해 데모 고객 3명을 시드한다.
--    customer_id 9111(user01) / 9112(user02) / 9113(user03)
--    password: Employee1234!  (직원 데모와 동일 BCrypt 해시 재사용 — V20 검증 해시)
--    역할 행(party_role) 없음 → 로그인 시 기본 ROLE_CUSTOMER 부여.
--
--  멱등성: 이미 동일 id 가 존재하는 로컬 DB 에서도 안전하도록 ON CONFLICT / NOT EXISTS 가드.
-- =============================================================================

-- party
INSERT INTO party (party_id, party_type_code, party_name, party_status_code, version)
OVERRIDING SYSTEM VALUE VALUES
    (9111, 'PERSON', '테스트고객01', 'ACTIVE', 0),
    (9112, 'PERSON', '테스트고객02', 'ACTIVE', 0),
    (9113, 'PERSON', '테스트고객03', 'ACTIVE', 0)
ON CONFLICT (party_id) DO NOTHING;

-- party_person
INSERT INTO party_person (party_id, is_pep_yn, version)
VALUES
    (9111, 'F', 0),
    (9112, 'F', 0),
    (9113, 'F', 0)
ON CONFLICT (party_id) DO NOTHING;

-- customer
INSERT INTO customer (customer_id, party_id, customer_status_code, main_customer_yn,
                      sms_receive_yn, email_receive_yn, postal_receive_yn,
                      joined_at, created_at, updated_at, version)
OVERRIDING SYSTEM VALUE VALUES
    (9111, 9111, 'ACTIVE', 'T', 'T', 'T', 'F', NOW(), NOW(), NOW(), 0),
    (9112, 9112, 'ACTIVE', 'T', 'T', 'T', 'F', NOW(), NOW(), NOW(), 0),
    (9113, 9113, 'ACTIVE', 'T', 'T', 'T', 'F', NOW(), NOW(), NOW(), 0)
ON CONFLICT (customer_id) DO NOTHING;

-- credential (password: Employee1234! / BCrypt strength 10, V20 검증 해시)
INSERT INTO credential (customer_id, login_id, password_hash, password_changed_at,
                        account_status_code, created_at, updated_at, version)
SELECT v.customer_id, v.login_id,
       '$2a$10$cLdYthdLyRkMgrSGeVwSBOrLmExEFpFvgwXqt.SFAovzo34fDvWRS',
       NOW(), 'ACTIVE', NOW(), NOW(), 0
FROM (VALUES
        (9111, 'user01'),
        (9112, 'user02'),
        (9113, 'user03')
     ) AS v(customer_id, login_id)
WHERE NOT EXISTS (
    SELECT 1 FROM credential c WHERE c.login_id = v.login_id AND c.deleted_at IS NULL
);

-- ---- V24__seed_admin_demo_customers.sql ----
-- =============================================================================
-- V24: 관리자 콘솔(고객계) 데모 고객 시드
--      (main 의 V23 = 대출 데모고객 시드와 번호 충돌 회피 → V24)
--
--  배경: 어드민 화면(/admin/customers·members·member-status·audit-log·join-stats·
--        agent·screening·duplicates·edd·fatca·minors)은 customer-service
--        /api/v1/internal/** 를 조회한다. 그러나 기존 시드는 직원 party(9001~9011)뿐이고
--        직원은 party_type_code='PERSON' 이라 고객 목록 쿼리(WHERE party_type_code='PERSONAL')
--        에서 제외돼, 어드민 고객 화면이 항상 비어 있었다.
--
--  목적: 권한·상태·등급·컴플라이언스가 다양한 "진짜 고객" party 를 시드해 어드민 고객계
--        전 화면과 검토 큐(대리인·제재·중복·EDD·FATCA·미성년·KYC만료)에 데이터가 뜨게 한다.
--
--  ID 범위: party_id / customer_id 8001~8016.
--    - 직원(9001~9011)·앱 생성(IDENTITY 시퀀스, 현재 9011 이후) 어느 쪽과도 충돌하지 않는다.
--    - GENERATED ALWAYS 라 OVERRIDING SYSTEM VALUE 로 명시 id 삽입, 끝에서 setval 보정.
--
--  주의: V11 선례(데모 직원 시드)와 동일 패턴. 운영 배포 시 제외하려면 별도 프로파일 분리 필요.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. party — 고객 14명(8001~8014) + 중복후보(8015) + 대리인(8016)
--    party_type_code='PERSONAL' 이어야 고객 목록에 노출된다(직원은 'PERSON').
-- -----------------------------------------------------------------------------
INSERT INTO party (party_id, party_type_code, party_name, party_status_code, version)
OVERRIDING SYSTEM VALUE VALUES
    (8001, 'PERSONAL', '김민준', 'ACTIVE', 0),
    (8002, 'PERSONAL', '이서연', 'ACTIVE', 0),
    (8003, 'PERSONAL', '박도윤', 'ACTIVE', 0),
    (8004, 'PERSONAL', '최지우', 'ACTIVE', 0),
    (8005, 'PERSONAL', '정하준', 'ACTIVE', 0),
    (8006, 'PERSONAL', '강서윤', 'ACTIVE', 0),
    (8007, 'PERSONAL', '윤지호', 'ACTIVE', 0),
    (8008, 'PERSONAL', '임하은', 'ACTIVE', 0),
    (8009, 'PERSONAL', '한지안', 'ACTIVE', 0),
    (8010, 'PERSONAL', '오시우', 'ACTIVE', 0),
    (8011, 'PERSONAL', '신아윤', 'ACTIVE', 0),
    (8012, 'PERSONAL', '권은우', 'ACTIVE', 0),
    (8013, 'PERSONAL', '황지율', 'ACTIVE', 0),
    (8014, 'PERSONAL', '안소율', 'ACTIVE', 0),
    (8015, 'PERSONAL', '김민준', 'ACTIVE', 0),   -- 8001 과 동명·동일생년 → 중복 검토 후보
    (8016, 'PERSONAL', '대리박', 'ACTIVE', 0);   -- 대리인(고객 아님, customer 미생성)

-- -----------------------------------------------------------------------------
-- 2. party_person — 생년월일·성별·국적·PEP. (PEP=T 는 pep_type_code 필수: chk_party_person_pep)
--    8014 안소율: 미성년(birth_date > now-19y) → 미성년 큐 노출.
-- -----------------------------------------------------------------------------
INSERT INTO party_person (party_id, birth_date, gender_code, nationality_code, is_pep_yn, pep_type_code, version)
VALUES
    (8001, '19850312', 'M', 'KOR', 'F', NULL,       0),
    (8002, '19900624', 'F', 'KOR', 'F', NULL,       0),
    (8003, '19780101', 'M', 'KOR', 'F', NULL,       0),
    (8004, '19951130', 'F', 'KOR', 'F', NULL,       0),
    (8005, '19820705', 'M', 'KOR', 'F', NULL,       0),
    (8006, '19931018', 'F', 'KOR', 'F', NULL,       0),
    (8007, '19880222', 'M', 'KOR', 'F', NULL,       0),
    (8008, '19970909', 'F', 'KOR', 'F', NULL,       0),
    (8009, '19751212', 'M', 'KOR', 'F', NULL,       0),
    (8010, '19700815', 'M', 'KOR', 'T', 'DOMESTIC', 0),   -- PEP(국내 고위공직자 등)
    (8011, '19861103', 'F', 'USA', 'F', NULL,       0),   -- 외국 국적 → FATCA/CRS 대상
    (8012, '19840417', 'M', 'KOR', 'F', NULL,       0),   -- 제재 스크리닝 Hit
    (8013, '19920529', 'F', 'KOR', 'F', NULL,       0),   -- EDD 대상
    (8014, '20100815', 'M', 'KOR', 'F', NULL,       0),   -- 미성년
    (8015, '19850312', 'M', 'KOR', 'F', NULL,       0),   -- 8001 과 동일 생년(중복 후보)
    (8016, '19800303', 'M', 'KOR', 'F', NULL,       0);   -- 대리인

-- -----------------------------------------------------------------------------
-- 3. compliance_info — 검토 큐 대상자만 적재(NOT NULL: aml/kyc/cdd/fatca/crs 코드).
--    EDD: edd_required_yn='T' / 제재: is_*_sanctioned_yn='T' / FATCA: *_reportable_yn='T'
--    KYC만료: kyc_status='COMPLETED' AND kyc_expiry_date <= 조회기준일.
-- -----------------------------------------------------------------------------
INSERT INTO compliance_info (
    party_id, aml_risk_level_code, kyc_status_code, cdd_level_code,
    fatca_status_code, crs_status_code,
    is_kr_sanctioned_yn, fatca_reportable_yn, crs_reportable_yn,
    edd_required_yn, edd_next_review_date, kyc_expiry_date, version)
VALUES
    -- 8010 PEP: 고위험 → EDD 대상
    (8010, 'HIGH', 'COMPLETED', 'EDD', 'NONE', 'NONE',
     'F', 'F', 'F', 'T', '20260731', '20270101', 0),
    -- 8011 외국적: FATCA·CRS 보고 대상
    (8011, 'MEDIUM', 'COMPLETED', 'CDD', 'REPORTABLE', 'REPORTABLE',
     'F', 'T', 'T', 'F', NULL, '20270301', 0),
    -- 8012 제재 대상(국내) + 스크리닝 Hit
    (8012, 'HIGH', 'COMPLETED', 'EDD', 'NONE', 'NONE',
     'T', 'F', 'F', 'F', NULL, '20270601', 0),
    -- 8013 EDD 대상 + KYC 만료 임박(과거일 → 어떤 기준일로 조회해도 노출)
    (8013, 'HIGH', 'COMPLETED', 'EDD', 'NONE', 'NONE',
     'F', 'F', 'F', 'T', '20260620', '20260601', 0);

-- -----------------------------------------------------------------------------
-- 4. customer — 상태/등급/연락처 다양화. 고객 목록·검색·상태관리·마스킹·가입통계의 데이터원.
--    상태별 생애주기 컬럼 필수(chk_customer_lifecycle):
--      DORMANT→dormant_at, SUSPENDED→suspended_at, CLOSED→closed_at+close_reason_code.
-- -----------------------------------------------------------------------------
INSERT INTO customer (customer_id, party_id, customer_grade_code, customer_status_code,
                      credit_rating_code, sms_receive_yn, email_receive_yn, postal_receive_yn,
                      email, phone, zip_code, address, address_detail,
                      join_channel_code, first_join_date, joined_at, last_transaction_at,
                      dormant_at, suspended_at, closed_at, close_reason_code,
                      created_at, updated_at, version)
OVERRIDING SYSTEM VALUE VALUES
    (8001, 8001, 'VIP',    'ACTIVE',    'AAA', 'T','T','F', 'minjun.kim@example.com',  '010-1001-2001', '06236','서울 강남구 테헤란로 1',  '101동 1001호', 'ONLINE','20200310', NOW() - INTERVAL '900 day', NOW() - INTERVAL '2 day',  NULL, NULL, NULL, NULL, NOW(), NOW(), 0),
    (8002, 8002, 'GOLD',   'ACTIVE',    'AA',  'T','F','F', 'seoyeon.lee@example.com', '010-1002-2002', '03187','서울 종로구 종로 2',     '5층',        'BRANCH','20210624', NOW() - INTERVAL '700 day', NOW() - INTERVAL '5 day',  NULL, NULL, NULL, NULL, NOW(), NOW(), 0),
    (8003, 8003, 'SILVER', 'ACTIVE',    'A',   'F','F','T', 'doyun.park@example.com',  '010-1003-2003', '48058','부산 해운대구 센텀1', '202호',       'ONLINE','20220101', NOW() - INTERVAL '500 day', NOW() - INTERVAL '20 day', NULL, NULL, NULL, NULL, NOW(), NOW(), 0),
    (8004, 8004, 'NORMAL', 'ACTIVE',    'BBB', 'T','T','F', 'jiwoo.choi@example.com',  '010-1004-2004', '13529','경기 성남시 분당구 3',  '301호',       'MOBILE','20230515', NOW() - INTERVAL '300 day', NOW() - INTERVAL '1 day',  NULL, NULL, NULL, NULL, NOW(), NOW(), 0),
    (8005, 8005, 'GOLD',   'DORMANT',   'AA',  'F','F','F', 'hajun.jung@example.com',  '010-1005-2005', '34126','대전 유성구 대학로 4',  '11층',        'ONLINE','20190705', NOW() - INTERVAL '1500 day', NOW() - INTERVAL '400 day', NOW() - INTERVAL '380 day', NULL, NULL, NULL, NOW(), NOW(), 0),
    (8006, 8006, 'NORMAL', 'DORMANT',   'BBB', 'F','F','F', 'seoyun.kang@example.com', '010-1006-2006', '61945','광주 서구 상무대로 5',  '8호',         'BRANCH','20200218', NOW() - INTERVAL '1300 day', NOW() - INTERVAL '420 day', NOW() - INTERVAL '400 day', NULL, NULL, NULL, NOW(), NOW(), 0),
    (8007, 8007, 'SILVER', 'SUSPENDED', 'BB',  'T','F','F', 'jiho.yoon@example.com',   '010-1007-2007', '05551','서울 송파구 올림픽로 6', '1503호',      'MOBILE','20210822', NOW() - INTERVAL '800 day', NOW() - INTERVAL '60 day', NULL, NOW() - INTERVAL '30 day', NULL, NULL, NOW(), NOW(), 0),
    (8008, 8008, 'NORMAL', 'SUSPENDED', 'CCC', 'T','T','F', 'haeun.lim@example.com',   '010-1008-2008', '21999','인천 연수구 송도과학로 7','704호',      'ONLINE','20220909', NOW() - INTERVAL '600 day', NOW() - INTERVAL '90 day', NULL, NOW() - INTERVAL '15 day', NULL, NULL, NOW(), NOW(), 0),
    (8009, 8009, 'NORMAL', 'CLOSED',    'B',   'F','F','F', 'jian.han@example.com',    '010-1009-2009', '44676','울산 남구 삼산로 8',    '9호',         'BRANCH','20180101', NOW() - INTERVAL '2000 day', NOW() - INTERVAL '200 day', NULL, NULL, NOW() - INTERVAL '120 day', 'CUST_REQ', NOW(), NOW(), 0),
    (8010, 8010, 'VIP',    'ACTIVE',    'AAA', 'T','T','T', 'siwoo.oh@example.com',    '010-1010-2010', '06164','서울 강남구 영동대로 9', 'PH',          'BRANCH','20170815', NOW() - INTERVAL '2500 day', NOW() - INTERVAL '3 day',  NULL, NULL, NULL, NULL, NOW(), NOW(), 0),
    (8011, 8011, 'GOLD',   'ACTIVE',    'AA',  'T','T','F', 'ayoon.shin@example.com',  '010-1011-2011', '04524','서울 중구 세종대로 10',  '20층',        'ONLINE','20211103', NOW() - INTERVAL '650 day', NOW() - INTERVAL '7 day',  NULL, NULL, NULL, NULL, NOW(), NOW(), 0),
    (8012, 8012, 'NORMAL', 'ACTIVE',    'CCC', 'F','F','F', 'eunwoo.kwon@example.com', '010-1012-2012', '07238','서울 영등포구 여의대로 11','30호',       'MOBILE','20230417', NOW() - INTERVAL '280 day', NOW() - INTERVAL '4 day',  NULL, NULL, NULL, NULL, NOW(), NOW(), 0),
    (8013, 8013, 'SILVER', 'ACTIVE',    'BBB', 'T','T','F', 'jiyul.hwang@example.com', '010-1013-2013', '34047','대전 대덕구 한밭대로 12','606호',       'ONLINE','20220529', NOW() - INTERVAL '470 day', NOW() - INTERVAL '10 day', NULL, NULL, NULL, NULL, NOW(), NOW(), 0),
    (8014, 8014, 'NORMAL', 'ACTIVE',    NULL,  'F','F','F', 'soyul.an@example.com',    '010-1014-2014', '16677','경기 수원시 영통구 13',  '1201호',      'BRANCH','20250815', NOW() - INTERVAL '120 day', NOW() - INTERVAL '12 day', NULL, NULL, NULL, NULL, NOW(), NOW(), 0),
    (8015, 8015, 'NORMAL', 'ACTIVE',    'BBB', 'F','F','F', 'minjun.kim2@example.com', '010-1015-2015', '06236','서울 강남구 테헤란로 1',  '101동 1002호','ONLINE','20260101', NOW() - INTERVAL '30 day',  NOW() - INTERVAL '6 day',  NULL, NULL, NULL, NULL, NOW(), NOW(), 0);
-- 8016(대리인)은 customer 를 만들지 않는다 — 고객이 아니라 위임 관계의 상대 party.

-- -----------------------------------------------------------------------------
-- 5. 대리인 위임장 검토 큐 — party_relation(review_status='PENDING')
--    최지우(8004) 가 대리박(8016) 에게 위임. /admin/agent 대기목록에 노출.
-- -----------------------------------------------------------------------------
INSERT INTO party_relation (from_party_id, to_party_id, relation_type_code, relation_detail_code,
                            representation_scope, proof_url, relation_start_date,
                            relation_review_status_code, version)
VALUES
    (8004, 8016, 'AGENT', 'DELEGATE', '전체 위임(조회·이체)', 'https://example.com/proxy/8004.pdf',
     '20260601', 'PENDING', 0);

-- -----------------------------------------------------------------------------
-- 6. 제재 스크리닝 Hit 검토 큐 — sanction_screening_hit(status='PENDING')
--    권은우(8012) OFAC SDN 유사 92%. /admin/screening 대기목록에 노출.
-- -----------------------------------------------------------------------------
INSERT INTO sanction_screening_hit (party_id, hit_type_code, match_rate, screening_status_code,
                                    detected_at, version)
VALUES
    (8012, 'OFAC_SDN', 92, 'PENDING', NOW() - INTERVAL '2 day', 0);

-- -----------------------------------------------------------------------------
-- 7. 중복고객 검토 큐 — duplicate_review_case(status='PENDING')
--    8015(신규 김민준) ↔ 8001(기존 김민준) 동명·동일생년. /admin/duplicates 대기목록에 노출.
-- -----------------------------------------------------------------------------
INSERT INTO duplicate_review_case (new_party_id, existing_party_id, match_type_code,
                                   review_status_code, detected_at, version)
VALUES
    (8015, 8001, 'NAME_BIRTH', 'PENDING', NOW() - INTERVAL '1 day', 0);

-- -----------------------------------------------------------------------------
-- 8. 조회 접근 감사로그 — /admin/audit-log 표시용 스냅샷 로그(FK 없음, append-only).
--    accessor_employee_id 는 직원 식별자(표시는 accessor_name 스냅샷 우선).
-- -----------------------------------------------------------------------------
INSERT INTO customer_access_log (accessor_employee_id, accessor_name, accessor_role, accessor_branch_code,
                                 target_customer_id, target_customer_name,
                                 access_action_code, access_reason, accessed_at)
VALUES
    (9003, '김감사',  'COMPLIANCE',     '0000', 8012, '권은우', 'CUSTOMER_DETAIL', '제재 스크리닝 Hit 검토',        NOW() - INTERVAL '3 hour'),
    (9008, '한직원',  'TELLER',         '0001', 8001, '김민준', 'CONTACT_VIEW',    '대출 상담 요청 연락',          NOW() - INTERVAL '5 hour'),
    (9007, '정담당',  'BRANCH_MANAGER', '0001', 8007, '윤지호', 'CUSTOMER_DETAIL', '이상거래 정지 사유 확인',       NOW() - INTERVAL '1 day'),
    (9004, '이심사',  'HQ_REVIEWER',    '0000', 8013, '황지율', 'CUSTOMER_DETAIL', 'EDD 심사 대상 검토',           NOW() - INTERVAL '1 day' - INTERVAL '2 hour'),
    (9008, '한직원',  'TELLER',         '0001', 8002, '이서연', 'CONTACT_VIEW',    '카드 발급 안내 연락',          NOW() - INTERVAL '2 day');

-- -----------------------------------------------------------------------------
-- 9. IDENTITY 시퀀스 보정 — OVERRIDING SYSTEM VALUE 로 명시 id 삽입했으므로
--    시퀀스를 현재 MAX 로 맞춰 앱 생성 id 와 충돌하지 않게 한다(V11 과 동일).
--    (직원 9011 이 더 크므로 실제 시퀀스 값은 변하지 않을 수 있다 — 안전한 no-op)
-- -----------------------------------------------------------------------------
SELECT setval(pg_get_serial_sequence('party', 'party_id'),
              COALESCE((SELECT MAX(party_id) FROM party), 1), true);
SELECT setval(pg_get_serial_sequence('customer', 'customer_id'),
              COALESCE((SELECT MAX(customer_id) FROM customer), 1), true);

-- ---- V25__backfill_demo_customer_demographics.sql ----
-- =============================================================================
-- V25: 데모 고객(user01~user10) 생년월일·성별 백필
--
--  배경: V23 은 데모 고객 user01~03 을 시드하지만 party_person.birth_date /
--        gender_code 를 NULL 로 남겨 둔다. user04~10 은 레포 밖 런타임 스크립트로
--        로컬 DB 에만 생성되므로 fresh DB 에는 존재하지 않는다.
--
--  본 마이그레이션은 존재하는 데모 고객의 생년월일/성별만 백필한다(UPDATE).
--    - fresh DB: V23 으로 시드된 9111~9113(user01~03)만 갱신, 9114~9120 은 행이
--      없어 no-op.
--    - 런타임으로 user04~10 이 채워진 로컬 DB: 9111~9120 전원 갱신.
--
--  포맷: birth_date CHAR(8) YYYYMMDD, gender_code CHAR(1) 'M'/'F'.
--  멱등성: UPDATE … FROM (VALUES …) 로 반복 실행해도 동일 결과.
-- =============================================================================

UPDATE party_person AS p
SET birth_date  = v.birth_date,
    gender_code = v.gender_code,
    updated_at  = NOW()
FROM (VALUES
        (9111, '19880312', 'M'),  -- user01
        (9112, '19920724', 'F'),  -- user02
        (9113, '19851103', 'M'),  -- user03
        (9114, '19960518', 'F'),  -- user04
        (9115, '19790130', 'M'),  -- user05
        (9116, '19901008', 'F'),  -- user06
        (9117, '19830622', 'M'),  -- user07
        (9118, '19980914', 'F'),  -- user08
        (9119, '19751205', 'M'),  -- user09
        (9120, '19930427', 'F')   -- user10
     ) AS v(party_id, birth_date, gender_code)
WHERE p.party_id = v.party_id;

-- ============================================================
-- SERVICE: deposit-service  (DB: deposit_db)
-- ============================================================

-- ---- V1__initial_schema.sql ----
-- ERD 기준: teamproject.drawio
-- deposit-service 전체 스키마 (chatbot_node_flow 제외)

BEGIN;

-- =============================================
-- 부서
-- =============================================
CREATE TABLE deposit_departments (
    department_id       BIGSERIAL    PRIMARY KEY,
    department_code     VARCHAR(50)  NOT NULL UNIQUE,
    department_name     VARCHAR(100) NOT NULL,
    parent_department_id BIGINT,
    department_type     VARCHAR(30),
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(100),
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(100),
    CONSTRAINT fk_deposit_departments_parent
        FOREIGN KEY (parent_department_id)
        REFERENCES deposit_departments (department_id)
        ON DELETE SET NULL ON UPDATE CASCADE
);

-- =============================================
-- 수신 상품 (메인)
-- =============================================
CREATE TABLE deposit_banking_products (
    banking_product_id          BIGSERIAL    PRIMARY KEY,
    deposit_product_type        VARCHAR(30)  NOT NULL,
    deposit_product_name        VARCHAR(200) NOT NULL,
    description                 TEXT,
    department_id               BIGINT,
    base_interest_rate          NUMERIC(5,2) NOT NULL DEFAULT 0,
    preferential_rate_condition TEXT,
    min_join_amount             NUMERIC(18,2),
    max_join_amount             NUMERIC(18,2),
    min_period_month            INT,
    max_period_month            INT,
    is_early_termination_allowed BOOLEAN     NOT NULL DEFAULT FALSE,
    is_tax_benefit_available    BOOLEAN      NOT NULL DEFAULT FALSE,
    is_auto_renewal_available   BOOLEAN      NOT NULL DEFAULT FALSE,
    is_passbook_issued          BOOLEAN      NOT NULL DEFAULT FALSE,
    released_at                 CHAR(8),
    ended_at                    CHAR(8),
    deposit_product_status      VARCHAR(20)  NOT NULL DEFAULT 'SELLING',
    max_interest_rate           NUMERIC(5,2),
    promotion_end_date          DATE,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by                  VARCHAR(100),
    updated_at                  TIMESTAMPTZ,
    updated_by                  VARCHAR(100),
    CONSTRAINT fk_deposit_banking_products_dept
        FOREIGN KEY (department_id)
        REFERENCES deposit_departments (department_id)
        ON DELETE SET NULL ON UPDATE CASCADE
);

-- =============================================
-- 예금 상품
-- =============================================
CREATE TABLE banking_deposit_products (
    deposit_product_id   BIGSERIAL   PRIMARY KEY,
    banking_product_id   BIGINT      NOT NULL,
    deposit_type         VARCHAR(20) NOT NULL,
    is_compound_interest BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by           VARCHAR(100),
    updated_at           TIMESTAMPTZ,
    updated_by           VARCHAR(100),
    CONSTRAINT fk_banking_deposit_products_product
        FOREIGN KEY (banking_product_id)
        REFERENCES deposit_banking_products (banking_product_id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- =============================================
-- 적금 상품
-- =============================================
CREATE TABLE deposit_savings_products (
    savings_product_id         BIGSERIAL    PRIMARY KEY,
    banking_product_id         BIGINT       NOT NULL,
    saving_type                VARCHAR(20)  NOT NULL,
    monthly_payment_min_amount NUMERIC(18,2),
    monthly_payment_max_amount NUMERIC(18,2),
    created_at                 TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by                 VARCHAR(100),
    updated_at                 TIMESTAMPTZ,
    updated_by                 VARCHAR(100),
    CONSTRAINT fk_deposit_savings_products_product
        FOREIGN KEY (banking_product_id)
        REFERENCES deposit_banking_products (banking_product_id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- =============================================
-- 청약 상품
-- =============================================
CREATE TABLE deposit_subscription_products (
    banking_product_id             BIGINT       PRIMARY KEY,
    monthly_payment_amount         NUMERIC(18,2) NOT NULL,
    min_monthly_payment            NUMERIC(18,2),
    max_monthly_payment            NUMERIC(18,2),
    max_recognized_payment_amount  NUMERIC(18,2),
    created_at                     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by                     VARCHAR(100),
    updated_at                     TIMESTAMPTZ,
    updated_by                     VARCHAR(100),
    CONSTRAINT fk_deposit_subscription_products_product
        FOREIGN KEY (banking_product_id)
        REFERENCES deposit_banking_products (banking_product_id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- =============================================
-- 상품 가입 방식
-- =============================================
CREATE TABLE banking_deposit_product_join_channels (
    channel_id         BIGSERIAL   PRIMARY KEY,
    banking_product_id BIGINT      NOT NULL,
    join_channel_code  VARCHAR(20) NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(100),
    updated_at         TIMESTAMPTZ,
    updated_by         VARCHAR(100),
    CONSTRAINT fk_bdp_join_channels_product
        FOREIGN KEY (banking_product_id)
        REFERENCES deposit_banking_products (banking_product_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT uq_bdp_join_channels_product_channel UNIQUE (banking_product_id, join_channel_code)
);

-- =============================================
-- 상품 금리
-- =============================================
CREATE TABLE banking_deposit_product_interest_rates (
    rate_id                  BIGSERIAL    PRIMARY KEY,
    banking_product_id       BIGINT       NOT NULL,
    rate_type                VARCHAR(30)  NOT NULL,
    minimum_contract_period  INT,
    maximum_contract_period  INT,
    minimum_join_amount      NUMERIC(18,2),
    maximum_join_amount      NUMERIC(18,2),
    rate                     NUMERIC(5,2) NOT NULL,
    condition_description    TEXT,
    effective_start_date     CHAR(8)      NOT NULL,
    effective_end_date       CHAR(8),
    is_active                BOOLEAN      NOT NULL DEFAULT TRUE,
    effective_date           DATE,
    expiry_date              DATE,
    status                   VARCHAR(20),
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by               VARCHAR(100),
    updated_at               TIMESTAMPTZ,
    updated_by               VARCHAR(100),
    CONSTRAINT fk_bdp_interest_rates_product
        FOREIGN KEY (banking_product_id)
        REFERENCES deposit_banking_products (banking_product_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- =============================================
-- 가입 대상 그룹
-- =============================================
CREATE TABLE deposit_target_groups (
    target_group_id   BIGSERIAL    PRIMARY KEY,
    target_group_name VARCHAR(100) NOT NULL UNIQUE,
    description       TEXT,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    min_age           INTEGER,
    max_age           INTEGER,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(100),
    updated_at        TIMESTAMPTZ,
    updated_by        VARCHAR(100)
);

-- =============================================
-- 상품 가입 대상 (N:M)
-- =============================================
CREATE TABLE banking_deposit_product_target_groups (
    banking_product_id BIGINT      NOT NULL,
    target_group_id    BIGINT      NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(100),
    updated_at         TIMESTAMPTZ,
    updated_by         VARCHAR(100),
    PRIMARY KEY (banking_product_id, target_group_id),
    CONSTRAINT fk_bdp_target_groups_product
        FOREIGN KEY (banking_product_id)
        REFERENCES deposit_banking_products (banking_product_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_bdp_target_groups_target
        FOREIGN KEY (target_group_id)
        REFERENCES deposit_target_groups (target_group_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- =============================================
-- 수신 특약
-- =============================================
CREATE TABLE deposit_special_terms (
    special_term_id                  BIGSERIAL    PRIMARY KEY,
    special_term_name                VARCHAR(200) NOT NULL,
    special_term_content             TEXT         NOT NULL,
    special_term_summary             TEXT,
    is_required                      BOOLEAN      NOT NULL DEFAULT FALSE,
    is_electronic_agreement_allowed  BOOLEAN      NOT NULL DEFAULT TRUE,
    special_term_version             VARCHAR(20)  NOT NULL,
    started_at                       CHAR(8),
    ended_at                         CHAR(8),
    status                           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    status_changed_at                CHAR(8),
    created_at                       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by                       VARCHAR(100),
    updated_at                       TIMESTAMPTZ,
    updated_by                       VARCHAR(100),
    CONSTRAINT uq_deposit_special_terms_name_ver UNIQUE (special_term_name, special_term_version)
);

-- =============================================
-- 수신 상품 특약 연결
-- =============================================
CREATE TABLE banking_deposit_product_special_terms (
    deposit_product_special_term_id BIGSERIAL   PRIMARY KEY,
    banking_product_id              BIGINT       NOT NULL,
    special_term_id                 BIGINT       NOT NULL,
    is_required                     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at                      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by                      VARCHAR(100),
    updated_at                      TIMESTAMPTZ,
    updated_by                      VARCHAR(100),
    CONSTRAINT fk_bdp_special_terms_product
        FOREIGN KEY (banking_product_id)
        REFERENCES deposit_banking_products (banking_product_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_bdp_special_terms_term
        FOREIGN KEY (special_term_id)
        REFERENCES deposit_special_terms (special_term_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT uq_bdp_special_terms_product_term UNIQUE (banking_product_id, special_term_id)
);

-- =============================================
-- 수신 계약
-- =============================================
CREATE TABLE deposit_contracts (
    contract_id                     BIGSERIAL    PRIMARY KEY,
    contract_number                 VARCHAR(50)  NOT NULL UNIQUE,
    customer_id                     VARCHAR(30)  NOT NULL,
    banking_product_id              BIGINT       NOT NULL,
    is_monthly_payment              BOOLEAN      NOT NULL DEFAULT FALSE,
    payment_count_total             INT,
    monthly_payment_day             VARCHAR(6),
    join_amount                     NUMERIC(18,2) NOT NULL,
    contract_interest_rate          NUMERIC(5,2) NOT NULL,
    total_preferential_rate         NUMERIC(5,2) NOT NULL DEFAULT 0,
    final_interest_rate             NUMERIC(5,2) NOT NULL,
    tax_benefit_type                VARCHAR(30)  NOT NULL DEFAULT 'GENERAL',
    applied_tax_rate                NUMERIC(5,2) NOT NULL DEFAULT 15.40,
    expected_interest_amount        NUMERIC(18,2),
    contract_period_month           INT          NOT NULL,
    started_at                      DATE         NOT NULL,
    maturity_at                     DATE,
    terminated_at                   DATE,
    termination_reason              VARCHAR(200),
    is_auto_renewal                 BOOLEAN      NOT NULL DEFAULT FALSE,
    auto_transfer_enabled           BOOLEAN      NOT NULL DEFAULT FALSE,
    auto_transfer_day               INT,
    contract_status                 VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    status_changed_at               DATE,
    join_channel                    VARCHAR(20)  NOT NULL,
    branch_id                       BIGINT,
    branch_code                     VARCHAR(20),
    branch_name                     VARCHAR(100),
    manager_id                      BIGINT,
    manager_name                    VARCHAR(100),
    is_proxy_joined                 BOOLEAN      NOT NULL DEFAULT FALSE,
    is_power_of_attorney_verified   BOOLEAN      NOT NULL DEFAULT FALSE,
    power_of_attorney_file_url      VARCHAR(500),
    terms_file_url                  VARCHAR(500),
    contract_file_url               VARCHAR(500),
    consecutive_miss_count          INT          NOT NULL DEFAULT 0,
    source_account_id               BIGINT,
    created_at                      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by                      VARCHAR(100),
    updated_at                      TIMESTAMPTZ,
    updated_by                      VARCHAR(100),
    CONSTRAINT fk_deposit_contracts_product
        FOREIGN KEY (banking_product_id)
        REFERENCES deposit_banking_products (banking_product_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- =============================================
-- 계약 우대 금리 적용 내역
-- =============================================
CREATE TABLE deposit_contract_applied_rates (
    applied_rate_id      BIGSERIAL    PRIMARY KEY,
    contract_id          BIGINT       NOT NULL,
    rate_id              BIGINT,
    applied_rate         NUMERIC(5,2) NOT NULL,
    condition_verified_yn BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by           VARCHAR(100),
    updated_at           TIMESTAMPTZ,
    updated_by           VARCHAR(100),
    CONSTRAINT fk_deposit_contract_applied_rates_contract
        FOREIGN KEY (contract_id)
        REFERENCES deposit_contracts (contract_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- =============================================
-- 수신 특약 동의
-- =============================================
CREATE TABLE deposit_contract_special_term_agreements (
    special_agreement_id      BIGSERIAL   PRIMARY KEY,
    contract_id               BIGINT      NOT NULL,
    special_term_id           BIGINT      NOT NULL,
    is_agreed                 BOOLEAN     NOT NULL,
    agreed_at                 CHAR(8),
    agreement_ip_address      VARCHAR(45),
    agreement_device_info     VARCHAR(255),
    is_electronic_signed      BOOLEAN     NOT NULL DEFAULT FALSE,
    is_agreement_withdrawn    BOOLEAN     NOT NULL DEFAULT FALSE,
    agreement_withdrawn_at    CHAR(8),
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by                VARCHAR(100),
    updated_at                TIMESTAMPTZ,
    updated_by                VARCHAR(100),
    CONSTRAINT fk_dcsta_contract
        FOREIGN KEY (contract_id)
        REFERENCES deposit_contracts (contract_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_dcsta_special_term
        FOREIGN KEY (special_term_id)
        REFERENCES deposit_special_terms (special_term_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT uq_dcsta_contract_term UNIQUE (contract_id, special_term_id)
);

-- =============================================
-- 수신 계좌
-- =============================================
CREATE TABLE deposit_accounts (
    account_id                 BIGSERIAL    PRIMARY KEY,
    account_number             VARCHAR(30)  NOT NULL UNIQUE,
    customer_id                VARCHAR(30)  NOT NULL,
    contract_id                BIGINT       NOT NULL UNIQUE,
    account_type               VARCHAR(30)  NOT NULL,
    saving_type                VARCHAR(20),
    bank_code                  VARCHAR(10)  NOT NULL DEFAULT '001',
    account_alias              VARCHAR(100),
    balance                    NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_paid_amount          NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_interest_amount      NUMERIC(18,2) NOT NULL DEFAULT 0,
    last_transaction_at        TIMESTAMPTZ,
    last_interest_paid_at      TIMESTAMPTZ,
    currency                   CHAR(3)      NOT NULL DEFAULT 'KRW',
    account_password           VARCHAR(255) NOT NULL,
    daily_withdraw_limit       NUMERIC(18,2),
    daily_withdraw_count_limit INT,
    atm_withdraw_limit         NUMERIC(18,2),
    is_withdrawable            BOOLEAN      NOT NULL DEFAULT TRUE,
    is_online_banking_enabled  BOOLEAN      NOT NULL DEFAULT FALSE,
    is_mobile_banking_enabled  BOOLEAN      NOT NULL DEFAULT FALSE,
    is_phone_banking_enabled   BOOLEAN      NOT NULL DEFAULT FALSE,
    account_status             VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    opened_at                  DATE         NOT NULL,
    maturity_at                DATE,
    dormant_at                 DATE,
    dormant_released_at        DATE,
    closed_at                  DATE,
    status_changed_at          DATE,
    version                    BIGINT       NOT NULL DEFAULT 0,
    created_at                 TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by                 VARCHAR(100),
    updated_at                 TIMESTAMPTZ,
    updated_by                 VARCHAR(100),
    CONSTRAINT fk_deposit_accounts_contract
        FOREIGN KEY (contract_id)
        REFERENCES deposit_contracts (contract_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- =============================================
-- 이자 내역
-- =============================================
CREATE TABLE deposit_interest_history (
    interest_id                      BIGSERIAL    PRIMARY KEY,
    contract_id                      BIGINT       NOT NULL,
    account_id                       BIGINT       NOT NULL,
    applied_interest_rate            NUMERIC(5,2) NOT NULL,
    interest_calculation_start_date  CHAR(8),
    interest_calculation_end_date    CHAR(8),
    interest_occurred_at             TIMESTAMPTZ,
    interest_amount                  NUMERIC(18,2) NOT NULL,
    tax_benefit_type                 VARCHAR(30)  NOT NULL,
    applied_tax_rate                 NUMERIC(6,4) NOT NULL,
    interest_before_tax              NUMERIC(18,2) NOT NULL,
    interest_tax_amount              NUMERIC(18,2) NOT NULL DEFAULT 0,
    local_income_tax_amount          NUMERIC(18,2) NOT NULL DEFAULT 0,
    interest_after_tax               NUMERIC(18,2) NOT NULL,
    interest_reason                  VARCHAR(30)  NOT NULL,
    interest_paid_at                 TIMESTAMPTZ  NOT NULL,
    created_at                       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by                       VARCHAR(100),
    updated_at                       TIMESTAMPTZ,
    updated_by                       VARCHAR(100),
    CONSTRAINT fk_deposit_interest_history_contract
        FOREIGN KEY (contract_id)
        REFERENCES deposit_contracts (contract_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_deposit_interest_history_account
        FOREIGN KEY (account_id)
        REFERENCES deposit_accounts (account_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- =============================================
-- 청약 납입 인정 이력
-- =============================================
CREATE TABLE deposit_subscription_payment_recognition_history (
    recognition_id     BIGSERIAL    PRIMARY KEY,
    contract_id        BIGINT       NOT NULL,
    payment_amount     NUMERIC(18,2) NOT NULL,
    recognized_amount  NUMERIC(18,2) NOT NULL,
    payment_month      VARCHAR(6)   NOT NULL,
    recognized_at      TIMESTAMPTZ,
    recognition_status VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(100),
    updated_at         TIMESTAMPTZ,
    updated_by         VARCHAR(100),
    CONSTRAINT fk_dspr_history_contract
        FOREIGN KEY (contract_id)
        REFERENCES deposit_contracts (contract_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- =============================================
-- 거래 내역
-- =============================================
CREATE TABLE deposit_transactions (
    transaction_id          BIGSERIAL    PRIMARY KEY,
    transaction_number      VARCHAR(50)  NOT NULL UNIQUE,
    account_id              BIGINT       NOT NULL,
    contract_id             BIGINT,
    transaction_type        VARCHAR(30)  NOT NULL,
    direction_type          VARCHAR(10)  NOT NULL,
    amount                  NUMERIC(18,2) NOT NULL,
    balance_before          NUMERIC(18,2) NOT NULL,
    balance_after           NUMERIC(18,2) NOT NULL,
    available_balance_after NUMERIC(18,2),
    fee_amount              NUMERIC(18,2) NOT NULL DEFAULT 0,
    currency                CHAR(3)      NOT NULL DEFAULT 'KRW',
    status                  VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS',
    channel_type            VARCHAR(30)  NOT NULL,
    ip_address              VARCHAR(45),
    terminal_id             VARCHAR(50),
    transaction_location    VARCHAR(100),
    transaction_memo        VARCHAR(255),
    transaction_summary     VARCHAR(100),
    transaction_at          TIMESTAMPTZ  NOT NULL,
    posted_at               TIMESTAMPTZ,
    canceled_at             TIMESTAMPTZ,
    depositor_customer_id   VARCHAR(30),
    depositor_name          VARCHAR(100),
    delegate_customer_id    VARCHAR(30),
    delegate_customer_name  VARCHAR(100),
    transfer_type           VARCHAR(30),
    counterparty_bank_code  VARCHAR(10),
    counterparty_bank_name  VARCHAR(100),
    counterparty_account_no VARCHAR(30),
    counterparty_account_id BIGINT,
    counterparty_customer_id VARCHAR(30),
    counterparty_name       VARCHAR(100),
    counterparty_name_verified_yn BOOLEAN,
    transfer_requested_at   TIMESTAMPTZ,
    transfer_completed_at   TIMESTAMPTZ,
    payment_method          VARCHAR(30),
    merchant_id             VARCHAR(50),
    merchant_name           VARCHAR(100),
    approval_number         VARCHAR(50),
    external_transaction_no VARCHAR(100),
    payment_round           INT,
    original_transaction_id BIGINT,
    failure_type            VARCHAR(30),
    failure_code            VARCHAR(50),
    failure_reason_code     VARCHAR(50),
    failure_at              TIMESTAMPTZ,
    retry_count             INTEGER      NOT NULL DEFAULT 0,
    idempotency_key         VARCHAR(64),
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(100),
    updated_at              TIMESTAMPTZ,
    updated_by              VARCHAR(100),
    CONSTRAINT fk_deposit_transactions_account
        FOREIGN KEY (account_id)
        REFERENCES deposit_accounts (account_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_deposit_transactions_contract
        FOREIGN KEY (contract_id)
        REFERENCES deposit_contracts (contract_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_deposit_transactions_counterparty
        FOREIGN KEY (counterparty_account_id)
        REFERENCES deposit_accounts (account_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_deposit_transactions_original
        FOREIGN KEY (original_transaction_id)
        REFERENCES deposit_transactions (transaction_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- =============================================
-- 인덱스
-- =============================================
CREATE INDEX idx_dbp_dept_status    ON deposit_banking_products (department_id, deposit_product_status);
CREATE INDEX idx_dbp_type_status    ON deposit_banking_products (deposit_product_type, deposit_product_status);
CREATE INDEX idx_bdp_product        ON banking_deposit_products (banking_product_id);
CREATE INDEX idx_dsp_product        ON deposit_savings_products (banking_product_id);
CREATE INDEX idx_bdp_ir_product     ON banking_deposit_product_interest_rates (banking_product_id, rate_type, is_active);
CREATE INDEX idx_bdp_ir_dates       ON banking_deposit_product_interest_rates (banking_product_id, effective_start_date, effective_end_date);
CREATE INDEX idx_bdp_tg_target      ON banking_deposit_product_target_groups (target_group_id);
CREATE INDEX idx_bdp_st_term        ON banking_deposit_product_special_terms (special_term_id);

CREATE INDEX idx_deposit_contracts_customer ON deposit_contracts (customer_id, contract_status);
CREATE INDEX idx_deposit_contracts_product  ON deposit_contracts (banking_product_id, contract_status);
CREATE INDEX idx_deposit_contracts_started  ON deposit_contracts (started_at);
CREATE INDEX idx_deposit_contracts_maturity ON deposit_contracts (maturity_at) WHERE contract_status = 'ACTIVE';
CREATE INDEX idx_deposit_car_contract       ON deposit_contract_applied_rates (contract_id);
CREATE INDEX idx_deposit_csta_contract      ON deposit_contract_special_term_agreements (contract_id);

CREATE INDEX idx_deposit_accounts_customer  ON deposit_accounts (customer_id, account_status);
CREATE INDEX idx_deposit_accounts_type      ON deposit_accounts (account_type, account_status);

CREATE INDEX idx_deposit_ih_contract        ON deposit_interest_history (contract_id, interest_paid_at DESC);
CREATE INDEX idx_deposit_ih_account         ON deposit_interest_history (account_id, interest_paid_at DESC);

CREATE INDEX idx_deposit_tx_account         ON deposit_transactions (account_id, transaction_at DESC);
CREATE INDEX idx_deposit_tx_contract        ON deposit_transactions (contract_id, transaction_at DESC) WHERE contract_id IS NOT NULL;
CREATE INDEX idx_deposit_tx_type_status     ON deposit_transactions (transaction_type, status, transaction_at DESC);

COMMIT;

-- ---- V2__seed_postman_data.sql ----
-- [MOVED] 시드 데이터는 환경별 분리를 위해 LocalDataSeeder.java(@Profile("local"))로 이관.
-- 기존 환경에서 flyway 체크섬 오류가 발생하면 `flyway repair` 실행 후 재기동하세요.

-- ---- V3__add_product_indexes.sql ----
-- V3: 수신 상품 조회 성능을 위한 인덱스 추가
-- V2 시드 데이터 이후, V5 전체 ERD 전 단계

CREATE INDEX IF NOT EXISTS idx_deposit_products_status
    ON deposit_banking_products (deposit_product_status);

CREATE INDEX IF NOT EXISTS idx_deposit_contracts_customer
    ON deposit_contracts (customer_id);

CREATE INDEX IF NOT EXISTS idx_deposit_accounts_customer
    ON deposit_accounts (customer_id);

CREATE INDEX IF NOT EXISTS idx_deposit_transactions_account
    ON deposit_transactions (account_id, status);

-- ---- V4__add_product_rate_constraints.sql ----
-- V4: 상품 금리 및 계약 제약조건 추가
-- V3 인덱스 이후, V5 전체 ERD 전 단계

ALTER TABLE deposit_banking_products
    ADD COLUMN IF NOT EXISTS max_interest_rate NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS promotion_end_date DATE;

ALTER TABLE banking_deposit_product_interest_rates
    ADD COLUMN IF NOT EXISTS effective_date DATE,
    ADD COLUMN IF NOT EXISTS expiry_date    DATE;

-- ---- V5__full_erd_schema.sql ----
-- ERD 전체 스키마 (2025-05-20)
-- 공통·수신·여신·결제·챗봇 테이블 통합
-- 기존 V1 deposit_* 테이블과 병존 (충돌 없음)

BEGIN;

-- =============================================
-- 관계자 (party)
-- =============================================
CREATE TABLE party (
    party_id            BIGSERIAL       PRIMARY KEY,
    party_type_code     VARCHAR(20)     NOT NULL,
    party_name          VARCHAR(100)    NOT NULL,
    party_english_name  VARCHAR(200),
    party_status_code   VARCHAR(20)     NOT NULL,
    created_at          TIMESTAMPTZ(3)  NOT NULL,
    created_by          BIGINT,
    updated_at          TIMESTAMPTZ(3)  NOT NULL,
    updated_by          BIGINT,
    deleted_at          TIMESTAMPTZ(3),
    deleted_by          BIGINT
);

-- =============================================
-- 관계자 - 개인 (party_person)
-- =============================================
CREATE TABLE party_person (
    party_id                    BIGINT          NOT NULL,
    rrn_encrypted               VARCHAR(255),
    ci_value                    VARCHAR(88),
    nationality_type_code       VARCHAR(10),
    nationality_code            CHAR(3),
    birth_date                  CHAR(8),
    gender_code                 CHAR(1),
    marital_status_code         VARCHAR(10),
    dependent_count             INT,
    occupation_code             VARCHAR(10),
    occupation_name             VARCHAR(100),
    workplace_name              VARCHAR(200),
    annual_income_amount        BIGINT,
    income_proof_code           VARCHAR(10),
    capacity_limit_type_code    VARCHAR(20),
    is_pep_yn                   CHAR(1)         NOT NULL,
    pep_type_code               VARCHAR(10),
    pep_country_code            CHAR(3),
    pep_position                VARCHAR(200),
    death_date                  CHAR(8),
    created_at                  TIMESTAMPTZ(3)  NOT NULL,
    created_by                  BIGINT,
    updated_at                  TIMESTAMPTZ(3)  NOT NULL,
    updated_by                  BIGINT,
    deleted_at                  TIMESTAMPTZ(3),
    deleted_by                  BIGINT,
    PRIMARY KEY (party_id),
    CONSTRAINT fk_party_person_party
        FOREIGN KEY (party_id) REFERENCES party (party_id)
);

-- =============================================
-- 관계자 - 조직 (party_organization)
-- =============================================
CREATE TABLE party_organization (
    party_id                        BIGINT          NOT NULL,
    org_subtype_code                VARCHAR(20)     NOT NULL,
    corp_reg_no                     CHAR(14),
    corp_formal_name                VARCHAR(200),
    corp_formal_english_name        VARCHAR(400),
    hq_country_code                 CHAR(3),
    foreign_corp_reg_no_encrypted   VARCHAR(255),
    corp_type_code                  VARCHAR(20),
    non_corp_type_code              VARCHAR(10),
    ownership_type_code             VARCHAR(10),
    representative_type_code        VARCHAR(10),
    establishment_date              CHAR(8),
    dissolution_date                CHAR(8),
    capital_amount                  BIGINT,
    fiscal_month                    SMALLINT,
    establishment_purpose           VARCHAR(500),
    member_count                    INT,
    charter_url                     VARCHAR(500),
    created_at                      TIMESTAMPTZ(3)  NOT NULL,
    created_by                      BIGINT,
    updated_at                      TIMESTAMPTZ(3)  NOT NULL,
    updated_by                      BIGINT,
    deleted_at                      TIMESTAMPTZ(3),
    deleted_by                      BIGINT,
    PRIMARY KEY (party_id),
    CONSTRAINT fk_party_org_party
        FOREIGN KEY (party_id) REFERENCES party (party_id)
);

-- =============================================
-- 관계자 역할 (party_role)
-- =============================================
CREATE TABLE party_role (
    role_id                 BIGSERIAL       PRIMARY KEY,
    party_id                BIGINT          NOT NULL,
    role_type_code          VARCHAR(20)     NOT NULL,
    role_status_code        VARCHAR(20)     NOT NULL,
    role_start_date         CHAR(8)         NOT NULL,
    role_end_date           CHAR(8),
    role_end_reason_code    VARCHAR(20),
    created_at              TIMESTAMPTZ(3)  NOT NULL,
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ(3)  NOT NULL,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ(3),
    deleted_by              BIGINT,
    CONSTRAINT fk_party_role_party
        FOREIGN KEY (party_id) REFERENCES party (party_id)
);

-- =============================================
-- 관계자 관계 (party_relation)
-- =============================================
CREATE TABLE party_relation (
    relation_id             BIGSERIAL       PRIMARY KEY,
    from_party_id           BIGINT          NOT NULL,
    to_party_id             BIGINT          NOT NULL,
    relation_type_code      VARCHAR(10)     NOT NULL,
    relation_detail_code    VARCHAR(10),
    equity_ratio_bps        INT,
    representation_scope    VARCHAR(200),
    proof_url               VARCHAR(500),
    relation_start_date     CHAR(8)         NOT NULL,
    relation_end_date       CHAR(8),
    relation_end_reason_code VARCHAR(20),
    created_at              TIMESTAMPTZ(3)  NOT NULL,
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ(3)  NOT NULL,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ(3),
    deleted_by              BIGINT,
    CONSTRAINT fk_party_relation_from
        FOREIGN KEY (from_party_id) REFERENCES party (party_id),
    CONSTRAINT fk_party_relation_to
        FOREIGN KEY (to_party_id)   REFERENCES party (party_id)
);

-- =============================================
-- 고객 (customer)
-- =============================================
CREATE TABLE customer (
    customer_id                 BIGSERIAL       PRIMARY KEY,
    party_id                    BIGINT          NOT NULL,
    customer_grade_code         VARCHAR(10),
    customer_status_code        VARCHAR(20)     NOT NULL,
    main_customer_yn            CHAR(1),
    credit_rating_code          VARCHAR(10),
    credit_evaluation_date      CHAR(8),
    credit_agency_code          VARCHAR(10),
    preferred_language_code     CHAR(2),
    sms_receive_yn              CHAR(1),
    email_receive_yn            CHAR(1),
    postal_receive_yn           CHAR(1),
    notification_method_code    VARCHAR(10),
    email                       VARCHAR(255),
    phone                       VARCHAR(20),
    zip_code                    VARCHAR(10),
    address                     VARCHAR(255),
    address_detail              VARCHAR(255),
    join_channel_code           VARCHAR(20),
    first_join_date             CHAR(8),
    joined_at                   TIMESTAMPTZ(3)  NOT NULL,
    last_transaction_at         TIMESTAMPTZ(3),
    dormant_datetime            TIMESTAMPTZ(3),
    closed_at                   TIMESTAMPTZ(3),
    close_reason_code           VARCHAR(20),
    privacy_expiry_date         CHAR(8),
    created_at                  TIMESTAMPTZ(3)  NOT NULL,
    created_by                  BIGINT,
    updated_at                  TIMESTAMPTZ(3)  NOT NULL,
    updated_by                  BIGINT,
    deleted_at                  TIMESTAMPTZ(3),
    deleted_by                  BIGINT,
    CONSTRAINT fk_customer_party
        FOREIGN KEY (party_id) REFERENCES party (party_id)
);

-- =============================================
-- 공통 상품 (common_product)
-- =============================================
CREATE TABLE common_product (
    product_id                  BIGSERIAL       PRIMARY KEY,
    product_cd                  VARCHAR(30)     NOT NULL UNIQUE,
    biz_div_cd                  VARCHAR(10)     NOT NULL,
    product_name                VARCHAR(200)    NOT NULL,
    product_type_cd             VARCHAR(20),
    product_description         TEXT,
    target_type_cd              VARCHAR(50),
    channel_cd                  VARCHAR(50),
    currency_cd                 CHAR(3),
    policy_product_yn           CHAR(1),
    min_amount                  BIGINT,
    max_amount                  BIGINT,
    min_period_mo               INT,
    max_period_mo               INT,
    sale_yn                     CHAR(1),
    sale_start_date             CHAR(8),
    sale_end_date               CHAR(8),
    product_brochure_url        VARCHAR(500),
    financial_consumer_act_yn   CHAR(1),
    product_status              VARCHAR(50),
    created_at                  TIMESTAMPTZ(3)  NOT NULL DEFAULT now(),
    created_by                  BIGINT,
    updated_at                  TIMESTAMPTZ(3)  NOT NULL DEFAULT now(),
    updated_by                  BIGINT
);

-- =============================================
-- 수신 상품 (deposit_product)  PK = FK
-- =============================================
CREATE TABLE deposit_product (
    product_id                      BIGINT          NOT NULL,
    deposit_product_type            VARCHAR(30),
    department_id                   BIGINT,
    base_interest_rate              NUMERIC(5,2),
    preferential_rate_condition     TEXT,
    is_early_termination_allowed    CHAR(1),
    is_tax_benefit_available        CHAR(1),
    is_auto_renewal_available       CHAR(1),
    is_passbook_issued              CHAR(1),
    PRIMARY KEY (product_id),
    CONSTRAINT fk_deposit_product_common
        FOREIGN KEY (product_id) REFERENCES common_product (product_id)
);

-- =============================================
-- 공통 약관 템플릿 (common_terms_template)
-- =============================================
CREATE TABLE common_terms_template (
    terms_template_id   BIGSERIAL       PRIMARY KEY,
    terms_no            VARCHAR(50)     NOT NULL UNIQUE,
    terms_name          VARCHAR(200)    NOT NULL,
    terms_category_cd   VARCHAR(10)     NOT NULL,
    description         TEXT,
    required_yn         CHAR(1)         NOT NULL DEFAULT 'Y',
    biz_div_cd          VARCHAR(50)     NOT NULL,
    active_yn           CHAR(1)         NOT NULL DEFAULT 'Y',
    created_at          TIMESTAMPTZ(3)  NOT NULL DEFAULT now(),
    created_by          BIGINT,
    updated_at          TIMESTAMPTZ(3)  NOT NULL DEFAULT now(),
    updated_by          BIGINT,
    deleted_at          TIMESTAMPTZ(3),
    deleted_by          BIGINT
);

-- =============================================
-- 약관 적용 관리 (terms_target_map)
-- =============================================
CREATE TABLE terms_target_map (
    terms_target_map_id BIGSERIAL       PRIMARY KEY,
    terms_template_id   BIGINT          NOT NULL,
    target_id           BIGINT,
    biz_div_cd          VARCHAR(10),
    required_yn         CHAR(1),
    created_by          BIGINT,
    created_at          TIMESTAMPTZ(3),
    updated_by          BIGINT,
    updated_at          TIMESTAMPTZ(3),
    CONSTRAINT fk_terms_target_map_template
        FOREIGN KEY (terms_template_id) REFERENCES common_terms_template (terms_template_id)
);

-- =============================================
-- 인증 토큰 (Korean physical names)
-- =============================================
CREATE TABLE "인증토큰" (
    "인증토큰번호"  VARCHAR(50)  NOT NULL,
    "고객ID"        BIGINT       NOT NULL,
    "인증유형"      VARCHAR(20)  NOT NULL,
    "최초등록일시"  TIMESTAMP    NOT NULL,
    "최초등록자ID"  BIGINT,
    "최종수정일시"  TIMESTAMP    NOT NULL,
    "최종수정자ID"  BIGINT,
    PRIMARY KEY ("인증토큰번호"),
    CONSTRAINT fk_auth_token_customer
        FOREIGN KEY ("고객ID") REFERENCES customer (customer_id)
);

-- =============================================
-- 결제 지시 (Korean physical names)
-- =============================================
CREATE TABLE "결제지시" (
    "결제지시번호"          VARCHAR(20)     NOT NULL,
    "연결된멱등키"          VARCHAR(50)     NOT NULL,
    "송신고객번호"          VARCHAR(30),
    "송신계좌번호"          VARCHAR(30),
    "인증토큰번호"          VARCHAR(50),
    "원거래참조번호"        VARCHAR(20),
    "거래번호"              VARCHAR(30)     NOT NULL,
    "송신계좌번호_스냅샷"   VARCHAR(30)     NOT NULL,
    "송신계좌별명_스냅샷"   VARCHAR(60),
    "수신은행코드"          CHAR(3)         NOT NULL,
    "수신계좌번호"          VARCHAR(30)     NOT NULL,
    "수신예금주명_스냅샷"   VARCHAR(60)     NOT NULL,
    "예금주조회일시"        TIMESTAMP       NOT NULL,
    "자행이체여부"          CHAR(1)         NOT NULL,
    "라우팅망종류"          VARCHAR(20)     NOT NULL,
    "이체금액"              NUMERIC(15,0)   NOT NULL,
    "결제수수료"            NUMERIC(15,0)   NOT NULL,
    "수신통장_송신자표시명" VARCHAR(60),
    "받는분통장메모"        VARCHAR(100),
    "내통장메모"            VARCHAR(100),
    "진행상태"              VARCHAR(20)     NOT NULL,
    "실패분류"              VARCHAR(30),
    "결제지시채널"          VARCHAR(20)     NOT NULL,
    "요청일시"              TIMESTAMP       NOT NULL,
    "완료일시"              TIMESTAMP,
    "영업일자"              CHAR(8)         NOT NULL,
    "다음재시도일시"        TIMESTAMP,
    "다음타임아웃일시"      TIMESTAMP,
    "트리거주체"            VARCHAR(20)     NOT NULL,
    "예약여부"              CHAR(1)         NOT NULL,
    "예약실행일시"          TIMESTAMP,
    "최초등록일시"          TIMESTAMP       NOT NULL,
    "최초등록자ID"          BIGINT,
    "최종수정일시"          TIMESTAMP       NOT NULL,
    "최종수정자ID"          BIGINT,
    PRIMARY KEY ("결제지시번호"),
    CONSTRAINT fk_payment_instr_token
        FOREIGN KEY ("인증토큰번호") REFERENCES "인증토큰" ("인증토큰번호")
);

-- =============================================
-- 원장 (Korean physical names)
-- =============================================
CREATE TABLE "원장" (
    "원장번호"                VARCHAR(20)     NOT NULL,
    "결제지시번호"            VARCHAR(20),
    "계좌번호"                VARCHAR(30)     NOT NULL,
    "원분개참조번호"          VARCHAR(20),
    "회계번호"                VARCHAR(20)     NOT NULL,
    "계좌번호_스냅샷"         VARCHAR(30)     NOT NULL,
    "예금주명_스냅샷"         VARCHAR(60)     NOT NULL,
    "차변대변구분"            CHAR(6)         NOT NULL,
    "분개종류"                VARCHAR(30)     NOT NULL,
    "금액"                    NUMERIC(15,0)   NOT NULL,
    "통화"                    CHAR(3)         NOT NULL,
    "분개직전잔액"            NUMERIC(15,0)   NOT NULL,
    "분개직후잔액"            NUMERIC(15,0)   NOT NULL,
    "상대계좌번호_스냅샷"     VARCHAR(30),
    "상대은행코드_스냅샷"     CHAR(3),
    "상대예금주명_스냅샷"     VARCHAR(60),
    "거래일자"                CHAR(8)         NOT NULL,
    "기장일자"                CHAR(8)         NOT NULL,
    "자금가용일"              CHAR(8)         NOT NULL,
    "기장일시"                TIMESTAMP       NOT NULL,
    "시스템적요"              VARCHAR(100)    NOT NULL,
    "통장에찍히는메모_스냅샷" VARCHAR(100),
    "역분개여부"              CHAR(1)         NOT NULL,
    "역분개사유"              VARCHAR(20),
    "기장상태"                VARCHAR(20)     NOT NULL,
    "최초등록일시"            TIMESTAMP       NOT NULL,
    "최초등록자ID"            BIGINT,
    "최종수정일시"            TIMESTAMP       NOT NULL,
    "최종수정자ID"            BIGINT,
    PRIMARY KEY ("원장번호"),
    CONSTRAINT fk_ledger_payment
        FOREIGN KEY ("결제지시번호") REFERENCES "결제지시" ("결제지시번호")
);

-- =============================================
-- 상태 이력 (Korean physical names)
-- =============================================
CREATE TABLE "상태이력" (
    "상태이력번호"    VARCHAR(20)  NOT NULL,
    "결제지시번호"    VARCHAR(20)  NOT NULL,
    "결제지시내순번"  INT          NOT NULL,
    "이전상태"        VARCHAR(20),
    "다음상태"        VARCHAR(20)  NOT NULL,
    "이벤트종류"      VARCHAR(30)  NOT NULL,
    "사유코드"        VARCHAR(10),
    "사유메시지"      VARCHAR(200),
    "트리거주체"      VARCHAR(20)  NOT NULL,
    "운영자ID"        VARCHAR(20),
    "이벤트발생일시"  TIMESTAMP    NOT NULL,
    "최초등록일시"    TIMESTAMP    NOT NULL,
    "최초등록자ID"    BIGINT,
    "최종수정일시"    TIMESTAMP    NOT NULL,
    "최종수정자ID"    BIGINT,
    PRIMARY KEY ("상태이력번호"),
    CONSTRAINT fk_status_history_payment
        FOREIGN KEY ("결제지시번호") REFERENCES "결제지시" ("결제지시번호")
);

-- =============================================
-- 금융결제원 청산 거래 (Korean physical names)
-- =============================================
CREATE TABLE "금융결제원청산거래" (
    "청산거래번호"          VARCHAR(20)     NOT NULL,
    "우리결제지시번호"      VARCHAR(20)     NOT NULL,
    "거래방향"              VARCHAR(5)      NOT NULL,
    "상대은행결제지시번호"  VARCHAR(50),
    "청산번호"              VARCHAR(50)     NOT NULL,
    "송신은행의청산ID"      VARCHAR(50),
    "수신은행의청산ID"      VARCHAR(50),
    "송신은행코드"          CHAR(3)         NOT NULL,
    "송신계좌번호_스냅샷"   VARCHAR(30)     NOT NULL,
    "송신예금주명_스냅샷"   VARCHAR(60)     NOT NULL,
    "수신은행코드"          CHAR(3)         NOT NULL,
    "수신계좌번호_스냅샷"   VARCHAR(30)     NOT NULL,
    "수신예금주명_스냅샷"   VARCHAR(60)     NOT NULL,
    "청산금액"              NUMERIC(15,0)   NOT NULL,
    "통화"                  CHAR(3)         NOT NULL,
    "청산상태"              VARCHAR(20)     NOT NULL,
    "거절코드"              VARCHAR(10),
    "거절메시지"            VARCHAR(200),
    "청산요청일시"          TIMESTAMP       NOT NULL,
    "ACK수신일시"           TIMESTAMP,
    "정산완료일시"          TIMESTAMP,
    "정산일자"              CHAR(8),
    "네트워크"              VARCHAR(30)     NOT NULL,
    "조회횟수"              INT             NOT NULL,
    "마지막조회일시"        TIMESTAMP,
    "최초등록일시"          TIMESTAMP       NOT NULL,
    "최초등록자ID"          BIGINT,
    "최종수정일시"          TIMESTAMP       NOT NULL,
    "최종수정자ID"          BIGINT,
    PRIMARY KEY ("청산거래번호"),
    CONSTRAINT fk_clearing_tx_payment
        FOREIGN KEY ("우리결제지시번호") REFERENCES "결제지시" ("결제지시번호")
);

-- =============================================
-- 한국은행 결제 거래 (Korean physical names)
-- =============================================
CREATE TABLE "한국은행결제거래" (
    "결제거래번호"              VARCHAR(20)     NOT NULL,
    "우리결제지시번호"          VARCHAR(20)     NOT NULL,
    "거래방향"                  VARCHAR(5)      NOT NULL,
    "상대은행결제지시번호"      VARCHAR(50),
    "한은망참조번호"            VARCHAR(50)     NOT NULL,
    "송신은행코드"              CHAR(3)         NOT NULL,
    "송신은행의결제지시번호"    VARCHAR(50)     NOT NULL,
    "송신계좌번호_스냅샷"       VARCHAR(30)     NOT NULL,
    "송신예금주명_스냅샷"       VARCHAR(60)     NOT NULL,
    "수신은행코드"              CHAR(3)         NOT NULL,
    "수신은행의결제지시번호"    VARCHAR(50),
    "수신계좌번호_스냅샷"       VARCHAR(30)     NOT NULL,
    "수신예금주명_스냅샷"       VARCHAR(60)     NOT NULL,
    "결제금액"                  NUMERIC(15,0)   NOT NULL,
    "통화"                      CHAR(3)         NOT NULL,
    "결제상태"                  VARCHAR(20)     NOT NULL,
    "거절코드"                  VARCHAR(10),
    "거절메시지"                VARCHAR(200),
    "결제요청일시"              TIMESTAMP       NOT NULL,
    "ACK수신일시"               TIMESTAMP,
    "결제확정일시"              TIMESTAMP,
    "영업일자"                  CHAR(8)         NOT NULL,
    "조회횟수"                  INT             NOT NULL,
    "마지막조회일시"            TIMESTAMP,
    "최초등록일시"              TIMESTAMP       NOT NULL,
    "최초등록자ID"              BIGINT,
    "최종수정일시"              TIMESTAMP       NOT NULL,
    "최종수정자ID"              BIGINT,
    PRIMARY KEY ("결제거래번호"),
    CONSTRAINT fk_bok_payment_instr
        FOREIGN KEY ("우리결제지시번호") REFERENCES "결제지시" ("결제지시번호")
);

-- =============================================
-- 공통 계약 (common_contract)
-- =============================================
CREATE TABLE common_contract (
    contract_id                 BIGSERIAL       PRIMARY KEY,
    contract_no                 VARCHAR(50),
    customer_id                 BIGINT          NOT NULL,
    customer_no                 VARCHAR(30),
    product_id                  BIGINT          NOT NULL,
    biz_div_cd                  VARCHAR(10),
    contract_amount             BIGINT,
    rate_type_cd                VARCHAR(10),
    base_rate_bps               INT,
    spread_bps                  INT,
    preferential_bps            INT,
    total_rate_bps              INT,
    interest_amount_at_maturity BIGINT,
    contract_start_date         CHAR(8),
    contract_end_date           CHAR(8),
    contract_cancel_date        CHAR(8),
    contract_cancel_reason      VARCHAR(200),
    auto_transfer_yn            CHAR(1),
    auto_transfer_day           INT,
    signed_at                   TIMESTAMPTZ(3),
    contract_channel_cd         VARCHAR(20),
    spot_id                     BIGINT,
    spot_name                   VARCHAR(100),
    manager_id                  BIGINT,
    manager_name                VARCHAR(100),
    proxy_yn                    CHAR(1),
    contract_status             VARCHAR(20),
    term_url                    VARCHAR(500),
    term_hash                   VARCHAR(64),
    contract_url                VARCHAR(500),
    contract_hash               VARCHAR(64),
    created_at                  TIMESTAMPTZ(3)  NOT NULL DEFAULT now(),
    created_by                  BIGINT,
    updated_at                  TIMESTAMPTZ(3)  NOT NULL DEFAULT now(),
    updated_by                  BIGINT,
    CONSTRAINT fk_common_contract_product
        FOREIGN KEY (product_id)  REFERENCES common_product (product_id)
);

-- =============================================
-- 수신 계약 (deposit_contract)  PK = FK
-- =============================================
CREATE TABLE deposit_contract (
    contract_id                     BIGINT          NOT NULL,
    is_monthly_payment              CHAR(1),
    payment_count_total             INT,
    contract_interest_rate          NUMERIC(5,2),
    total_preferential_rate         NUMERIC(5,2),
    final_interest_rate             NUMERIC(5,2),
    tax_benefit_type                VARCHAR(30),
    applied_tax_rate                NUMERIC(5,2),
    expected_interest_amount        NUMERIC(18,2),
    is_auto_renewal                 CHAR(1),
    status_changed_at               CHAR(8),
    "계약 지점 코드"                VARCHAR(20),
    is_power_of_attorney_verified   CHAR(1),
    power_of_attorney_file_url      VARCHAR(500),
    PRIMARY KEY (contract_id),
    CONSTRAINT fk_deposit_contract_common
        FOREIGN KEY (contract_id) REFERENCES common_contract (contract_id)
);

-- =============================================
-- 공통 약관 동의 (common_terms_consent)
-- =============================================
CREATE TABLE common_terms_consent (
    consent_id          BIGSERIAL       NOT NULL,
    customer_id         BIGINT          NOT NULL,
    terms_template_id   BIGINT          NOT NULL,
    biz_div_cd          VARCHAR(10)     NOT NULL,
    consent_target_id   BIGINT,
    consent_status_cd   VARCHAR(10)     NOT NULL,
    agreed_yn           CHAR(1)         NOT NULL,
    agreed_at           CHAR(8)         NOT NULL,
    consent_method_cd   VARCHAR(10)     NOT NULL,
    consent_tool        VARCHAR(500),
    signed_doc_url      VARCHAR(500),
    signed_doc_hash     VARCHAR(64),
    client_ip           INET,
    withdrawn_yn        CHAR(1)         NOT NULL DEFAULT 'N',
    withdrawn_at        TIMESTAMPTZ(3),
    withdrawn_reason    VARCHAR(500),
    retention_until     CHAR(8),
    created_at          TIMESTAMPTZ(3)  NOT NULL DEFAULT now(),
    created_by          BIGINT,
    updated_at          TIMESTAMPTZ(3)  NOT NULL DEFAULT now(),
    updated_by          BIGINT,
    deleted_at          TIMESTAMPTZ(3),
    deleted_by          BIGINT,
    PRIMARY KEY (consent_id, customer_id),
    CONSTRAINT fk_terms_consent_template
        FOREIGN KEY (terms_template_id) REFERENCES common_terms_template (terms_template_id)
);


-- =============================================
-- 공통 거래 (common_transaction)
-- 자기참조 FK (original_transaction_id) → DEFERRABLE
-- =============================================
CREATE TABLE common_transaction (
    transaction_id              BIGSERIAL       PRIMARY KEY,
    transaction_no              VARCHAR(50),
    account_id                  BIGINT,
    contract_id                 BIGINT,
    transaction_type_cd         VARCHAR(30),
    debit_credit_type           VARCHAR(10),
    transaction_amount          BIGINT,
    balance_before              BIGINT,
    balance_after               BIGINT,
    fee_amount                  BIGINT,
    channel_cd                  VARCHAR(30),
    counterparty_bank_cd        VARCHAR(10),
    counterparty_bank_name      VARCHAR(100),
    counterparty_account_no     VARCHAR(30),
    counterparty_name           VARCHAR(100),
    counterparty_customer_id    BIGINT,
    counterparty_account_id     BIGINT,
    counterparty_name_verified_yn CHAR(1),
    original_transaction_id     BIGINT,
    transaction_memo            VARCHAR(255),
    transaction_status          VARCHAR(20),
    transacted_at               TIMESTAMPTZ(3),
    currency_cd                 CHAR(3),
    available_balance           BIGINT,
    transaction_summary         VARCHAR(100),
    transfer_type_cd            VARCHAR(30),
    transfer_requested_at       TIMESTAMPTZ(3),
    transfer_completed_at       TIMESTAMPTZ(3),
    transfer_failed_yn          CHAR(1),
    payment_method_code         VARCHAR(30),
    card_payment_yn             CHAR(1),
    payment_failed_yn           CHAR(1),
    merchant_no                 VARCHAR(50),
    merchant_name               VARCHAR(100),
    failure_type_cd             VARCHAR(30),
    failure_reason_cd           VARCHAR(50),
    failure_cause_cd            VARCHAR(50),
    failed_at                   TIMESTAMPTZ(3),
    retry_count                 INT,
    approval_no                 VARCHAR(50),
    external_transaction_no     VARCHAR(100),
    terminal_id                 VARCHAR(50),
    client_ip                   VARCHAR(45),
    transaction_location        VARCHAR(100),
    ledger_posted_at            TIMESTAMPTZ(3),
    cancelled_at                TIMESTAMPTZ(3),
    created_at                  TIMESTAMPTZ(3)  NOT NULL DEFAULT now(),
    created_by                  BIGINT,
    updated_at                  TIMESTAMPTZ(3)  NOT NULL DEFAULT now(),
    updated_by                  BIGINT,
    CONSTRAINT fk_common_tx_account
        FOREIGN KEY (account_id)              REFERENCES common_account (account_id),
    CONSTRAINT fk_common_tx_contract
        FOREIGN KEY (contract_id)             REFERENCES common_contract (contract_id),
    CONSTRAINT fk_common_tx_original
        FOREIGN KEY (original_transaction_id) REFERENCES common_transaction (transaction_id)
        DEFERRABLE INITIALLY DEFERRED
);

-- =============================================
-- 챗봇·상담 테이블은 consultation-service가 소유.
-- SQLAlchemy create_all() 로 consultation-service 기동 시 자동 생성됨.
-- (V12에서 기존 불일치 테이블 DROP 처리)
-- =============================================
-- 인덱스
-- =============================================
CREATE INDEX idx_party_type_status      ON party (party_type_code, party_status_code);
CREATE INDEX idx_party_role_party       ON party_role (party_id);
CREATE INDEX idx_party_relation_from    ON party_relation (from_party_id);
CREATE INDEX idx_party_relation_to      ON party_relation (to_party_id);

CREATE INDEX idx_customer_party         ON customer (party_id);
CREATE INDEX idx_customer_status        ON customer (customer_status_code);

CREATE INDEX idx_common_product_biz     ON common_product (biz_div_cd, product_status);
CREATE INDEX idx_common_product_cd      ON common_product (product_cd);

CREATE INDEX idx_common_terms_no        ON common_terms_template (terms_no);
CREATE INDEX idx_terms_target_template  ON terms_target_map (terms_template_id);
CREATE INDEX idx_terms_consent_customer ON common_terms_consent (customer_id);
CREATE INDEX idx_terms_consent_template ON common_terms_consent (terms_template_id);

CREATE INDEX idx_common_contract_cust   ON common_contract (customer_id, contract_status);
CREATE INDEX idx_common_contract_prod   ON common_contract (product_id);
CREATE INDEX idx_common_account_cust    ON common_account (customer_id, account_status);

CREATE INDEX idx_common_tx_account      ON common_transaction (account_id, transacted_at DESC);
CREATE INDEX idx_common_tx_contract     ON common_transaction (contract_id, transacted_at DESC)
    WHERE contract_id IS NOT NULL;

COMMIT;

-- ---- V6__term_application_management.sql ----
-- ERD 기준 누락 테이블 추가: deposit_term_application_management (약관 적용 관리)
-- common_term_id : 공통 서비스 약관 ID (MVP: 외부 서비스 미구현으로 FK 미적용)
-- term_target_id : 약관 적용 대상 ID   (MVP: 대상 테이블 별도 관리로 FK 미적용)

BEGIN;

CREATE TABLE IF NOT EXISTS deposit_term_application_management (
    term_application_id  BIGSERIAL    PRIMARY KEY,
    common_term_id       BIGINT,
    term_target_id       BIGINT,
    business_type_code   VARCHAR(10),
    is_required          CHAR(1)      NOT NULL DEFAULT 'N',
    registered_at        CHAR(8),
    modified_at          CHAR(8),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by           VARCHAR(100),
    updated_at           TIMESTAMPTZ,
    updated_by           VARCHAR(100)
);

COMMENT ON TABLE  deposit_term_application_management                    IS '약관 적용 관리';
COMMENT ON COLUMN deposit_term_application_management.common_term_id    IS '공통 약관 ID (외부 서비스 참조)';
COMMENT ON COLUMN deposit_term_application_management.term_target_id    IS '약관 적용 대상 ID';
COMMENT ON COLUMN deposit_term_application_management.business_type_code IS '업무 구분 코드 (DEPOSIT/SAVINGS/SUBSCRIPTION)';
COMMENT ON COLUMN deposit_term_application_management.is_required        IS '필수 여부 Y/N';
COMMENT ON COLUMN deposit_term_application_management.registered_at      IS '등록일 YYYYMMDD';
COMMENT ON COLUMN deposit_term_application_management.modified_at        IS '수정일 YYYYMMDD';

COMMIT;

-- ---- V7__seed_regular_savings.sql ----
-- [MOVED] 시드 데이터는 환경별 분리를 위해 LocalDataSeeder.java(@Profile("local"))로 이관.

-- ---- V8__seed_customer_frontend_products.sql ----
-- [MOVED] 시드 데이터는 환경별 분리를 위해 LocalDataSeeder.java(@Profile("local"))로 이관.

-- ---- V9__add_account_version_column.sql ----
-- V9: deposit_accounts 테이블에 낙관적 락 version 컬럼 추가
-- JPA @Version 필드와 매핑 — 동시 잔액 변경 시 충돌 감지용

ALTER TABLE deposit_accounts
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- ---- V10__account_dates_and_number_sequence.sql ----
BEGIN;

CREATE SEQUENCE IF NOT EXISTS deposit_account_number_seq
    START WITH 100000000001
    INCREMENT BY 1
    NO CYCLE;

ALTER TABLE deposit_contracts
    ALTER COLUMN started_at TYPE DATE USING to_date(started_at, 'YYYYMMDD'),
    ALTER COLUMN maturity_at TYPE DATE USING CASE WHEN maturity_at IS NULL THEN NULL ELSE to_date(maturity_at, 'YYYYMMDD') END,
    ALTER COLUMN terminated_at TYPE DATE USING CASE WHEN terminated_at IS NULL THEN NULL ELSE to_date(terminated_at, 'YYYYMMDD') END,
    ALTER COLUMN status_changed_at TYPE DATE USING CASE WHEN status_changed_at IS NULL THEN NULL ELSE to_date(status_changed_at, 'YYYYMMDD') END;

ALTER TABLE deposit_accounts
    ALTER COLUMN opened_at TYPE DATE USING to_date(opened_at, 'YYYYMMDD'),
    ALTER COLUMN maturity_at TYPE DATE USING CASE WHEN maturity_at IS NULL THEN NULL ELSE to_date(maturity_at, 'YYYYMMDD') END,
    ALTER COLUMN dormant_at TYPE DATE USING CASE WHEN dormant_at IS NULL THEN NULL ELSE to_date(dormant_at, 'YYYYMMDD') END,
    ALTER COLUMN dormant_released_at TYPE DATE USING CASE WHEN dormant_released_at IS NULL THEN NULL ELSE to_date(dormant_released_at, 'YYYYMMDD') END,
    ALTER COLUMN closed_at TYPE DATE USING CASE WHEN closed_at IS NULL THEN NULL ELSE to_date(closed_at, 'YYYYMMDD') END,
    ALTER COLUMN status_changed_at TYPE DATE USING CASE WHEN status_changed_at IS NULL THEN NULL ELSE to_date(status_changed_at, 'YYYYMMDD') END;

COMMIT;

-- ---- V11__payment_schedules.sql ----
BEGIN;

-- 납입 스케줄 테이블 (자동이체 + 수동 납입 지연 추적)
CREATE TABLE deposit_payment_schedules (
    schedule_id          BIGSERIAL       PRIMARY KEY,
    contract_id          BIGINT          NOT NULL,
    account_id           BIGINT          NOT NULL,
    payment_round        INT             NOT NULL,
    scheduled_date       DATE            NOT NULL,
    scheduled_amount     NUMERIC(18,2)   NOT NULL,
    is_auto_transfer     BOOLEAN         NOT NULL DEFAULT false,
    source_account_id    BIGINT,
    status               VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    paid_at              TIMESTAMPTZ(3),
    actual_amount        NUMERIC(18,2),
    transaction_id       BIGINT,
    failure_reason_code  VARCHAR(50),
    created_at           TIMESTAMPTZ(3)  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ(3),
    CONSTRAINT fk_payment_schedule_contract
        FOREIGN KEY (contract_id) REFERENCES deposit_contracts (contract_id),
    CONSTRAINT fk_payment_schedule_account
        FOREIGN KEY (account_id) REFERENCES deposit_accounts (account_id)
);

CREATE INDEX idx_payment_schedules_contract ON deposit_payment_schedules (contract_id);
CREATE INDEX idx_payment_schedules_scheduled_date ON deposit_payment_schedules (scheduled_date, status);

-- 연속 미납 횟수, 자동이체 출금 계좌 컬럼 추가
ALTER TABLE deposit_contracts
    ADD COLUMN consecutive_miss_count INT  NOT NULL DEFAULT 0,
    ADD COLUMN source_account_id      BIGINT;

COMMIT;

-- ---- V12__drop_chatbot_consultation_tables.sql ----
-- =============================================
-- V12: 챗봇·상담 테이블 제거
--
-- 배경: V5에서 ERD 전체를 일괄 생성할 때 chatbot·consultation 테이블이
--       deposit-db에 포함됐으나, 해당 테이블의 실제 소유자는 consultation-service.
--       consultation-service가 SQLAlchemy create_all()로 올바른 스키마를 관리하므로
--       deposit-db에서 제거하고 consultation-service가 재생성하도록 한다.
--
-- 삭제 순서: FK 의존 순서 (자식 → 부모)
-- =============================================

DROP TABLE IF EXISTS chatbot_conversation_history  CASCADE;
DROP TABLE IF EXISTS chatbot_node_flow             CASCADE;
DROP TABLE IF EXISTS chatbot_node_button           CASCADE;
DROP TABLE IF EXISTS chatbot_consultation          CASCADE;
DROP TABLE IF EXISTS chat_message_history          CASCADE;
DROP TABLE IF EXISTS chat_consultation             CASCADE;
DROP TABLE IF EXISTS chatbot_node                  CASCADE;
DROP TABLE IF EXISTS chatbot_intent                CASCADE;
DROP TABLE IF EXISTS chatbot_scenario              CASCADE;
DROP TABLE IF EXISTS consultation                  CASCADE;

-- ---- V13__add_idempotency_key_to_transactions.sql ----
-- V13: deposit_transactions 테이블에 idempotency_key 컬럼 추가
-- 클라이언트가 동일 키로 재시도해도 이체가 중복 실행되지 않도록 보장한다.
-- 기존 행은 NULL로 채워지며(nullable), 신규 이체 요청부터 키를 강제한다.

ALTER TABLE deposit_transactions
    ADD COLUMN idempotency_key VARCHAR(64) NULL;

CREATE UNIQUE INDEX uq_deposit_transactions_idempotency_key
    ON deposit_transactions (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

-- ---- V14__add_age_range_to_target_groups.sql ----
ALTER TABLE deposit_target_groups
    ADD COLUMN IF NOT EXISTS min_age INTEGER,
    ADD COLUMN IF NOT EXISTS max_age INTEGER;

-- 청년고객: 만 19~34세
UPDATE deposit_target_groups SET min_age = 19, max_age = 34 WHERE target_group_name = '청년고객';

-- 국군장병: 만 18~27세 (현역 복무 연령 기준)
UPDATE deposit_target_groups SET min_age = 18, max_age = 27 WHERE target_group_name = '국군장병';

-- ---- V15__seed_employee01_accounts.sql ----
-- =============================================================================
-- V13: 홍길동(customer_id=9001) 테스트 계좌 시드
-- 예금 1건(AXful 정기예금) + 적금 1건(AXful 내맘대로적금)
-- DB 재시작 후에도 항상 복구됨
-- =============================================================================

-- 계약 시퀀스 충돌 방지: 2001번부터 사용
INSERT INTO deposit_contracts (
    contract_id, contract_number, customer_id, banking_product_id,
    is_monthly_payment, join_amount,
    contract_interest_rate, total_preferential_rate, final_interest_rate,
    tax_benefit_type, applied_tax_rate, expected_interest_amount,
    contract_period_month, started_at, maturity_at,
    contract_status, join_channel, is_auto_renewal, auto_transfer_enabled,
    is_proxy_joined, is_power_of_attorney_verified, consecutive_miss_count
) VALUES
-- 정기예금 12개월
(2001, 'DEMO-EMP1-DEP-001', '9001', 1,
 false, 5000000.00,
 2.15, 0.00, 2.15,
 'GENERAL', 15.40, 107500.00,
 12, '2026-01-09', '2027-01-09',
 'ACTIVE', 'WEB', false, false,
 false, false, 0),
-- 적금 12개월
(2002, 'DEMO-EMP1-SAV-001', '9001', 5,
 true, 300000.00,
 2.95, 0.00, 2.95,
 'GENERAL', 15.40, 35400.00,
 12, '2026-01-09', '2027-01-09',
 'ACTIVE', 'WEB', false, true,
 false, false, 0)
ON CONFLICT (contract_id) DO NOTHING;

-- 계좌 시퀀스 충돌 방지: 2001번부터 사용
-- account_password: bcrypt("123456") = $2a$10$VKbbcNGoISwIX.xrAERLe.ehKBatPhD6qhq1VePuHOHwDh0SBukje
INSERT INTO deposit_accounts (
    account_id, account_number, customer_id, contract_id,
    account_type, saving_type, bank_code,
    balance, total_paid_amount, total_interest_amount,
    currency, account_password,
    is_withdrawable, is_online_banking_enabled, is_mobile_banking_enabled, is_phone_banking_enabled,
    account_status, opened_at, maturity_at, version
) VALUES
-- 정기예금 계좌
(2001, '001-901-000001', '9001', 2001,
 'DEPOSIT', NULL, '001',
 5000000.00, 5000000.00, 0.00,
 'KRW', '$2a$10$VKbbcNGoISwIX.xrAERLe.ehKBatPhD6qhq1VePuHOHwDh0SBukje',
 true, false, false, false,
 'ACTIVE', '2026-01-09', '2027-01-09', 0),
-- 적금 계좌
(2002, '001-901-000002', '9001', 2002,
 'SAVINGS', 'REGULAR', '001',
 1500000.00, 1500000.00, 0.00,
 'KRW', '$2a$10$VKbbcNGoISwIX.xrAERLe.ehKBatPhD6qhq1VePuHOHwDh0SBukje',
 true, false, false, false,
 'ACTIVE', '2026-01-09', '2027-01-09', 0)
ON CONFLICT (account_id) DO NOTHING;

-- 시퀀스를 2003 이상으로 조정 (새 가입이 충돌나지 않도록)
SELECT setval('deposit_contracts_contract_id_seq', GREATEST(2002, (SELECT MAX(contract_id) FROM deposit_contracts)));
SELECT setval('deposit_accounts_account_id_seq',   GREATEST(2002, (SELECT MAX(account_id)  FROM deposit_accounts)));

-- 최근 3개월 거래 시드 (현금흐름 추천 채점용)
-- 월 급여 약 330만원 입금, 지출 약 166만원 → 월 잉여자금 약 164만원
INSERT INTO deposit_transactions (
    transaction_number, account_id, transaction_type, direction_type,
    amount, balance_before, balance_after, available_balance_after,
    fee_amount, currency, status, channel_type,
    transaction_memo, transaction_summary, transaction_at
) VALUES
-- 3개월 전
('TX-9001-M3-IN-01',  2001, 'DEPOSIT',  'IN',  3300000, 5000000, 8300000, 8300000, 0, 'KRW', 'SUCCESS', 'WEB', '급여', '급여입금', NOW() - INTERVAL '89 days'),
('TX-9001-M3-OUT-01', 2001, 'WITHDRAW', 'OUT', 1660000, 8300000, 6640000, 6640000, 0, 'KRW', 'SUCCESS', 'WEB', '생활비', '생활비출금', NOW() - INTERVAL '75 days'),
-- 2개월 전
('TX-9001-M2-IN-01',  2001, 'DEPOSIT',  'IN',  3300000, 6640000, 9940000, 9940000, 0, 'KRW', 'SUCCESS', 'WEB', '급여', '급여입금', NOW() - INTERVAL '59 days'),
('TX-9001-M2-OUT-01', 2001, 'WITHDRAW', 'OUT', 1660000, 9940000, 8280000, 8280000, 0, 'KRW', 'SUCCESS', 'WEB', '생활비', '생활비출금', NOW() - INTERVAL '45 days'),
-- 1개월 전
('TX-9001-M1-IN-01',  2001, 'DEPOSIT',  'IN',  3300000, 8280000, 11580000, 11580000, 0, 'KRW', 'SUCCESS', 'WEB', '급여', '급여입금', NOW() - INTERVAL '29 days'),
('TX-9001-M1-OUT-01', 2001, 'WITHDRAW', 'OUT', 1660000, 11580000, 9920000, 9920000, 0, 'KRW', 'SUCCESS', 'WEB', '생활비', '생활비출금', NOW() - INTERVAL '15 days')
ON CONFLICT (transaction_number) DO NOTHING;

-- ============================================================
-- SERVICE: loan-service  (DB: loan_db)
-- ============================================================

-- ---- V1__init_loan_schema.sql ----
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
    min_guarantor_count      INT          NOT NULL DEFAULT 0,
    application_validity_days INT,
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
    branch_id            VARCHAR(10),
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
    ai_track_cd           VARCHAR(20),
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

-- loan_document_ocr는 V21에서 DROP → loan_document_submission으로 대체
CREATE TABLE loan_document_submission (
    submission_id     VARCHAR(36)   NOT NULL,
    doc_id            BIGINT        REFERENCES loan_document(doc_id),
    application_id    VARCHAR(30)   NOT NULL,
    doc_code          VARCHAR(50)   NOT NULL,
    verify_status     VARCHAR(50),
    confidence_score  DECIMAL(5,4),
    occurred_at       TIMESTAMPTZ,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_loan_document_submission PRIMARY KEY (submission_id)
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
    reviewer_id              BIGINT,
    reviewed_at              TIMESTAMPTZ,
    approved_at              TIMESTAMPTZ,
    approver_id              BIGINT,
    approved_decision_cd     VARCHAR(50),
    override_reason_cd       VARCHAR(50),
    override_remark          VARCHAR(500),
    bias_severity_cd         VARCHAR(20),
    bias_override_by         BIGINT,
    bias_override_reason     VARCHAR(500),
    bias_overridden_at       TIMESTAMPTZ,
    pending_approver_since   TIMESTAMPTZ,
    rev_ai_track_cd          VARCHAR(20),
    rev_ai_pd                DECIMAL(10,6),
    rev_ai_rationale         TEXT,
    agent_opinion_json       JSONB,
    owner_id                 BIGINT,
    escalated_at             TIMESTAMPTZ(3),
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
    holder_name_enc    BYTEA,
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
    pi_id                         VARCHAR(100),
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
    holiday_adjusted_yn CHAR(1)       NOT NULL DEFAULT 'N',
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
    pi_id                  VARCHAR(100),
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
    dlq_id            BIGINT,                   -- V2.1: 연체 자동 발화 출처 dlq 추적
    external_ack_no   VARCHAR(100),             -- V6: ACK callback 외부 기관 추적 번호
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
    clos_doc_url            VARCHAR(500),
    clos_doc_hash           VARCHAR(128),
    write_off_amount        BIGINT,
    subrogation_amount      BIGINT,
    subrogation_party_ref   VARCHAR(200),
    write_off_reason_cd     VARCHAR(50),
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

-- ---- V2__add_agent_opinion_json.sql ----
-- ============================================================
-- A2 migration: loan_review 테이블에 사전 심사 에이전트 의견 컬럼 추가
--
-- pre-review-agent-plan.md 운영 대비책 §DB 마이그레이션
-- - JSONB NULL: 에이전트 미실행(Track 1 skip, fallback, 미도입 레거시) 허용
-- - 크기 CHECK: 단일 컬럼이 64KB 초과 시 저장 거부 (비정상 응답 방어)
-- - 인덱스 없음: 현 단계 — 필요시 GIN 인덱스 추가
-- ============================================================

ALTER TABLE loan_review
    ADD COLUMN IF NOT EXISTS agent_opinion_json JSONB NULL
        CONSTRAINT chk_agent_opinion_json_size
            CHECK (pg_column_size(agent_opinion_json) < 65536);

COMMENT ON COLUMN loan_review.agent_opinion_json
    IS 'Pre-Review Agent 의견 JSON (schema_version v1). NULL = 에이전트 미실행 또는 fallback.';

-- ---- V2.1__credit_info_report_add_dlq_id.sql ----
-- 연체 자동 발화 신고의 출처 dlq 추적 컬럼.
-- 자동 발화 외 신고(수동/약정 체결/종결)는 NULL.
-- 멱등 UNIQUE 인덱스는 step 3 (신고 멱등 가드) 마이그레이션에서 추가한다.

ALTER TABLE credit_info_report
    ADD COLUMN dlq_id BIGINT;

CREATE INDEX idx_credit_info_report_dlq_id
    ON credit_info_report (dlq_id)
    WHERE dlq_id IS NOT NULL;

-- ---- V3__credit_info_report_dlq_unique.sql ----
-- 연체 자동 발화 멱등 가드.
-- 같은 (cntrId, dlqId, crptTypeCd, reportReasonCd) 신고가 SENT/ACKED 상태로 중복 적재되는 것을 차단한다.
-- dlqId 가 NULL 인 수동/약정/종결 신고는 본 제약 대상 외.

CREATE UNIQUE INDEX uk_credit_info_report_dlq_idem
    ON credit_info_report (cntr_id, dlq_id, crpt_type_cd, report_reason_cd)
    WHERE dlq_id IS NOT NULL AND crpt_status_cd IN ('SENT', 'ACKED');

-- ---- V4__credit_info_report_outbox.sql ----
-- 신용정보 신고 outbox.
-- submit() 시 신고 row 와 함께 적재되어 dispatch 배치가 외부 어댑터를 호출한다.

CREATE TABLE credit_info_report_outbox (
    outbox_id        BIGSERIAL     PRIMARY KEY,
    crpt_id          BIGINT        NOT NULL REFERENCES credit_info_report(crpt_id),
    status           VARCHAR(50)   NOT NULL,
    attempt_no       INT           NOT NULL DEFAULT 0,
    max_attempt      INT           NOT NULL DEFAULT 5,
    next_attempt_at  TIMESTAMPTZ   NOT NULL,
    last_error       VARCHAR(500),
    sent_at          TIMESTAMPTZ,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by       BIGINT        NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by       BIGINT        NOT NULL,
    deleted_at       TIMESTAMPTZ,
    deleted_by       BIGINT,
    version          INT           NOT NULL DEFAULT 0
);

-- dispatch 배치 핫패스: 처리 대상 row 후보 픽업.
CREATE INDEX idx_credit_info_report_outbox_dispatch
    ON credit_info_report_outbox (status, next_attempt_at)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_credit_info_report_outbox_crpt_id
    ON credit_info_report_outbox (crpt_id)
    WHERE deleted_at IS NULL;

-- ---- V5__credit_info_report_dlq_unique_widen.sql ----
-- 자동 발화 멱등 가드 확장.
-- submit 직후 상태가 SENT 였던 stub 동작이 outbox 도입(plan 02 step 4)으로 REQUESTED 로 바뀌었다.
-- 따라서 PENDING 단계의 중복 발화도 차단하도록 REQUESTED 를 UNIQUE 대상에 포함시킨다.
-- FAILED/DEAD 는 운영자/배치가 재시도 결정을 내리므로 제외 — 재발화 자유.

DROP INDEX IF EXISTS uk_credit_info_report_dlq_idem;

CREATE UNIQUE INDEX uk_credit_info_report_dlq_idem
    ON credit_info_report (cntr_id, dlq_id, crpt_type_cd, report_reason_cd)
    WHERE dlq_id IS NOT NULL AND crpt_status_cd IN ('REQUESTED', 'SENT', 'ACKED');

-- ---- V6__credit_info_report_external_ack_no.sql ----
-- ACK callback 으로 들어오는 외부 기관 추적 번호.
-- nullable — dispatch SENT 단계에서는 아직 ACK 안 옴.

ALTER TABLE credit_info_report
    ADD COLUMN external_ack_no VARCHAR(100);

-- ---- V7__notification_outbox.sql ----
-- 알림 outbox.
-- listener (신청/약정/실행/상환/심사) 가 이벤트별로 row 를 적재하면 dispatch 배치가 채널 어댑터로 송신한다.
-- 멱등 키 (event_type_cd + reference_id + channel_cd) 로 동일 이벤트 재발행 차단.

CREATE TABLE notification_outbox (
    outbox_id         BIGSERIAL     PRIMARY KEY,
    event_type_cd     VARCHAR(50)   NOT NULL,
    reference_id      BIGINT        NOT NULL,
    channel_cd        VARCHAR(50)   NOT NULL,
    payload           JSONB,
    status            VARCHAR(50)   NOT NULL,
    attempt_no        INT           NOT NULL DEFAULT 0,
    max_attempt       INT           NOT NULL DEFAULT 5,
    next_attempt_at   TIMESTAMPTZ   NOT NULL,
    last_error        VARCHAR(500),
    sent_at           TIMESTAMPTZ,
    idempotency_key   VARCHAR(200)  NOT NULL UNIQUE,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by        BIGINT        NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by        BIGINT        NOT NULL,
    deleted_at        TIMESTAMPTZ,
    deleted_by        BIGINT,
    version           INT           NOT NULL DEFAULT 0
);

-- dispatch 핫패스
CREATE INDEX idx_notification_outbox_dispatch
    ON notification_outbox (status, next_attempt_at)
    WHERE deleted_at IS NULL;

-- 운영자 조회
CREATE INDEX idx_notification_outbox_event_ref
    ON notification_outbox (event_type_cd, reference_id)
    WHERE deleted_at IS NULL;

-- ---- V8__repayment_schedule_holiday_adjusted.sql ----
-- 상환 스케줄 회차에 휴일 보정 여부 플래그 추가.
-- 신규 약정부터 생성 시 due_date 가 비영업일이면 다음 영업일로 이동(`following`), 그 경우 'Y' 로 기록.
-- 이미 생성된 회차는 'N' 유지 (마이그레이션 시 backfill 안 함 — 하드 갱신 금지).

ALTER TABLE repayment_schedule
    ADD COLUMN holiday_adjusted_yn CHAR(1) NOT NULL DEFAULT 'N';

-- ---- V9__business_calendar_seed_kr_2026_2035.sql ----
-- 한국 법정공휴일 시드 2026-2035
-- 근거: 관공서의 공휴일에 관한 규정 (대통령령)
--
-- 포함 항목:
--   고정 공휴일: 신정/삼일절/어린이날/현충일/광복절/개천절/한글날/크리스마스
--   음력 공휴일: 설날 3일·부처님오신날·추석 3일 (양력 환산)
--
-- 제외 항목 (운영자 추가 책임):
--   대체공휴일(代替公休日) — 매년 확정 일정은 운영자가 직접 등록
--   임시공휴일 — 동일
--
-- 2028년 10월 3일: 개천절 + 추석 당일이 같은 날 → 단일 row (개천절·추석)
-- cal_date UNIQUE 충돌 시 DO NOTHING (멱등, 재실행 안전)

INSERT INTO business_calendar
    (cal_date, business_day_yn, holiday_type_cd, holiday_name, base_country_cd, created_by, updated_by)
VALUES

-- ============================================================
-- 2026
-- ============================================================
-- 고정
('20260101','N','PUBLIC','신정',        'KR',0,0),
('20260301','N','PUBLIC','삼일절',      'KR',0,0),
('20260505','N','PUBLIC','어린이날',    'KR',0,0),
('20260606','N','PUBLIC','현충일',      'KR',0,0),
('20260815','N','PUBLIC','광복절',      'KR',0,0),
('20261003','N','PUBLIC','개천절',      'KR',0,0),
('20261009','N','PUBLIC','한글날',      'KR',0,0),
('20261225','N','PUBLIC','크리스마스',  'KR',0,0),
-- 설날 (양력 2026-02-17)
('20260216','N','LUNAR', '설날 전날',   'KR',0,0),
('20260217','N','LUNAR', '설날',        'KR',0,0),
('20260218','N','LUNAR', '설날 다음날', 'KR',0,0),
-- 부처님오신날 (양력 2026-05-24)
('20260524','N','LUNAR', '부처님오신날','KR',0,0),
-- 추석 (양력 2026-09-25)
('20260924','N','LUNAR', '추석 전날',   'KR',0,0),
('20260925','N','LUNAR', '추석',        'KR',0,0),
('20260926','N','LUNAR', '추석 다음날', 'KR',0,0),

-- ============================================================
-- 2027
-- ============================================================
('20270101','N','PUBLIC','신정',        'KR',0,0),
('20270301','N','PUBLIC','삼일절',      'KR',0,0),
('20270505','N','PUBLIC','어린이날',    'KR',0,0),
('20270606','N','PUBLIC','현충일',      'KR',0,0),
('20270815','N','PUBLIC','광복절',      'KR',0,0),
('20271003','N','PUBLIC','개천절',      'KR',0,0),
('20271009','N','PUBLIC','한글날',      'KR',0,0),
('20271225','N','PUBLIC','크리스마스',  'KR',0,0),
-- 설날 (양력 2027-02-06)
('20270205','N','LUNAR', '설날 전날',   'KR',0,0),
('20270206','N','LUNAR', '설날',        'KR',0,0),
('20270207','N','LUNAR', '설날 다음날', 'KR',0,0),
-- 부처님오신날 (양력 2027-05-13)
('20270513','N','LUNAR', '부처님오신날','KR',0,0),
-- 추석 (양력 2027-10-15)
('20271014','N','LUNAR', '추석 전날',   'KR',0,0),
('20271015','N','LUNAR', '추석',        'KR',0,0),
('20271016','N','LUNAR', '추석 다음날', 'KR',0,0),

-- ============================================================
-- 2028
-- ============================================================
('20280101','N','PUBLIC','신정',        'KR',0,0),
('20280301','N','PUBLIC','삼일절',      'KR',0,0),
('20280505','N','PUBLIC','어린이날',    'KR',0,0),
('20280606','N','PUBLIC','현충일',      'KR',0,0),
('20280815','N','PUBLIC','광복절',      'KR',0,0),
-- 2028-10-03: 개천절 + 추석 당일 겹침 → 단일 row
('20281003','N','PUBLIC','개천절·추석', 'KR',0,0),
('20281009','N','PUBLIC','한글날',      'KR',0,0),
('20281225','N','PUBLIC','크리스마스',  'KR',0,0),
-- 설날 (양력 2028-01-26)
('20280125','N','LUNAR', '설날 전날',   'KR',0,0),
('20280126','N','LUNAR', '설날',        'KR',0,0),
('20280127','N','LUNAR', '설날 다음날', 'KR',0,0),
-- 부처님오신날 (양력 2028-05-02)
('20280502','N','LUNAR', '부처님오신날','KR',0,0),
-- 추석 (양력 2028-10-03) 전날·다음날 (당일은 개천절·추석 행으로 기등록)
('20281002','N','LUNAR', '추석 전날',   'KR',0,0),
('20281004','N','LUNAR', '추석 다음날', 'KR',0,0),

-- ============================================================
-- 2029
-- ============================================================
('20290101','N','PUBLIC','신정',        'KR',0,0),
('20290301','N','PUBLIC','삼일절',      'KR',0,0),
('20290505','N','PUBLIC','어린이날',    'KR',0,0),
('20290606','N','PUBLIC','현충일',      'KR',0,0),
('20290815','N','PUBLIC','광복절',      'KR',0,0),
('20291003','N','PUBLIC','개천절',      'KR',0,0),
('20291009','N','PUBLIC','한글날',      'KR',0,0),
('20291225','N','PUBLIC','크리스마스',  'KR',0,0),
-- 설날 (양력 2029-02-13)
('20290212','N','LUNAR', '설날 전날',   'KR',0,0),
('20290213','N','LUNAR', '설날',        'KR',0,0),
('20290214','N','LUNAR', '설날 다음날', 'KR',0,0),
-- 부처님오신날 (양력 2029-05-20)
('20290520','N','LUNAR', '부처님오신날','KR',0,0),
-- 추석 (양력 2029-09-22)
('20290921','N','LUNAR', '추석 전날',   'KR',0,0),
('20290922','N','LUNAR', '추석',        'KR',0,0),
('20290923','N','LUNAR', '추석 다음날', 'KR',0,0),

-- ============================================================
-- 2030
-- ============================================================
('20300101','N','PUBLIC','신정',        'KR',0,0),
('20300301','N','PUBLIC','삼일절',      'KR',0,0),
('20300505','N','PUBLIC','어린이날',    'KR',0,0),
('20300606','N','PUBLIC','현충일',      'KR',0,0),
('20300815','N','PUBLIC','광복절',      'KR',0,0),
('20301003','N','PUBLIC','개천절',      'KR',0,0),
('20301009','N','PUBLIC','한글날',      'KR',0,0),
('20301225','N','PUBLIC','크리스마스',  'KR',0,0),
-- 설날 (양력 2030-02-03)
('20300202','N','LUNAR', '설날 전날',   'KR',0,0),
('20300203','N','LUNAR', '설날',        'KR',0,0),
('20300204','N','LUNAR', '설날 다음날', 'KR',0,0),
-- 부처님오신날 (양력 2030-05-09)
('20300509','N','LUNAR', '부처님오신날','KR',0,0),
-- 추석 (양력 2030-10-11)
('20301010','N','LUNAR', '추석 전날',   'KR',0,0),
('20301011','N','LUNAR', '추석',        'KR',0,0),
('20301012','N','LUNAR', '추석 다음날', 'KR',0,0),

-- ============================================================
-- 2031
-- ============================================================
('20310101','N','PUBLIC','신정',        'KR',0,0),
('20310301','N','PUBLIC','삼일절',      'KR',0,0),
('20310505','N','PUBLIC','어린이날',    'KR',0,0),
('20310606','N','PUBLIC','현충일',      'KR',0,0),
('20310815','N','PUBLIC','광복절',      'KR',0,0),
('20311003','N','PUBLIC','개천절',      'KR',0,0),
('20311009','N','PUBLIC','한글날',      'KR',0,0),
('20311225','N','PUBLIC','크리스마스',  'KR',0,0),
-- 설날 (양력 2031-01-23)
('20310122','N','LUNAR', '설날 전날',   'KR',0,0),
('20310123','N','LUNAR', '설날',        'KR',0,0),
('20310124','N','LUNAR', '설날 다음날', 'KR',0,0),
-- 부처님오신날 (양력 2031-05-29)
('20310529','N','LUNAR', '부처님오신날','KR',0,0),
-- 추석 (양력 2031-10-01)
('20310930','N','LUNAR', '추석 전날',   'KR',0,0),
('20311001','N','LUNAR', '추석',        'KR',0,0),
('20311002','N','LUNAR', '추석 다음날', 'KR',0,0),

-- ============================================================
-- 2032
-- ============================================================
('20320101','N','PUBLIC','신정',        'KR',0,0),
('20320301','N','PUBLIC','삼일절',      'KR',0,0),
('20320505','N','PUBLIC','어린이날',    'KR',0,0),
('20320606','N','PUBLIC','현충일',      'KR',0,0),
('20320815','N','PUBLIC','광복절',      'KR',0,0),
('20321003','N','PUBLIC','개천절',      'KR',0,0),
('20321009','N','PUBLIC','한글날',      'KR',0,0),
('20321225','N','PUBLIC','크리스마스',  'KR',0,0),
-- 설날 (양력 2032-02-11)
('20320210','N','LUNAR', '설날 전날',   'KR',0,0),
('20320211','N','LUNAR', '설날',        'KR',0,0),
('20320212','N','LUNAR', '설날 다음날', 'KR',0,0),
-- 부처님오신날 (양력 2032-05-17)
('20320517','N','LUNAR', '부처님오신날','KR',0,0),
-- 추석 (양력 2032-09-19)
('20320918','N','LUNAR', '추석 전날',   'KR',0,0),
('20320919','N','LUNAR', '추석',        'KR',0,0),
('20320920','N','LUNAR', '추석 다음날', 'KR',0,0),

-- ============================================================
-- 2033
-- ============================================================
('20330101','N','PUBLIC','신정',        'KR',0,0),
('20330301','N','PUBLIC','삼일절',      'KR',0,0),
('20330505','N','PUBLIC','어린이날',    'KR',0,0),
('20330606','N','PUBLIC','현충일',      'KR',0,0),
('20330815','N','PUBLIC','광복절',      'KR',0,0),
('20331003','N','PUBLIC','개천절',      'KR',0,0),
('20331009','N','PUBLIC','한글날',      'KR',0,0),
('20331225','N','PUBLIC','크리스마스',  'KR',0,0),
-- 설날 (양력 2033-01-31)
('20330130','N','LUNAR', '설날 전날',   'KR',0,0),
('20330131','N','LUNAR', '설날',        'KR',0,0),
('20330201','N','LUNAR', '설날 다음날', 'KR',0,0),
-- 부처님오신날 (양력 2033-05-06)
('20330506','N','LUNAR', '부처님오신날','KR',0,0),
-- 추석 (양력 2033-09-08)
('20330907','N','LUNAR', '추석 전날',   'KR',0,0),
('20330908','N','LUNAR', '추석',        'KR',0,0),
('20330909','N','LUNAR', '추석 다음날', 'KR',0,0),

-- ============================================================
-- 2034
-- ============================================================
('20340101','N','PUBLIC','신정',        'KR',0,0),
('20340301','N','PUBLIC','삼일절',      'KR',0,0),
('20340505','N','PUBLIC','어린이날',    'KR',0,0),
('20340606','N','PUBLIC','현충일',      'KR',0,0),
('20340815','N','PUBLIC','광복절',      'KR',0,0),
('20341003','N','PUBLIC','개천절',      'KR',0,0),
('20341009','N','PUBLIC','한글날',      'KR',0,0),
('20341225','N','PUBLIC','크리스마스',  'KR',0,0),
-- 설날 (양력 2034-02-19)
('20340218','N','LUNAR', '설날 전날',   'KR',0,0),
('20340219','N','LUNAR', '설날',        'KR',0,0),
('20340220','N','LUNAR', '설날 다음날', 'KR',0,0),
-- 부처님오신날 (양력 2034-05-26)
('20340526','N','LUNAR', '부처님오신날','KR',0,0),
-- 추석 (양력 2034-09-27)
('20340926','N','LUNAR', '추석 전날',   'KR',0,0),
('20340927','N','LUNAR', '추석',        'KR',0,0),
('20340928','N','LUNAR', '추석 다음날', 'KR',0,0),

-- ============================================================
-- 2035
-- ============================================================
('20350101','N','PUBLIC','신정',        'KR',0,0),
('20350301','N','PUBLIC','삼일절',      'KR',0,0),
('20350505','N','PUBLIC','어린이날',    'KR',0,0),
('20350606','N','PUBLIC','현충일',      'KR',0,0),
('20350815','N','PUBLIC','광복절',      'KR',0,0),
('20351003','N','PUBLIC','개천절',      'KR',0,0),
('20351009','N','PUBLIC','한글날',      'KR',0,0),
('20351225','N','PUBLIC','크리스마스',  'KR',0,0),
-- 설날 (양력 2035-02-08)
('20350207','N','LUNAR', '설날 전날',   'KR',0,0),
('20350208','N','LUNAR', '설날',        'KR',0,0),
('20350209','N','LUNAR', '설날 다음날', 'KR',0,0),
-- 부처님오신날 (양력 2035-05-15)
('20350515','N','LUNAR', '부처님오신날','KR',0,0),
-- 추석 (양력 2035-09-17)
('20350916','N','LUNAR', '추석 전날',   'KR',0,0),
('20350917','N','LUNAR', '추석',        'KR',0,0),
('20350918','N','LUNAR', '추석 다음날', 'KR',0,0)

ON CONFLICT (cal_date) DO NOTHING;

-- ---- V10__loan_product_min_guarantor_count.sql ----
-- LoanProduct 보증인 최소 수 정책 필드 추가.
-- guarantor_required_yn='Y' 인 경우 min_guarantor_count >= 1 이 강제됨 (서비스 레이어).
-- 기존 데이터는 0 으로 backfill — 보증 불필요(구 기본값)와 동일 의미.

ALTER TABLE loan_product
    ADD COLUMN min_guarantor_count INT NOT NULL DEFAULT 0;

-- ---- V11__loan_product_application_validity_days.sql ----
-- Plan 09: 신청 승인 유효기간 상품별 차등
-- NULL = 시스템 기본 14일, 1~90 사이 값 허용
ALTER TABLE loan_product
    ADD COLUMN application_validity_days INT;

-- ---- V12__loan_closure_writeoff_subrogation.sql ----
-- Plan 08: WRITE_OFF / SUBROGATION 종결 실로직 — 정산 외 사고종결 메타 컬럼
ALTER TABLE loan_closure
    ADD COLUMN write_off_amount        BIGINT,
    ADD COLUMN subrogation_amount      BIGINT,
    ADD COLUMN subrogation_party_ref   VARCHAR(200),
    ADD COLUMN write_off_reason_cd     VARCHAR(50);

-- ---- V13__spring_batch_schema.sql ----
-- Spring Batch 메타데이터 테이블 (schema-postgresql.sql 기반)

CREATE SEQUENCE IF NOT EXISTS BATCH_STEP_EXECUTION_SEQ MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS BATCH_JOB_EXECUTION_SEQ MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS BATCH_JOB_SEQ MAXVALUE 9223372036854775807 NO CYCLE;

CREATE TABLE IF NOT EXISTS BATCH_JOB_INSTANCE (
    JOB_INSTANCE_ID BIGINT NOT NULL PRIMARY KEY,
    VERSION         BIGINT,
    JOB_NAME        VARCHAR(100) NOT NULL,
    JOB_KEY         VARCHAR(32)  NOT NULL,
    CONSTRAINT JOB_INST_UN UNIQUE (JOB_NAME, JOB_KEY)
);

CREATE TABLE IF NOT EXISTS BATCH_JOB_EXECUTION (
    JOB_EXECUTION_ID BIGINT  NOT NULL PRIMARY KEY,
    VERSION          BIGINT,
    JOB_INSTANCE_ID  BIGINT  NOT NULL,
    CREATE_TIME      TIMESTAMP NOT NULL,
    START_TIME       TIMESTAMP DEFAULT NULL,
    END_TIME         TIMESTAMP DEFAULT NULL,
    STATUS           VARCHAR(10),
    EXIT_CODE        VARCHAR(2500),
    EXIT_MESSAGE     VARCHAR(2500),
    LAST_UPDATED     TIMESTAMP,
    CONSTRAINT JOB_INST_EXEC_FK FOREIGN KEY (JOB_INSTANCE_ID) REFERENCES BATCH_JOB_INSTANCE(JOB_INSTANCE_ID)
);

CREATE TABLE IF NOT EXISTS BATCH_JOB_EXECUTION_PARAMS (
    JOB_EXECUTION_ID BIGINT       NOT NULL,
    PARAMETER_NAME   VARCHAR(100) NOT NULL,
    PARAMETER_TYPE   VARCHAR(100) NOT NULL,
    PARAMETER_VALUE  VARCHAR(2500),
    IDENTIFYING      CHAR(1)      NOT NULL,
    CONSTRAINT JOB_EXEC_PARAMS_FK FOREIGN KEY (JOB_EXECUTION_ID) REFERENCES BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
);

CREATE TABLE IF NOT EXISTS BATCH_STEP_EXECUTION (
    STEP_EXECUTION_ID  BIGINT       NOT NULL PRIMARY KEY,
    VERSION            BIGINT       NOT NULL,
    STEP_NAME          VARCHAR(100) NOT NULL,
    JOB_EXECUTION_ID   BIGINT       NOT NULL,
    CREATE_TIME        TIMESTAMP    NOT NULL,
    START_TIME         TIMESTAMP    DEFAULT NULL,
    END_TIME           TIMESTAMP    DEFAULT NULL,
    STATUS             VARCHAR(10),
    COMMIT_COUNT       BIGINT,
    READ_COUNT         BIGINT,
    FILTER_COUNT       BIGINT,
    WRITE_COUNT        BIGINT,
    READ_SKIP_COUNT    BIGINT,
    WRITE_SKIP_COUNT   BIGINT,
    PROCESS_SKIP_COUNT BIGINT,
    ROLLBACK_COUNT     BIGINT,
    EXIT_CODE          VARCHAR(2500),
    EXIT_MESSAGE       VARCHAR(2500),
    LAST_UPDATED       TIMESTAMP,
    CONSTRAINT JOB_EXEC_STEP_FK FOREIGN KEY (JOB_EXECUTION_ID) REFERENCES BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
);

CREATE TABLE IF NOT EXISTS BATCH_STEP_EXECUTION_CONTEXT (
    STEP_EXECUTION_ID  BIGINT        NOT NULL PRIMARY KEY,
    SHORT_CONTEXT      VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT TEXT,
    CONSTRAINT STEP_EXEC_CTX_FK FOREIGN KEY (STEP_EXECUTION_ID) REFERENCES BATCH_STEP_EXECUTION(STEP_EXECUTION_ID)
);

CREATE TABLE IF NOT EXISTS BATCH_JOB_EXECUTION_CONTEXT (
    JOB_EXECUTION_ID   BIGINT        NOT NULL PRIMARY KEY,
    SHORT_CONTEXT      VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT TEXT,
    CONSTRAINT JOB_EXEC_CTX_FK FOREIGN KEY (JOB_EXECUTION_ID) REFERENCES BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
);

-- ---- V14__overdue_accrual.sql ----
-- 연체 이자 일별 발생 (append-only)
-- UNIQUE (cntr_id, accrual_date) 로 동일 baseDate 재실행 시 멱등 보장

CREATE TABLE overdue_accrual (
    oa_id                      BIGSERIAL     PRIMARY KEY,
    cntr_id                    BIGINT        NOT NULL REFERENCES loan_contract(cntr_id),
    dlq_id                     BIGINT        NOT NULL REFERENCES delinquency(dlq_id),
    accrual_date               VARCHAR(8)    NOT NULL,
    overdue_principal          BIGINT        NOT NULL,
    overdue_rate_bps           INT           NOT NULL,
    dlq_days                   INT           NOT NULL,
    daily_overdue_interest     BIGINT        NOT NULL,
    cumulative_overdue_interest BIGINT       NOT NULL,
    oa_status_cd               VARCHAR(50)   NOT NULL,
    accrued_at                 TIMESTAMPTZ   NOT NULL,
    created_at                 TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by                 BIGINT        NOT NULL
);

CREATE UNIQUE INDEX uk_overdue_accrual_cntr_date
    ON overdue_accrual (cntr_id, accrual_date);

CREATE INDEX idx_overdue_accrual_dlq
    ON overdue_accrual (dlq_id, accrual_date);

-- ---- V15__daily_accounting_summary.sql ----
-- 일일 회계 요약 (EOD 산출)
-- 본격 복식부기 전표는 본 단계 범위 외 — 일별 합계만 적재한다.
-- UNIQUE(summary_date) 로 동일 baseDate 재실행 시 멱등.

CREATE TABLE daily_accounting_summary (
    das_id                       BIGSERIAL     PRIMARY KEY,
    summary_date                 VARCHAR(8)    NOT NULL UNIQUE,
    interest_revenue             BIGINT        NOT NULL DEFAULT 0,
    overdue_interest_revenue     BIGINT        NOT NULL DEFAULT 0,
    auto_debit_principal         BIGINT        NOT NULL DEFAULT 0,
    auto_debit_interest          BIGINT        NOT NULL DEFAULT 0,
    auto_debit_overdue_interest  BIGINT        NOT NULL DEFAULT 0,
    auto_debit_count             INT           NOT NULL DEFAULT 0,
    disbursed_amount             BIGINT        NOT NULL DEFAULT 0,
    disbursed_count              INT           NOT NULL DEFAULT 0,
    active_contract_count        INT           NOT NULL DEFAULT 0,
    active_delinquency_count     INT           NOT NULL DEFAULT 0,
    summarized_at                TIMESTAMPTZ   NOT NULL,
    created_at                   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by                   BIGINT        NOT NULL
);

-- ---- V16__monthly_accounting_summary.sql ----
-- 월별 회계 요약 (EOM 산출)
-- summary_month = YYYYMM. UNIQUE 로 동일 baseMonth 재실행 시 멱등.

CREATE TABLE monthly_accounting_summary (
    mas_id                         BIGSERIAL     PRIMARY KEY,
    summary_month                  VARCHAR(6)    NOT NULL UNIQUE,
    base_month_start_date          VARCHAR(8)    NOT NULL,
    base_month_end_date            VARCHAR(8)    NOT NULL,

    -- 매출 (트랜잭션 합계)
    interest_revenue               BIGINT        NOT NULL DEFAULT 0,
    overdue_interest_revenue       BIGINT        NOT NULL DEFAULT 0,
    auto_debit_principal           BIGINT        NOT NULL DEFAULT 0,
    auto_debit_interest            BIGINT        NOT NULL DEFAULT 0,
    auto_debit_overdue_interest    BIGINT        NOT NULL DEFAULT 0,
    auto_debit_count               INT           NOT NULL DEFAULT 0,

    -- 신규 실행
    new_disbursed_amount           BIGINT        NOT NULL DEFAULT 0,
    new_disbursed_count            INT           NOT NULL DEFAULT 0,

    -- 월말 시점 잔액·연체 통계
    month_end_active_contracts     INT           NOT NULL DEFAULT 0,
    month_end_active_delinquencies INT           NOT NULL DEFAULT 0,
    month_end_npl_count            INT           NOT NULL DEFAULT 0,
    month_end_npl_principal        BIGINT        NOT NULL DEFAULT 0,

    summarized_at                  TIMESTAMPTZ   NOT NULL,
    created_at                     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by                     BIGINT        NOT NULL
);

-- ---- V17__loan_ecl_summary.sql ----
-- IFRS9 ECL (Expected Credit Loss) 월별 산출 결과.
-- UNIQUE(cntr_id, summary_month) 로 동일 baseMonth 재실행 시 자연 멱등.
--
-- 본 단계 LGD 는 50% 고정 (담보·보증보험 차등은 후속).
-- PD 는 연체 stage 기반 단순 매핑 (외부 통계 모델 연동은 후속).

CREATE TABLE loan_ecl_summary (
    ecl_id          BIGSERIAL     PRIMARY KEY,
    cntr_id         BIGINT        NOT NULL REFERENCES loan_contract(cntr_id),
    summary_month   VARCHAR(6)    NOT NULL,
    ifrs_stage_cd   VARCHAR(50)   NOT NULL,      -- STAGE_1 / STAGE_2 / STAGE_3
    pd_bps          INT           NOT NULL,       -- bps (10000 bps = 100%)
    lgd_bps         INT           NOT NULL,
    ead             BIGINT        NOT NULL,
    ecl             BIGINT        NOT NULL,
    engine_version  VARCHAR(50)   NOT NULL,
    calculated_at   TIMESTAMPTZ   NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by      BIGINT        NOT NULL
);

CREATE UNIQUE INDEX uk_loan_ecl_summary_cntr_month
    ON loan_ecl_summary (cntr_id, summary_month);

CREATE INDEX idx_loan_ecl_summary_month_stage
    ON loan_ecl_summary (summary_month, ifrs_stage_cd);

-- ---- V18__ai_review_advice.sql ----
-- AI 심사 조언 테이블.
-- 편향 검증 에이전트(BIAS_CHECK) 및 향후 LLM 보조 기능(SUMMARY, REJECTION_LETTER 등)의
-- 결과를 append-only 로 저장한다. 결정권은 사람에게 있으며 이 테이블은 보조 기록만 한다.
-- severity_cd: BLOCKED(명백한 규정위반) / HIGH / MEDIUM / LOW / NONE / null(비편향 advice 유형)

CREATE TABLE ai_review_advice (
    advice_id       BIGSERIAL     PRIMARY KEY,
    rev_id          BIGINT        NOT NULL REFERENCES loan_review(rev_id),
    advice_type_cd  VARCHAR(40)   NOT NULL,
    severity_cd     VARCHAR(20),
    advice_body     TEXT          NOT NULL,
    model           VARCHAR(80),
    model_version   VARCHAR(40),
    prompt_hash     CHAR(64),
    input_token     INT,
    output_token    INT,
    latency_ms      INT,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by      BIGINT
);

CREATE INDEX ix_ai_review_advice_rev_type_created
    ON ai_review_advice (rev_id, advice_type_cd, created_at DESC);

-- ---- V19__loan_review_approver_bias_columns.sql ----
-- 본심사 승인자 단계 + 편향 검증 상태 컬럼 추가.
-- 상태값: BIAS_REVIEWING (편향 검증 진행/대기), PENDING_APPROVER (승인자 대기)
-- bias_override_*: 상급자가 BLOCKED severity 를 우회 승인한 경우 기록.

ALTER TABLE loan_review
    ADD COLUMN approver_id          BIGINT,
    ADD COLUMN approved_decision_cd VARCHAR(50),
    ADD COLUMN override_reason_cd   VARCHAR(50),
    ADD COLUMN override_remark      VARCHAR(500),
    ADD COLUMN bias_severity_cd     VARCHAR(20),
    ADD COLUMN bias_override_by     BIGINT,
    ADD COLUMN bias_override_reason VARCHAR(500),
    ADD COLUMN bias_overridden_at   TIMESTAMPTZ;

CREATE INDEX ix_loan_review_status_bias
    ON loan_review (rev_status_cd)
    WHERE rev_status_cd IN ('BIAS_REVIEWING', 'PENDING_APPROVER');

-- ---- V20__loan_review_pending_approver_since.sql ----
-- 승인자 대기 진입 시각 기록 컬럼 추가.
-- PENDING_APPROVER 타임아웃 배치(expire-pending-approver)의 cutoff 기준으로 사용.
-- 기존 PENDING_APPROVER 건은 NULL 허용 — 배치는 NULL 인 경우 updated_at 을 fallback 으로 처리.
ALTER TABLE loan_review
    ADD COLUMN IF NOT EXISTS pending_approver_since TIMESTAMPTZ;

-- ---- V21__doc_agent_submission_table.sql ----
-- doc-agent 연동 마이그레이션
-- 1. loan_document_ocr 제거 — OCR은 doc-agent L3 파이프라인이 담당, loan-service 불필요
-- 2. loan_document_submission 추가 — doc-agent API 호출 이력 관리

DROP TABLE IF EXISTS loan_document_ocr;

-- loan_document_submission CREATE TABLE은 위 V1 섹션에 최종 상태로 정의됨
CREATE INDEX idx_lds_doc_id        ON loan_document_submission (doc_id);
CREATE INDEX idx_lds_application_id ON loan_document_submission (application_id);

-- ---- V22__loan_review_ai_track.sql ----
ALTER TABLE loan_review
    ADD COLUMN rev_ai_track_cd  VARCHAR(20)   NULL,
    ADD COLUMN rev_ai_pd        DECIMAL(10,6) NULL,
    ADD COLUMN rev_ai_rationale TEXT          NULL;

COMMENT ON COLUMN loan_review.rev_ai_track_cd   IS 'AI 트랙 분기 결과 (TRACK_1/2/3)';
COMMENT ON COLUMN loan_review.rev_ai_pd         IS 'AI PD 스코어 (0~1)';
COMMENT ON COLUMN loan_review.rev_ai_rationale  IS 'AI 결정 근거 한 줄 요약';

-- ---- V23__prescreening_ai_track.sql ----
ALTER TABLE loan_prescreening
    ADD COLUMN ai_track_cd VARCHAR(20) NULL;

COMMENT ON COLUMN loan_prescreening.ai_track_cd IS 'AI 트랙 분기 결과 (TRACK_1/2/3) — PASS 건만 저장';

-- ---- V24__repayment_transaction_pi_id.sql ----
-- repayment_transaction 에 payment-service 결제지시 ID(pi_id) 컬럼 추가.
-- loan-payment-integration-spec §3 "paymentInstructionId 저장 권장" 반영.
-- CLEARING 콜백 수신 시 pi_id 로 거래를 조회하므로 인덱스도 함께 생성.
ALTER TABLE repayment_transaction
    ADD COLUMN pi_id VARCHAR(100);

CREATE INDEX idx_repayment_transaction_pi_id
    ON repayment_transaction (pi_id)
    WHERE pi_id IS NOT NULL;

-- ---- V25__loan_execution_pi_id.sql ----
-- loan_execution 에 payment-service 결제지시 ID(pi_id) 컬럼 추가.
-- 대출실행 출금 요청 결과를 추적하고 FAILED/CLEARING 재처리에 활용.
ALTER TABLE loan_execution
    ADD COLUMN pi_id VARCHAR(100);

CREATE INDEX idx_loan_execution_pi_id
    ON loan_execution (pi_id)
    WHERE pi_id IS NOT NULL;

-- ---- V26__repayment_account_holder_name_enc.sql ----
-- repayment_account 에 예금주명 암호화 컬럼 추가.
-- 역분개 환급 시 payment-service receiverHolderName 검증에 사용.
ALTER TABLE repayment_account
    ADD COLUMN holder_name_enc BYTEA;

-- ---- V27__auto_debit_clearing_pending.sql ----
-- 자동이체 타행 청산(CLEARING) 대기 매핑.
-- payment.* Kafka 이벤트에는 piId 만 실려오므로(idempotencyKey 없음),
-- CLEARING 응답 시점에 piId ↔ 회차 정보를 미리 저장해 두고
-- 완결/실패 이벤트 수신 시 piId 로 조회해 상환을 완결한다.
CREATE TABLE auto_debit_clearing_pending (
    pending_id       BIGSERIAL    PRIMARY KEY,
    pi_id            VARCHAR(100) NOT NULL UNIQUE,
    cntr_id          BIGINT       NOT NULL,
    rsch_id          BIGINT       NOT NULL,
    installment_no   INT          NOT NULL,
    base_date        CHAR(8)      NOT NULL,
    idempotency_key  VARCHAR(100) NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    resolved_at      TIMESTAMPTZ,
    CONSTRAINT chk_adcp_status CHECK (status IN ('PENDING', 'DONE', 'FAILED'))
);

-- 미해소 대기건 조회 핫패스
CREATE INDEX idx_adcp_pending
    ON auto_debit_clearing_pending (status)
    WHERE status = 'PENDING';

-- ---- V28__branch_table.sql ----
-- 지점 마스터. MVP는 시드 3개로 시작.
CREATE TABLE branch (
    branch_id   VARCHAR(10)  NOT NULL,
    branch_name VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_branch PRIMARY KEY (branch_id)
);

INSERT INTO branch (branch_id, branch_name) VALUES
    ('0001', '강남지점'),
    ('0002', '종로지점'),
    ('HQ',   '본사');

-- ---- V29__loan_application_branch_id.sql ----
-- 대출 신청 접수 지점. 기존 행은 NULL 허용, 신규 신청부터 채움.
ALTER TABLE loan_application
    ADD COLUMN branch_id VARCHAR(10)
        REFERENCES branch(branch_id);

-- ---- V30__loan_review_outbox.sql ----
-- 유사 케이스 적재용 LOAN_REVIEW outbox (Phase E E3-3).
-- 결정 완료된 심사 건의 PII-free 케이스 청크를 동일 트랜잭션으로 적재하면,
-- polling worker 가 Kafka topic(loan-review.case-indexed.v1)으로 발행 후 status=SENT 로 전이한다.
-- 멱등 키(event_type_cd + aggregate_id) 로 동일 케이스 중복 발행을 차단.

CREATE TABLE loan_review_outbox (
    outbox_id         BIGSERIAL     PRIMARY KEY,
    aggregate_id      BIGINT        NOT NULL,           -- LOAN_REVIEW rev_id
    event_type_cd     VARCHAR(50)   NOT NULL,           -- 예: CASE_INDEXED
    payload           JSONB         NOT NULL,           -- PII 마스킹된 케이스 청크 페이로드
    status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING',  -- PENDING / SENT / FAILED
    attempt_no        INT           NOT NULL DEFAULT 0,
    max_attempt       INT           NOT NULL DEFAULT 5,
    next_attempt_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    last_error        VARCHAR(500),
    sent_at           TIMESTAMPTZ,
    idempotency_key   VARCHAR(200)  NOT NULL UNIQUE,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- polling worker 핫패스 (발행 대기 건 조회)
CREATE INDEX idx_loan_review_outbox_dispatch
    ON loan_review_outbox (status, next_attempt_at);

-- 운영자 조회 (특정 심사 건 추적)
CREATE INDEX idx_loan_review_outbox_aggregate
    ON loan_review_outbox (aggregate_id);

-- ---- V31__access_audit_log.sql ----
-- 조회·열람·break-glass 접근 이벤트 전용 감사 로그. append-only.
-- 상태 전이 이력(status_history)과 분리: 접근 행위 자체를 기록한다.
CREATE TABLE access_audit_log (
    log_id             BIGINT GENERATED ALWAYS AS IDENTITY,
    actor_id           BIGINT         NOT NULL,
    target_type        VARCHAR(50)    NOT NULL,
    target_id          BIGINT         NOT NULL,
    action_cd          VARCHAR(30)    NOT NULL,
    branch_id          VARCHAR(10),
    break_glass_reason TEXT,
    logged_at          TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_access_audit_log PRIMARY KEY (log_id),
    CONSTRAINT chk_aal_action_cd CHECK (action_cd IN ('VIEW', 'UNMASK', 'BREAK_GLASS')),
    CONSTRAINT chk_aal_target_type CHECK (target_type IN ('LOAN_APPLICATION', 'LOAN_REVIEW', 'DOCUMENT'))
);

CREATE INDEX idx_aal_actor    ON access_audit_log (actor_id, logged_at DESC);
CREATE INDEX idx_aal_target   ON access_audit_log (target_type, target_id, logged_at DESC);

-- ---- V32__common_sync_outbox.sql ----
-- 공통 계층(common_db) write-through outbox.
-- loan_db ↔ common_db 는 별도 datasource·XA 미구성이므로, 도메인 트랜잭션에서는 본 outbox row 만
-- 적재하고(loan_db), CommonSyncDispatchService 가 비동기로 common_db upsert + loan 브리지 컬럼 백필을 수행한다.
--
-- 흐름: 도메인 tx 가 PENDING 적재 → 디스패처가 common_db upsert(자연키 source_no 멱등)
--        → 생성된 common PK 를 common_id 에 기록 + loan 측 브리지 컬럼(product_id/contract_id/transaction_id) 백필 → DONE.
--
-- target_type_cd: PRODUCT(common_product) / CONTRACT(common_contract) / TRANSACTION(common_transaction)
-- source_id     : loan_db 원본 PK (prod_id / cntr_id / exec_id|rtx_id)
-- source_no     : common 자연키 (product_cd / contract_no / transaction_no) — upsert dedupe 키
-- common_id     : upsert 성공 후 common_db 가 채번한 PK (백필 대상 값)
-- 멱등 키 (target_type_cd + source_id) 로 동일 원본 중복 적재 차단.

CREATE TABLE common_sync_outbox (
    outbox_id         BIGSERIAL     PRIMARY KEY,
    target_type_cd    VARCHAR(20)   NOT NULL,
    source_id         BIGINT        NOT NULL,
    source_no         VARCHAR(50),
    payload           JSONB,
    common_id         BIGINT,
    status            VARCHAR(50)   NOT NULL,
    attempt_no        INT           NOT NULL DEFAULT 0,
    max_attempt       INT           NOT NULL DEFAULT 5,
    next_attempt_at   TIMESTAMPTZ   NOT NULL,
    last_error        VARCHAR(500),
    synced_at         TIMESTAMPTZ,
    idempotency_key   VARCHAR(100)  NOT NULL UNIQUE,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by        BIGINT        NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by        BIGINT        NOT NULL,
    deleted_at        TIMESTAMPTZ,
    deleted_by        BIGINT,
    version           INT           NOT NULL DEFAULT 0,
    CONSTRAINT chk_common_sync_outbox_target
        CHECK (target_type_cd IN ('PRODUCT', 'CONTRACT', 'TRANSACTION'))
);

-- dispatch 핫패스: 처리 대상 후보 픽업
CREATE INDEX idx_common_sync_outbox_dispatch
    ON common_sync_outbox (status, next_attempt_at)
    WHERE deleted_at IS NULL;

-- 운영자 조회: 원본 추적
CREATE INDEX idx_common_sync_outbox_source
    ON common_sync_outbox (target_type_cd, source_id)
    WHERE deleted_at IS NULL;

-- ---- V33__repayment_transaction_active_reversal_unique.sql ----
-- 원 거래(rtx)당 활성 역분개(reversal)는 최대 1건만 허용한다.
-- 동시/중복 reverse() 요청이 existsActiveReversal SELECT 가드를 동시에 통과해
-- 이중 환급되는 것을 DB 레벨에서 차단한다(애플리케이션 단 가드는 빠른 사전 검사일 뿐).
CREATE UNIQUE INDEX IF NOT EXISTS ux_rtx_active_reversal_target
    ON repayment_transaction (reversal_target_rtx_id)
    WHERE reversal_yn = 'Y'
      AND rtx_status_cd = 'SUCCESS'
      AND deleted_at IS NULL;

-- ---- V34__loan_review_owner_escalated.sql ----
-- 담당자(접수 텔러) ID.
ALTER TABLE loan_review
    ADD COLUMN IF NOT EXISTS owner_id BIGINT;

-- 본사 상신 타임스탬프. NULL = 정상 건, NOT NULL = 이상거래 상신 건.
ALTER TABLE loan_review
    ADD COLUMN IF NOT EXISTS escalated_at TIMESTAMPTZ(3);

-- 본사 상신 건 조회용 인덱스.
CREATE INDEX IF NOT EXISTS idx_loan_review_escalated ON loan_review (escalated_at)
    WHERE escalated_at IS NOT NULL;

-- ---- V35__seed_admin_demo_data.sql ----
-- ============================================================
-- V35: Admin 화면 데모 시드 데이터
--
-- 목적: 관리자 화면(계약 모니터링, 어드바이저리, RAG 문서 등)에서
--       비어있지 않은 화면을 확인할 수 있는 최소한의 샘플 데이터 삽입.
-- 멱등: 모든 INSERT 는 ON CONFLICT ... DO NOTHING 또는 WHERE NOT EXISTS 사용.
-- 참조: customer_id=1001~1003 은 customer-service 에 등록된 가상 고객.
--       created_by=0 은 시스템(migration) 행위자.
-- ============================================================

-- ------------------------------------------------------------
-- 1. 대출 상품 (loan_product)
-- ------------------------------------------------------------
INSERT INTO loan_product (
    prod_id, prod_cd, prod_name, loan_type_cd, target_customer_cd,
    repayment_method_cd, rate_type_cd,
    base_rate_bps, min_rate_bps, max_rate_bps,
    min_amount, max_amount, min_period_mo, max_period_mo,
    collateral_required_yn, guarantor_required_yn,
    sale_start_date, prod_status_cd,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9001, 'DEMO_PERSONAL', '데모 개인신용대출', 'PERSONAL', 'INDIVIDUAL',
 'EQUAL_PRINCIPAL_INTEREST', 'FIXED',
 450, 350, 1500,
 1000000, 50000000, 6, 60,
 'N', 'N',
 '20250101', 'ACTIVE',
 '2025-01-01 00:00:00+09', 0, '2025-01-01 00:00:00+09', 0, 0),
(9002, 'DEMO_MORTGAGE', '데모 주택담보대출', 'MORTGAGE', 'INDIVIDUAL',
 'EQUAL_PRINCIPAL_INTEREST', 'VARIABLE',
 350, 280, 900,
 10000000, 500000000, 12, 360,
 'Y', 'N',
 '20250101', 'ACTIVE',
 '2025-01-01 00:00:00+09', 0, '2025-01-01 00:00:00+09', 0, 0)
ON CONFLICT (prod_id) DO NOTHING;

-- ------------------------------------------------------------
-- 2. 대출 신청 (loan_application)
--    9001/9002: 계약 완료(CONTRACTED)
--    9003    : 심사 중(REVIEWING) — 어드바이저리 대상
--    9004    : 거절(REJECTED)
-- ------------------------------------------------------------
INSERT INTO loan_application (
    appl_id, appl_no, customer_id, prod_id, channel_cd,
    requested_amount, requested_period_mo, loan_purpose_cd,
    repayment_method_cd, estimated_income_amt, employment_type_cd,
    appl_status_cd, applied_at, idempotency_key,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9001, 'DEMO-2025-001', 1001, 9001, 'INTERNET',
 20000000, 36, 'LIVING_EXPENSE',
 'EQUAL_PRINCIPAL_INTEREST', 48000000, 'EMPLOYED',
 'CONTRACTED', '2025-01-10 10:00:00+09', 'DEMO-IDEM-2025-001',
 '2025-01-10 10:00:00+09', 0, '2025-01-20 14:00:00+09', 0, 0),
(9002, 'DEMO-2025-002', 1001, 9002, 'BRANCH',
 100000000, 120, 'HOUSE_PURCHASE',
 'EQUAL_PRINCIPAL_INTEREST', 80000000, 'EMPLOYED',
 'CONTRACTED', '2025-02-05 09:30:00+09', 'DEMO-IDEM-2025-002',
 '2025-02-05 09:30:00+09', 0, '2025-02-15 15:00:00+09', 0, 0),
(9003, 'DEMO-2025-003', 1002, 9001, 'INTERNET',
 15000000, 24, 'LIVING_EXPENSE',
 'EQUAL_PRINCIPAL_INTEREST', 36000000, 'EMPLOYED',
 'REVIEWING', '2025-03-01 11:00:00+09', 'DEMO-IDEM-2025-003',
 '2025-03-01 11:00:00+09', 0, '2025-03-05 09:00:00+09', 0, 0),
(9004, 'DEMO-2025-004', 1003, 9001, 'MOBILE',
 10000000, 12, 'LIVING_EXPENSE',
 'EQUAL_PRINCIPAL_INTEREST', 30000000, 'SELF_EMPLOYED',
 'REJECTED', '2025-03-10 14:00:00+09', 'DEMO-IDEM-2025-004',
 '2025-03-10 14:00:00+09', 0, '2025-03-12 10:00:00+09', 0, 0)
ON CONFLICT (appl_id) DO NOTHING;

-- ------------------------------------------------------------
-- 3. 신용평가 (credit_evaluation)
-- ------------------------------------------------------------
INSERT INTO credit_evaluation (
    ceval_id, appl_id, customer_id,
    ceval_engine, ceval_engine_version,
    ceval_grade, ceval_score, pd_bps,
    ceval_decision_cd, eval_limit_amount, eval_rate_bps,
    ceval_status_cd, ceval_factors, evaluated_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9001, 9001, 1001,
 'KCB', 'v2.1',
 'B', 720, 85,
 'APPROVED', 25000000, 520,
 'COMPLETED', '{"main_factor":"income_stability","detail":"정규직 3년 이상"}', '2025-01-11 10:00:00+09',
 '2025-01-11 10:00:00+09', 0, '2025-01-11 10:00:00+09', 0, 0),
(9002, 9002, 1001,
 'KCB', 'v2.1',
 'A', 800, 40,
 'APPROVED', 120000000, 410,
 'COMPLETED', '{"main_factor":"credit_history","detail":"장기 우량 고객"}', '2025-02-06 09:00:00+09',
 '2025-02-06 09:00:00+09', 0, '2025-02-06 09:00:00+09', 0, 0),
(9003, 9003, 1002,
 'KCB', 'v2.1',
 'C', 650, 180,
 'APPROVED', 15000000, 680,
 'COMPLETED', '{"main_factor":"dsr_marginal","detail":"DSR 한도 소폭 초과 — 심사관 판단 요"}', '2025-03-02 09:00:00+09',
 '2025-03-02 09:00:00+09', 0, '2025-03-02 09:00:00+09', 0, 0),
(9004, 9004, 1003,
 'KCB', 'v2.1',
 'D', 580, 320,
 'REJECTED', 0, 0,
 'COMPLETED', '{"main_factor":"low_score","detail":"신용점수 580 미만 자동 거절"}', '2025-03-11 09:00:00+09',
 '2025-03-11 09:00:00+09', 0, '2025-03-11 09:00:00+09', 0, 0)
ON CONFLICT (ceval_id) DO NOTHING;

-- ------------------------------------------------------------
-- 4. DSR 계산 (dsr_calculation)
-- ------------------------------------------------------------
INSERT INTO dsr_calculation (
    dsr_id, appl_id, customer_id,
    annual_income_amt, existing_principal_total, existing_annual_repay_amt,
    new_annual_repay_amt, total_annual_repay_amt,
    dsr_ratio_bps, dsr_limit_bps, dsr_status_cd,
    dsr_reg_type_cd, calculated_at, calc_engine_version,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9001, 9001, 1001,
 48000000, 5000000, 2400000,
 7680000, 10080000,
 2100, 4000, 'PASS',
 'STANDARD', '2025-01-11 10:05:00+09', 'v1.3',
 '2025-01-11 10:05:00+09', 0, '2025-01-11 10:05:00+09', 0, 0),
(9002, 9002, 1001,
 80000000, 0, 0,
 14400000, 14400000,
 1800, 4000, 'PASS',
 'STANDARD', '2025-02-06 09:05:00+09', 'v1.3',
 '2025-02-06 09:05:00+09', 0, '2025-02-06 09:05:00+09', 0, 0),
(9003, 9003, 1002,
 36000000, 8000000, 4200000,
 9600000, 13800000,
 3833, 4000, 'FAIL',
 'STANDARD', '2025-03-02 09:05:00+09', 'v1.3',
 '2025-03-02 09:05:00+09', 0, '2025-03-02 09:05:00+09', 0, 0),
(9004, 9004, 1003,
 30000000, 12000000, 6000000,
 4800000, 10800000,
 3600, 4000, 'FAIL',
 'STANDARD', '2025-03-11 09:05:00+09', 'v1.3',
 '2025-03-11 09:05:00+09', 0, '2025-03-11 09:05:00+09', 0, 0)
ON CONFLICT (dsr_id) DO NOTHING;

-- ------------------------------------------------------------
-- 5. 본심사 (loan_review)
--    9001/9002: COMPLETED + APPROVED
--    9003    : PENDING_APPROVER (어드바이저리 발행됨)
--    9004    : COMPLETED + REJECTED
-- ------------------------------------------------------------
INSERT INTO loan_review (
    rev_id, appl_id, rev_type_cd, rev_status_cd, rev_decision_cd,
    approved_amount, approved_rate_bps, approved_period_mo,
    reject_reason_cd, reviewer_id, reviewed_at, approved_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9001, 9001, 'MANUAL', 'COMPLETED', 'APPROVED',
 20000000, 520, 36,
 NULL, 9002, '2025-01-12 14:00:00+09', '2025-01-13 10:00:00+09',
 '2025-01-12 14:00:00+09', 0, '2025-01-13 10:00:00+09', 0, 0),
(9002, 9002, 'MANUAL', 'COMPLETED', 'APPROVED',
 100000000, 410, 120,
 NULL, 9002, '2025-02-07 11:00:00+09', '2025-02-08 09:00:00+09',
 '2025-02-07 11:00:00+09', 0, '2025-02-08 09:00:00+09', 0, 0),
(9003, 9003, 'MANUAL', 'PENDING_APPROVER', NULL,
 NULL, NULL, NULL,
 NULL, 9002, '2025-03-03 15:00:00+09', NULL,
 '2025-03-03 15:00:00+09', 0, '2025-03-05 09:00:00+09', 0, 0),
(9004, 9004, 'AUTO', 'COMPLETED', 'REJECTED',
 NULL, NULL, NULL,
 'LOW_CREDIT_SCORE', 9002, '2025-03-11 09:10:00+09', NULL,
 '2025-03-11 09:10:00+09', 0, '2025-03-11 09:10:00+09', 0, 0)
ON CONFLICT (rev_id) DO NOTHING;

-- ------------------------------------------------------------
-- 6. 대출 계약 (loan_contract)
-- ------------------------------------------------------------
INSERT INTO loan_contract (
    cntr_id, cntr_no, appl_id, rev_id,
    customer_id, prod_id,
    contracted_amount, currency_cd, contracted_period_mo,
    total_rate_bps, base_rate_bps, spread_bps, preferential_rate_bps,
    rate_type_cd, repayment_method_cd,
    cntr_status_cd, cntr_start_date, cntr_end_date,
    signed_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9001, 'DEMO-CNTR-2025-001', 9001, 9001,
 1001, 9001,
 20000000, 'KRW', 36,
 520, 450, 70, 0,
 'FIXED', 'EQUAL_PRINCIPAL_INTEREST',
 'ACTIVE', '20250115', '20280115',
 '2025-01-15 10:00:00+09',
 '2025-01-15 10:00:00+09', 0, '2025-01-15 10:00:00+09', 0, 0),
(9002, 'DEMO-CNTR-2025-002', 9002, 9002,
 1001, 9002,
 100000000, 'KRW', 120,
 410, 350, 60, 0,
 'VARIABLE', 'EQUAL_PRINCIPAL_INTEREST',
 'ACTIVE', '20250210', '20350210',
 '2025-02-10 09:00:00+09',
 '2025-02-10 09:00:00+09', 0, '2025-02-10 09:00:00+09', 0, 0)
ON CONFLICT (cntr_id) DO NOTHING;

-- ------------------------------------------------------------
-- 7. 신용정보 신고 (credit_info_report)
-- ------------------------------------------------------------
INSERT INTO credit_info_report (
    crpt_id, cntr_id, customer_id,
    crpt_type_cd, crpt_agency_cd, crpt_status_cd,
    report_target_cd, report_reason_cd, report_payload,
    external_tx_no, reported_at, ack_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9001, 9001, 1001,
 'NEW_LOAN', 'KCB', 'ACKED',
 'NEW', 'NEW_LOAN_CONTRACTED',
 '{"contractedAmount":20000000,"period":36,"productCd":"DEMO_PERSONAL"}',
 'KCB-TX-2025-001', '2025-01-15 10:05:00+09', '2025-01-15 10:10:00+09',
 '2025-01-15 10:05:00+09', 0, '2025-01-15 10:10:00+09', 0, 0),
(9002, 9002, 1001,
 'NEW_LOAN', 'KCB', 'ACKED',
 'NEW', 'NEW_LOAN_CONTRACTED',
 '{"contractedAmount":100000000,"period":120,"productCd":"DEMO_MORTGAGE"}',
 'KCB-TX-2025-002', '2025-02-10 09:05:00+09', '2025-02-10 09:12:00+09',
 '2025-02-10 09:05:00+09', 0, '2025-02-10 09:12:00+09', 0, 0)
ON CONFLICT (crpt_id) DO NOTHING;

-- ------------------------------------------------------------
-- 8. 알림 발송함 (notification_outbox)
-- ------------------------------------------------------------
INSERT INTO notification_outbox (
    outbox_id, event_type_cd, reference_id, channel_cd,
    payload, status, attempt_no, max_attempt, next_attempt_at,
    idempotency_key, sent_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9001, 'LOAN_APPROVED', 9001, 'EMAIL',
 '{"customerId":1001,"applNo":"DEMO-2025-001","message":"대출 심사 승인 안내"}',
 'SENT', 1, 5, '2025-01-13 10:01:00+09',
 'DEMO:LOAN_APPROVED:9001:EMAIL', '2025-01-13 10:01:00+09',
 '2025-01-13 10:00:00+09', 0, '2025-01-13 10:01:00+09', 0, 0),
(9002, 'LOAN_CONTRACTED', 9001, 'SMS',
 '{"customerId":1001,"cntrNo":"DEMO-CNTR-2025-001","message":"대출 약정 완료 안내"}',
 'SENT', 1, 5, '2025-01-15 10:01:00+09',
 'DEMO:LOAN_CONTRACTED:9001:SMS', '2025-01-15 10:01:00+09',
 '2025-01-15 10:00:00+09', 0, '2025-01-15 10:01:00+09', 0, 0),
(9003, 'LOAN_APPROVED', 9002, 'EMAIL',
 '{"customerId":1001,"applNo":"DEMO-2025-002","message":"대출 심사 승인 안내"}',
 'SENT', 1, 5, '2025-02-08 09:01:00+09',
 'DEMO:LOAN_APPROVED:9002:EMAIL', '2025-02-08 09:01:00+09',
 '2025-02-08 09:00:00+09', 0, '2025-02-08 09:01:00+09', 0, 0),
(9004, 'LOAN_REVIEW_PENDING', 9003, 'EMAIL',
 '{"customerId":1002,"applNo":"DEMO-2025-003","message":"심사 결재 대기 중 안내"}',
 'SENT', 1, 5, '2025-03-05 09:01:00+09',
 'DEMO:LOAN_REVIEW_PENDING:9003:EMAIL', '2025-03-05 09:01:00+09',
 '2025-03-05 09:00:00+09', 0, '2025-03-05 09:01:00+09', 0, 0),
(9005, 'LOAN_REJECTED', 9004, 'EMAIL',
 '{"customerId":1003,"applNo":"DEMO-2025-004","message":"대출 심사 결과 안내"}',
 'SENT', 1, 5, '2025-03-11 09:11:00+09',
 'DEMO:LOAN_REJECTED:9004:EMAIL', '2025-03-11 09:11:00+09',
 '2025-03-11 09:10:00+09', 0, '2025-03-11 09:11:00+09', 0, 0)
ON CONFLICT (outbox_id) DO NOTHING;

-- ------------------------------------------------------------
-- 어드바이저리 시드(review_advisory_rule / review_advisory_report /
-- ai_audit_opinion / advisory_document)는 advisory 스트림으로 이전했다.
-- 해당 테이블은 advisory 전용 Flyway(AdvisoryFlywayConfig)가 loan 기본
-- Flyway '이후'에 생성하므로, loan 스트림에서 INSERT 하면 부팅이 실패한다.
-- → db/advisory-migration/V29__seed_advisory_demo_data.sql 참조.
-- ------------------------------------------------------------

-- ============================================================
-- 끝.
-- ============================================================

-- ---- V36__normalize_loan_product_status.sql ----
-- 대출상품 상태 코드 정규화.
-- 일부 상품이 도메인에 없는 'ON_SALE' 값으로 적재되어 신청 검증(STATUS_ACTIVE)에서
-- LOAN_010("판매 중인 상품이 아닙니다")으로 거절되었다.
-- 도메인 표준(DRAFT/ACTIVE/DISCONTINUED)에 맞춰 ON_SALE -> ACTIVE 로 정렬한다.
UPDATE loan_product
   SET prod_status_cd = 'ACTIVE'
 WHERE prod_status_cd = 'ON_SALE';

-- ---- V37__expand_admin_demo_data.sql ----
-- ============================================================
-- V37: Admin 화면 데모 시드 데이터 확장
--
-- 목적: V35 의 최소 시드를 보강하여 관리자 화면(심사 큐, 본심사,
--       신청서류, 어드바이저리)이 다양한 파이프라인 단계로 채워지도록 한다.
--       특히 신청서류는 검증 4상태(VERIFIED/REJECTED/HOLD/PENDING)를 모두 포함하여
--       doc-agent 미연결 시 검증 보류(PENDING) 강등 동작까지 화면에서 확인 가능하게 한다.
-- 멱등: 모든 INSERT 는 ON CONFLICT ... DO NOTHING.
-- 참조: customer_id=1002~1005 는 customer-service 의 가상 고객. created_by=0 은 시스템(migration) 행위자.
--       V35 가 9001~9004 를 점유하므로 본 파일은 9005 번대부터 사용한다.
-- ============================================================

-- ------------------------------------------------------------
-- 1. 대출 상품 추가 (loan_product) — 상품 목록 다양화
-- ------------------------------------------------------------
INSERT INTO loan_product (
    prod_id, prod_cd, prod_name, loan_type_cd, target_customer_cd,
    repayment_method_cd, rate_type_cd,
    base_rate_bps, min_rate_bps, max_rate_bps,
    min_amount, max_amount, min_period_mo, max_period_mo,
    collateral_required_yn, guarantor_required_yn,
    sale_start_date, prod_status_cd,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9003, 'DEMO_JEONSE', '데모 전세자금대출', 'JEONSE', 'INDIVIDUAL',
 'EQUAL_PRINCIPAL_INTEREST', 'VARIABLE',
 380, 300, 1100,
 5000000, 300000000, 12, 240,
 'N', 'Y',
 '20250101', 'ACTIVE',
 '2025-01-01 00:00:00+09', 0, '2025-01-01 00:00:00+09', 0, 0),
(9004, 'DEMO_BIZ', '데모 개인사업자대출', 'BUSINESS', 'INDIVIDUAL',
 'EQUAL_PRINCIPAL_INTEREST', 'FIXED',
 600, 480, 1800,
 3000000, 100000000, 6, 84,
 'N', 'N',
 '20250101', 'ACTIVE',
 '2025-01-01 00:00:00+09', 0, '2025-01-01 00:00:00+09', 0, 0)
ON CONFLICT (prod_id) DO NOTHING;

-- ------------------------------------------------------------
-- 2. 대출 신청 (loan_application) — 파이프라인 전 단계 커버
--    9005: SUBMITTED        (접수, 서류 보완 필요)
--    9006: PRESCREENED      (가심사 통과, 서류 검증 보류 PENDING)
--    9007: REVIEWING        (본심사 진행 — 어드바이저리 대상)
--    9008: APPROVED         (심사 승인, 약정 전)
--    9009: CONTRACTED       (계약 완료)
-- ------------------------------------------------------------
INSERT INTO loan_application (
    appl_id, appl_no, customer_id, prod_id, channel_cd,
    requested_amount, requested_period_mo, loan_purpose_cd,
    repayment_method_cd, estimated_income_amt, employment_type_cd,
    appl_status_cd, applied_at, idempotency_key,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9005, 'DEMO-2025-005', 1002, 9001, 'MOBILE',
 12000000, 24, 'LIVING_EXPENSE',
 'EQUAL_PRINCIPAL_INTEREST', 42000000, 'EMPLOYED',
 'SUBMITTED', '2025-04-01 09:00:00+09', 'DEMO-IDEM-2025-005',
 '2025-04-01 09:00:00+09', 0, '2025-04-01 09:00:00+09', 0, 0),
(9006, 'DEMO-2025-006', 1004, 9003, 'INTERNET',
 80000000, 24, 'HOUSE_RENT',
 'EQUAL_PRINCIPAL_INTEREST', 55000000, 'EMPLOYED',
 'PRESCREENED', '2025-04-03 10:30:00+09', 'DEMO-IDEM-2025-006',
 '2025-04-03 10:30:00+09', 0, '2025-04-03 11:00:00+09', 0, 0),
(9007, 'DEMO-2025-007', 1004, 9001, 'INTERNET',
 30000000, 36, 'LIVING_EXPENSE',
 'EQUAL_PRINCIPAL_INTEREST', 52000000, 'EMPLOYED',
 'REVIEWING', '2025-04-05 14:00:00+09', 'DEMO-IDEM-2025-007',
 '2025-04-05 14:00:00+09', 0, '2025-04-07 09:00:00+09', 0, 0),
(9008, 'DEMO-2025-008', 1005, 9004, 'BRANCH',
 25000000, 48, 'BUSINESS_FUND',
 'EQUAL_PRINCIPAL_INTEREST', 60000000, 'SELF_EMPLOYED',
 'APPROVED', '2025-04-08 11:00:00+09', 'DEMO-IDEM-2025-008',
 '2025-04-08 11:00:00+09', 0, '2025-04-10 15:00:00+09', 0, 0),
(9009, 'DEMO-2025-009', 1005, 9001, 'MOBILE',
 18000000, 36, 'LIVING_EXPENSE',
 'EQUAL_PRINCIPAL_INTEREST', 47000000, 'EMPLOYED',
 'CONTRACTED', '2025-03-20 09:00:00+09', 'DEMO-IDEM-2025-009',
 '2025-03-20 09:00:00+09', 0, '2025-03-28 16:00:00+09', 0, 0)
ON CONFLICT (appl_id) DO NOTHING;

-- ------------------------------------------------------------
-- 3. 신용평가 (credit_evaluation) — 심사 단계 진입 건만
-- ------------------------------------------------------------
INSERT INTO credit_evaluation (
    ceval_id, appl_id, customer_id,
    ceval_engine, ceval_engine_version,
    ceval_grade, ceval_score, pd_bps,
    ceval_decision_cd, eval_limit_amount, eval_rate_bps,
    ceval_status_cd, ceval_factors, evaluated_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9007, 9007, 1004,
 'KCB', 'v2.1',
 'B', 710, 95,
 'REVIEW', 30000000, 540,
 'COMPLETED', '{"main_factor":"peer_divergence","detail":"유사군 결정 분기 — 수동 심사 권고"}', '2025-04-06 09:00:00+09',
 '2025-04-06 09:00:00+09', 0, '2025-04-06 09:00:00+09', 0, 0),
(9008, 9008, 1005,
 'KCB', 'v2.1',
 'B', 735, 70,
 'APPROVED', 25000000, 590,
 'COMPLETED', '{"main_factor":"business_cashflow","detail":"사업소득 안정"}', '2025-04-09 09:00:00+09',
 '2025-04-09 09:00:00+09', 0, '2025-04-09 09:00:00+09', 0, 0),
(9009, 9009, 1005,
 'KCB', 'v2.1',
 'A', 780, 50,
 'APPROVED', 20000000, 470,
 'COMPLETED', '{"main_factor":"credit_history","detail":"우량 신용 이력"}', '2025-03-21 09:00:00+09',
 '2025-03-21 09:00:00+09', 0, '2025-03-21 09:00:00+09', 0, 0)
ON CONFLICT (ceval_id) DO NOTHING;

-- ------------------------------------------------------------
-- 4. DSR 계산 (dsr_calculation)
-- ------------------------------------------------------------
INSERT INTO dsr_calculation (
    dsr_id, appl_id, customer_id,
    annual_income_amt, existing_principal_total, existing_annual_repay_amt,
    new_annual_repay_amt, total_annual_repay_amt,
    dsr_ratio_bps, dsr_limit_bps, dsr_status_cd,
    dsr_reg_type_cd, calculated_at, calc_engine_version,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9007, 9007, 1004,
 52000000, 6000000, 3000000,
 10800000, 13800000,
 2654, 4000, 'PASS',
 'STANDARD', '2025-04-06 09:05:00+09', 'v1.3',
 '2025-04-06 09:05:00+09', 0, '2025-04-06 09:05:00+09', 0, 0),
(9008, 9008, 1005,
 60000000, 10000000, 4800000,
 7200000, 12000000,
 2000, 4000, 'PASS',
 'STANDARD', '2025-04-09 09:05:00+09', 'v1.3',
 '2025-04-09 09:05:00+09', 0, '2025-04-09 09:05:00+09', 0, 0),
(9009, 9009, 1005,
 47000000, 0, 0,
 6480000, 6480000,
 1378, 4000, 'PASS',
 'STANDARD', '2025-03-21 09:05:00+09', 'v1.3',
 '2025-03-21 09:05:00+09', 0, '2025-03-21 09:05:00+09', 0, 0)
ON CONFLICT (dsr_id) DO NOTHING;

-- ------------------------------------------------------------
-- 5. 본심사 (loan_review)
--    9007: PENDING_APPROVER (어드바이저리 발행 — 결재 대기)
--    9008: COMPLETED + APPROVED (약정 전)
--    9009: COMPLETED + APPROVED (계약 완료 건)
-- ------------------------------------------------------------
INSERT INTO loan_review (
    rev_id, appl_id, rev_type_cd, rev_status_cd, rev_decision_cd,
    approved_amount, approved_rate_bps, approved_period_mo,
    reject_reason_cd, reviewer_id, reviewed_at, approved_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9007, 9007, 'MANUAL', 'PENDING_APPROVER', NULL,
 NULL, NULL, NULL,
 NULL, 9002, '2025-04-06 15:00:00+09', NULL,
 '2025-04-06 15:00:00+09', 0, '2025-04-07 09:00:00+09', 0, 0),
(9008, 9008, 'MANUAL', 'COMPLETED', 'APPROVED',
 25000000, 590, 48,
 NULL, 9002, '2025-04-09 14:00:00+09', '2025-04-10 10:00:00+09',
 '2025-04-09 14:00:00+09', 0, '2025-04-10 10:00:00+09', 0, 0),
(9009, 9009, 'MANUAL', 'COMPLETED', 'APPROVED',
 18000000, 470, 36,
 NULL, 9002, '2025-03-22 11:00:00+09', '2025-03-23 09:00:00+09',
 '2025-03-22 11:00:00+09', 0, '2025-03-23 09:00:00+09', 0, 0)
ON CONFLICT (rev_id) DO NOTHING;

-- ------------------------------------------------------------
-- 어드바이저리 리포트(rev_id=9007 유사군 결정 분기 자문)는 advisory
-- 스트림으로 이전했다. db/advisory-migration/V29__seed_advisory_demo_data.sql 참조.
-- ------------------------------------------------------------

-- ------------------------------------------------------------
-- 7. 신청서류 (loan_document) — 검증 4상태 모두 포함
--    VERIFIED(AUTO_PASS) / REJECTED(NEEDS_RESUBMIT) / UPLOADED(HOLD) /
--    UPLOADED(PENDING=doc-agent 미연결 검증 보류)
--    doc_url 에는 doc-agent submission_id 를 보존(보류 건은 NULL).
-- ------------------------------------------------------------
INSERT INTO loan_document (
    doc_id, appl_id, doc_type_cd, doc_status_cd, doc_source_cd,
    doc_name, doc_url, mime_type, file_size_bytes,
    submitted_at, verified_at, verify_result_cd,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
-- 9001 (CONTRACTED): 정상 검증 완료 서류
(9001, 9001, 'EMPLOYMENT_CERT', 'VERIFIED', 'MOBILE',
 'employment_cert.pdf', 'sub-demo-0001', 'application/pdf', 184320,
 '2025-01-10 10:20:00+09', '2025-01-10 10:21:00+09', 'AUTO_PASS',
 '2025-01-10 10:20:00+09', 0, '2025-01-10 10:21:00+09', 0, 0),
(9002, 9001, 'INCOME_PROOF', 'VERIFIED', 'MOBILE',
 'income_proof.pdf', 'sub-demo-0002', 'application/pdf', 220160,
 '2025-01-10 10:22:00+09', '2025-01-10 10:23:00+09', 'AUTO_PASS',
 '2025-01-10 10:22:00+09', 0, '2025-01-10 10:23:00+09', 0, 0),
-- 9003 (REVIEWING): 신분증 검증완료 + 통장사본 보류(HOLD)
(9003, 9003, 'ID_CARD', 'VERIFIED', 'INTERNET',
 'id_card.jpg', 'sub-demo-0003', 'image/jpeg', 98304,
 '2025-03-01 11:10:00+09', '2025-03-01 11:11:00+09', 'AUTO_PASS',
 '2025-03-01 11:10:00+09', 0, '2025-03-01 11:11:00+09', 0, 0),
(9004, 9003, 'BANKBOOK', 'UPLOADED', 'INTERNET',
 'bankbook.pdf', 'sub-demo-0004', 'application/pdf', 131072,
 '2025-03-01 11:12:00+09', NULL, 'HOLD',
 '2025-03-01 11:12:00+09', 0, '2025-03-01 11:12:00+09', 0, 0),
-- 9005 (SUBMITTED): 신분증 검증완료 + 소득증빙 재제출 필요(NEEDS_RESUBMIT)
(9005, 9005, 'ID_CARD', 'VERIFIED', 'MOBILE',
 'id_card.png', 'sub-demo-0005', 'image/png', 76800,
 '2025-04-01 09:10:00+09', '2025-04-01 09:11:00+09', 'AUTO_PASS',
 '2025-04-01 09:10:00+09', 0, '2025-04-01 09:11:00+09', 0, 0),
(9006, 9005, 'INCOME_PROOF', 'REJECTED', 'MOBILE',
 'income_blurry.jpg', 'sub-demo-0006', 'image/jpeg', 54200,
 '2025-04-01 09:12:00+09', NULL, 'NEEDS_RESUBMIT',
 '2025-04-01 09:12:00+09', 0, '2025-04-01 09:13:00+09', 0, 0),
-- 9006 (PRESCREENED): 재직증명 검증 보류(PENDING — doc-agent 미연결 강등). doc_url NULL.
(9007, 9006, 'EMPLOYMENT_CERT', 'UPLOADED', 'INTERNET',
 'employment_cert.pdf', NULL, 'application/pdf', 201728,
 '2025-04-03 10:40:00+09', NULL, 'PENDING',
 '2025-04-03 10:40:00+09', 0, '2025-04-03 10:40:00+09', 0, 0),
-- 9007 (REVIEWING): 정상 제출 서류 2종
(9008, 9007, 'ID_CARD', 'VERIFIED', 'INTERNET',
 'id_card.jpg', 'sub-demo-0008', 'image/jpeg', 91240,
 '2025-04-05 14:10:00+09', '2025-04-05 14:11:00+09', 'AUTO_PASS',
 '2025-04-05 14:10:00+09', 0, '2025-04-05 14:11:00+09', 0, 0),
(9009, 9007, 'INCOME_PROOF', 'VERIFIED', 'INTERNET',
 'income_proof.pdf', 'sub-demo-0009', 'application/pdf', 237568,
 '2025-04-05 14:12:00+09', '2025-04-05 14:13:00+09', 'AUTO_PASS',
 '2025-04-05 14:12:00+09', 0, '2025-04-05 14:13:00+09', 0, 0)
ON CONFLICT (doc_id) DO NOTHING;

-- ============================================================
-- 끝.
-- ============================================================

-- ---- V38__expand_admin_demo_data_v2.sql ----
-- ============================================================
-- V38: Admin 화면 데모 시드 데이터 확장 (V37 후속 / 비충돌 대역)
--
-- 배경: 일부 dev DB 에는 9001~9010 대역을 점유하는 별도 시드(SEED-2026 등)가
--       이미 존재하여 V37(9001~9009 대역)의 INSERT 가 ON CONFLICT 로 스킵되는
--       드리프트가 관찰됨. 본 마이그레이션은 9101~ 대역을 사용해 그 충돌을 피하고
--       의도한 데모 데이터(특히 서류 검증 4상태 — VERIFIED/REJECTED/HOLD/PENDING)가
--       어떤 환경에서도 실제로 적재되도록 한다.
-- 주의: 클린 DB 에서는 V37·V38 이 모두 실행되므로 UNIQUE 키(prod_cd/appl_no/
--       idempotency_key)는 V37 과 겹치지 않게 별도 값을 사용한다.
-- 멱등: 모든 INSERT 는 ON CONFLICT ... DO NOTHING.
-- ============================================================

-- ------------------------------------------------------------
-- 1. 대출 상품 (loan_product) — 9101~9102
-- ------------------------------------------------------------
INSERT INTO loan_product (
    prod_id, prod_cd, prod_name, loan_type_cd, target_customer_cd,
    repayment_method_cd, rate_type_cd,
    base_rate_bps, min_rate_bps, max_rate_bps,
    min_amount, max_amount, min_period_mo, max_period_mo,
    collateral_required_yn, guarantor_required_yn,
    sale_start_date, prod_status_cd,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9101, 'DEMO_JEONSE_V2', '데모 전세자금대출', 'JEONSE', 'INDIVIDUAL',
 'EQUAL_PRINCIPAL_INTEREST', 'VARIABLE',
 380, 300, 1100,
 5000000, 300000000, 12, 240,
 'N', 'Y',
 '20250101', 'ACTIVE',
 '2025-01-01 00:00:00+09', 0, '2025-01-01 00:00:00+09', 0, 0),
(9102, 'DEMO_BIZ_V2', '데모 개인사업자대출', 'BUSINESS', 'INDIVIDUAL',
 'EQUAL_PRINCIPAL_INTEREST', 'FIXED',
 600, 480, 1800,
 3000000, 100000000, 6, 84,
 'N', 'N',
 '20250101', 'ACTIVE',
 '2025-01-01 00:00:00+09', 0, '2025-01-01 00:00:00+09', 0, 0)
ON CONFLICT (prod_id) DO NOTHING;

-- ------------------------------------------------------------
-- 2. 대출 신청 (loan_application) — 9101~9105, 파이프라인 전 단계
--    9101: SUBMITTED   (접수, 서류 보완 필요)
--    9102: PRESCREENED (가심사 통과, 서류 검증 보류 PENDING)
--    9103: REVIEWING   (본심사 진행 — 어드바이저리 대상)
--    9104: APPROVED    (심사 승인, 약정 전)
--    9105: CONTRACTED  (계약 완료)
-- ------------------------------------------------------------
INSERT INTO loan_application (
    appl_id, appl_no, customer_id, prod_id, channel_cd,
    requested_amount, requested_period_mo, loan_purpose_cd,
    repayment_method_cd, estimated_income_amt, employment_type_cd,
    appl_status_cd, applied_at, idempotency_key,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9101, 'DEMO-2025-101', 1002, 9001, 'MOBILE',
 12000000, 24, 'LIVING_EXPENSE',
 'EQUAL_PRINCIPAL_INTEREST', 42000000, 'EMPLOYED',
 'SUBMITTED', '2025-04-01 09:00:00+09', 'DEMO-IDEM-2025-101',
 '2025-04-01 09:00:00+09', 0, '2025-04-01 09:00:00+09', 0, 0),
(9102, 'DEMO-2025-102', 1004, 9101, 'INTERNET',
 80000000, 24, 'HOUSE_RENT',
 'EQUAL_PRINCIPAL_INTEREST', 55000000, 'EMPLOYED',
 'PRESCREENED', '2025-04-03 10:30:00+09', 'DEMO-IDEM-2025-102',
 '2025-04-03 10:30:00+09', 0, '2025-04-03 11:00:00+09', 0, 0),
(9103, 'DEMO-2025-103', 1004, 9001, 'INTERNET',
 30000000, 36, 'LIVING_EXPENSE',
 'EQUAL_PRINCIPAL_INTEREST', 52000000, 'EMPLOYED',
 'REVIEWING', '2025-04-05 14:00:00+09', 'DEMO-IDEM-2025-103',
 '2025-04-05 14:00:00+09', 0, '2025-04-07 09:00:00+09', 0, 0),
(9104, 'DEMO-2025-104', 1005, 9102, 'BRANCH',
 25000000, 48, 'BUSINESS_FUND',
 'EQUAL_PRINCIPAL_INTEREST', 60000000, 'SELF_EMPLOYED',
 'APPROVED', '2025-04-08 11:00:00+09', 'DEMO-IDEM-2025-104',
 '2025-04-08 11:00:00+09', 0, '2025-04-10 15:00:00+09', 0, 0),
(9105, 'DEMO-2025-105', 1005, 9001, 'MOBILE',
 18000000, 36, 'LIVING_EXPENSE',
 'EQUAL_PRINCIPAL_INTEREST', 47000000, 'EMPLOYED',
 'CONTRACTED', '2025-03-20 09:00:00+09', 'DEMO-IDEM-2025-105',
 '2025-03-20 09:00:00+09', 0, '2025-03-28 16:00:00+09', 0, 0)
ON CONFLICT (appl_id) DO NOTHING;

-- ------------------------------------------------------------
-- 3. 신용평가 (credit_evaluation) — 심사 단계 진입 건만 (9103~9105)
-- ------------------------------------------------------------
INSERT INTO credit_evaluation (
    ceval_id, appl_id, customer_id,
    ceval_engine, ceval_engine_version,
    ceval_grade, ceval_score, pd_bps,
    ceval_decision_cd, eval_limit_amount, eval_rate_bps,
    ceval_status_cd, ceval_factors, evaluated_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9103, 9103, 1004,
 'KCB', 'v2.1',
 'B', 710, 95,
 'REVIEW', 30000000, 540,
 'COMPLETED', '{"main_factor":"peer_divergence","detail":"유사군 결정 분기 — 수동 심사 권고"}', '2025-04-06 09:00:00+09',
 '2025-04-06 09:00:00+09', 0, '2025-04-06 09:00:00+09', 0, 0),
(9104, 9104, 1005,
 'KCB', 'v2.1',
 'B', 735, 70,
 'APPROVED', 25000000, 590,
 'COMPLETED', '{"main_factor":"business_cashflow","detail":"사업소득 안정"}', '2025-04-09 09:00:00+09',
 '2025-04-09 09:00:00+09', 0, '2025-04-09 09:00:00+09', 0, 0),
(9105, 9105, 1005,
 'KCB', 'v2.1',
 'A', 780, 50,
 'APPROVED', 20000000, 470,
 'COMPLETED', '{"main_factor":"credit_history","detail":"우량 신용 이력"}', '2025-03-21 09:00:00+09',
 '2025-03-21 09:00:00+09', 0, '2025-03-21 09:00:00+09', 0, 0)
ON CONFLICT (ceval_id) DO NOTHING;

-- ------------------------------------------------------------
-- 4. DSR 계산 (dsr_calculation) — 9103~9105
-- ------------------------------------------------------------
INSERT INTO dsr_calculation (
    dsr_id, appl_id, customer_id,
    annual_income_amt, existing_principal_total, existing_annual_repay_amt,
    new_annual_repay_amt, total_annual_repay_amt,
    dsr_ratio_bps, dsr_limit_bps, dsr_status_cd,
    dsr_reg_type_cd, calculated_at, calc_engine_version,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9103, 9103, 1004,
 52000000, 6000000, 3000000,
 10800000, 13800000,
 2654, 4000, 'PASS',
 'STANDARD', '2025-04-06 09:05:00+09', 'v1.3',
 '2025-04-06 09:05:00+09', 0, '2025-04-06 09:05:00+09', 0, 0),
(9104, 9104, 1005,
 60000000, 10000000, 4800000,
 7200000, 12000000,
 2000, 4000, 'PASS',
 'STANDARD', '2025-04-09 09:05:00+09', 'v1.3',
 '2025-04-09 09:05:00+09', 0, '2025-04-09 09:05:00+09', 0, 0),
(9105, 9105, 1005,
 47000000, 0, 0,
 6480000, 6480000,
 1378, 4000, 'PASS',
 'STANDARD', '2025-03-21 09:05:00+09', 'v1.3',
 '2025-03-21 09:05:00+09', 0, '2025-03-21 09:05:00+09', 0, 0)
ON CONFLICT (dsr_id) DO NOTHING;

-- ------------------------------------------------------------
-- 5. 본심사 (loan_review) — 9103~9105
--    9103: PENDING_APPROVER (어드바이저리 발행 — 결재 대기)
--    9104: COMPLETED + APPROVED (약정 전)
--    9105: COMPLETED + APPROVED (계약 완료 건)
-- ------------------------------------------------------------
INSERT INTO loan_review (
    rev_id, appl_id, rev_type_cd, rev_status_cd, rev_decision_cd,
    approved_amount, approved_rate_bps, approved_period_mo,
    reject_reason_cd, reviewer_id, reviewed_at, approved_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9103, 9103, 'MANUAL', 'PENDING_APPROVER', NULL,
 NULL, NULL, NULL,
 NULL, 9002, '2025-04-06 15:00:00+09', NULL,
 '2025-04-06 15:00:00+09', 0, '2025-04-07 09:00:00+09', 0, 0),
(9104, 9104, 'MANUAL', 'COMPLETED', 'APPROVED',
 25000000, 590, 48,
 NULL, 9002, '2025-04-09 14:00:00+09', '2025-04-10 10:00:00+09',
 '2025-04-09 14:00:00+09', 0, '2025-04-10 10:00:00+09', 0, 0),
(9105, 9105, 'MANUAL', 'COMPLETED', 'APPROVED',
 18000000, 470, 36,
 NULL, 9002, '2025-03-22 11:00:00+09', '2025-03-23 09:00:00+09',
 '2025-03-22 11:00:00+09', 0, '2025-03-23 09:00:00+09', 0, 0)
ON CONFLICT (rev_id) DO NOTHING;

-- ------------------------------------------------------------
-- 어드바이저리 리포트(advr_id=9102, rev_id=9103 유사군 결정 분기 자문)는
-- advisory 스트림으로 이전했다.
-- db/advisory-migration/V29__seed_advisory_demo_data.sql 참조.
-- ------------------------------------------------------------

-- ------------------------------------------------------------
-- 7. 신청서류 (loan_document) — 9101~9109, 검증 4상태 모두 포함
--    VERIFIED(AUTO_PASS) / REJECTED(NEEDS_RESUBMIT) / UPLOADED(HOLD) /
--    UPLOADED(PENDING = doc-agent 미연결 검증 보류). 보류 건은 doc_url NULL.
-- ------------------------------------------------------------
INSERT INTO loan_document (
    doc_id, appl_id, doc_type_cd, doc_status_cd, doc_source_cd,
    doc_name, doc_url, mime_type, file_size_bytes,
    submitted_at, verified_at, verify_result_cd,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
-- 9101 (SUBMITTED): 신분증 검증완료 + 소득증빙 재제출 필요(NEEDS_RESUBMIT)
(9101, 9101, 'ID_CARD', 'VERIFIED', 'MOBILE',
 'id_card.png', 'sub-demo-9101', 'image/png', 76800,
 '2025-04-01 09:10:00+09', '2025-04-01 09:11:00+09', 'AUTO_PASS',
 '2025-04-01 09:10:00+09', 0, '2025-04-01 09:11:00+09', 0, 0),
(9102, 9101, 'INCOME_PROOF', 'REJECTED', 'MOBILE',
 'income_blurry.jpg', 'sub-demo-9102', 'image/jpeg', 54200,
 '2025-04-01 09:12:00+09', NULL, 'NEEDS_RESUBMIT',
 '2025-04-01 09:12:00+09', 0, '2025-04-01 09:13:00+09', 0, 0),
-- 9103 (PRESCREENED, appl 9102): 재직증명 검증 보류(PENDING — doc-agent 미연결 강등). doc_url NULL.
(9103, 9102, 'EMPLOYMENT_CERT', 'UPLOADED', 'INTERNET',
 'employment_cert.pdf', NULL, 'application/pdf', 201728,
 '2025-04-03 10:40:00+09', NULL, 'PENDING',
 '2025-04-03 10:40:00+09', 0, '2025-04-03 10:40:00+09', 0, 0),
-- 9104~9106 (REVIEWING, appl 9103): 신분증·소득증빙 검증완료 + 통장사본 보류(HOLD)
(9104, 9103, 'ID_CARD', 'VERIFIED', 'INTERNET',
 'id_card.jpg', 'sub-demo-9104', 'image/jpeg', 91240,
 '2025-04-05 14:10:00+09', '2025-04-05 14:11:00+09', 'AUTO_PASS',
 '2025-04-05 14:10:00+09', 0, '2025-04-05 14:11:00+09', 0, 0),
(9105, 9103, 'INCOME_PROOF', 'VERIFIED', 'INTERNET',
 'income_proof.pdf', 'sub-demo-9105', 'application/pdf', 237568,
 '2025-04-05 14:12:00+09', '2025-04-05 14:13:00+09', 'AUTO_PASS',
 '2025-04-05 14:12:00+09', 0, '2025-04-05 14:13:00+09', 0, 0),
(9106, 9103, 'BANKBOOK', 'UPLOADED', 'INTERNET',
 'bankbook.pdf', 'sub-demo-9106', 'application/pdf', 131072,
 '2025-04-05 14:14:00+09', NULL, 'HOLD',
 '2025-04-05 14:14:00+09', 0, '2025-04-05 14:14:00+09', 0, 0),
-- 9107~9108 (APPROVED, appl 9104): 신분증·사업자등록증 검증완료
(9107, 9104, 'ID_CARD', 'VERIFIED', 'BRANCH',
 'id_card.jpg', 'sub-demo-9107', 'image/jpeg', 88210,
 '2025-04-08 11:10:00+09', '2025-04-08 11:11:00+09', 'AUTO_PASS',
 '2025-04-08 11:10:00+09', 0, '2025-04-08 11:11:00+09', 0, 0),
(9108, 9104, 'BIZ_REG', 'VERIFIED', 'BRANCH',
 'biz_reg.pdf', 'sub-demo-9108', 'application/pdf', 158720,
 '2025-04-08 11:12:00+09', '2025-04-08 11:13:00+09', 'AUTO_PASS',
 '2025-04-08 11:12:00+09', 0, '2025-04-08 11:13:00+09', 0, 0),
-- 9109 (CONTRACTED, appl 9105): 소득증빙 검증완료
(9109, 9105, 'INCOME_PROOF', 'VERIFIED', 'MOBILE',
 'income_proof.pdf', 'sub-demo-9109', 'application/pdf', 219000,
 '2025-03-20 09:30:00+09', '2025-03-20 09:31:00+09', 'AUTO_PASS',
 '2025-03-20 09:30:00+09', 0, '2025-03-20 09:31:00+09', 0, 0)
ON CONFLICT (doc_id) DO NOTHING;

-- ============================================================
-- 끝.
-- ============================================================

-- ---- V39__seed_repayment_lifecycle.sql ----
-- ============================================================
-- V39: 상환 라이프사이클 데모 시드
--
-- 목적: 계약 이후 운영(서비싱) 화면이 비어있는 공백을 메운다.
--       "완전히 서비싱 중인 계약 1건"을 풀체인으로 구성:
--         신청(CONTRACTED) → 심사 → 계약(ACTIVE) → 대출실행(DONE)
--         → 상환계좌(VERIFIED, 자동이체) → 상환스케줄 12회차 → 납부거래 3건
--       스케줄은 납부완료(PAID)·연체(OVERDUE)·예정(DUE)을 섞어 상환 화면을 채운다.
-- 대역: 9201~ (V37=90xx, V38=91xx 와 비충돌). customer_id=1006 가상 고객.
-- 멱등: 모든 INSERT 는 ON CONFLICT ... DO NOTHING.
-- FK 순서: contract → execution / account / schedule → transaction.
-- ============================================================

-- ------------------------------------------------------------
-- 1. 신청 / 심사 / 계약 (CONTRACTED 라인)
-- ------------------------------------------------------------
INSERT INTO loan_application (
    appl_id, appl_no, customer_id, prod_id, channel_cd,
    requested_amount, requested_period_mo, loan_purpose_cd,
    repayment_method_cd, estimated_income_amt, employment_type_cd,
    appl_status_cd, applied_at, idempotency_key,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9201, 'DEMO-2025-201', 1006, 9001, 'MOBILE',
 12000000, 12, 'LIVING_EXPENSE',
 'EQUAL_PRINCIPAL', 45000000, 'EMPLOYED',
 'CONTRACTED', '2025-03-02 09:00:00+09', 'DEMO-IDEM-2025-201',
 '2025-03-02 09:00:00+09', 0, '2025-03-10 16:00:00+09', 0, 0)
ON CONFLICT (appl_id) DO NOTHING;

INSERT INTO credit_evaluation (
    ceval_id, appl_id, customer_id,
    ceval_engine, ceval_engine_version,
    ceval_grade, ceval_score, pd_bps,
    ceval_decision_cd, eval_limit_amount, eval_rate_bps,
    ceval_status_cd, ceval_factors, evaluated_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9201, 9201, 1006,
 'KCB', 'v2.1',
 'A', 790, 45,
 'APPROVED', 15000000, 470,
 'COMPLETED', '{"main_factor":"credit_history","detail":"우량 신용 이력"}', '2025-03-03 09:00:00+09',
 '2025-03-03 09:00:00+09', 0, '2025-03-03 09:00:00+09', 0, 0)
ON CONFLICT (ceval_id) DO NOTHING;

INSERT INTO dsr_calculation (
    dsr_id, appl_id, customer_id,
    annual_income_amt, existing_principal_total, existing_annual_repay_amt,
    new_annual_repay_amt, total_annual_repay_amt,
    dsr_ratio_bps, dsr_limit_bps, dsr_status_cd,
    dsr_reg_type_cd, calculated_at, calc_engine_version,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9201, 9201, 1006,
 45000000, 0, 0,
 12480000, 12480000,
 2773, 4000, 'PASS',
 'STANDARD', '2025-03-03 09:05:00+09', 'v1.3',
 '2025-03-03 09:05:00+09', 0, '2025-03-03 09:05:00+09', 0, 0)
ON CONFLICT (dsr_id) DO NOTHING;

INSERT INTO loan_review (
    rev_id, appl_id, rev_type_cd, rev_status_cd, rev_decision_cd,
    approved_amount, approved_rate_bps, approved_period_mo,
    reject_reason_cd, reviewer_id, reviewed_at, approved_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9201, 9201, 'MANUAL', 'COMPLETED', 'APPROVED',
 12000000, 470, 12,
 NULL, 9002, '2025-03-04 11:00:00+09', '2025-03-05 09:00:00+09',
 '2025-03-04 11:00:00+09', 0, '2025-03-05 09:00:00+09', 0, 0)
ON CONFLICT (rev_id) DO NOTHING;

INSERT INTO loan_contract (
    cntr_id, cntr_no, appl_id, rev_id,
    customer_id, prod_id,
    contracted_amount, currency_cd, contracted_period_mo,
    total_rate_bps, base_rate_bps, spread_bps, preferential_rate_bps,
    rate_type_cd, repayment_method_cd,
    cntr_status_cd, cntr_start_date, cntr_end_date,
    signed_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9201, 'DEMO-CNTR-2025-201', 9201, 9201,
 1006, 9001,
 12000000, 'KRW', 12,
 470, 450, 20, 0,
 'FIXED', 'EQUAL_PRINCIPAL',
 'ACTIVE', '20250310', '20260310',
 '2025-03-10 10:00:00+09',
 '2025-03-10 10:00:00+09', 0, '2025-03-10 10:00:00+09', 0, 0)
ON CONFLICT (cntr_id) DO NOTHING;

-- ------------------------------------------------------------
-- 2. 대출 실행 (loan_execution) — 인출 완료(DONE)
-- ------------------------------------------------------------
INSERT INTO loan_execution (
    exec_id, cntr_id, executed_amount, currency_cd, exec_status_cd,
    executed_at, value_date, fee_amount, idempotency_key,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9201, 9201, 12000000, 'KRW', 'DONE',
 '2025-03-10 10:30:00+09', '20250310', 0, 'EXEC-IDEM-2025-201',
 '2025-03-10 10:30:00+09', 0, '2025-03-10 10:30:00+09', 0, 0)
ON CONFLICT (exec_id) DO NOTHING;

-- ------------------------------------------------------------
-- 3. 상환 계좌 (repayment_account) — 검증완료, 자동이체 매월 10일
-- ------------------------------------------------------------
INSERT INTO repayment_account (
    racct_id, cntr_id, account_no_masked, bank_cd, holder_name_masked,
    racct_status_cd, auto_debit_yn, debit_day, verified_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9201, 9201, '123-**-****56', '004', '홍**',
 'VERIFIED', 'Y', 10, '2025-03-10 10:40:00+09',
 '2025-03-10 10:40:00+09', 0, '2025-03-10 10:40:00+09', 0, 0)
ON CONFLICT (racct_id) DO NOTHING;

-- ------------------------------------------------------------
-- 4. 상환 스케줄 (repayment_schedule) — 12회차 원금균등(원금 100만/월)
--    1~3회: PAID, 4회: OVERDUE(미납), 5~12회: DUE(예정)
-- ------------------------------------------------------------
INSERT INTO repayment_schedule (
    rsch_id, cntr_id, installment_no, due_date,
    scheduled_principal, scheduled_interest, scheduled_total, remaining_balance,
    applied_rate_bps, rsch_status_cd, rsch_version_cd, holiday_adjusted_yn,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9201, 9201,  1, '20250410', 1000000, 47000, 1047000, 11000000, 470, 'PAID',    'V1', 'N', '2025-03-10 10:45:00+09', 0, '2025-04-10 10:00:00+09', 0, 0),
(9202, 9201,  2, '20250510', 1000000, 43000, 1043000, 10000000, 470, 'PAID',    'V1', 'N', '2025-03-10 10:45:00+09', 0, '2025-05-10 10:00:00+09', 0, 0),
(9203, 9201,  3, '20250610', 1000000, 39000, 1039000,  9000000, 470, 'PAID',    'V1', 'N', '2025-03-10 10:45:00+09', 0, '2025-06-10 10:00:00+09', 0, 0),
(9204, 9201,  4, '20250710', 1000000, 35000, 1035000,  8000000, 470, 'OVERDUE', 'V1', 'N', '2025-03-10 10:45:00+09', 0, '2025-07-11 00:00:00+09', 0, 0),
(9205, 9201,  5, '20250810', 1000000, 31000, 1031000,  7000000, 470, 'DUE',     'V1', 'N', '2025-03-10 10:45:00+09', 0, '2025-03-10 10:45:00+09', 0, 0),
(9206, 9201,  6, '20250910', 1000000, 27000, 1027000,  6000000, 470, 'DUE',     'V1', 'N', '2025-03-10 10:45:00+09', 0, '2025-03-10 10:45:00+09', 0, 0),
(9207, 9201,  7, '20251010', 1000000, 24000, 1024000,  5000000, 470, 'DUE',     'V1', 'N', '2025-03-10 10:45:00+09', 0, '2025-03-10 10:45:00+09', 0, 0),
(9208, 9201,  8, '20251110', 1000000, 20000, 1020000,  4000000, 470, 'DUE',     'V1', 'N', '2025-03-10 10:45:00+09', 0, '2025-03-10 10:45:00+09', 0, 0),
(9209, 9201,  9, '20251210', 1000000, 16000, 1016000,  3000000, 470, 'DUE',     'V1', 'N', '2025-03-10 10:45:00+09', 0, '2025-03-10 10:45:00+09', 0, 0),
(9210, 9201, 10, '20260110', 1000000, 12000, 1012000,  2000000, 470, 'DUE',     'V1', 'N', '2025-03-10 10:45:00+09', 0, '2025-03-10 10:45:00+09', 0, 0),
(9211, 9201, 11, '20260210', 1000000,  8000, 1008000,  1000000, 470, 'DUE',     'V1', 'N', '2025-03-10 10:45:00+09', 0, '2025-03-10 10:45:00+09', 0, 0),
(9212, 9201, 12, '20260310', 1000000,  4000, 1004000,        0, 470, 'DUE',     'V1', 'N', '2025-03-10 10:45:00+09', 0, '2025-03-10 10:45:00+09', 0, 0)
ON CONFLICT (rsch_id) DO NOTHING;

-- ------------------------------------------------------------
-- 5. 상환 거래 (repayment_transaction) — 1~3회차 정상 납부(SUCCESS, 자동이체)
-- ------------------------------------------------------------
INSERT INTO repayment_transaction (
    rtx_id, cntr_id, rsch_id, rtx_type_cd,
    total_amount, principal_amount, interest_amount, overdue_interest_amount, fee_amount,
    currency_cd, channel_cd, rtx_status_cd, paid_at, value_date, balance_after,
    idempotency_key, reversal_yn,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9201, 9201, 9201, 'SCHEDULED',
 1047000, 1000000, 47000, 0, 0,
 'KRW', 'AUTO_DEBIT', 'SUCCESS', '2025-04-10 06:00:00+09', '20250410', 11000000,
 'RTX-IDEM-2025-201', 'N',
 '2025-04-10 06:00:00+09', 0, '2025-04-10 06:00:00+09', 0, 0),
(9202, 9201, 9202, 'SCHEDULED',
 1043000, 1000000, 43000, 0, 0,
 'KRW', 'AUTO_DEBIT', 'SUCCESS', '2025-05-10 06:00:00+09', '20250510', 10000000,
 'RTX-IDEM-2025-202', 'N',
 '2025-05-10 06:00:00+09', 0, '2025-05-10 06:00:00+09', 0, 0),
(9203, 9201, 9203, 'SCHEDULED',
 1039000, 1000000, 39000, 0, 0,
 'KRW', 'AUTO_DEBIT', 'SUCCESS', '2025-06-10 06:00:00+09', '20250610', 9000000,
 'RTX-IDEM-2025-203', 'N',
 '2025-06-10 06:00:00+09', 0, '2025-06-10 06:00:00+09', 0, 0)
ON CONFLICT (rtx_id) DO NOTHING;

-- ============================================================
-- 끝.
-- ============================================================

-- ---- V40__seed_review_bias_escalation.sql ----
-- ============================================================
-- V40: 본심사 분기(편향검증·본사 상신) 데모 시드
--
-- 목적: 권한/공정성 화면이 비어있는 공백을 메운다. 본심사 워크플로의
--       분기 상태 5종을 표본으로 만든다:
--         9301 BIAS_REVIEWING + HIGH      (편향 경고, 확인 대기)
--         9302 BIAS_REVIEWING + BLOCKED   (편향 차단, 미우회 — 정정/상급자 우회 필요)
--         9303 COMPLETED + 상급자 우회승인 (BLOCKED 를 지점장이 OVERRIDE_APPROVED)
--         9304 ESCALATED_TO_HQ           (이상거래 본사 상신)
--         9305 PENDING_APPROVER + NONE    (편향 없음, 승인자 결재 대기)
--       편향 상세는 ai_review_advice(BIAS_CHECK)에, 체크 이력은 review_check_log 에 적재.
-- 대역: 9301~ (V37=90xx, V38=91xx, V39=92xx 와 비충돌). customer_id=1007~1011 가상 고객.
-- 멱등: 모든 INSERT 는 ON CONFLICT ... DO NOTHING.
-- FK 순서: application → credit_evaluation/dsr_calculation → loan_review → ai_review_advice/review_check_log.
-- 행위자: reviewer=9002(부지점장), approver/override=9001(지점장). 4-eye(심사≠승인) 준수.
-- ============================================================

-- ------------------------------------------------------------
-- 1. 신청 (loan_application) — 9301~9305
-- ------------------------------------------------------------
INSERT INTO loan_application (
    appl_id, appl_no, customer_id, prod_id, channel_cd,
    requested_amount, requested_period_mo, loan_purpose_cd,
    repayment_method_cd, estimated_income_amt, employment_type_cd,
    appl_status_cd, applied_at, idempotency_key,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9301, 'DEMO-2025-301', 1007, 9001, 'INTERNET',
 22000000, 36, 'LIVING_EXPENSE', 'EQUAL_PRINCIPAL_INTEREST', 50000000, 'EMPLOYED',
 'REVIEWING', '2025-05-01 10:00:00+09', 'DEMO-IDEM-2025-301',
 '2025-05-01 10:00:00+09', 0, '2025-05-02 09:00:00+09', 0, 0),
(9302, 'DEMO-2025-302', 1008, 9001, 'INTERNET',
 18000000, 24, 'LIVING_EXPENSE', 'EQUAL_PRINCIPAL_INTEREST', 38000000, 'EMPLOYED',
 'REVIEWING', '2025-05-03 11:00:00+09', 'DEMO-IDEM-2025-302',
 '2025-05-03 11:00:00+09', 0, '2025-05-04 09:00:00+09', 0, 0),
(9303, 'DEMO-2025-303', 1009, 9001, 'BRANCH',
 20000000, 36, 'LIVING_EXPENSE', 'EQUAL_PRINCIPAL_INTEREST', 46000000, 'EMPLOYED',
 'APPROVED', '2025-05-05 14:00:00+09', 'DEMO-IDEM-2025-303',
 '2025-05-05 14:00:00+09', 0, '2025-05-08 16:00:00+09', 0, 0),
(9304, 'DEMO-2025-304', 1010, 9001, 'MOBILE',
 30000000, 48, 'LIVING_EXPENSE', 'EQUAL_PRINCIPAL_INTEREST', 55000000, 'EMPLOYED',
 'REVIEWING', '2025-05-07 09:30:00+09', 'DEMO-IDEM-2025-304',
 '2025-05-07 09:30:00+09', 0, '2025-05-07 15:00:00+09', 0, 0),
(9305, 'DEMO-2025-305', 1011, 9001, 'INTERNET',
 16000000, 24, 'LIVING_EXPENSE', 'EQUAL_PRINCIPAL_INTEREST', 41000000, 'EMPLOYED',
 'REVIEWING', '2025-05-09 13:00:00+09', 'DEMO-IDEM-2025-305',
 '2025-05-09 13:00:00+09', 0, '2025-05-10 09:00:00+09', 0, 0)
ON CONFLICT (appl_id) DO NOTHING;

-- ------------------------------------------------------------
-- 2. 신용평가 (credit_evaluation) — 9301~9305
-- ------------------------------------------------------------
INSERT INTO credit_evaluation (
    ceval_id, appl_id, customer_id,
    ceval_engine, ceval_engine_version,
    ceval_grade, ceval_score, pd_bps,
    ceval_decision_cd, eval_limit_amount, eval_rate_bps,
    ceval_status_cd, ceval_factors, evaluated_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9301, 9301, 1007, 'KCB', 'v2.1', 'B', 705, 100, 'REVIEW', 22000000, 560,
 'COMPLETED', '{"main_factor":"manual_review","detail":"편향 검증 대상"}', '2025-05-01 10:30:00+09',
 '2025-05-01 10:30:00+09', 0, '2025-05-01 10:30:00+09', 0, 0),
(9302, 9302, 1008, 'KCB', 'v2.1', 'C', 660, 170, 'REVIEW', 18000000, 690,
 'COMPLETED', '{"main_factor":"manual_review","detail":"편향 BLOCKED 대상"}', '2025-05-03 11:30:00+09',
 '2025-05-03 11:30:00+09', 0, '2025-05-03 11:30:00+09', 0, 0),
(9303, 9303, 1009, 'KCB', 'v2.1', 'B', 715, 90, 'REVIEW', 20000000, 530,
 'COMPLETED', '{"main_factor":"manual_review","detail":"BLOCKED 우회 승인 사례"}', '2025-05-05 14:30:00+09',
 '2025-05-05 14:30:00+09', 0, '2025-05-05 14:30:00+09', 0, 0),
(9304, 9304, 1010, 'KCB', 'v2.1', 'B', 700, 110, 'REVIEW', 30000000, 580,
 'COMPLETED', '{"main_factor":"fraud_signal","detail":"이상거래 의심 — 본사 상신"}', '2025-05-07 10:00:00+09',
 '2025-05-07 10:00:00+09', 0, '2025-05-07 10:00:00+09', 0, 0),
(9305, 9305, 1011, 'KCB', 'v2.1', 'A', 770, 55, 'APPROVED', 16000000, 480,
 'COMPLETED', '{"main_factor":"clean","detail":"편향 신호 없음"}', '2025-05-09 13:30:00+09',
 '2025-05-09 13:30:00+09', 0, '2025-05-09 13:30:00+09', 0, 0)
ON CONFLICT (ceval_id) DO NOTHING;

-- ------------------------------------------------------------
-- 3. DSR 계산 (dsr_calculation) — 9301~9305 (모두 PASS)
-- ------------------------------------------------------------
INSERT INTO dsr_calculation (
    dsr_id, appl_id, customer_id,
    annual_income_amt, existing_principal_total, existing_annual_repay_amt,
    new_annual_repay_amt, total_annual_repay_amt,
    dsr_ratio_bps, dsr_limit_bps, dsr_status_cd,
    dsr_reg_type_cd, calculated_at, calc_engine_version,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9301, 9301, 1007, 50000000, 4000000, 2000000, 8800000, 10800000, 2160, 4000, 'PASS', 'STANDARD', '2025-05-01 10:35:00+09', 'v1.3', '2025-05-01 10:35:00+09', 0, '2025-05-01 10:35:00+09', 0, 0),
(9302, 9302, 1008, 38000000, 5000000, 2400000, 8400000, 10800000, 2842, 4000, 'PASS', 'STANDARD', '2025-05-03 11:35:00+09', 'v1.3', '2025-05-03 11:35:00+09', 0, '2025-05-03 11:35:00+09', 0, 0),
(9303, 9303, 1009, 46000000, 3000000, 1500000, 8000000,  9500000, 2065, 4000, 'PASS', 'STANDARD', '2025-05-05 14:35:00+09', 'v1.3', '2025-05-05 14:35:00+09', 0, '2025-05-05 14:35:00+09', 0, 0),
(9304, 9304, 1010, 55000000, 6000000, 3000000, 9600000, 12600000, 2291, 4000, 'PASS', 'STANDARD', '2025-05-07 10:05:00+09', 'v1.3', '2025-05-07 10:05:00+09', 0, '2025-05-07 10:05:00+09', 0, 0),
(9305, 9305, 1011, 41000000, 0, 0, 8160000, 8160000, 1990, 4000, 'PASS', 'STANDARD', '2025-05-09 13:35:00+09', 'v1.3', '2025-05-09 13:35:00+09', 0, '2025-05-09 13:35:00+09', 0, 0)
ON CONFLICT (dsr_id) DO NOTHING;

-- ------------------------------------------------------------
-- 4. 본심사 (loan_review) — 분기 상태 5종
-- ------------------------------------------------------------
INSERT INTO loan_review (
    rev_id, appl_id, rev_type_cd, rev_status_cd, rev_decision_cd,
    approved_amount, approved_rate_bps, approved_period_mo, reject_reason_cd,
    reviewer_id, reviewed_at, approved_at,
    bias_severity_cd, bias_override_by, bias_override_reason, bias_overridden_at,
    approver_id, approved_decision_cd, override_reason_cd, override_remark,
    pending_approver_since, owner_id, escalated_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
-- 9301: 편향 HIGH, 확인 대기 (BIAS_REVIEWING)
(9301, 9301, 'MANUAL', 'BIAS_REVIEWING', NULL,
 NULL, NULL, NULL, NULL,
 9002, '2025-05-02 09:00:00+09', NULL,
 'HIGH', NULL, NULL, NULL,
 NULL, NULL, NULL, NULL,
 NULL, 9002, NULL,
 '2025-05-02 09:00:00+09', 0, '2025-05-02 09:00:00+09', 0, 0),
-- 9302: 편향 BLOCKED, 미우회 (정정 또는 상급자 우회 필요)
(9302, 9302, 'MANUAL', 'BIAS_REVIEWING', NULL,
 NULL, NULL, NULL, NULL,
 9002, '2025-05-04 09:00:00+09', NULL,
 'BLOCKED', NULL, NULL, NULL,
 NULL, NULL, NULL, NULL,
 NULL, 9002, NULL,
 '2025-05-04 09:00:00+09', 0, '2025-05-04 09:00:00+09', 0, 0),
-- 9303: BLOCKED 를 지점장이 우회 승인 → COMPLETED + OVERRIDE_APPROVED
(9303, 9303, 'MANUAL', 'COMPLETED', 'APPROVED',
 20000000, 530, 36, NULL,
 9002, '2025-05-06 10:00:00+09', '2025-05-08 16:00:00+09',
 'BLOCKED', 9001, '신용등급·소득 안정성 감안, 편향 경고는 표본 부족에 기인 — 예외 승인', '2025-05-08 15:30:00+09',
 9001, 'OVERRIDE_APPROVED', 'POLICY_EXCEPTION', '여신심사 기준서 §3.2 예외 요건 충족',
 '2025-05-06 10:00:00+09', 9002, NULL,
 '2025-05-06 10:00:00+09', 0, '2025-05-08 16:00:00+09', 0, 0),
-- 9304: 이상거래 본사 상신 (ESCALATED_TO_HQ)
(9304, 9304, 'MANUAL', 'ESCALATED_TO_HQ', NULL,
 NULL, NULL, NULL, NULL,
 9002, '2025-05-07 14:00:00+09', NULL,
 'MEDIUM', NULL, NULL, NULL,
 NULL, NULL, NULL, NULL,
 NULL, 9002, '2025-05-07 14:30:00+09',
 '2025-05-07 14:00:00+09', 0, '2025-05-07 14:30:00+09', 0, 0),
-- 9305: 편향 없음, 승인자 결재 대기 (PENDING_APPROVER)
(9305, 9305, 'MANUAL', 'PENDING_APPROVER', NULL,
 NULL, NULL, NULL, NULL,
 9002, '2025-05-10 09:00:00+09', NULL,
 'NONE', NULL, NULL, NULL,
 NULL, NULL, NULL, NULL,
 '2025-05-10 09:00:00+09', 9002, NULL,
 '2025-05-10 09:00:00+09', 0, '2025-05-10 09:00:00+09', 0, 0)
ON CONFLICT (rev_id) DO NOTHING;

-- ------------------------------------------------------------
-- 5. AI 편향 검증 조언 (ai_review_advice) — BIAS_CHECK
--    편향 분기 건(9301 HIGH / 9302 BLOCKED / 9303 BLOCKED)에 상세 사유 기록.
-- ------------------------------------------------------------
INSERT INTO ai_review_advice (
    advice_id, rev_id, advice_type_cd, severity_cd, advice_body,
    model, model_version, input_token, output_token, latency_ms,
    created_at, created_by
) OVERRIDING SYSTEM VALUE VALUES
(9301, 9301, 'BIAS_CHECK', 'HIGH',
 '동일 코호트(연령·소득 구간) 대비 거절 성향이 +1.8σ 로 관측됨. 결정 전 근거 보강 권고.',
 'claude', 'demo', 540, 120, 850, '2025-05-02 09:05:00+09', 0),
(9302, 9302, 'BIAS_CHECK', 'BLOCKED',
 '보호속성 추정 변수와 거절 간 통계적 연관(>임계) 탐지. 명백한 규정위반 가능 — 차단. 정정 또는 상급자 우회 필요.',
 'claude', 'demo', 610, 145, 910, '2025-05-04 09:05:00+09', 0),
(9303, 9303, 'BIAS_CHECK', 'BLOCKED',
 '9302 와 동일 패턴 차단. 단 표본 30 미만으로 신뢰구간 넓음 — 심사관 판단 여지 있음.',
 'claude', 'demo', 600, 138, 880, '2025-05-06 10:05:00+09', 0)
ON CONFLICT (advice_id) DO NOTHING;

-- ------------------------------------------------------------
-- 6. 심사 체크 이력 (review_check_log) — 완결 건(9303)의 자동 체크로그 5종
-- ------------------------------------------------------------
INSERT INTO review_check_log (
    rchk_id, rev_id, check_item_cd, check_result_cd, check_remark, checker_id, checked_at,
    created_at, created_by
) OVERRIDING SYSTEM VALUE VALUES
(9301, 9303, 'PRESCREEN_PASS', 'PASS', NULL, 9002, '2025-05-06 10:00:00+09', '2025-05-06 10:00:00+09', 0),
(9302, 9303, 'CB_DECISION',    'REVIEW', 'CB=REVIEW 수동 심사', 9002, '2025-05-06 10:00:00+09', '2025-05-06 10:00:00+09', 0),
(9303, 9303, 'DSR_CHECK',      'PASS', 'dsr=2065bps', 9002, '2025-05-06 10:00:00+09', '2025-05-06 10:00:00+09', 0),
(9304, 9303, 'LTV_CHECK',      'N_A', '신용대출(담보 없음)', 9002, '2025-05-06 10:00:00+09', '2025-05-06 10:00:00+09', 0),
(9305, 9303, 'FINAL_DECISION', 'PASS', 'OVERRIDE_APPROVED (편향 BLOCKED 우회)', 9001, '2025-05-08 16:00:00+09', '2025-05-08 16:00:00+09', 0)
ON CONFLICT (rchk_id) DO NOTHING;

-- ============================================================
-- 끝.
-- ============================================================

-- ---- V41__seed_collateral_guarantor.sql ----
-- ============================================================
-- V41: 담보·LTV·보증인·보증보험 데모 시드 (origination 보강)
--
-- 목적: 담보/보증 분기 화면 공백을 메운다.
--         9401 주택담보대출: 담보(APPROVED) + LTV 산정(PASS) + 계약 + 보증보험(ISSUED)
--         9402 신용대출    : 보증인(SIGNED, 연대보증)
-- 대역: 9401~ (V37~V40 의 90xx~93xx 와 비충돌). customer_id=1012~1013 가상 고객.
-- 멱등: 모든 INSERT 는 ON CONFLICT ... DO NOTHING.
-- FK 순서: application → ceval/dsr → review → contract → collateral/ltv/guarantee_insurance;
--          guarantor_master → guarantor_agreement.
-- ============================================================

-- ------------------------------------------------------------
-- 1. 신청 (loan_application) — 9401 주택담보, 9402 보증부 신용
-- ------------------------------------------------------------
INSERT INTO loan_application (
    appl_id, appl_no, customer_id, prod_id, channel_cd,
    requested_amount, requested_period_mo, loan_purpose_cd,
    repayment_method_cd, estimated_income_amt, employment_type_cd,
    appl_status_cd, applied_at, idempotency_key,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9401, 'DEMO-2025-401', 1012, 9002, 'BRANCH',
 150000000, 240, 'HOUSE_PURCHASE', 'EQUAL_PRINCIPAL_INTEREST', 90000000, 'EMPLOYED',
 'CONTRACTED', '2025-04-15 10:00:00+09', 'DEMO-IDEM-2025-401',
 '2025-04-15 10:00:00+09', 0, '2025-04-22 16:00:00+09', 0, 0),
(9402, 'DEMO-2025-402', 1013, 9001, 'INTERNET',
 20000000, 36, 'LIVING_EXPENSE', 'EQUAL_PRINCIPAL_INTEREST', 35000000, 'EMPLOYED',
 'REVIEWING', '2025-04-18 11:00:00+09', 'DEMO-IDEM-2025-402',
 '2025-04-18 11:00:00+09', 0, '2025-04-19 09:00:00+09', 0, 0)
ON CONFLICT (appl_id) DO NOTHING;

-- ------------------------------------------------------------
-- 2. 신용평가 / DSR (화면 결합용 최소 1건씩)
-- ------------------------------------------------------------
INSERT INTO credit_evaluation (
    ceval_id, appl_id, customer_id, ceval_engine, ceval_engine_version,
    ceval_grade, ceval_score, pd_bps, ceval_decision_cd, eval_limit_amount, eval_rate_bps,
    ceval_status_cd, ceval_factors, evaluated_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9401, 9401, 1012, 'KCB', 'v2.1', 'A', 800, 40, 'APPROVED', 160000000, 380,
 'COMPLETED', '{"main_factor":"collateral_backed","detail":"담보부 우량"}', '2025-04-16 09:00:00+09',
 '2025-04-16 09:00:00+09', 0, '2025-04-16 09:00:00+09', 0, 0),
(9402, 9402, 1013, 'KCB', 'v2.1', 'C', 640, 200, 'REVIEW', 20000000, 720,
 'COMPLETED', '{"main_factor":"guarantor_required","detail":"보증인 보강 필요"}', '2025-04-18 11:30:00+09',
 '2025-04-18 11:30:00+09', 0, '2025-04-18 11:30:00+09', 0, 0)
ON CONFLICT (ceval_id) DO NOTHING;

INSERT INTO dsr_calculation (
    dsr_id, appl_id, customer_id, annual_income_amt, existing_principal_total, existing_annual_repay_amt,
    new_annual_repay_amt, total_annual_repay_amt, dsr_ratio_bps, dsr_limit_bps, dsr_status_cd,
    dsr_reg_type_cd, calculated_at, calc_engine_version,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9401, 9401, 1012, 90000000, 0, 0, 9000000, 9000000, 1000, 4000, 'PASS', 'STANDARD', '2025-04-16 09:05:00+09', 'v1.3', '2025-04-16 09:05:00+09', 0, '2025-04-16 09:05:00+09', 0, 0),
(9402, 9402, 1013, 35000000, 3000000, 1500000, 7200000, 8700000, 2485, 4000, 'PASS', 'STANDARD', '2025-04-18 11:35:00+09', 'v1.3', '2025-04-18 11:35:00+09', 0, '2025-04-18 11:35:00+09', 0, 0)
ON CONFLICT (dsr_id) DO NOTHING;

-- ------------------------------------------------------------
-- 3. 본심사 + 계약 (9401 주택담보, 보증보험 부착 대상)
-- ------------------------------------------------------------
INSERT INTO loan_review (
    rev_id, appl_id, rev_type_cd, rev_status_cd, rev_decision_cd,
    approved_amount, approved_rate_bps, approved_period_mo, reject_reason_cd,
    reviewer_id, reviewed_at, approved_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9401, 9401, 'MANUAL', 'COMPLETED', 'APPROVED',
 150000000, 380, 240, NULL,
 9002, '2025-04-20 14:00:00+09', '2025-04-21 10:00:00+09',
 '2025-04-20 14:00:00+09', 0, '2025-04-21 10:00:00+09', 0, 0)
ON CONFLICT (rev_id) DO NOTHING;

INSERT INTO loan_contract (
    cntr_id, cntr_no, appl_id, rev_id, customer_id, prod_id,
    contracted_amount, currency_cd, contracted_period_mo,
    total_rate_bps, base_rate_bps, spread_bps, preferential_rate_bps,
    rate_type_cd, repayment_method_cd, cntr_status_cd, cntr_start_date, cntr_end_date, signed_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9401, 'DEMO-CNTR-2025-401', 9401, 9401, 1012, 9002,
 150000000, 'KRW', 240,
 380, 350, 30, 0,
 'VARIABLE', 'EQUAL_PRINCIPAL_INTEREST', 'ACTIVE', '20250422', '20450422', '2025-04-22 10:00:00+09',
 '2025-04-22 10:00:00+09', 0, '2025-04-22 10:00:00+09', 0, 0)
ON CONFLICT (cntr_id) DO NOTHING;

-- ------------------------------------------------------------
-- 4. 담보 (collateral) — 아파트, 선순위 있음, APPROVED
-- ------------------------------------------------------------
INSERT INTO collateral (
    col_id, appl_id, col_type_cd, col_status_cd, col_no,
    col_name, col_address, col_registry_no, declared_value, currency_cd,
    ownership_type_cd, senior_lien_yn, senior_lien_amount,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9401, 9401, 'APARTMENT', 'APPROVED', 'DEMO-COL-9401',
 '데모아파트 101동 1001호', '서울 강남구 데모로 1', '1146-1996-012345', 300000000, 'KRW',
 'SOLE', 'Y', 50000000,
 '2025-04-16 10:00:00+09', 0, '2025-04-19 10:00:00+09', 0, 0)
ON CONFLICT (col_id) DO NOTHING;

-- ------------------------------------------------------------
-- 5. LTV 산정 (ltv_calculation) — 한도 70%, PASS
--    max_loan = 감정가 3억 × 70% - 선순위 5천만 = 1.6억 ≥ 요청 1.5억 → PASS
--    ltv_ratio = 요청 1.5억 / 감정가 3억 = 5000bps
-- ------------------------------------------------------------
INSERT INTO ltv_calculation (
    ltv_id, appl_id, col_id, applied_col_value, senior_lien_amount, requested_amount,
    ltv_ratio_bps, ltv_limit_bps, max_loan_amount, ltv_status_cd, calculated_at, calc_engine_version,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9401, 9401, 9401, 300000000, 50000000, 150000000,
 5000, 7000, 160000000, 'PASS', '2025-04-17 09:00:00+09', 'v1.1',
 '2025-04-17 09:00:00+09', 0, '2025-04-17 09:00:00+09', 0, 0)
ON CONFLICT (ltv_id) DO NOTHING;

-- ------------------------------------------------------------
-- 6. 보증인 마스터 (guarantor_master) — PII enc 는 placeholder(NOT NULL 충족용)
-- ------------------------------------------------------------
INSERT INTO guarantor_master (
    gmst_id, guarantor_name_enc, guarantor_name_masked, guarantor_ci_hash,
    relation_type_cd, mobile_no_masked,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9401, '\x00'::bytea, '김**', 'demo-ci-hash-9401',
 'FAMILY', '010-****-1234',
 '2025-04-18 12:00:00+09', 0, '2025-04-18 12:00:00+09', 0, 0)
ON CONFLICT (gmst_id) DO NOTHING;

-- ------------------------------------------------------------
-- 7. 보증인 약정 (guarantor_agreement) — 연대보증(JOINT), SIGNED
-- ------------------------------------------------------------
INSERT INTO guarantor_agreement (
    gagr_id, appl_id, gmst_id, gagr_type_cd, guarantee_amount, guarantee_ratio_bps,
    gagr_status_cd, consented_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9401, 9402, 9401, 'JOINT', 20000000, 10000,
 'SIGNED', '2025-04-19 09:30:00+09',
 '2025-04-18 12:10:00+09', 0, '2025-04-19 09:30:00+09', 0, 0)
ON CONFLICT (gagr_id) DO NOTHING;

-- ------------------------------------------------------------
-- 8. 보증보험 (guarantee_insurance) — 계약 9401 에 발급(ISSUED)
-- ------------------------------------------------------------
INSERT INTO guarantee_insurance (
    gins_id, cntr_id, gins_agency_cd, gins_policy_no, guarantee_amount, guarantee_ratio_bps,
    premium_amount, gins_status_cd, gins_start_date, gins_end_date, issued_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9401, 9401, 'SGI', 'DEMO-GINS-9401', 150000000, 10000,
 450000, 'ISSUED', '20250422', '20450422', '2025-04-22 10:30:00+09',
 '2025-04-22 10:30:00+09', 0, '2025-04-22 10:30:00+09', 0, 0)
ON CONFLICT (gins_id) DO NOTHING;

-- ============================================================
-- 끝.
-- ============================================================

-- ---- V42__seed_delinquency_closure_maturity.sql ----
-- ============================================================
-- V42: 연체·종결·만기 데모 시드 (서비싱 심화)
--
-- 목적: 대출 운영 후반 라이프사이클 화면 공백을 메운다.
--   연체: V39 의 계약 9201(4회차 OVERDUE)에 연체(ACTIVE/STAGE_1) + 연체이자 적립 부착
--   종결: 신규 계약 3건을 종결 — 9501 정상(NORMAL) / 9502 대손(WRITE_OFF) / 9503 대위변제(SUBROGATION)
--   만기: 9201 진행중(ACTIVE) / 9504 만기도래(MATURED) / 9505 기한연장(extension_count=1)
-- 대역: 9501~ 신규(계약/신청/심사), 도메인 PK 는 9201·9501~ 사용. customer_id=1014~1018.
-- 멱등: 모든 INSERT 는 ON CONFLICT ... DO NOTHING.
-- 계약 상태: CLOSED(종결) / ACTIVE(만기도래·연장은 계약은 ACTIVE, 만기상태는 maturity 테이블).
-- ============================================================

-- ------------------------------------------------------------
-- 1. 종결·만기용 신규 신청/심사/계약 (9501~9505)
-- ------------------------------------------------------------
INSERT INTO loan_application (
    appl_id, appl_no, customer_id, prod_id, channel_cd,
    requested_amount, requested_period_mo, loan_purpose_cd,
    repayment_method_cd, estimated_income_amt, employment_type_cd,
    appl_status_cd, applied_at, idempotency_key,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9501, 'DEMO-2025-501', 1014, 9001, 'MOBILE',   5000000, 12, 'LIVING_EXPENSE', 'EQUAL_PRINCIPAL', 40000000, 'EMPLOYED',      'CONTRACTED', '2025-03-01 09:00:00+09', 'DEMO-IDEM-2025-501', '2025-03-01 09:00:00+09', 0, '2025-03-10 10:00:00+09', 0, 0),
(9502, 'DEMO-2024-502', 1015, 9001, 'INTERNET', 8000000, 24, 'LIVING_EXPENSE', 'EQUAL_PRINCIPAL', 36000000, 'SELF_EMPLOYED', 'CONTRACTED', '2024-03-01 09:00:00+09', 'DEMO-IDEM-2024-502', '2024-03-01 09:00:00+09', 0, '2024-03-10 10:00:00+09', 0, 0),
(9503, 'DEMO-2024-503', 1016, 9001, 'BRANCH',  10000000, 24, 'LIVING_EXPENSE', 'EQUAL_PRINCIPAL', 38000000, 'EMPLOYED',      'CONTRACTED', '2024-06-01 09:00:00+09', 'DEMO-IDEM-2024-503', '2024-06-01 09:00:00+09', 0, '2024-06-10 10:00:00+09', 0, 0),
(9504, 'DEMO-2025-504', 1017, 9001, 'MOBILE',   6000000,  6, 'LIVING_EXPENSE', 'EQUAL_PRINCIPAL', 42000000, 'EMPLOYED',      'CONTRACTED', '2025-03-01 09:00:00+09', 'DEMO-IDEM-2025-504', '2025-03-01 09:00:00+09', 0, '2025-03-10 10:00:00+09', 0, 0),
(9505, 'DEMO-2025-505', 1018, 9001, 'INTERNET', 7000000, 12, 'LIVING_EXPENSE', 'EQUAL_PRINCIPAL', 44000000, 'EMPLOYED',      'CONTRACTED', '2025-03-01 09:00:00+09', 'DEMO-IDEM-2025-505', '2025-03-01 09:00:00+09', 0, '2025-03-10 10:00:00+09', 0, 0)
ON CONFLICT (appl_id) DO NOTHING;

INSERT INTO loan_review (
    rev_id, appl_id, rev_type_cd, rev_status_cd, rev_decision_cd,
    approved_amount, approved_rate_bps, approved_period_mo, reject_reason_cd,
    reviewer_id, reviewed_at, approved_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9501, 9501, 'AUTO', 'COMPLETED', 'APPROVED',  5000000, 470, 12, NULL, 9002, '2025-03-05 10:00:00+09', '2025-03-05 10:00:00+09', '2025-03-05 10:00:00+09', 0, '2025-03-05 10:00:00+09', 0, 0),
(9502, 9502, 'AUTO', 'COMPLETED', 'APPROVED',  8000000, 620, 24, NULL, 9002, '2024-03-05 10:00:00+09', '2024-03-05 10:00:00+09', '2024-03-05 10:00:00+09', 0, '2024-03-05 10:00:00+09', 0, 0),
(9503, 9503, 'MANUAL','COMPLETED', 'APPROVED', 10000000, 580, 24, NULL, 9002, '2024-06-05 10:00:00+09', '2024-06-05 10:00:00+09', '2024-06-05 10:00:00+09', 0, '2024-06-05 10:00:00+09', 0, 0),
(9504, 9504, 'AUTO', 'COMPLETED', 'APPROVED',  6000000, 460,  6, NULL, 9002, '2025-03-05 10:00:00+09', '2025-03-05 10:00:00+09', '2025-03-05 10:00:00+09', 0, '2025-03-05 10:00:00+09', 0, 0),
(9505, 9505, 'AUTO', 'COMPLETED', 'APPROVED',  7000000, 490, 12, NULL, 9002, '2025-03-05 10:00:00+09', '2025-03-05 10:00:00+09', '2025-03-05 10:00:00+09', 0, '2025-03-05 10:00:00+09', 0, 0)
ON CONFLICT (rev_id) DO NOTHING;

INSERT INTO loan_contract (
    cntr_id, cntr_no, appl_id, rev_id, customer_id, prod_id,
    contracted_amount, currency_cd, contracted_period_mo,
    total_rate_bps, base_rate_bps, spread_bps, preferential_rate_bps,
    rate_type_cd, repayment_method_cd, cntr_status_cd, cntr_start_date, cntr_end_date, signed_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9501, 'DEMO-CNTR-2025-501', 9501, 9501, 1014, 9001,  5000000, 'KRW', 12, 470, 450, 20, 0, 'FIXED', 'EQUAL_PRINCIPAL', 'CLOSED', '20250310', '20260310', '2025-03-10 10:00:00+09', '2025-03-10 10:00:00+09', 0, '2026-03-10 10:00:00+09', 0, 0),
(9502, 'DEMO-CNTR-2024-502', 9502, 9502, 1015, 9001,  8000000, 'KRW', 24, 620, 450, 170, 0, 'FIXED', 'EQUAL_PRINCIPAL', 'CLOSED', '20240310', '20260310', '2024-03-10 10:00:00+09', '2024-03-10 10:00:00+09', 0, '2025-11-15 10:00:00+09', 0, 0),
(9503, 'DEMO-CNTR-2024-503', 9503, 9503, 1016, 9001, 10000000, 'KRW', 24, 580, 450, 130, 0, 'FIXED', 'EQUAL_PRINCIPAL', 'CLOSED', '20240610', '20260610', '2024-06-10 10:00:00+09', '2024-06-10 10:00:00+09', 0, '2025-12-20 10:00:00+09', 0, 0),
(9504, 'DEMO-CNTR-2025-504', 9504, 9504, 1017, 9001,  6000000, 'KRW',  6, 460, 450, 10, 0, 'FIXED', 'EQUAL_PRINCIPAL', 'ACTIVE', '20250310', '20250910', '2025-03-10 10:00:00+09', '2025-03-10 10:00:00+09', 0, '2025-09-10 10:00:00+09', 0, 0),
(9505, 'DEMO-CNTR-2025-505', 9505, 9505, 1018, 9001,  7000000, 'KRW', 12, 490, 450, 40, 0, 'FIXED', 'EQUAL_PRINCIPAL', 'ACTIVE', '20250310', '20260310', '2025-03-10 10:00:00+09', '2025-03-10 10:00:00+09', 0, '2026-03-01 10:00:00+09', 0, 0)
ON CONFLICT (cntr_id) DO NOTHING;

-- ------------------------------------------------------------
-- 2. 연체 (delinquency) — 계약 9201 의 4회차 미납, STAGE_1, 20일 경과
-- ------------------------------------------------------------
INSERT INTO delinquency (
    dlq_id, cntr_id, dlq_status_cd, dlq_start_date, dlq_end_date, dlq_days,
    dlq_principal_amt, dlq_interest_amt, dlq_total_amt, overdue_rate_bps, dlq_stage_cd, resolved_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9201, 9201, 'ACTIVE', '20250711', NULL, 20,
 1000000, 35000, 1035000, 800, 'STAGE_1', NULL,
 '2025-07-11 06:00:00+09', 0, '2025-07-30 06:00:00+09', 0, 0)
ON CONFLICT (dlq_id) DO NOTHING;

-- 연체이자 적립 (overdue_accrual) — 적립일 기준 1건 (멱등 UNIQUE: cntr_id+accrual_date)
INSERT INTO overdue_accrual (
    oa_id, cntr_id, dlq_id, accrual_date, overdue_principal, overdue_rate_bps, dlq_days,
    daily_overdue_interest, cumulative_overdue_interest, oa_status_cd, accrued_at,
    created_at, created_by
) OVERRIDING SYSTEM VALUE VALUES
(9201, 9201, 9201, '20250730', 1000000, 800, 20,
 219, 4380, 'ACCRUED', '2025-07-30 06:00:00+09',
 '2025-07-30 06:00:00+09', 0)
ON CONFLICT (oa_id) DO NOTHING;

-- ------------------------------------------------------------
-- 3. 종결 (loan_closure) — 정상 / 대손 / 대위변제
-- ------------------------------------------------------------
INSERT INTO loan_closure (
    clos_id, cntr_id, clos_type_cd, clos_reason_cd, clos_status_cd,
    final_principal_amt, final_interest_amt, final_fee_amt, prepayment_fee_amt, total_settled_amt,
    clos_date, closed_at,
    write_off_amount, subrogation_amount, subrogation_party_ref, write_off_reason_cd,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
-- 정상 만기 종결 (잔액 0)
(9501, 9501, 'NORMAL', 'MATURITY_FULL_REPAY', 'COMPLETED',
 5000000, 150000, 0, 0, 5150000,
 '20260310', '2026-03-10 10:00:00+09',
 NULL, NULL, NULL, NULL,
 '2026-03-10 10:00:00+09', 0, '2026-03-10 10:00:00+09', 0, 0),
-- 대손 상각 (회수불능)
(9502, 9502, 'WRITE_OFF', 'UNCOLLECTIBLE', 'COMPLETED',
 0, 0, 0, 0, 0,
 '20251115', '2025-11-15 10:00:00+09',
 6500000, NULL, NULL, 'UNCOLLECTIBLE',
 '2025-11-15 10:00:00+09', 0, '2025-11-15 10:00:00+09', 0, 0),
-- 대위변제 (보증보험 대납)
(9503, 9503, 'SUBROGATION', 'GUARANTOR_PAID', 'COMPLETED',
 0, 0, 0, 0, 9000000,
 '20251220', '2025-12-20 10:00:00+09',
 NULL, 9000000, 'SGI보증보험(대위변제)', NULL,
 '2025-12-20 10:00:00+09', 0, '2025-12-20 10:00:00+09', 0, 0)
ON CONFLICT (clos_id) DO NOTHING;

-- ------------------------------------------------------------
-- 4. 만기 (maturity) — 진행중 / 만기도래 / 기한연장
-- ------------------------------------------------------------
INSERT INTO maturity (
    mat_id, cntr_id, original_maturity_date, current_maturity_date, mat_status_cd,
    extension_type_cd, extension_count, last_extended_date, extended_period_mo,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
-- 9201: 진행중 (V39 계약), 연장 없음
(9201, 9201, '20260310', '20260310', 'ACTIVE', NULL, 0, NULL, NULL,
 '2025-03-10 10:50:00+09', 0, '2025-03-10 10:50:00+09', 0, 0),
-- 9504: 만기 도래 (6개월 단기, 배치로 MATURED)
(9504, 9504, '20250910', '20250911', 'MATURED', NULL, 0, NULL, NULL,
 '2025-03-10 10:50:00+09', 0, '2025-09-11 04:00:00+09', 0, 0),
-- 9505: 기한 연장 1회 (+6개월)
(9505, 9505, '20260310', '20260910', 'ACTIVE', 'GRACE', 1, '20260301', 6,
 '2025-03-10 10:50:00+09', 0, '2026-03-01 11:00:00+09', 0, 0)
ON CONFLICT (mat_id) DO NOTHING;

-- ============================================================
-- 끝.
-- ============================================================

-- ---- V43__seed_prescreen_consent_idv_ocr_cert.sql ----
-- ============================================================
-- V43: 자투리 데모 시드 — 가심사·신용동의·본인확인·서류OCR·증명서·자동이체 청산대기
--
-- 목적: 구현은 됐으나 시드가 없던 나머지 영역을 채운다. 기존 신청/계약/서류에 결합.
--   가심사   : 신청 9102·9103·9301 (PASS)
--   신용동의 : 신청 9103·9301 (CB 조회 동의)
--   본인확인 : 신청 9103·9301 (휴대폰 인증 DONE/PASS)
--   증명서   : 계약 9201(잔액·부채) / 9501(상환완료)
--   자동이체 : 계약 9201 5회차 청산 대기(PENDING)
-- 대역: PK 9601~ (기존과 비충돌). 멱등: ON CONFLICT ... DO NOTHING.
-- 참고: 결합 대상 신청/계약/서류는 V38/V39/V40/V42 에서 생성됨.
-- ============================================================

-- ------------------------------------------------------------
-- 1. 가심사 (loan_prescreening) — appl_id UNIQUE 이므로 가심사 미보유 신청에만
-- ------------------------------------------------------------
INSERT INTO loan_prescreening (
    presc_id, appl_id, presc_result_cd, estimated_limit_amt, estimated_rate_bps,
    estimated_grade, estimated_score, reject_reason_cd, presc_remark,
    prescreened_at, presc_engine_version,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9601, 9102, 'PASS', 80000000, 380, 'A', 760, NULL, '한도조회 통과',
 '2025-04-03 10:35:00+09', 'v1.2', '2025-04-03 10:35:00+09', 0, '2025-04-03 10:35:00+09', 0, 0),
(9602, 9103, 'PASS', 30000000, 540, 'B', 710, NULL, '가심사 통과',
 '2025-04-05 14:05:00+09', 'v1.2', '2025-04-05 14:05:00+09', 0, '2025-04-05 14:05:00+09', 0, 0),
(9603, 9301, 'PASS', 22000000, 560, 'B', 705, NULL, '가심사 통과(수동 심사 대상)',
 '2025-05-01 10:05:00+09', 'v1.2', '2025-05-01 10:05:00+09', 0, '2025-05-01 10:05:00+09', 0, 0)
ON CONFLICT (presc_id) DO NOTHING;

-- ------------------------------------------------------------
-- 2. 신용조회 동의 (credit_consent)
-- ------------------------------------------------------------
INSERT INTO credit_consent (
    csnt_id, appl_id, customer_id, consent_type_cd, consent_scope_cd, consent_target_cd,
    consent_yn, consented_at, consent_method_cd, retention_until, withdrawn_yn,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9601, 9103, 1004, 'CREDIT_INQUIRY', 'ALL', 'CB', 'Y', '2025-04-05 13:55:00+09', 'EFORM', '20300405', 'N',
 '2025-04-05 13:55:00+09', 0, '2025-04-05 13:55:00+09', 0, 0),
(9602, 9301, 1007, 'CREDIT_INQUIRY', 'ALL', 'CB', 'Y', '2025-05-01 09:55:00+09', 'EFORM', '20300501', 'N',
 '2025-05-01 09:55:00+09', 0, '2025-05-01 09:55:00+09', 0, 0)
ON CONFLICT (csnt_id) DO NOTHING;

-- ------------------------------------------------------------
-- 3. 본인확인 (loan_identity_verification) — 휴대폰 인증 완료. enc 컬럼은 NULL.
-- ------------------------------------------------------------
INSERT INTO loan_identity_verification (
    idv_id, appl_id, customer_id, idv_method_cd, idv_status_cd, idv_result_cd, idv_target_cd,
    ci_hash, mobile_no_masked, verified_at, external_tx_no,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9601, 9103, 1004, 'MOBILE_AUTH', 'DONE', 'PASS', 'APPLICANT',
 'demo-ci-9601', '010-****-3456', '2025-04-05 13:58:00+09', 'IDV-TX-9601',
 '2025-04-05 13:58:00+09', 0, '2025-04-05 13:58:00+09', 0, 0),
(9602, 9301, 1007, 'MOBILE_AUTH', 'DONE', 'PASS', 'APPLICANT',
 'demo-ci-9602', '010-****-7788', '2025-05-01 09:58:00+09', 'IDV-TX-9602',
 '2025-05-01 09:58:00+09', 0, '2025-05-01 09:58:00+09', 0, 0)
ON CONFLICT (idv_id) DO NOTHING;

-- (서류 OCR 은 V21 에서 loan_document_ocr 가 제거됨 — OCR 은 doc-agent L3 파이프라인 담당.
--  loan-service 시드 대상 아님.)

-- ------------------------------------------------------------
-- 4. 증명서 (loan_certificate) — 계약 9201(잔액·부채), 9501(상환완료)
-- ------------------------------------------------------------
INSERT INTO loan_certificate (
    cert_id, cntr_id, customer_id, cert_type_cd, cert_no, cert_status_cd,
    cert_purpose_cd, issue_channel_cd, issued_at, retention_until,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9601, 9201, 1006, 'BALANCE',   'DEMO-CERT-9601', 'ISSUED', 'PROOF_OF_BALANCE', 'MOBILE',  '2025-06-01 10:00:00+09', '20300601', '2025-06-01 10:00:00+09', 0, '2025-06-01 10:00:00+09', 0, 0),
(9602, 9201, 1006, 'DEBT',      'DEMO-CERT-9602', 'ISSUED', 'LOAN_APPLICATION', 'COUNTER', '2025-06-02 11:00:00+09', '20300602', '2025-06-02 11:00:00+09', 0, '2025-06-02 11:00:00+09', 0, 0),
(9603, 9501, 1014, 'REPAYMENT', 'DEMO-CERT-9603', 'ISSUED', 'PROOF_OF_REPAYMENT', 'MOBILE', '2026-03-11 09:00:00+09', '20310311', '2026-03-11 09:00:00+09', 0, '2026-03-11 09:00:00+09', 0, 0)
ON CONFLICT (cert_id) DO NOTHING;

-- ------------------------------------------------------------
-- 6. 자동이체 청산 대기 (auto_debit_clearing_pending) — 계약 9201 5회차 진행중
--    (BaseEntity 미상속: created_by/updated_* 컬럼 없음. status CHECK: PENDING/DONE/FAILED)
-- ------------------------------------------------------------
INSERT INTO auto_debit_clearing_pending (
    pending_id, pi_id, cntr_id, rsch_id, installment_no, base_date, idempotency_key, status,
    created_at, resolved_at
) OVERRIDING SYSTEM VALUE VALUES
(9601, 'PI-DEMO-9601', 9201, 9205, 5, '20250810', 'AUTODEBIT-9201-5-20250810', 'PENDING',
 '2025-08-10 06:00:00+09', NULL)
ON CONFLICT (pending_id) DO NOTHING;

-- ============================================================
-- advisory-service (advisory-migration V20~V28, DB: loan_db 공유)
-- 심사관 어드바이저리 · RAG · 감사 에이전트
-- ============================================================

-- ---- advisory-service V20__advisory_tables.sql ----

CREATE TABLE review_advisory_rule (
    rule_id                BIGSERIAL     PRIMARY KEY,
    rule_cd                VARCHAR(50)   NOT NULL,
    rule_name              VARCHAR(200)  NOT NULL,
    advisory_type_cd       VARCHAR(50)   NOT NULL,
    rule_category_cd       VARCHAR(50)   NOT NULL,
    severity_cd            VARCHAR(50)   NOT NULL,
    rule_params            JSONB,
    rule_version           VARCHAR(50)   NOT NULL,
    active_yn              CHAR(1)       NOT NULL DEFAULT 'Y',
    effective_start_date   VARCHAR(8),
    effective_end_date     VARCHAR(8),
    rule_desc              VARCHAR(500),
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by             BIGINT        NOT NULL,
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by             BIGINT        NOT NULL,
    deleted_at             TIMESTAMPTZ,
    deleted_by             BIGINT,
    version                INT           NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_review_advisory_rule_cd
    ON review_advisory_rule (rule_cd)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_review_advisory_rule_active
    ON review_advisory_rule (active_yn, advisory_type_cd)
    WHERE deleted_at IS NULL;

-- quarantined_at: V23__quarantine_signal.sql 에서 추가
CREATE TABLE review_advisory_report (
    advr_id              BIGSERIAL     PRIMARY KEY,
    rev_id               BIGINT        NOT NULL REFERENCES loan_review(rev_id) ON DELETE NO ACTION,
    rule_id              BIGINT        NOT NULL REFERENCES review_advisory_rule(rule_id) ON DELETE NO ACTION,
    advisory_type_cd     VARCHAR(50)   NOT NULL,
    severity_cd          VARCHAR(50)   NOT NULL,
    advr_status_cd       VARCHAR(50)   NOT NULL,
    advr_title           VARCHAR(200)  NOT NULL,
    advr_summary         TEXT,
    advr_payload         JSONB,
    target_reviewer_id   BIGINT,
    generated_at         TIMESTAMPTZ   NOT NULL,
    first_viewed_at      TIMESTAMPTZ,
    resolved_at          TIMESTAMPTZ,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by           BIGINT        NOT NULL,
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by           BIGINT        NOT NULL,
    deleted_at           TIMESTAMPTZ,
    deleted_by           BIGINT,
    version              INT           NOT NULL DEFAULT 0,
    quarantined_at       TIMESTAMPTZ
);
CREATE INDEX idx_review_advisory_report_rev
    ON review_advisory_report (rev_id);
CREATE INDEX idx_review_advisory_report_reviewer_status
    ON review_advisory_report (target_reviewer_id, advr_status_cd)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_review_advisory_report_unresolved_critical
    ON review_advisory_report (rev_id, severity_cd, advr_status_cd)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_review_advisory_report_quarantine
    ON review_advisory_report (quarantined_at DESC)
    WHERE advr_status_cd = 'QUARANTINE' AND deleted_at IS NULL;

CREATE TABLE review_advisory_signal (
    advs_id                BIGSERIAL     PRIMARY KEY,
    advr_id                BIGINT        NOT NULL REFERENCES review_advisory_report(advr_id) ON DELETE NO ACTION,
    signal_kind_cd         VARCHAR(50)   NOT NULL,
    signal_metric          VARCHAR(100)  NOT NULL,
    observed_value         DECIMAL(20,6),
    threshold_value        DECIMAL(20,6),
    peer_baseline_value    DECIMAL(20,6),
    sample_size            INT,
    signal_detail          JSONB,
    observed_window_start  VARCHAR(8),
    observed_window_end    VARCHAR(8),
    observed_at            TIMESTAMPTZ   NOT NULL,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by             BIGINT        NOT NULL
);
CREATE INDEX idx_review_advisory_signal_advr
    ON review_advisory_signal (advr_id);

CREATE TABLE review_advisory_ack (
    advk_id              BIGSERIAL     PRIMARY KEY,
    advr_id              BIGINT        NOT NULL REFERENCES review_advisory_report(advr_id) ON DELETE NO ACTION,
    ack_reviewer_id      BIGINT        NOT NULL,
    ack_response_cd      VARCHAR(50)   NOT NULL,
    decision_change_yn   CHAR(1)       NOT NULL DEFAULT 'N',
    ack_reason_cd        VARCHAR(50),
    ack_remark           VARCHAR(500),
    before_decision_cd   VARCHAR(50),
    after_decision_cd    VARCHAR(50),
    acked_at             TIMESTAMPTZ   NOT NULL,
    client_ip            VARCHAR(64),
    device               VARCHAR(200),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by           BIGINT        NOT NULL
);
CREATE INDEX idx_review_advisory_ack_advr
    ON review_advisory_ack (advr_id, acked_at);

CREATE TABLE reviewer_decision_snapshot (
    rds_id                      BIGSERIAL     PRIMARY KEY,
    reviewer_id                 BIGINT        NOT NULL,
    snapshot_date               VARCHAR(8)    NOT NULL,
    aggregation_window_cd       VARCHAR(50)   NOT NULL,
    cohort_dimension_cd         VARCHAR(50)   NOT NULL,
    cohort_value                VARCHAR(100)  NOT NULL,
    total_review_count          INT           NOT NULL DEFAULT 0,
    approve_count               INT           NOT NULL DEFAULT 0,
    reject_count                INT           NOT NULL DEFAULT 0,
    pending_count               INT           NOT NULL DEFAULT 0,
    approve_rate_bps            INT           NOT NULL DEFAULT 0,
    reject_rate_bps             INT           NOT NULL DEFAULT 0,
    peer_avg_reject_rate_bps    INT,
    deviation_sigma             DECIMAL(10,4),
    snapshotted_at              TIMESTAMPTZ   NOT NULL,
    created_at                  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by                  BIGINT        NOT NULL
);
CREATE INDEX idx_reviewer_decision_snapshot_reviewer_date
    ON reviewer_decision_snapshot (reviewer_id, snapshot_date);
CREATE UNIQUE INDEX uk_reviewer_decision_snapshot_unit
    ON reviewer_decision_snapshot
       (reviewer_id, snapshot_date, aggregation_window_cd, cohort_dimension_cd, cohort_value);

-- ---- advisory-service V21__advisory_rag_tables.sql ----

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE advisory_document (
    doc_id                BIGSERIAL    PRIMARY KEY,
    doc_cd                VARCHAR(50)  NOT NULL,
    doc_title             VARCHAR(500) NOT NULL,
    doc_category_cd       VARCHAR(50)  NOT NULL,
    doc_version           VARCHAR(50)  NOT NULL,
    effective_start_date  VARCHAR(8),
    effective_end_date    VARCHAR(8),
    source_uri            VARCHAR(500),
    active_yn             CHAR(1)      NOT NULL DEFAULT 'N',
    doc_desc              VARCHAR(500),
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by            BIGINT       NOT NULL,
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by            BIGINT       NOT NULL,
    deleted_at            TIMESTAMPTZ,
    deleted_by            BIGINT,
    version               INT          NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_advisory_document_cd_version
    ON advisory_document (doc_cd, doc_version)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_advisory_document_active_effective
    ON advisory_document (active_yn, effective_start_date, effective_end_date)
    WHERE deleted_at IS NULL;

CREATE TABLE advisory_document_chunk (
    chunk_id            BIGSERIAL     PRIMARY KEY,
    doc_id              BIGINT        NOT NULL REFERENCES advisory_document(doc_id) ON DELETE NO ACTION,
    chunk_seq           INT           NOT NULL,
    chunk_text          TEXT          NOT NULL,
    section_path        VARCHAR(500),
    chunk_token_count   INT,
    embedding_model_cd  VARCHAR(50)   NOT NULL,
    embedding           VECTOR(1536),
    chunk_meta          JSONB,
    indexed_at          TIMESTAMPTZ   NOT NULL,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by          BIGINT        NOT NULL
);
CREATE INDEX idx_advisory_document_chunk_doc
    ON advisory_document_chunk (doc_id, chunk_seq);
CREATE INDEX idx_advisory_document_chunk_model
    ON advisory_document_chunk (embedding_model_cd);
CREATE INDEX idx_advisory_document_chunk_embedding
    ON advisory_document_chunk USING ivfflat (embedding vector_cosine_ops) WITH (lists = 10);

CREATE TABLE advisory_case_index (
    case_idx_id                BIGSERIAL    PRIMARY KEY,
    rev_id                     BIGINT       NOT NULL REFERENCES loan_review(rev_id) ON DELETE NO ACTION,
    decision_cd                VARCHAR(50)  NOT NULL,
    overturn_yn                CHAR(1)      NOT NULL DEFAULT 'N',
    credit_score               INT,
    credit_score_band_cd       VARCHAR(50),
    dsr_ratio_bps              INT,
    ltv_ratio_bps              INT,
    cohort_age_band_cd         VARCHAR(50),
    cohort_employment_type_cd  VARCHAR(50),
    cohort_loan_purpose_cd     VARCHAR(50),
    summary_text               TEXT,
    embedding_model_cd         VARCHAR(50)  NOT NULL,
    embedding                  VECTOR(1536),
    indexed_at                 TIMESTAMPTZ  NOT NULL,
    created_at                 TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by                 BIGINT       NOT NULL
);
CREATE INDEX idx_advisory_case_index_rev
    ON advisory_case_index (rev_id);
CREATE INDEX idx_advisory_case_index_decision_overturn
    ON advisory_case_index (decision_cd, overturn_yn);
CREATE INDEX idx_advisory_case_index_embedding
    ON advisory_case_index USING ivfflat (embedding vector_cosine_ops) WITH (lists = 10);

CREATE TABLE advisory_retrieval_log (
    retr_id                  BIGSERIAL     PRIMARY KEY,
    advr_id                  BIGINT        REFERENCES review_advisory_report(advr_id) ON DELETE NO ACTION,
    retrieval_kind_cd        VARCHAR(50)   NOT NULL,
    rule_cd                  VARCHAR(50),
    query_text               TEXT,
    query_embedding_model_cd VARCHAR(50)   NOT NULL,
    result_count             INT           NOT NULL DEFAULT 0,
    top_score                DECIMAL(10,6),
    result_detail            JSONB,
    requested_by             BIGINT,
    requested_at             TIMESTAMPTZ   NOT NULL,
    created_at               TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by               BIGINT        NOT NULL
);
CREATE INDEX idx_advisory_retrieval_log_advr
    ON advisory_retrieval_log (advr_id, requested_at);
CREATE INDEX idx_advisory_retrieval_log_kind
    ON advisory_retrieval_log (retrieval_kind_cd, requested_at);

-- ---- advisory-service V22__audit_agent_tables.sql ----
-- cited_chunk_ids: V26__audit_cited_chunks.sql 에서 추가

CREATE TABLE ai_audit_opinion (
    opinion_id          BIGSERIAL     PRIMARY KEY,
    advr_id             BIGINT,
    rev_id              BIGINT        NOT NULL,
    reviewer_id         BIGINT,
    analysis_type_cd    VARCHAR(50)   NOT NULL,
    conclusion_cd       VARCHAR(50)   NOT NULL,
    reasoning_summary   VARCHAR(2000),
    confidence_score    NUMERIC(5,4),
    input_tokens        INTEGER,
    output_tokens       INTEGER,
    generated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    cited_chunk_ids     TEXT
);
CREATE INDEX idx_ai_audit_opinion_advr     ON ai_audit_opinion (advr_id);
CREATE INDEX idx_ai_audit_opinion_reviewer ON ai_audit_opinion (reviewer_id, generated_at DESC);
CREATE INDEX idx_ai_audit_opinion_type     ON ai_audit_opinion (analysis_type_cd, conclusion_cd);

CREATE TABLE reviewer_risk_score (
    score_id            BIGSERIAL     PRIMARY KEY,
    reviewer_id         BIGINT        NOT NULL UNIQUE,
    bias_score          NUMERIC(5,2)  NOT NULL DEFAULT 0,
    compliance_score    NUMERIC(5,2)  NOT NULL DEFAULT 0,
    evaluation_count    INTEGER       NOT NULL DEFAULT 0,
    last_evaluated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_reviewer_risk_score_bias       ON reviewer_risk_score (bias_score DESC);
CREATE INDEX idx_reviewer_risk_score_compliance ON reviewer_risk_score (compliance_score DESC);

-- ============================================================
-- 끝.
-- ============================================================

-- ============================================================
-- SERVICE: payment-service  (DB: payment_db)
-- ============================================================

-- ---- V1__create_payment_instruction.sql ----
-- =============================================================
-- V1__create_payment_instruction.sql
-- 결제지시 마스터 테이블
-- PostgreSQL 16 / Flyway
-- =============================================================

CREATE TABLE payment_instruction (

    -- ── 식별자 ──────────────────────────────────────────────────────────────
    payment_instruction_id              VARCHAR(20)     NOT NULL,
    idempotency_key                     VARCHAR(50)     NOT NULL,

    -- ── 송신 (외부 도메인 참조 — FK 없음, 박제 의도) ─────────────────────────
    sender_user_id                      VARCHAR(20)     NULL,
    sender_account_id                   VARCHAR(20)     NULL,
    auth_token_id                       VARCHAR(20)     NULL,

    -- ── 역전/취소 원거래 참조 (self) ───────────────────────────────────────
    original_payment_id                 VARCHAR(20)     NULL,

    -- ── 거래 식별 ───────────────────────────────────────────────────────────
    transaction_no                      VARCHAR(30)     NOT NULL,

    -- ── 송신 스냅샷 (박제 — 변경 불가) ─────────────────────────────────────
    sender_account_no_snap              VARCHAR(30)     NOT NULL,
    sender_account_alias_snap           VARCHAR(60)     NULL,

    -- ── 수신 ───────────────────────────────────────────────────────────────
    receiver_bank_code                  CHAR(3)         NOT NULL,
    receiver_account_no                 VARCHAR(30)     NOT NULL,
    receiver_holder_name_snap           VARCHAR(60)     NOT NULL,
    holder_inquiry_at                   TIMESTAMP(3)    NOT NULL,

    -- ── 라우팅 ─────────────────────────────────────────────────────────────
    is_intra_bank                       BOOLEAN         NOT NULL,
    routing_network_type                VARCHAR(20)     NOT NULL,

    -- ── 금액 ───────────────────────────────────────────────────────────────
    transfer_amount                     DECIMAL(15,0)   NOT NULL,
    fee_amount                          DECIMAL(15,0)   NOT NULL        DEFAULT 0,

    -- ── 통장 표시 ──────────────────────────────────────────────────────────
    receiver_passbook_sender_display    VARCHAR(60)     NULL,
    receiver_memo                       VARCHAR(100)    NULL,
    sender_memo                         VARCHAR(100)    NULL,

    -- ── 상태 ───────────────────────────────────────────────────────────────
    status                              VARCHAR(20)     NOT NULL,
    failure_category                    VARCHAR(30)     NULL,

    -- ── 채널 ───────────────────────────────────────────────────────────────
    channel                             VARCHAR(20)     NOT NULL,

    -- ── 시각 ───────────────────────────────────────────────────────────────
    requested_at                        TIMESTAMP(3)    NOT NULL,
    completed_at                        TIMESTAMP(3)    NULL,
    business_date                       VARCHAR(8)      NOT NULL,
    next_retry_at                       TIMESTAMP(3)    NULL,
    next_timeout_at                     TIMESTAMP(3)    NULL,

    -- ── 낙관적락 ────────────────────────────────────────────────────────────
    version                             INT             NOT NULL        DEFAULT 0,

    -- ── 트리거/예약 ─────────────────────────────────────────────────────────
    trigger_source                      VARCHAR(20)     NOT NULL        DEFAULT 'USER',
    is_scheduled                        BOOLEAN         NOT NULL        DEFAULT FALSE,
    scheduled_execution_at              TIMESTAMP(3)    NULL,

    -- ── 등록/수정 (외부 도메인 직원 ID — FK 없음) ─────────────────────────
    first_registered_at                 TIMESTAMP(3)    NOT NULL        DEFAULT CURRENT_TIMESTAMP(3),
    first_registrant_id                 VARCHAR(20)     NULL,
    last_modified_at                    TIMESTAMP(3)    NOT NULL    DEFAULT CURRENT_TIMESTAMP(3),
    last_modifier_id                    VARCHAR(20)     NULL,


    -- ── 기본 키 ────────────────────────────────────────────────────────────
    CONSTRAINT pk_payment_instruction
        PRIMARY KEY (payment_instruction_id),

    -- ── 유니크 ─────────────────────────────────────────────────────────────
    CONSTRAINT uq_payment_instruction_idempotency_key
        UNIQUE (idempotency_key),
    CONSTRAINT uq_payment_instruction_transaction_no
        UNIQUE (transaction_no),
    CONSTRAINT uq_payment_instruction_auth_token_id
        UNIQUE (auth_token_id),

    -- ── 자기 참조 FK ───────────────────────────────────────────────────────
    CONSTRAINT fk_payment_instruction_original
        FOREIGN KEY (original_payment_id)
        REFERENCES payment_instruction (payment_instruction_id),

    -- ── 진행상태 CHECK (9개) ──────────────────────────────────────────────
    CONSTRAINT chk_payment_instruction_status
        CHECK (status IN (
            'DRAFT', 'AUTHORIZED', 'SCHEDULED', 'PROCESSING',
            'CLEARING', 'REVERSING', 'COMPLETED', 'FAILED', 'CANCELED'
        )),

    -- ── 실패분류 CHECK (14개, NULL 허용) ─────────────────────────────────
    CONSTRAINT chk_payment_instruction_failure_category
        CHECK (failure_category IS NULL OR failure_category IN (
            'INSUFFICIENT_BALANCE', 'LIMIT_EXCEEDED', 'AUTH_FAILED', 'OWNER_INQUIRY_FAILED',
            'KFTC_REJECTED', 'KFTC_TIMEOUT', 'BOK_REJECTED', 'BOK_TIMEOUT',
            'INVALID_ACCOUNT', 'FRAUD_DETECTED', 'SYSTEM_ERROR',
            'ACCOUNT_RESTRICTED', 'ACCOUNT_NOT_FOUND', 'ACCOUNT_CLOSED'
        )),

    -- ── 채널 CHECK (6개) ─────────────────────────────────────────────────
    CONSTRAINT chk_payment_instruction_channel
        CHECK (channel IN ('WEB', 'MOBILE', 'BRANCH', 'ATM', 'OPEN_BANKING', 'INBOUND')),

    -- ── 트리거주체 CHECK (5개) ───────────────────────────────────────────
    CONSTRAINT chk_payment_instruction_trigger_source
        CHECK (trigger_source IN (
            'USER', 'AUTO_TRANSFER', 'SCHEDULER', 'OPERATOR', 'COUNTERPARTY_BANK'
        )),

    -- ── 라우팅망 CHECK (3개) ─────────────────────────────────────────────
    CONSTRAINT chk_payment_instruction_routing_network_type
        CHECK (routing_network_type IN ('INTERNAL', 'KFTC', 'BOK')),

    -- ── 금액 CHECK ───────────────────────────────────────────────────────
    CONSTRAINT chk_payment_instruction_transfer_amount
        CHECK (transfer_amount >= 0),
    CONSTRAINT chk_payment_instruction_fee_amount
        CHECK (fee_amount >= 0),

    -- ── version CHECK ────────────────────────────────────────────────────
    CONSTRAINT chk_payment_instruction_version
        CHECK (version >= 0),

    -- ── 예약 일관성 CHECK ────────────────────────────────────────────────
    CONSTRAINT chk_payment_instruction_scheduled_consistency
        CHECK (is_scheduled = FALSE OR scheduled_execution_at IS NOT NULL)
);


-- ── 테이블/컬럼 한글 코멘트 (36개) ──────────────────────────────────────────

COMMENT ON TABLE payment_instruction IS '결제지시';

COMMENT ON COLUMN payment_instruction.payment_instruction_id            IS '결제지시번호';
COMMENT ON COLUMN payment_instruction.idempotency_key                   IS '연결된멱등키값';
COMMENT ON COLUMN payment_instruction.sender_user_id                    IS '송신고객번호';
COMMENT ON COLUMN payment_instruction.sender_account_id                 IS '송신계좌번호';
COMMENT ON COLUMN payment_instruction.auth_token_id                     IS '인증토큰번호';
COMMENT ON COLUMN payment_instruction.original_payment_id               IS '원거래참조';
COMMENT ON COLUMN payment_instruction.transaction_no                    IS '거래번호';
COMMENT ON COLUMN payment_instruction.sender_account_no_snap            IS '송신계좌번호_스냅샷';
COMMENT ON COLUMN payment_instruction.sender_account_alias_snap         IS '송신계좌별명_스냅샷';
COMMENT ON COLUMN payment_instruction.receiver_bank_code                IS '수신은행코드';
COMMENT ON COLUMN payment_instruction.receiver_account_no               IS '수신계좌번호';
COMMENT ON COLUMN payment_instruction.receiver_holder_name_snap         IS '수신예금주명_스냅샷';
COMMENT ON COLUMN payment_instruction.holder_inquiry_at                 IS '예금주조회시각';
COMMENT ON COLUMN payment_instruction.is_intra_bank                     IS '자행이체여부';
COMMENT ON COLUMN payment_instruction.routing_network_type              IS '라우팅망종류';
COMMENT ON COLUMN payment_instruction.transfer_amount                   IS '이체금액';
COMMENT ON COLUMN payment_instruction.fee_amount                        IS '수수료';
COMMENT ON COLUMN payment_instruction.receiver_passbook_sender_display  IS '수신통장_송신자표시명';
COMMENT ON COLUMN payment_instruction.receiver_memo                     IS '받는분통장메모';
COMMENT ON COLUMN payment_instruction.sender_memo                       IS '내통장메모';
COMMENT ON COLUMN payment_instruction.status                            IS '진행상태';
COMMENT ON COLUMN payment_instruction.failure_category                  IS '실패분류';
COMMENT ON COLUMN payment_instruction.channel                           IS '채널';
COMMENT ON COLUMN payment_instruction.requested_at                      IS '요청시각';
COMMENT ON COLUMN payment_instruction.completed_at                      IS '완료시각';
COMMENT ON COLUMN payment_instruction.business_date                     IS '영업일자';
COMMENT ON COLUMN payment_instruction.next_retry_at                     IS '다음재시도시각';
COMMENT ON COLUMN payment_instruction.next_timeout_at                   IS '다음타임아웃시각';
COMMENT ON COLUMN payment_instruction.version                           IS '낙관적락버전';
COMMENT ON COLUMN payment_instruction.trigger_source                    IS '트리거주체';
COMMENT ON COLUMN payment_instruction.is_scheduled                      IS '예약여부';
COMMENT ON COLUMN payment_instruction.scheduled_execution_at            IS '예약실행시각';
COMMENT ON COLUMN payment_instruction.first_registered_at               IS '최초등록일시';
COMMENT ON COLUMN payment_instruction.first_registrant_id               IS '최초등록자식별번호';
COMMENT ON COLUMN payment_instruction.last_modified_at                  IS '최종수정일시';
COMMENT ON COLUMN payment_instruction.last_modifier_id                  IS '최종수정자식별번호';

-- ---- V2__create_idempotency_key.sql ----
-- =============================================================
-- V2__create_idempotency_key.sql
-- 멱등키 테이블 + payment_instruction FK 마무리
-- PostgreSQL 16 / Flyway
-- =============================================================

CREATE TABLE idempotency_key (

    -- ── 식별자 ──────────────────────────────────────────────────────────────
    idempotency_key                     VARCHAR(50)     NOT NULL,

    -- ── 클라이언트 ─────────────────────────────────────────────────────────
    client_id                           VARCHAR(30)     NOT NULL,

    -- ── 요청 무결성 ────────────────────────────────────────────────────────
    request_hash                        VARCHAR(100)    NOT NULL,

    -- ── 상태 ───────────────────────────────────────────────────────────────
    idempotency_status                  VARCHAR(20)     NOT NULL        DEFAULT 'PROCESSING',

    -- ── 응답 박제 ──────────────────────────────────────────────────────────
    first_response_snap                 JSONB           NULL,

    -- ── 재시도 ─────────────────────────────────────────────────────────────
    retry_count                         INT             NOT NULL        DEFAULT 0,

    -- ── 시각 ───────────────────────────────────────────────────────────────
    first_received_at                   TIMESTAMP(3)    NOT NULL,
    last_received_at                    TIMESTAMP(3)    NOT NULL,
    expires_at                          TIMESTAMP(3)    NOT NULL,

    -- ── 등록/수정 (외부 도메인 직원 ID — FK 없음) ─────────────────────────
    first_registered_at                 TIMESTAMP(3)    NOT NULL        DEFAULT CURRENT_TIMESTAMP(3),
    first_registrant_id                 VARCHAR(20)     NULL,
    last_modified_at                    TIMESTAMP(3)    NOT NULL        DEFAULT CURRENT_TIMESTAMP(3),
    last_modifier_id                    VARCHAR(20)     NULL,


    -- ── 기본 키 ────────────────────────────────────────────────────────────
    CONSTRAINT pk_idempotency_key
        PRIMARY KEY (idempotency_key),

    -- ── 유니크 (client_id + idempotency_key 조합) ─────────────────────────
    CONSTRAINT uq_idempotency_key_client_combo
        UNIQUE (client_id, idempotency_key),

    -- ── 멱등키상태 CHECK (3개) ────────────────────────────────────────────
    CONSTRAINT chk_idempotency_key_status
        CHECK (idempotency_status IN ('PROCESSING', 'COMPLETED', 'FAILED')),

    -- ── retry_count CHECK ────────────────────────────────────────────────
    CONSTRAINT chk_idempotency_key_retry_count
        CHECK (retry_count >= 0)
);


-- ── V1 payment_instruction → idempotency_key FK 마무리 ──────────────────────
-- (V1 작성 시점엔 idempotency_key 테이블 미존재. V2에서 테이블 생성 후 ALTER.)

ALTER TABLE payment_instruction
    ADD CONSTRAINT fk_payment_instruction_idempotency
    FOREIGN KEY (idempotency_key)
    REFERENCES idempotency_key (idempotency_key);


-- ── 테이블/컬럼 한글 코멘트 (13개) ──────────────────────────────────────────

COMMENT ON TABLE idempotency_key IS '멱등키';

COMMENT ON COLUMN idempotency_key.idempotency_key       IS '멱등키값';
COMMENT ON COLUMN idempotency_key.client_id             IS '클라이언트식별자';
COMMENT ON COLUMN idempotency_key.request_hash          IS '요청내용해시';
COMMENT ON COLUMN idempotency_key.idempotency_status    IS '멱등키상태';
COMMENT ON COLUMN idempotency_key.first_response_snap   IS '첫응답스냅샷';
COMMENT ON COLUMN idempotency_key.retry_count           IS '재시도횟수';
COMMENT ON COLUMN idempotency_key.first_received_at     IS '최초수신시각';
COMMENT ON COLUMN idempotency_key.last_received_at      IS '마지막수신시각';
COMMENT ON COLUMN idempotency_key.expires_at            IS '만료시각';
COMMENT ON COLUMN idempotency_key.first_registered_at   IS '최초등록일시';
COMMENT ON COLUMN idempotency_key.first_registrant_id   IS '최초등록자식별번호';
COMMENT ON COLUMN idempotency_key.last_modified_at      IS '최종수정일시';
COMMENT ON COLUMN idempotency_key.last_modifier_id      IS '최종수정자식별번호';

-- ---- V3__create_ledger.sql ----
-- =============================================================
-- V3__create_ledger.sql
-- 계좌원장 테이블
-- PostgreSQL 16 / Flyway
-- ★ spec 본문 결함 처리:
--   - debit_credit: spec CHAR(6) → VARCHAR(20) (enum 컬럼 일관성)
--   - 테이블명: spec 'account_ledger' → 'ledger' (타 산출물 전체 기준)
-- =============================================================

CREATE TABLE ledger (

    -- ── 식별자 ──────────────────────────────────────────────────────────────
    ledger_id                           VARCHAR(20)     NOT NULL,

    -- ── 결제지시 참조 (이자/수기분개 시 NULL) ─────────────────────────────
    payment_instruction_id              VARCHAR(20)     NULL,

    -- ── 계좌 (외부 도메인 — FK 없음) ──────────────────────────────────────
    account_id                          VARCHAR(20)     NOT NULL,

    -- ── 역분개 원분개 참조 (self) ─────────────────────────────────────────
    original_ledger_id                  VARCHAR(20)     NULL,

    -- ── 분개 그룹 ──────────────────────────────────────────────────────────
    journal_no                          VARCHAR(20)     NOT NULL,

    -- ── 계좌 스냅샷 (박제 — 변경 불가) ────────────────────────────────────
    account_no_snap                     VARCHAR(30)     NOT NULL,
    holder_name_snap                    VARCHAR(60)     NOT NULL,

    -- ── 회계 구분 ──────────────────────────────────────────────────────────
    debit_credit                        VARCHAR(20)     NOT NULL,
    journal_type                        VARCHAR(30)     NOT NULL,

    -- ── 금액/잔액 ──────────────────────────────────────────────────────────
    amount                              DECIMAL(15,0)   NOT NULL,
    currency                            CHAR(3)         NOT NULL        DEFAULT 'KRW',
    balance_before                      DECIMAL(15,0)   NOT NULL,
    balance_after                       DECIMAL(15,0)   NOT NULL,

    -- ── 상대방 스냅샷 (박제, NULL 허용) ───────────────────────────────────
    counterparty_account_no_snap        VARCHAR(30)     NULL,
    counterparty_bank_code_snap         CHAR(3)         NULL,
    counterparty_holder_name_snap       VARCHAR(60)     NULL,

    -- ── 일자 ───────────────────────────────────────────────────────────────
    transaction_date                    VARCHAR(8)      NOT NULL,
    posting_date                        VARCHAR(8)      NOT NULL,
    value_date                          VARCHAR(8)      NOT NULL,

    -- ── 기장 시각/적요 ─────────────────────────────────────────────────────
    posted_at                           TIMESTAMP(3)    NOT NULL,
    system_description                  VARCHAR(100)    NOT NULL,
    passbook_memo_snap                  VARCHAR(100)    NULL,

    -- ── 역분개 ─────────────────────────────────────────────────────────────
    is_reversal                         BOOLEAN         NOT NULL        DEFAULT FALSE,
    reversal_reason                     VARCHAR(20)     NULL,

    -- ── 기장 상태 ──────────────────────────────────────────────────────────
    posting_status                      VARCHAR(20)     NOT NULL        DEFAULT 'PENDING',

    -- ── 등록/수정 (외부 도메인 직원 ID — FK 없음) ─────────────────────────
    first_registered_at                 TIMESTAMP(3)    NOT NULL        DEFAULT CURRENT_TIMESTAMP(3),
    first_registrant_id                 VARCHAR(20)     NULL,
    last_modified_at                    TIMESTAMP(3)    NOT NULL        DEFAULT CURRENT_TIMESTAMP(3),
    last_modifier_id                    VARCHAR(20)     NULL,


    -- ── 기본 키 ────────────────────────────────────────────────────────────
    CONSTRAINT pk_ledger
        PRIMARY KEY (ledger_id),

    -- ── FK: 결제지시 (NULL 허용) ───────────────────────────────────────────
    CONSTRAINT fk_ledger_payment_instruction
        FOREIGN KEY (payment_instruction_id)
        REFERENCES payment_instruction (payment_instruction_id),

    -- ── FK: 원분개 self (NULL 허용) ────────────────────────────────────────
    CONSTRAINT fk_ledger_original
        FOREIGN KEY (original_ledger_id)
        REFERENCES ledger (ledger_id),

    -- ── 차변대변 CHECK (2개) ──────────────────────────────────────────────
    CONSTRAINT chk_ledger_debit_credit
        CHECK (debit_credit IN ('DEBIT', 'CREDIT')),

    -- ── 분개종류 CHECK (9개) ──────────────────────────────────────────────
    CONSTRAINT chk_ledger_journal_type
        CHECK (journal_type IN (
            'TRANSFER_OUT', 'TRANSFER_IN', 'CLEARING_PENDING', 'FEE', 'FEE_INCOME',
            'REVERSAL_TRANSFER_OUT', 'REVERSAL_CLEARING_PENDING',
            'REVERSAL_FEE', 'REVERSAL_FEE_INCOME'
        )),

    -- ── 기장상태 CHECK (3개) ──────────────────────────────────────────────
    CONSTRAINT chk_ledger_posting_status
        CHECK (posting_status IN ('PENDING', 'POSTED', 'CANCELED')),

    -- ── 역분개사유 CHECK (7개, NULL 허용) ────────────────────────────────
    CONSTRAINT chk_ledger_reversal_reason
        CHECK (reversal_reason IS NULL OR reversal_reason IN (
            'PUBLISH_FAILURE', 'SYSTEM_FAILURE', 'COMPENSATION',
            'KFTC_REJECTION', 'BOK_REJECTION', 'SETTLEMENT_FAILURE', 'OPERATOR'
        )),

    -- ── 금액 CHECK ───────────────────────────────────────────────────────
    CONSTRAINT chk_ledger_amount
        CHECK (amount >= 0),
    CONSTRAINT chk_ledger_balance_before
        CHECK (balance_before >= 0),
    CONSTRAINT chk_ledger_balance_after
        CHECK (balance_after >= 0),

    -- ── 역분개 일관성 CHECK ───────────────────────────────────────────────
    CONSTRAINT chk_ledger_reversal_original_consistency
        CHECK (is_reversal = FALSE OR original_ledger_id IS NOT NULL),
    CONSTRAINT chk_ledger_reversal_reason_consistency
        CHECK (is_reversal = FALSE OR reversal_reason IS NOT NULL)
);


-- ── 테이블/컬럼 한글 코멘트 (29개) ──────────────────────────────────────────

COMMENT ON TABLE ledger IS '계좌원장';

COMMENT ON COLUMN ledger.ledger_id                          IS '분개번호';
COMMENT ON COLUMN ledger.payment_instruction_id             IS '결제지시번호';
COMMENT ON COLUMN ledger.account_id                         IS '계좌번호';
COMMENT ON COLUMN ledger.original_ledger_id                 IS '원분개참조';
COMMENT ON COLUMN ledger.journal_no                         IS '회계번호';
COMMENT ON COLUMN ledger.account_no_snap                    IS '계좌번호_스냅샷';
COMMENT ON COLUMN ledger.holder_name_snap                   IS '예금주명_스냅샷';
COMMENT ON COLUMN ledger.debit_credit                       IS '차변대변구분';
COMMENT ON COLUMN ledger.journal_type                       IS '분개종류';
COMMENT ON COLUMN ledger.amount                             IS '금액';
COMMENT ON COLUMN ledger.currency                           IS '통화';
COMMENT ON COLUMN ledger.balance_before                     IS '분개직전잔액';
COMMENT ON COLUMN ledger.balance_after                      IS '분개직후잔액';
COMMENT ON COLUMN ledger.counterparty_account_no_snap       IS '상대계좌번호_스냅샷';
COMMENT ON COLUMN ledger.counterparty_bank_code_snap        IS '상대은행코드_스냅샷';
COMMENT ON COLUMN ledger.counterparty_holder_name_snap      IS '상대예금주명_스냅샷';
COMMENT ON COLUMN ledger.transaction_date                   IS '거래일자';
COMMENT ON COLUMN ledger.posting_date                       IS '기장일자';
COMMENT ON COLUMN ledger.value_date                         IS '자금가용일';
COMMENT ON COLUMN ledger.posted_at                          IS '기장시각';
COMMENT ON COLUMN ledger.system_description                 IS '시스템적요';
COMMENT ON COLUMN ledger.passbook_memo_snap                 IS '통장에찍히는메모_스냅샷';
COMMENT ON COLUMN ledger.is_reversal                        IS '역분개여부';
COMMENT ON COLUMN ledger.reversal_reason                    IS '역분개사유';
COMMENT ON COLUMN ledger.posting_status                     IS '기장상태';
COMMENT ON COLUMN ledger.first_registered_at                IS '최초등록일시';
COMMENT ON COLUMN ledger.first_registrant_id                IS '최초등록자식별번호';
COMMENT ON COLUMN ledger.last_modified_at                   IS '최종수정일시';
COMMENT ON COLUMN ledger.last_modifier_id                   IS '최종수정자식별번호';

-- ---- V4__create_external_call.sql ----
-- =============================================================
-- V4__create_external_call.sql
-- 외부호출 테이블
-- PostgreSQL 16 / Flyway
-- ★ spec 비고:
--   - INBOUND_RESPONSE: v6 deprecated, 데이터 호환용으로 enum 유지
--   - BALANCE_WITHDRAW / BALANCE_DEPOSIT / LIMIT_CONSUME: spec 시트 12개 기준으로만 박음
-- =============================================================

CREATE TABLE external_call (

    -- ── 식별자 ──────────────────────────────────────────────────────────────
    call_id                             VARCHAR(20)     NOT NULL,
    call_idempotency_key                VARCHAR(150)    NOT NULL,

    -- ── 보상/재시도 계보 ────────────────────────────────────────────────────
    compensation_type                   VARCHAR(20)     NOT NULL        DEFAULT 'ORIGINAL',
    compensation_target_call_id         VARCHAR(20)     NULL,
    payment_instruction_id              VARCHAR(20)     NULL,
    parent_call_id                      VARCHAR(20)     NULL,

    -- ── 호출 컨텍스트 (외부 도메인 — FK 없음) ─────────────────────────────
    session_id                          VARCHAR(50)     NULL,
    user_id                             VARCHAR(20)     NULL,

    -- ── 호출 명세 ──────────────────────────────────────────────────────────
    call_type                           VARCHAR(30)     NOT NULL,
    target_system                       VARCHAR(50)     NOT NULL,
    endpoint_url                        VARCHAR(500)    NOT NULL,
    http_method                         VARCHAR(10)     NOT NULL,
    request_id                          VARCHAR(50)     NOT NULL,

    -- ── 요청 원본 박제 ─────────────────────────────────────────────────────
    request_header                      JSONB           NULL,
    request_body                        JSONB           NULL,
    request_body_hash                   VARCHAR(100)    NULL,

    -- ── 응답 원본 박제 ─────────────────────────────────────────────────────
    response_status_code                INT             NULL,
    response_header                     JSONB           NULL,
    response_body                       JSONB           NULL,
    business_response_code              VARCHAR(10)     NULL,
    response_message                    VARCHAR(500)    NULL,

    -- ── 결과/재시도 ────────────────────────────────────────────────────────
    result                              VARCHAR(20)     NOT NULL,
    attempt_no                          INT             NOT NULL        DEFAULT 1,

    -- ── 시각/성능 ──────────────────────────────────────────────────────────
    requested_at                        TIMESTAMP(3)    NOT NULL,
    responded_at                        TIMESTAMP(3)    NULL,
    response_time_ms                    INT             NULL,
    timeout_ms                          INT             NOT NULL,

    -- ── 등록/수정 (외부 도메인 직원 ID — FK 없음) ─────────────────────────
    first_registered_at                 TIMESTAMP(3)    NOT NULL        DEFAULT CURRENT_TIMESTAMP(3),
    first_registrant_id                 VARCHAR(20)     NULL,
    last_modified_at                    TIMESTAMP(3)    NOT NULL        DEFAULT CURRENT_TIMESTAMP(3),
    last_modifier_id                    VARCHAR(20)     NULL,


    -- ── 기본 키 ────────────────────────────────────────────────────────────
    CONSTRAINT pk_external_call
        PRIMARY KEY (call_id),

    -- ── 유니크 ─────────────────────────────────────────────────────────────
    CONSTRAINT uq_external_call_idempotency_key
        UNIQUE (call_idempotency_key),
    CONSTRAINT uq_external_call_request_id
        UNIQUE (request_id),

    -- ── FK: 결제지시 (NULL 허용 — 예금주조회는 결제지시 생성 전) ─────────
    CONSTRAINT fk_external_call_payment_instruction
        FOREIGN KEY (payment_instruction_id)
        REFERENCES payment_instruction (payment_instruction_id),

    -- ── FK: 보상대상 self (NULL 허용) ──────────────────────────────────────
    CONSTRAINT fk_external_call_compensation_target
        FOREIGN KEY (compensation_target_call_id)
        REFERENCES external_call (call_id),

    -- ── FK: 부모호출 self (NULL 허용) ──────────────────────────────────────
    CONSTRAINT fk_external_call_parent
        FOREIGN KEY (parent_call_id)
        REFERENCES external_call (call_id),

    -- ── 보상유형 CHECK (3개) ──────────────────────────────────────────────
    CONSTRAINT chk_external_call_compensation_type
        CHECK (compensation_type IN ('ORIGINAL', 'RETRY', 'COMPENSATION')),

    -- ── 호출종류 CHECK (12개) ─────────────────────────────────────────────
    CONSTRAINT chk_external_call_call_type
        CHECK (call_type IN (
            'ACCOUNT_OWNER_INQUIRY', 'BALANCE_INQUIRY', 'LIMIT_CHECK', 'AUTH_VERIFY',
            'FRAUD_CHECK', 'KFTC_GATEWAY', 'BOK_GATEWAY', 'INBOUND_RESPONSE',
            'BALANCE_WITHDRAW_CANCEL', 'BALANCE_DEPOSIT_CANCEL',
            'LIMIT_CONSUME_CANCEL', 'AUTH_REVOKE'
        )),

    -- ── HTTP메서드 CHECK (4개) ────────────────────────────────────────────
    CONSTRAINT chk_external_call_http_method
        CHECK (http_method IN ('GET', 'POST', 'PUT', 'DELETE')),

    -- ── 결과 CHECK (4개) ─────────────────────────────────────────────────
    CONSTRAINT chk_external_call_result
        CHECK (result IN ('SUCCESS', 'FAIL', 'TIMEOUT', 'NETWORK_ERROR')),

    -- ── 카운터 CHECK ─────────────────────────────────────────────────────
    CONSTRAINT chk_external_call_attempt_no
        CHECK (attempt_no >= 1),
    CONSTRAINT chk_external_call_response_time_ms
        CHECK (response_time_ms IS NULL OR response_time_ms >= 0),
    CONSTRAINT chk_external_call_timeout_ms
        CHECK (timeout_ms >= 0),

    -- ── 보상/재시도 일관성 CHECK ──────────────────────────────────────────
    CONSTRAINT chk_external_call_compensation_consistency
        CHECK (compensation_type <> 'COMPENSATION' OR compensation_target_call_id IS NOT NULL),
    CONSTRAINT chk_external_call_retry_consistency
        CHECK (compensation_type <> 'RETRY' OR parent_call_id IS NOT NULL)
);


-- ── 테이블/컬럼 한글 코멘트 (31개) ──────────────────────────────────────────

COMMENT ON TABLE external_call IS '외부호출';

COMMENT ON COLUMN external_call.call_id                         IS '외부호출번호';
COMMENT ON COLUMN external_call.call_idempotency_key            IS '호출멱등키';
COMMENT ON COLUMN external_call.compensation_type               IS '보상유형';
COMMENT ON COLUMN external_call.compensation_target_call_id     IS '보상대상호출번호';
COMMENT ON COLUMN external_call.payment_instruction_id          IS '결제지시번호';
COMMENT ON COLUMN external_call.parent_call_id                  IS '부모호출번호';
COMMENT ON COLUMN external_call.session_id                      IS '세션ID';
COMMENT ON COLUMN external_call.user_id                         IS '고객번호';
COMMENT ON COLUMN external_call.call_type                       IS '호출종류';
COMMENT ON COLUMN external_call.target_system                   IS '대상시스템';
COMMENT ON COLUMN external_call.endpoint_url                    IS '엔드포인트URL';
COMMENT ON COLUMN external_call.http_method                     IS 'HTTP메서드';
COMMENT ON COLUMN external_call.request_id                      IS '요청ID';
COMMENT ON COLUMN external_call.request_header                  IS '요청헤더';
COMMENT ON COLUMN external_call.request_body                    IS '요청본문';
COMMENT ON COLUMN external_call.request_body_hash               IS '요청본문해시';
COMMENT ON COLUMN external_call.response_status_code            IS '응답상태코드';
COMMENT ON COLUMN external_call.response_header                 IS '응답헤더';
COMMENT ON COLUMN external_call.response_body                   IS '응답본문';
COMMENT ON COLUMN external_call.business_response_code          IS '비즈니스응답코드';
COMMENT ON COLUMN external_call.response_message                IS '응답메시지';
COMMENT ON COLUMN external_call.result                          IS '결과';
COMMENT ON COLUMN external_call.attempt_no                      IS '시도번호';
COMMENT ON COLUMN external_call.requested_at                    IS '요청시각';
COMMENT ON COLUMN external_call.responded_at                    IS '응답시각';
COMMENT ON COLUMN external_call.response_time_ms                IS '응답시간_ms';
COMMENT ON COLUMN external_call.timeout_ms                      IS '타임아웃설정값';
COMMENT ON COLUMN external_call.first_registered_at             IS '최초등록일시';
COMMENT ON COLUMN external_call.first_registrant_id             IS '최초등록자식별번호';
COMMENT ON COLUMN external_call.last_modified_at                IS '최종수정일시';
COMMENT ON COLUMN external_call.last_modifier_id                IS '최종수정자식별번호';

-- ---- V5__create_outbox_message.sql ----
-- =============================================================
-- V5__create_outbox_message.sql
-- Outbox메시지 테이블
-- PostgreSQL 16 / Flyway
-- =============================================================

CREATE TABLE outbox_message (

    -- ── 식별자 ──────────────────────────────────────────────────────────────
    message_id                          VARCHAR(20)     NOT NULL,

    -- ── 결제지시 참조 (항상 연관 — NOT NULL) ──────────────────────────────
    payment_instruction_id              VARCHAR(20)     NOT NULL,

    -- ── 이벤트 명세 ────────────────────────────────────────────────────────
    event_type                          VARCHAR(30)     NOT NULL,
    event_schema_version                VARCHAR(10)     NOT NULL,
    payload                             JSONB           NOT NULL,

    -- ── 발행 상태/재시도 ────────────────────────────────────────────────────
    publish_status                      VARCHAR(20)     NOT NULL        DEFAULT 'PENDING',
    attempt_count                       INT             NOT NULL        DEFAULT 0,

    -- ── 워커 폴링 기준 ─────────────────────────────────────────────────────
    available_at                        TIMESTAMP(3)    NOT NULL,
    last_error                          VARCHAR(500)    NULL,
    published_at                        TIMESTAMP(3)    NULL,

    -- ── 등록/수정 (외부 도메인 직원 ID — FK 없음) ─────────────────────────
    first_registered_at                 TIMESTAMP(3)    NOT NULL        DEFAULT CURRENT_TIMESTAMP(3),
    first_registrant_id                 VARCHAR(20)     NULL,
    last_modified_at                    TIMESTAMP(3)    NOT NULL        DEFAULT CURRENT_TIMESTAMP(3),
    last_modifier_id                    VARCHAR(20)     NULL,


    -- ── 기본 키 ────────────────────────────────────────────────────────────
    CONSTRAINT pk_outbox_message
        PRIMARY KEY (message_id),

    -- ── FK: 결제지시 (NOT NULL) ────────────────────────────────────────────
    CONSTRAINT fk_outbox_message_payment_instruction
        FOREIGN KEY (payment_instruction_id)
        REFERENCES payment_instruction (payment_instruction_id),

    -- ── 이벤트종류 CHECK (19개) ───────────────────────────────────────────
    CONSTRAINT chk_outbox_message_event_type
        CHECK (event_type IN (
            'PAYMENT_REQUESTED', 'PAYMENT_SCHEDULED', 'PAYMENT_SCHEDULE_CANCELED',
            'KFTC_REQUEST_SENT', 'KFTC_REJECTED',
            'BOK_REQUEST_SENT', 'BOK_REJECTED', 'BOK_CONFIRMED',
            'PAYMENT_REVERSED', 'PAYMENT_COMPLETED', 'PAYMENT_FAILED', 'PAYMENT_CANCELED',
            'INBOUND_RECEIVED',
            'KFTC_ACK_SENT', 'BOK_ACK_SENT',
            'KFTC_SETTLEMENT_SENT', 'BOK_CONFIRM_SENT',
            'KFTC_REJECT_SENT', 'BOK_REJECT_SENT'
        )),

    -- ── 발행상태 CHECK (3개) ──────────────────────────────────────────────
    CONSTRAINT chk_outbox_message_publish_status
        CHECK (publish_status IN ('PENDING', 'SENT', 'FAILED')),

    -- ── 시도횟수 CHECK ────────────────────────────────────────────────────
    CONSTRAINT chk_outbox_message_attempt_count
        CHECK (attempt_count >= 0)
);


-- ── 테이블/컬럼 한글 코멘트 (14개) ──────────────────────────────────────────

COMMENT ON TABLE outbox_message IS 'Outbox메시지';

COMMENT ON COLUMN outbox_message.message_id                 IS '메시지번호';
COMMENT ON COLUMN outbox_message.payment_instruction_id     IS '결제지시번호';
COMMENT ON COLUMN outbox_message.event_type                 IS '이벤트종류';
COMMENT ON COLUMN outbox_message.event_schema_version       IS '이벤트스키마버전';
COMMENT ON COLUMN outbox_message.payload                    IS '페이로드';
COMMENT ON COLUMN outbox_message.publish_status             IS '발행상태';
COMMENT ON COLUMN outbox_message.attempt_count              IS '시도횟수';
COMMENT ON COLUMN outbox_message.available_at               IS '처리가능시각';
COMMENT ON COLUMN outbox_message.last_error                 IS '마지막오류';
COMMENT ON COLUMN outbox_message.published_at               IS '발행시각';
COMMENT ON COLUMN outbox_message.first_registered_at        IS '최초등록일시';
COMMENT ON COLUMN outbox_message.first_registrant_id        IS '최초등록자식별번호';
COMMENT ON COLUMN outbox_message.last_modified_at           IS '최종수정일시';
COMMENT ON COLUMN outbox_message.last_modifier_id           IS '최종수정자식별번호';

-- ---- V6__create_status_history.sql ----
-- =============================================================
-- V6__create_status_history.sql
-- 상태이력 테이블 (append-only audit log)
-- PostgreSQL 16 / Flyway
-- =============================================================

CREATE TABLE status_history (

    -- ── 식별자 ──────────────────────────────────────────────────────────────
    history_id                          VARCHAR(20)     NOT NULL,

    -- ── 결제지시 참조 (항상 연관 — NOT NULL) ──────────────────────────────
    payment_instruction_id              VARCHAR(20)     NOT NULL,

    -- ── 관련 외부호출 (트리거가 외부호출인 경우만) ────────────────────────
    related_external_call_id            VARCHAR(20)     NULL,

    -- ── 순번 ───────────────────────────────────────────────────────────────
    sequence_in_payment                 INT             NOT NULL,

    -- ── 상태 전이 ──────────────────────────────────────────────────────────
    previous_status                     VARCHAR(20)     NULL,
    next_status                         VARCHAR(20)     NOT NULL,

    -- ── 이벤트 ─────────────────────────────────────────────────────────────
    event_type                          VARCHAR(30)     NOT NULL,
    reason_code                         VARCHAR(10)     NULL,
    reason_message                      VARCHAR(200)    NULL,

    -- ── 트리거 주체 (외부 도메인 사용자 — FK 없음) ────────────────────────
    triggered_by                        VARCHAR(20)     NOT NULL,
    operator_id                         VARCHAR(20)     NULL,

    -- ── 이벤트 페이로드 ────────────────────────────────────────────────────
    payload_snapshot                    JSONB           NULL,

    -- ── 시각 ───────────────────────────────────────────────────────────────
    event_occurred_at                   TIMESTAMP(3)    NOT NULL,
    db_recorded_at                      TIMESTAMP(3)    NOT NULL        DEFAULT CURRENT_TIMESTAMP(3),

    -- ── 등록/수정 (외부 도메인 직원 ID — FK 없음) ─────────────────────────
    first_registered_at                 TIMESTAMP(3)    NOT NULL        DEFAULT CURRENT_TIMESTAMP(3),
    first_registrant_id                 VARCHAR(20)     NULL,
    last_modified_at                    TIMESTAMP(3)    NOT NULL        DEFAULT CURRENT_TIMESTAMP(3),
    last_modifier_id                    VARCHAR(20)     NULL,


    -- ── 기본 키 ────────────────────────────────────────────────────────────
    CONSTRAINT pk_status_history
        PRIMARY KEY (history_id),

    -- ── 유니크: 결제지시 내 순번 조합 ─────────────────────────────────────
    CONSTRAINT uq_status_history_payment_sequence
        UNIQUE (payment_instruction_id, sequence_in_payment),

    -- ── FK: 결제지시 (NOT NULL) ────────────────────────────────────────────
    CONSTRAINT fk_status_history_payment_instruction
        FOREIGN KEY (payment_instruction_id)
        REFERENCES payment_instruction (payment_instruction_id),

    -- ── FK: 관련 외부호출 (NULL 허용) ─────────────────────────────────────
    CONSTRAINT fk_status_history_external_call
        FOREIGN KEY (related_external_call_id)
        REFERENCES external_call (call_id),

    -- ── 이전상태 CHECK (9개, NULL 허용) ───────────────────────────────────
    CONSTRAINT chk_status_history_previous_status
        CHECK (previous_status IS NULL OR previous_status IN (
            'DRAFT', 'AUTHORIZED', 'SCHEDULED', 'PROCESSING',
            'CLEARING', 'REVERSING', 'COMPLETED', 'FAILED', 'CANCELED'
        )),

    -- ── 다음상태 CHECK (9개) ──────────────────────────────────────────────
    CONSTRAINT chk_status_history_next_status
        CHECK (next_status IN (
            'DRAFT', 'AUTHORIZED', 'SCHEDULED', 'PROCESSING',
            'CLEARING', 'REVERSING', 'COMPLETED', 'FAILED', 'CANCELED'
        )),

    -- ── 이벤트종류 CHECK (37개) ───────────────────────────────────────────
    CONSTRAINT chk_status_history_event_type
        CHECK (event_type IN (
            'INSTRUCTION_CREATED',
            'OWNER_INQUIRY_DONE', 'OWNER_INQUIRY_FAILED',
            'AUTH_PASSED', 'AUTH_FAILED',
            'SCHEDULED_REGISTERED', 'SCHEDULED_TRIGGERED', 'SCHEDULED_CANCELED',
            'PROCESSING_STARTED',
            'BALANCE_CHECK_FAILED', 'LIMIT_CHECK_FAILED',
            'KFTC_REQUEST_SENT', 'KFTC_ACK_RECEIVED', 'KFTC_REJECT_RECEIVED', 'KFTC_SETTLED',
            'BOK_REQUEST_SENT', 'BOK_ACK_RECEIVED', 'BOK_REJECT_RECEIVED', 'BOK_CONFIRMED',
            'REVERSAL_STARTED', 'REVERSAL_COMPLETED',
            'PAYMENT_COMPLETED', 'PAYMENT_FAILED', 'PAYMENT_CANCELED',
            'INBOUND_REJECTED', 'INBOUND_RECEIVED',
            'COMPENSATION_STARTED', 'COMPENSATION_COMPLETED', 'COMPENSATION_FAILED',
            'KFTC_ACK_SENT', 'BOK_ACK_SENT',
            'KFTC_SETTLEMENT_SENT', 'BOK_CONFIRM_SENT',
            'KFTC_REJECT_SENT', 'BOK_REJECT_SENT',
            'INBOUND_VALIDATION_PASSED', 'INBOUND_VALIDATION_FAILED'
        )),

    -- ── 트리거주체 CHECK (6개) ────────────────────────────────────────────
    CONSTRAINT chk_status_history_triggered_by
        CHECK (triggered_by IN ('USER', 'SYSTEM', 'KFTC', 'BOK', 'OPERATOR', 'SCHEDULER')),

    -- ── 순번 CHECK ───────────────────────────────────────────────────────
    CONSTRAINT chk_status_history_sequence_in_payment
        CHECK (sequence_in_payment >= 1),

    -- ── 운영자 일관성 CHECK ───────────────────────────────────────────────
    CONSTRAINT chk_status_history_operator_consistency
        CHECK (triggered_by <> 'OPERATOR' OR operator_id IS NOT NULL)
);


-- ── 테이블/컬럼 한글 코멘트 (18개) ──────────────────────────────────────────

COMMENT ON TABLE status_history IS '상태이력';

COMMENT ON COLUMN status_history.history_id                 IS '상태이력번호';
COMMENT ON COLUMN status_history.payment_instruction_id     IS '결제지시번호';
COMMENT ON COLUMN status_history.related_external_call_id   IS '관련외부호출번호';
COMMENT ON COLUMN status_history.sequence_in_payment        IS '결제지시내순번';
COMMENT ON COLUMN status_history.previous_status            IS '이전상태';
COMMENT ON COLUMN status_history.next_status                IS '다음상태';
COMMENT ON COLUMN status_history.event_type                 IS '이벤트종류';
COMMENT ON COLUMN status_history.reason_code                IS '사유코드';
COMMENT ON COLUMN status_history.reason_message             IS '사유메시지';
COMMENT ON COLUMN status_history.triggered_by               IS '트리거주체';
COMMENT ON COLUMN status_history.operator_id                IS '운영자ID';
COMMENT ON COLUMN status_history.payload_snapshot           IS '페이로드스냅샷';
COMMENT ON COLUMN status_history.event_occurred_at          IS '이벤트발생시각';
COMMENT ON COLUMN status_history.db_recorded_at             IS 'DB기록시각';
COMMENT ON COLUMN status_history.first_registered_at        IS '최초등록일시';
COMMENT ON COLUMN status_history.first_registrant_id        IS '최초등록자식별번호';
COMMENT ON COLUMN status_history.last_modified_at           IS '최종수정일시';
COMMENT ON COLUMN status_history.last_modifier_id           IS '최종수정자식별번호';

-- ---- V7__add_external_call_types.sql ----
-- V7: external_call.call_type CHECK에 정상 원호출 3종 추가
-- ACCOUNT_INQUIRY(A-1), BALANCE_WITHDRAW(B-3), BALANCE_DEPOSIT(B-4)
-- 기존 _CANCEL 보상호출만 있고 원호출이 누락됐던 spec 버그 수정
-- LIMIT_CONSUME은 제외: deposit API 합의서 가정6(한도차감 일체화 — B-3 출금이
--   잔액+한도 동시 차감)에 따라 별도 호출이 발생하지 않음. 단 가정6은 deposit 팀
--   미확정(🟡)이므로, deposit이 "한도 별도 차감"으로 확정 시 B-3.5 LIMIT_CONSUME
--   부활 + 본 CHECK 재검토 필요.

ALTER TABLE external_call
    DROP CONSTRAINT IF EXISTS chk_external_call_call_type;

ALTER TABLE external_call
    ADD CONSTRAINT chk_external_call_call_type
        CHECK (call_type IN (
            'ACCOUNT_OWNER_INQUIRY', 'BALANCE_INQUIRY', 'LIMIT_CHECK', 'AUTH_VERIFY',
            'FRAUD_CHECK', 'KFTC_GATEWAY', 'BOK_GATEWAY', 'INBOUND_RESPONSE',
            'BALANCE_WITHDRAW_CANCEL', 'BALANCE_DEPOSIT_CANCEL', 'LIMIT_CONSUME_CANCEL', 'AUTH_REVOKE',
            'ACCOUNT_INQUIRY', 'BALANCE_WITHDRAW', 'BALANCE_DEPOSIT'
        ));

-- ---- V8__relax_pi_snapshot_not_null.sql ----
-- V8: payment_instruction 박제 컬럼 2종 NOT NULL 해제 (P-028 정합)
-- receiver_holder_name_snap / holder_inquiry_at 는 외부조회(Step2 A-2 예금주조회)
--   결과를 박제하는 사후값. P-028상 DRAFT INSERT(Step1 TX-1)는 외부조회 전이라
--   이 시점엔 값이 없음 → nullable이어야 정합.
-- 컬럼명세서 v12.2가 NOT NULL로 정의했으나 P-028(정책 시트13)과 모순. 정책 시트10
--   CHECK제약_정의에도 해당 NOT NULL 근거 없음. 컬럼명세서 v12.3에서 N→Y 수정 메모.
-- sender_account_no_snap은 입력값(송신계좌번호)이라 DRAFT에 존재 → NOT NULL 유지(건드리지 않음).

ALTER TABLE payment_instruction ALTER COLUMN receiver_holder_name_snap DROP NOT NULL;
ALTER TABLE payment_instruction ALTER COLUMN holder_inquiry_at DROP NOT NULL;

-- ---- V9__add_system_failure_detected_event_type.sql ----
-- =============================================================
-- V9__add_system_failure_detected_event_type.sql
-- F8(자행 입금실패 보상) 이벤트종류 추가
-- chk_status_history_event_type 에 SYSTEM_FAILURE_DETECTED 추가
-- =============================================================

ALTER TABLE status_history
    DROP CONSTRAINT chk_status_history_event_type;

ALTER TABLE status_history
    ADD CONSTRAINT chk_status_history_event_type
        CHECK (event_type IN (
            'INSTRUCTION_CREATED',
            'OWNER_INQUIRY_DONE', 'OWNER_INQUIRY_FAILED',
            'AUTH_PASSED', 'AUTH_FAILED',
            'SCHEDULED_REGISTERED', 'SCHEDULED_TRIGGERED', 'SCHEDULED_CANCELED',
            'PROCESSING_STARTED',
            'BALANCE_CHECK_FAILED', 'LIMIT_CHECK_FAILED',
            'KFTC_REQUEST_SENT', 'KFTC_ACK_RECEIVED', 'KFTC_REJECT_RECEIVED', 'KFTC_SETTLED',
            'BOK_REQUEST_SENT', 'BOK_ACK_RECEIVED', 'BOK_REJECT_RECEIVED', 'BOK_CONFIRMED',
            'REVERSAL_STARTED', 'REVERSAL_COMPLETED',
            'PAYMENT_COMPLETED', 'PAYMENT_FAILED', 'PAYMENT_CANCELED',
            'INBOUND_REJECTED', 'INBOUND_RECEIVED',
            'SYSTEM_FAILURE_DETECTED',
            'COMPENSATION_STARTED', 'COMPENSATION_COMPLETED', 'COMPENSATION_FAILED',
            'KFTC_ACK_SENT', 'BOK_ACK_SENT',
            'KFTC_SETTLEMENT_SENT', 'BOK_CONFIRM_SENT',
            'KFTC_REJECT_SENT', 'BOK_REJECT_SENT',
            'INBOUND_VALIDATION_PASSED', 'INBOUND_VALIDATION_FAILED'
        ));

-- ---- V10__kftc_clearing_transaction.sql ----
-- =============================================================
-- V10__kftc_clearing_transaction.sql
-- 1-A: KFTC 청산거래 테이블 생성
-- 1-B: outbox_message event_type CHECK에 KFTC_SETTLED 추가
-- =============================================================

-- ─────────────────────────────────────────────────────────────
-- 1-A: kftc_clearing_transaction
-- ─────────────────────────────────────────────────────────────

CREATE TABLE kftc_clearing_transaction (

    -- ── 식별자 ──────────────────────────────────────────────────────────────
    clearing_transaction_id     VARCHAR(20)     NOT NULL,

    -- ── 결제지시 참조 (1:1, OUT=우리 PI, IN=상대 PI) ─────────────────────
    our_payment_instruction_id  VARCHAR(20)     NOT NULL,

    -- ── 방향 (OUT=타행송신, IN=타행수신) ─────────────────────────────────
    direction                   VARCHAR(5)      NOT NULL,

    -- ── 상대방 거래 참조 (IN 방향 시 상대 은행 결제지시 ID, nullable) ─────
    counterparty_payment_id     VARCHAR(50)     NULL,

    -- ── KFTC 청산 식별 ──────────────────────────────────────────────────
    clearing_no                 VARCHAR(50)     NOT NULL,
    sender_bank_clearing_id     VARCHAR(50)     NULL,
    receiver_bank_clearing_id   VARCHAR(50)     NULL,

    -- ── 송신 박제 ──────────────────────────────────────────────────────────
    sender_bank_code            CHAR(3)         NOT NULL,
    sender_account_no_snap      VARCHAR(30)     NOT NULL,
    sender_holder_name_snap     VARCHAR(60)     NOT NULL,

    -- ── 수신 박제 ──────────────────────────────────────────────────────────
    receiver_bank_code          CHAR(3)         NOT NULL,
    receiver_account_no_snap    VARCHAR(30)     NOT NULL,
    receiver_holder_name_snap   VARCHAR(60)     NOT NULL,

    -- ── 금액 ───────────────────────────────────────────────────────────────
    clearing_amount             DECIMAL(15,0)   NOT NULL,
    currency                    CHAR(3)         NOT NULL    DEFAULT 'KRW',

    -- ── 청산 상태 ──────────────────────────────────────────────────────────
    clearing_status             VARCHAR(20)     NOT NULL,
    reject_code                 VARCHAR(10)     NULL,
    reject_message              VARCHAR(200)    NULL,

    -- ── 청산 시각 (VARCHAR14 = yyyyMMddHHmmss) ───────────────────────────
    clearing_requested_at       VARCHAR(14)     NOT NULL,
    ack_received_at             VARCHAR(14)     NULL,
    settled_at                  VARCHAR(14)     NULL,
    settlement_date             VARCHAR(8)      NULL,

    -- ── 망 ─────────────────────────────────────────────────────────────────
    network                     VARCHAR(30)     NOT NULL,

    -- ── 조회 추적 ──────────────────────────────────────────────────────────
    last_inquiry_at             TIMESTAMP(3)    NULL,
    inquiry_count               INT             NOT NULL    DEFAULT 0,

    -- ── 등록/수정 ──────────────────────────────────────────────────────────
    first_registered_at         TIMESTAMP(3)    NOT NULL    DEFAULT CURRENT_TIMESTAMP(3),
    first_registrant_id         VARCHAR(20)     NULL,
    last_modified_at            TIMESTAMP(3)    NOT NULL    DEFAULT CURRENT_TIMESTAMP(3),
    last_modifier_id            VARCHAR(20)     NULL,


    -- ── 기본 키 ────────────────────────────────────────────────────────────
    CONSTRAINT pk_kftc_clearing_transaction
        PRIMARY KEY (clearing_transaction_id),

    -- ── 유니크 ─────────────────────────────────────────────────────────────
    CONSTRAINT uq_kct_payment_instruction
        UNIQUE (our_payment_instruction_id),
    CONSTRAINT uq_kct_clearing_no
        UNIQUE (clearing_no),

    -- ── 방향 CHECK (2개) ──────────────────────────────────────────────────
    CONSTRAINT chk_kct_direction
        CHECK (direction IN ('OUT', 'IN')),

    -- ── 청산상태 CHECK (5개) ──────────────────────────────────────────────
    CONSTRAINT chk_kct_clearing_status
        CHECK (clearing_status IN ('REQUESTED', 'ACK', 'SETTLED', 'REJECTED', 'TIMEOUT')),

    -- ── 금액 CHECK ────────────────────────────────────────────────────────
    CONSTRAINT chk_kct_clearing_amount
        CHECK (clearing_amount >= 0),

    -- ── 망 CHECK (4개 — 컬럼명세서 EBANKING/INTERBANK + 시나리오 KFTC_CLEARING/BOK_CLEARING) ──
    CONSTRAINT chk_kct_network
        CHECK (network IN ('KFTC_CLEARING', 'INTERBANK', 'EBANKING', 'BOK_CLEARING')),

    -- ── FK: 결제지시 ───────────────────────────────────────────────────────
    CONSTRAINT fk_kct_pi
        FOREIGN KEY (our_payment_instruction_id)
        REFERENCES payment_instruction (payment_instruction_id)
);


-- ── 테이블/컬럼 한글 코멘트 ──────────────────────────────────────────────────

COMMENT ON TABLE kftc_clearing_transaction IS 'KFTC청산거래';

COMMENT ON COLUMN kftc_clearing_transaction.clearing_transaction_id     IS '청산거래번호';
COMMENT ON COLUMN kftc_clearing_transaction.our_payment_instruction_id  IS '결제지시번호(자행)';
COMMENT ON COLUMN kftc_clearing_transaction.direction                   IS '방향(OUT=타행송신/IN=타행수신)';
COMMENT ON COLUMN kftc_clearing_transaction.counterparty_payment_id     IS '상대방거래참조';
COMMENT ON COLUMN kftc_clearing_transaction.clearing_no                 IS 'KFTC청산식별번호';
COMMENT ON COLUMN kftc_clearing_transaction.sender_bank_clearing_id     IS '송신은행청산ID';
COMMENT ON COLUMN kftc_clearing_transaction.receiver_bank_clearing_id   IS '수신은행청산ID';
COMMENT ON COLUMN kftc_clearing_transaction.sender_bank_code            IS '송신은행코드';
COMMENT ON COLUMN kftc_clearing_transaction.sender_account_no_snap      IS '송신계좌번호_스냅샷';
COMMENT ON COLUMN kftc_clearing_transaction.sender_holder_name_snap     IS '송신예금주명_스냅샷';
COMMENT ON COLUMN kftc_clearing_transaction.receiver_bank_code          IS '수신은행코드';
COMMENT ON COLUMN kftc_clearing_transaction.receiver_account_no_snap    IS '수신계좌번호_스냅샷';
COMMENT ON COLUMN kftc_clearing_transaction.receiver_holder_name_snap   IS '수신예금주명_스냅샷';
COMMENT ON COLUMN kftc_clearing_transaction.clearing_amount             IS '청산금액';
COMMENT ON COLUMN kftc_clearing_transaction.currency                    IS '통화';
COMMENT ON COLUMN kftc_clearing_transaction.clearing_status             IS '청산상태';
COMMENT ON COLUMN kftc_clearing_transaction.reject_code                 IS '거절코드';
COMMENT ON COLUMN kftc_clearing_transaction.reject_message              IS '거절메시지';
COMMENT ON COLUMN kftc_clearing_transaction.clearing_requested_at       IS '청산요청시각(yyyyMMddHHmmss)';
COMMENT ON COLUMN kftc_clearing_transaction.ack_received_at             IS 'ACK수신시각(yyyyMMddHHmmss)';
COMMENT ON COLUMN kftc_clearing_transaction.settled_at                  IS '정산완료시각(yyyyMMddHHmmss)';
COMMENT ON COLUMN kftc_clearing_transaction.settlement_date             IS '정산일자(yyyyMMdd)';
COMMENT ON COLUMN kftc_clearing_transaction.network                     IS '청산망종류';
COMMENT ON COLUMN kftc_clearing_transaction.last_inquiry_at             IS '마지막조회시각';
COMMENT ON COLUMN kftc_clearing_transaction.inquiry_count               IS '조회횟수';
COMMENT ON COLUMN kftc_clearing_transaction.first_registered_at         IS '최초등록일시';
COMMENT ON COLUMN kftc_clearing_transaction.first_registrant_id         IS '최초등록자식별번호';
COMMENT ON COLUMN kftc_clearing_transaction.last_modified_at            IS '최종수정일시';
COMMENT ON COLUMN kftc_clearing_transaction.last_modifier_id            IS '최종수정자식별번호';


-- =============================================================
-- 1-B: outbox_message event_type CHECK에 KFTC_SETTLED 추가
-- 기존 V5 19개 그대로 + KFTC_SETTLED 1개 = 20개
-- =============================================================

ALTER TABLE outbox_message
    DROP CONSTRAINT chk_outbox_message_event_type;

ALTER TABLE outbox_message
    ADD CONSTRAINT chk_outbox_message_event_type
        CHECK (event_type IN (
            'PAYMENT_REQUESTED', 'PAYMENT_SCHEDULED', 'PAYMENT_SCHEDULE_CANCELED',
            'KFTC_REQUEST_SENT', 'KFTC_REJECTED', 'KFTC_SETTLED',
            'BOK_REQUEST_SENT', 'BOK_REJECTED', 'BOK_CONFIRMED',
            'PAYMENT_REVERSED', 'PAYMENT_COMPLETED', 'PAYMENT_FAILED', 'PAYMENT_CANCELED',
            'INBOUND_RECEIVED',
            'KFTC_ACK_SENT', 'BOK_ACK_SENT',
            'KFTC_SETTLEMENT_SENT', 'BOK_CONFIRM_SENT',
            'KFTC_REJECT_SENT', 'BOK_REJECT_SENT'
        ));

-- ---- V11__bok_settlement_transaction.sql ----
-- =============================================================
-- V11__bok_settlement_transaction.sql
-- BOK(한은망) 거액이체 정산거래 테이블 생성
-- PostgreSQL 16 / Flyway
-- =============================================================

CREATE TABLE bok_settlement_transaction (

    -- ── 식별자 ──────────────────────────────────────────────────────────────
    settlement_transaction_id   VARCHAR(20)     NOT NULL,

    -- ── 결제지시 참조 (1:1, OUT=우리 PI, IN=상대 PI) ─────────────────────
    our_payment_instruction_id  VARCHAR(20)     NOT NULL,

    -- ── 방향 (OUT=타행송신, IN=타행수신) ─────────────────────────────────
    direction                   VARCHAR(5)      NOT NULL,

    -- ── 상대방 거래 참조 (IN 방향 시 상대 은행 결제지시 ID, nullable) ─────
    counterparty_payment_id     VARCHAR(50)     NULL,

    -- ── BOK 정산 식별 ──────────────────────────────────────────────────────
    bok_reference_no            VARCHAR(50)     NOT NULL,
    sender_bank_clearing_id     VARCHAR(50)     NULL,
    receiver_bank_clearing_id   VARCHAR(50)     NULL,

    -- ── 송신 박제 ──────────────────────────────────────────────────────────
    sender_bank_code            CHAR(3)         NOT NULL,
    sender_account_no_snap      VARCHAR(30)     NOT NULL,
    sender_holder_name_snap     VARCHAR(60)     NOT NULL,

    -- ── 수신 박제 ──────────────────────────────────────────────────────────
    receiver_bank_code          CHAR(3)         NOT NULL,
    receiver_account_no_snap    VARCHAR(30)     NOT NULL,
    receiver_holder_name_snap   VARCHAR(60)     NOT NULL,

    -- ── 금액 ───────────────────────────────────────────────────────────────
    settlement_amount           DECIMAL(15,0)   NOT NULL,
    currency                    CHAR(3)         NOT NULL    DEFAULT 'KRW',

    -- ── 정산 상태 ──────────────────────────────────────────────────────────
    settlement_status           VARCHAR(20)     NOT NULL,
    reject_code                 VARCHAR(10)     NULL,
    reject_message              VARCHAR(200)    NULL,

    -- ── 정산 시각 (VARCHAR14 = yyyyMMddHHmmss) ───────────────────────────
    settlement_requested_at     VARCHAR(14)     NOT NULL,
    ack_received_at             VARCHAR(14)     NULL,
    settled_at                  VARCHAR(14)     NULL,
    settlement_date             VARCHAR(8)      NULL,

    -- ── 망 ─────────────────────────────────────────────────────────────────
    network                     VARCHAR(30)     NOT NULL,

    -- ── 조회 추적 ──────────────────────────────────────────────────────────
    last_inquiry_at             TIMESTAMP(3)    NULL,
    inquiry_count               INT             NOT NULL    DEFAULT 0,

    -- ── 등록/수정 ──────────────────────────────────────────────────────────
    first_registered_at         TIMESTAMP(3)    NOT NULL    DEFAULT CURRENT_TIMESTAMP(3),
    first_registrant_id         VARCHAR(20)     NULL,
    last_modified_at            TIMESTAMP(3)    NOT NULL    DEFAULT CURRENT_TIMESTAMP(3),
    last_modifier_id            VARCHAR(20)     NULL,


    -- ── 기본 키 ────────────────────────────────────────────────────────────
    CONSTRAINT pk_bok_settlement_transaction
        PRIMARY KEY (settlement_transaction_id),

    -- ── 유니크 ─────────────────────────────────────────────────────────────
    CONSTRAINT uq_bst_payment_instruction
        UNIQUE (our_payment_instruction_id),
    CONSTRAINT uq_bst_bok_reference_no
        UNIQUE (bok_reference_no),

    -- ── 방향 CHECK (2개) ──────────────────────────────────────────────────
    CONSTRAINT chk_bst_direction
        CHECK (direction IN ('OUT', 'IN')),

    -- ── 정산상태 CHECK (5개) ──────────────────────────────────────────────
    CONSTRAINT chk_bst_settlement_status
        CHECK (settlement_status IN ('REQUESTED', 'ACK_RECEIVED', 'SETTLED', 'REJECTED', 'TIMEOUT')),

    -- ── 금액 CHECK ────────────────────────────────────────────────────────
    CONSTRAINT chk_bst_settlement_amount
        CHECK (settlement_amount >= 0),

    -- ── 망 CHECK (BOK 전용 테이블 — 단일값 고정) ─────────────────────────
    CONSTRAINT chk_bst_network
        CHECK (network IN ('BOK_CLEARING')),

    -- ── FK: 결제지시 ───────────────────────────────────────────────────────
    CONSTRAINT fk_bst_pi
        FOREIGN KEY (our_payment_instruction_id)
        REFERENCES payment_instruction (payment_instruction_id)
);


-- ── 테이블/컬럼 한글 코멘트 ──────────────────────────────────────────────────

COMMENT ON TABLE bok_settlement_transaction IS 'BOK한은망정산거래';

COMMENT ON COLUMN bok_settlement_transaction.settlement_transaction_id   IS '정산거래번호';
COMMENT ON COLUMN bok_settlement_transaction.our_payment_instruction_id  IS '결제지시번호(자행)';
COMMENT ON COLUMN bok_settlement_transaction.direction                   IS '방향(OUT=타행송신/IN=타행수신)';
COMMENT ON COLUMN bok_settlement_transaction.counterparty_payment_id     IS '상대방거래참조';
COMMENT ON COLUMN bok_settlement_transaction.bok_reference_no            IS 'BOK정산식별번호';
COMMENT ON COLUMN bok_settlement_transaction.sender_bank_clearing_id     IS '송신은행청산ID';
COMMENT ON COLUMN bok_settlement_transaction.receiver_bank_clearing_id   IS '수신은행청산ID';
COMMENT ON COLUMN bok_settlement_transaction.sender_bank_code            IS '송신은행코드';
COMMENT ON COLUMN bok_settlement_transaction.sender_account_no_snap      IS '송신계좌번호_스냅샷';
COMMENT ON COLUMN bok_settlement_transaction.sender_holder_name_snap     IS '송신예금주명_스냅샷';
COMMENT ON COLUMN bok_settlement_transaction.receiver_bank_code          IS '수신은행코드';
COMMENT ON COLUMN bok_settlement_transaction.receiver_account_no_snap    IS '수신계좌번호_스냅샷';
COMMENT ON COLUMN bok_settlement_transaction.receiver_holder_name_snap   IS '수신예금주명_스냅샷';
COMMENT ON COLUMN bok_settlement_transaction.settlement_amount           IS '정산금액';
COMMENT ON COLUMN bok_settlement_transaction.currency                    IS '통화';
COMMENT ON COLUMN bok_settlement_transaction.settlement_status           IS '정산상태';
COMMENT ON COLUMN bok_settlement_transaction.reject_code                 IS '거절코드';
COMMENT ON COLUMN bok_settlement_transaction.reject_message              IS '거절메시지';
COMMENT ON COLUMN bok_settlement_transaction.settlement_requested_at     IS '정산요청시각(yyyyMMddHHmmss)';
COMMENT ON COLUMN bok_settlement_transaction.ack_received_at             IS 'ACK수신시각(yyyyMMddHHmmss)';
COMMENT ON COLUMN bok_settlement_transaction.settled_at                  IS '정산완료시각(yyyyMMddHHmmss)';
COMMENT ON COLUMN bok_settlement_transaction.settlement_date             IS '정산일자(yyyyMMdd)';
COMMENT ON COLUMN bok_settlement_transaction.network                     IS '청산망종류';
COMMENT ON COLUMN bok_settlement_transaction.last_inquiry_at             IS '마지막조회시각';
COMMENT ON COLUMN bok_settlement_transaction.inquiry_count               IS '조회횟수';
COMMENT ON COLUMN bok_settlement_transaction.first_registered_at         IS '최초등록일시';
COMMENT ON COLUMN bok_settlement_transaction.first_registrant_id         IS '최초등록자식별번호';
COMMENT ON COLUMN bok_settlement_transaction.last_modified_at            IS '최종수정일시';
COMMENT ON COLUMN bok_settlement_transaction.last_modifier_id            IS '최종수정자식별번호';

-- ---- V12__add_counterparty_bank_triggered_by.sql ----
-- V12: status_history.triggered_by CHECK에 COUNTERPARTY_BANK 추가
-- IN(수신) 트랜잭션의 triggered_by='COUNTERPARTY_BANK' 기록을 허용하기 위함
-- 기존 데이터(6개 값 내)는 새 제약(상위집합) 조건을 모두 충족하므로 안전
ALTER TABLE status_history DROP CONSTRAINT chk_status_history_triggered_by;
ALTER TABLE status_history ADD CONSTRAINT chk_status_history_triggered_by
    CHECK (triggered_by IN ('USER', 'SYSTEM', 'KFTC', 'BOK', 'OPERATOR', 'SCHEDULER', 'COUNTERPARTY_BANK'));

-- ---- V13__add_f4_f6_f7_event_types.sql ----
-- =============================================================
-- V13__add_f4_f6_f7_event_types.sql
-- status_history.event_type CHECK에 F4/F6/F7 관련 6개 값 추가
-- 기존 V9 38개 값 전부 포함 (상위집합, 기존 데이터 안전)
--
-- 추가 값:
--   KFTC_REQUEST_FAILED     : F4  Kafka/네트워크 장애로 KFTC 송신 실패
--   KFTC_TIMEOUT_DETECTED   : F6  ACK/정산 미수신 타임아웃 도래 (폴링 워커)
--   KFTC_SETTLEMENT_FAILED  : F7  ACK 이후 정산실패 통보
--   OPERATOR_CANCEL_DECIDED : F6  운영자 강제 취소 결정 (triggered_by=OPERATOR)
--   BOK_TIMEOUT_DETECTED    : F6 BOK 대칭 (선반영, 이번 구현 미사용)
--   BOK_SETTLEMENT_FAILED   : F7 BOK 대칭 (선반영, 이번 구현 미사용)
-- =============================================================

ALTER TABLE status_history
    DROP CONSTRAINT chk_status_history_event_type;

ALTER TABLE status_history
    ADD CONSTRAINT chk_status_history_event_type
        CHECK (event_type IN (
            -- ── V9 기존 38개 (누락 없이 원문 그대로) ──────────────────────
            'INSTRUCTION_CREATED',
            'OWNER_INQUIRY_DONE', 'OWNER_INQUIRY_FAILED',
            'AUTH_PASSED', 'AUTH_FAILED',
            'SCHEDULED_REGISTERED', 'SCHEDULED_TRIGGERED', 'SCHEDULED_CANCELED',
            'PROCESSING_STARTED',
            'BALANCE_CHECK_FAILED', 'LIMIT_CHECK_FAILED',
            'KFTC_REQUEST_SENT', 'KFTC_ACK_RECEIVED', 'KFTC_REJECT_RECEIVED', 'KFTC_SETTLED',
            'BOK_REQUEST_SENT', 'BOK_ACK_RECEIVED', 'BOK_REJECT_RECEIVED', 'BOK_CONFIRMED',
            'REVERSAL_STARTED', 'REVERSAL_COMPLETED',
            'PAYMENT_COMPLETED', 'PAYMENT_FAILED', 'PAYMENT_CANCELED',
            'INBOUND_REJECTED', 'INBOUND_RECEIVED',
            'SYSTEM_FAILURE_DETECTED',
            'COMPENSATION_STARTED', 'COMPENSATION_COMPLETED', 'COMPENSATION_FAILED',
            'KFTC_ACK_SENT', 'BOK_ACK_SENT',
            'KFTC_SETTLEMENT_SENT', 'BOK_CONFIRM_SENT',
            'KFTC_REJECT_SENT', 'BOK_REJECT_SENT',
            'INBOUND_VALIDATION_PASSED', 'INBOUND_VALIDATION_FAILED',
            -- ── V13 신규 6개 (F4/F6/F7 + BOK 대칭 선반영) ────────────────
            'KFTC_REQUEST_FAILED',
            'KFTC_TIMEOUT_DETECTED',
            'KFTC_SETTLEMENT_FAILED',
            'OPERATOR_CANCEL_DECIDED',
            'BOK_TIMEOUT_DETECTED',
            'BOK_SETTLEMENT_FAILED'
        ));

-- ---- V14__extend_reason_code_reject_code.sql ----
-- =============================================================
-- V14__extend_reason_code_reject_code.sql
-- reason_code / reject_code VARCHAR(10) → VARCHAR(20) 확장
--
-- 배경: F4 보상 경로에서 reason_code/reject_code에 "PUBLISH_FAILURE"(15자) 사용.
--       기존 10자 제약으로는 저장 불가 → 20자로 상향. F2/F3 기존값(≤5자) 영향 없음.
--
-- 영향 컬럼:
--   status_history.reason_code          VARCHAR(10) → VARCHAR(20)
--   kftc_clearing_transaction.reject_code VARCHAR(10) → VARCHAR(20)
-- =============================================================

ALTER TABLE status_history
    ALTER COLUMN reason_code TYPE VARCHAR(20);

ALTER TABLE kftc_clearing_transaction
    ALTER COLUMN reject_code TYPE VARCHAR(20);

-- ---- V15__add_next_timeout_at_index.sql ----
-- =============================================================
-- V15__add_next_timeout_at_index.sql
-- F6 폴링워커 성능용 부분 인덱스
--
-- next_timeout_at IS NOT NULL인 행(= CLEARING 대기 중인 PI)만 인덱스.
-- NULL(종료상태 등)은 대상 외 → 인덱스 크기 최소화.
-- =============================================================

CREATE INDEX idx_pi_next_timeout_at ON payment_instruction (next_timeout_at)
    WHERE next_timeout_at IS NOT NULL;

-- ---- V16__extend_reason_code_for_bok.sql ----
-- =============================================================
-- V16__extend_reason_code_for_bok.sql
-- BOK 이벤트 문자열(BOK_SETTLEMENT_FAILED=21자) 수용을 위한 컬럼 확장.
--
-- V14에서 status_history.reason_code / kftc_clearing_transaction.reject_code → VARCHAR(20).
-- bok_settlement_transaction.reject_code는 V11 VARCHAR(10)으로 V14 미포함.
--
-- 영향 컬럼:
--   status_history.reason_code                VARCHAR(20) → VARCHAR(30)  ← BOK_SETTLEMENT_FAILED(21자) 수용
--   bok_settlement_transaction.reject_code    VARCHAR(10) → VARCHAR(30)  ← V14 미포함, 전부 초과
--   kftc_clearing_transaction.reject_code     VARCHAR(20) → VARCHAR(30)  ← 일관성 (KFTC 이벤트 ≤20자라 당장 무관)
-- =============================================================

ALTER TABLE status_history
    ALTER COLUMN reason_code TYPE VARCHAR(30);

ALTER TABLE bok_settlement_transaction
    ALTER COLUMN reject_code TYPE VARCHAR(30);

ALTER TABLE kftc_clearing_transaction
    ALTER COLUMN reject_code TYPE VARCHAR(30);

-- ---- V17__add_bok_request_failed_event_type.sql ----
-- V13에서 KFTC_REQUEST_FAILED는 추가했으나 BOK 대칭 항목 누락. BOK F4 보상 경로에서 사용.
ALTER TABLE status_history DROP CONSTRAINT chk_status_history_event_type;
ALTER TABLE status_history ADD CONSTRAINT chk_status_history_event_type CHECK (event_type IN (
    'INSTRUCTION_CREATED', 'OWNER_INQUIRY_DONE', 'OWNER_INQUIRY_FAILED',
    'AUTH_PASSED', 'AUTH_FAILED',
    'SCHEDULED_REGISTERED', 'SCHEDULED_TRIGGERED', 'SCHEDULED_CANCELED',
    'PROCESSING_STARTED', 'BALANCE_CHECK_FAILED', 'LIMIT_CHECK_FAILED',
    'KFTC_REQUEST_SENT', 'KFTC_ACK_RECEIVED', 'KFTC_REJECT_RECEIVED', 'KFTC_SETTLED',
    'BOK_REQUEST_SENT', 'BOK_ACK_RECEIVED', 'BOK_REJECT_RECEIVED', 'BOK_CONFIRMED',
    'REVERSAL_STARTED', 'REVERSAL_COMPLETED',
    'PAYMENT_COMPLETED', 'PAYMENT_FAILED', 'PAYMENT_CANCELED',
    'INBOUND_REJECTED', 'INBOUND_RECEIVED',
    'SYSTEM_FAILURE_DETECTED', 'COMPENSATION_STARTED', 'COMPENSATION_COMPLETED', 'COMPENSATION_FAILED',
    'KFTC_ACK_SENT', 'BOK_ACK_SENT', 'KFTC_SETTLEMENT_SENT', 'BOK_CONFIRM_SENT',
    'KFTC_REJECT_SENT', 'BOK_REJECT_SENT',
    'INBOUND_VALIDATION_PASSED', 'INBOUND_VALIDATION_FAILED',
    'KFTC_REQUEST_FAILED', 'KFTC_TIMEOUT_DETECTED', 'KFTC_SETTLEMENT_FAILED',
    'OPERATOR_CANCEL_DECIDED',
    'BOK_REQUEST_FAILED', 'BOK_TIMEOUT_DETECTED', 'BOK_SETTLEMENT_FAILED'
));

-- ---- V18__add_account_check_failed_event_type.sql ----
-- =============================================================
-- V18__add_account_check_failed_event_type.sql
-- step2 외부검증 실패 경로 정합:
--   수신계좌 비활성(FROZEN/DORMANT→ACCOUNT_RESTRICTED, CLOSED→ACCOUNT_CLOSED)·
--   사고신고(ACCOUNT_RESTRICTED) 케이스에서 status_history에 기록할
--   event_type ACCOUNT_CHECK_FAILED 추가.
-- V17 기존 45개 전부 포함 (상위집합, 기존 데이터 안전)
--
-- 추가 값:
--   ACCOUNT_CHECK_FAILED : 수신계좌 비활성(FROZEN/DORMANT/CLOSED) 또는
--                          사고신고(fraudFlag=true) 검증 실패 이벤트
-- =============================================================

ALTER TABLE status_history
    DROP CONSTRAINT chk_status_history_event_type;

ALTER TABLE status_history
    ADD CONSTRAINT chk_status_history_event_type
        CHECK (event_type IN (
            -- ── V17 기존 45개 (누락 없이 원문 그대로) ─────────────────────
            'INSTRUCTION_CREATED',
            'OWNER_INQUIRY_DONE', 'OWNER_INQUIRY_FAILED',
            'AUTH_PASSED', 'AUTH_FAILED',
            'SCHEDULED_REGISTERED', 'SCHEDULED_TRIGGERED', 'SCHEDULED_CANCELED',
            'PROCESSING_STARTED',
            'BALANCE_CHECK_FAILED', 'LIMIT_CHECK_FAILED',
            'KFTC_REQUEST_SENT', 'KFTC_ACK_RECEIVED', 'KFTC_REJECT_RECEIVED', 'KFTC_SETTLED',
            'BOK_REQUEST_SENT', 'BOK_ACK_RECEIVED', 'BOK_REJECT_RECEIVED', 'BOK_CONFIRMED',
            'REVERSAL_STARTED', 'REVERSAL_COMPLETED',
            'PAYMENT_COMPLETED', 'PAYMENT_FAILED', 'PAYMENT_CANCELED',
            'INBOUND_REJECTED', 'INBOUND_RECEIVED',
            'SYSTEM_FAILURE_DETECTED',
            'COMPENSATION_STARTED', 'COMPENSATION_COMPLETED', 'COMPENSATION_FAILED',
            'KFTC_ACK_SENT', 'BOK_ACK_SENT',
            'KFTC_SETTLEMENT_SENT', 'BOK_CONFIRM_SENT',
            'KFTC_REJECT_SENT', 'BOK_REJECT_SENT',
            'INBOUND_VALIDATION_PASSED', 'INBOUND_VALIDATION_FAILED',
            'KFTC_REQUEST_FAILED',
            'KFTC_TIMEOUT_DETECTED',
            'KFTC_SETTLEMENT_FAILED',
            'OPERATOR_CANCEL_DECIDED',
            'BOK_REQUEST_FAILED', 'BOK_TIMEOUT_DETECTED', 'BOK_SETTLEMENT_FAILED',
            -- ── V18 신규 1개 ────────────────────────────────────────────────
            'ACCOUNT_CHECK_FAILED'
        ));

-- ---- V19__add_publishing_status.sql ----
-- =============================================================
-- V19__add_publishing_status.sql
-- outbox_message.publish_status CHECK 제약에 'PUBLISHING' 추가
-- PostgreSQL 16 / Flyway
-- =============================================================
-- PUBLISHING: Outbox 워커 인스턴스가 행을 선점(claim)한 상태.
--   PENDING → PUBLISHING (claimPending) → SENT / FAILED
--   크래시로 PUBLISHING 고착 시 Stuck 복구 워커가 PENDING 재설정.
-- =============================================================

ALTER TABLE outbox_message
    DROP CONSTRAINT chk_outbox_message_publish_status;

ALTER TABLE outbox_message
    ADD CONSTRAINT chk_outbox_message_publish_status
        CHECK (publish_status IN ('PENDING', 'PUBLISHING', 'SENT', 'FAILED'));

-- ---- V20__widen_external_call_business_response_code.sql ----
-- =============================================================
-- V20__widen_external_call_business_response_code.sql
-- external_call.business_response_code VARCHAR(10) → VARCHAR(50) 확장
--
-- 배경: real deposit 연동 시 ErrorCode.name() 원문(예: INSUFFICIENT_BALANCE 20자,
--       TRANSACTION_NOT_FOUND 21자, INTERNAL_SERVER_ERROR 21자)을 박제하다 PSQLException:
--       value too long for type character varying(10) → step3_withdraw 등 FAIL 경로 INSERT 500.
--       fallback "DEPOSIT_HTTP_<status>"(16자)도 동일 오버플로.
--       CLAUDE.md §5(외부 응답 박제 원문 보존) 원칙 → substring 금지, 컬럼 확장이 정답.
--
-- 폭 선정: VARCHAR(50). 동일 테이블 target_system(50)·call_type(30)과 일관.
--          deposit ErrorCode 최대 21자 + KFTC/BOK 외부 코드 확장 여유 포함.
--
-- 영향 컬럼:
--   external_call.business_response_code  VARCHAR(10) → VARCHAR(50)
-- =============================================================

ALTER TABLE external_call
    ALTER COLUMN business_response_code TYPE VARCHAR(50);

COMMENT ON COLUMN external_call.business_response_code
    IS '비즈니스응답코드 — 외부시스템(deposit ErrorCode.name(), KFTC/BOK 코드 등) 원문 박제. V20: VARCHAR(10)→(50)';

-- ---- V21__add_settlement_journal_types.sql ----
-- =============================================================
-- V21__add_settlement_journal_types.sql
-- ledger journal_type CHECK에 정산 분개 2종 추가
--
-- 추가 값:
--   CLEARING_PENDING_UNWIND  — 청산대기 해소 차변 (KB-CLR-088/KB-CLR-BOK DEBIT)
--                              KFTC 마감 정산 / BOK RTGS 정산 시점 사용
--   INTERBANK_SETTLEMENT     — 한은당좌 정산 대변 (KB-DDA CREDIT)
--                              CLEARING_PENDING_UNWIND 의 차대변 상대편
--
-- is_reversal=FALSE (정산은 역분개가 아닌 신규 회계 사건)
-- reversal_reason CHECK / 기타 CHECK 변경 없음.
-- =============================================================

ALTER TABLE ledger
    DROP CONSTRAINT chk_ledger_journal_type;

ALTER TABLE ledger
    ADD CONSTRAINT chk_ledger_journal_type
        CHECK (journal_type IN (
            'TRANSFER_OUT',
            'TRANSFER_IN',
            'CLEARING_PENDING',
            'FEE',
            'FEE_INCOME',
            'REVERSAL_TRANSFER_OUT',
            'REVERSAL_CLEARING_PENDING',
            'REVERSAL_FEE',
            'REVERSAL_FEE_INCOME',
            'CLEARING_PENDING_UNWIND',
            'INTERBANK_SETTLEMENT'
        ));

-- ---- V22__add_patch_to_external_call_http_method.sql ----
-- withdrawCancel(@PatchMapping)이 PATCH를 기록할 때 CHECK 위반으로 INSERT 실패하던 문제 수정
ALTER TABLE external_call
    DROP CONSTRAINT chk_external_call_http_method;

ALTER TABLE external_call
    ADD CONSTRAINT chk_external_call_http_method
        CHECK (http_method IN ('GET', 'POST', 'PUT', 'DELETE', 'PATCH'));

-- ============================================================
-- SERVICE: auto-loan-review  (DB: auto_loan_review_db)
-- ============================================================

-- ---- V1__create_agent_audit_log.sql ----
-- ============================================================
-- V1: 에이전트 감사 로그 테이블 (불변 INSERT-ONLY)
-- 보존 정책: 5년 (여신전문금융업법 §52의2)
-- ============================================================

CREATE TABLE agent_audit_log (
    id                  BIGSERIAL       PRIMARY KEY,
    rev_id              BIGINT          NOT NULL,
    schema_version      VARCHAR(10)     NOT NULL DEFAULT 'v1',
    track               VARCHAR(16)     NOT NULL,
    request_snapshot    JSONB           NOT NULL,
    opinion_json        JSONB           NOT NULL,
    tool_calls_json     JSONB           NOT NULL DEFAULT '[]',
    raw_llm_response    TEXT,
    pii_masked          BOOLEAN         NOT NULL DEFAULT TRUE,
    fallback_reason     VARCHAR(64),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    retention_until     DATE            NOT NULL
        GENERATED ALWAYS AS (timezone('UTC', created_at)::DATE + '5 years'::INTERVAL) STORED,

    CONSTRAINT chk_aal_schema_version CHECK (schema_version IN ('v1')),
    CONSTRAINT chk_aal_track          CHECK (track IN ('TRACK_1','TRACK_2','TRACK_3')),
    CONSTRAINT chk_aal_opinion_size   CHECK (pg_column_size(opinion_json) < 65536),
    CONSTRAINT chk_aal_request_size   CHECK (pg_column_size(request_snapshot) < 131072)
);

CREATE INDEX idx_aal_rev_id     ON agent_audit_log(rev_id);
CREATE INDEX idx_aal_created_at ON agent_audit_log(created_at DESC);

-- INSERT-ONLY 보장: UPDATE/DELETE 차단 트리거
CREATE OR REPLACE FUNCTION fn_aal_block_mutate()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'agent_audit_log is INSERT-ONLY. UPDATE/DELETE is forbidden (여신전문금융업법 §52의2).';
END;
$$;

CREATE TRIGGER trg_aal_no_update
    BEFORE UPDATE ON agent_audit_log
    FOR EACH ROW EXECUTE FUNCTION fn_aal_block_mutate();

CREATE TRIGGER trg_aal_no_delete
    BEFORE DELETE ON agent_audit_log
    FOR EACH ROW EXECUTE FUNCTION fn_aal_block_mutate();

-- 보존 기간 만료 파티션 분리용 뷰 (실제 삭제는 DBA 승인 프로세스)
CREATE VIEW agent_audit_log_expired AS
    SELECT * FROM agent_audit_log WHERE retention_until < CURRENT_DATE;

-- ---- V2__create_shadow_run_result.sql ----
-- ============================================================
-- V2: Shadow Mode 비교 결과 테이블
-- 에이전트 의사결정 shadow run 결과 보관 — phase-b-operational.md §B3.
-- ai.shadow.enabled=false(기본) 시에도 테이블은 생성됨.
-- ============================================================

CREATE TABLE shadow_run_result (
    id                    BIGSERIAL       PRIMARY KEY,
    rev_id                BIGINT          NOT NULL,
    prod_opinion_json     JSONB           NOT NULL,
    shadow_opinion_json   JSONB           NOT NULL,
    diverged              BOOLEAN         NOT NULL DEFAULT FALSE,
    diverge_reasons       TEXT            NOT NULL DEFAULT '[]',   -- JSON array string
    prod_track            VARCHAR(16)     NOT NULL,
    shadow_track          VARCHAR(16)     NOT NULL,
    prod_decision_score   NUMERIC(6,4),
    shadow_decision_score NUMERIC(6,4),
    shadow_model          VARCHAR(64)     NOT NULL,
    shadow_prompt_version VARCHAR(32)     NOT NULL,
    created_at            TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT chk_prod_track   CHECK (prod_track   IN ('TRACK_1','TRACK_2','TRACK_3')),
    CONSTRAINT chk_shadow_track CHECK (shadow_track IN ('TRACK_1','TRACK_2','TRACK_3'))
);

CREATE INDEX idx_srr_rev_id     ON shadow_run_result(rev_id);
CREATE INDEX idx_srr_created_at ON shadow_run_result(created_at DESC);
CREATE INDEX idx_srr_diverged   ON shadow_run_result(diverged) WHERE diverged = TRUE;

-- ---- V3__create_drift_tables.sql ----
-- psi_baseline 테이블: 훈련 시점 분포 기준
CREATE TABLE psi_baseline (
    id             BIGSERIAL    PRIMARY KEY,
    feature_name   VARCHAR(128) NOT NULL,
    bucket_index   SMALLINT     NOT NULL,
    bucket_low     NUMERIC(18,6),
    bucket_high    NUMERIC(18,6),
    baseline_ratio NUMERIC(8,6) NOT NULL,
    baseline_date  DATE         NOT NULL,
    model_version  VARCHAR(64)  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_psi_baseline UNIQUE (feature_name, bucket_index, model_version)
);

-- psi_drift_result 테이블: 주간 PSI 계산 결과
CREATE TABLE psi_drift_result (
    id            BIGSERIAL    PRIMARY KEY,
    feature_name  VARCHAR(128) NOT NULL,
    calc_week     DATE         NOT NULL,
    psi_value     NUMERIC(8,6) NOT NULL,
    status        VARCHAR(16)  NOT NULL,
    sample_count  INT          NOT NULL,
    model_version VARCHAR(64)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_psi_status CHECK (status IN ('STABLE','WARNING','CRITICAL')),
    CONSTRAINT uq_psi_week    UNIQUE (feature_name, calc_week, model_version)
);
CREATE INDEX idx_pdr_feature_week ON psi_drift_result(feature_name, calc_week DESC);

-- fairness_report 테이블: 월별 집단별 승인률 (rate_gap은 app에서 계산, H2 호환)
CREATE TABLE fairness_report (
    id            BIGSERIAL    PRIMARY KEY,
    report_month  DATE         NOT NULL,
    group_key     VARCHAR(64)  NOT NULL,
    approval_rate NUMERIC(5,4) NOT NULL,
    sample_count  INT          NOT NULL,
    overall_rate  NUMERIC(5,4) NOT NULL,
    rate_gap      NUMERIC(5,4) NOT NULL,
    flagged       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_fairness UNIQUE (report_month, group_key)
);
CREATE INDEX idx_fr_month   ON fairness_report(report_month DESC);

-- ---- V4__create_ai_embedding.sql ----
-- ============================================================
-- V4: RAG 임베딩 스토어 (ai_embedding)
-- 3 코퍼스 공통 테이블 — rag-corpora.md §2 참조
-- ============================================================

-- pgvector: 벡터 유사도 검색 (IVFFlat)
CREATE EXTENSION IF NOT EXISTS vector;

-- pg_trgm: FTS 보조 (한국어 trigram fallback)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE ai_embedding (
    id              BIGSERIAL       PRIMARY KEY,
    corpus          TEXT            NOT NULL,           -- 'policy_regulation' | 'similar_cases' | 'internal_faq'
    source_id       TEXT            NOT NULL,           -- 코퍼스별 원본 키
    chunk_seq       SMALLINT        NOT NULL DEFAULT 0, -- 한 source 에서 여러 chunk 일 때 순서
    chunk_text      TEXT            NOT NULL,           -- 검색 결과 표시용 원문
    chunk_summary   TEXT,                               -- LLM 입력용 짧은 요약 (선택)
    embedding       vector(1024)    NOT NULL,           -- bge-m3 / text-embedding-005 (1024 차원)
    embedding_model TEXT            NOT NULL DEFAULT 'text-embedding-005',
    metadata        JSONB           NOT NULL DEFAULT '{}'::JSONB,
    fts_tokens      TSVECTOR,                           -- FTS 보조 (pg_trgm + simple config)
    effective_date  DATE,                               -- 정책: 발효일 / 케이스: 결정일 / FAQ: 갱신일
    expiry_date     DATE,                               -- 정책: 폐지일 (그 외 NULL)
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_ai_embedding UNIQUE (corpus, source_id, chunk_seq, embedding_model)
);

-- 벡터 유사도 인덱스 (IVFFlat — 100만 chunk 이하, HNSW 는 초과 시 검토)
CREATE INDEX ai_embedding_vec_idx ON ai_embedding
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- 코퍼스 필터 인덱스 (is_active 조건 포함)
CREATE INDEX ai_embedding_corpus_active_idx ON ai_embedding (corpus) WHERE is_active;

-- metadata JSONB 필터 인덱스 (matrix_coord, tags 등 metaFilter 용)
CREATE INDEX ai_embedding_meta_gin_idx ON ai_embedding USING GIN (metadata);

-- FTS 인덱스 (하이브리드 검색 BM25 경로)
CREATE INDEX ai_embedding_fts_gin_idx ON ai_embedding USING GIN (fts_tokens);

-- 정책 유효기간 필터 인덱스
CREATE INDEX ai_embedding_effective_idx ON ai_embedding (corpus, effective_date DESC)
    WHERE is_active;

-- updated_at 자동 갱신 트리거
CREATE OR REPLACE FUNCTION ai_embedding_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ai_embedding_updated_at
    BEFORE UPDATE ON ai_embedding
    FOR EACH ROW EXECUTE FUNCTION ai_embedding_set_updated_at();

-- ---- V5__add_rag_enabled_to_shadow_run_result.sql ----
-- ============================================================
-- V5: shadow_run_result 에 rag_enabled 컬럼 추가 — phase-d-rag.md D4-2.
-- shadow run 이 RAG 컨텍스트를 사용했는지 여부 기록.
-- ============================================================

ALTER TABLE shadow_run_result
    ADD COLUMN rag_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_srr_rag_enabled ON shadow_run_result(rag_enabled);

-- ---- V6__tune_ivfflat_lists.sql ----
-- ============================================================
-- V6: IVFFlat lists 100 → 10 조정
-- pgvector 권고: lists = max(rows/1000, 10)
-- 시드 데이터 13개 기준 lists=100 은 빈 클러스터 다수 → 검색 품질 저하
-- 데이터 100k 초과 시 REINDEX CONCURRENTLY 또는 HNSW 교체 검토
-- ============================================================

DROP INDEX IF EXISTS ai_embedding_vec_idx;

CREATE INDEX ai_embedding_vec_idx ON ai_embedding
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 10);

-- ---- V7__add_rag_backend_to_shadow_run_result.sql ----
-- ============================================================
-- V7: shadow_run_result 에 rag_backend 컬럼 추가 — phase-e-elasticsearch.md E4-2.
-- shadow run 이 사용한 RAG 백엔드(inline / es)를 기록.
-- ============================================================

ALTER TABLE shadow_run_result
    ADD COLUMN rag_backend VARCHAR(16) NOT NULL DEFAULT 'inline';

CREATE INDEX idx_srr_rag_backend ON shadow_run_result(rag_backend);

-- ============================================================
-- SERVICE: doc-agent  (DB: doc_agent_db)
-- ============================================================

-- ---- V1__init_doc_agent_schema.sql ----
-- doc-agent Phase D-0: 초기 스키마

-- 대출 상품별 필수 서류 마스터
CREATE TABLE loan_product_documents (
    product_id          VARCHAR(10)  NOT NULL,
    product_name        VARCHAR(100) NOT NULL,
    req_doc_code        VARCHAR(10)  NOT NULL,
    req_doc_name        VARCHAR(100) NOT NULL,
    is_essential        BOOLEAN      NOT NULL DEFAULT TRUE,
    valid_days          INT,                              -- NULL = 만료 없음
    accepted_formats    VARCHAR(100) NOT NULL DEFAULT 'pdf,jpg,png',
    min_dpi             INT          NOT NULL DEFAULT 200,
    issuer_type         VARCHAR(20)  NOT NULL,            -- GOV24|COMPANY|BANK|PRIVATE
    auto_verify_enabled BOOLEAN      NOT NULL DEFAULT TRUE, -- FALSE = 무조건 심사원 라우팅
    retention_days      INT,
    PRIMARY KEY (product_id, req_doc_code)
);

-- 초기 마스터 데이터
INSERT INTO loan_product_documents VALUES
  ('P001','직장인 신용대출','DOC_01','신분증 (주민증/면허증)',TRUE,NULL,'pdf,jpg,png',200,'GOV24',TRUE,1825),
  ('P001','직장인 신용대출','DOC_02','주민등록등본',         TRUE,90,  'pdf,jpg,png',200,'GOV24',TRUE,1825),
  ('P001','직장인 신용대출','DOC_03','재직증명서',           TRUE,30,  'pdf,jpg,png',200,'COMPANY',TRUE,1825),
  ('P001','직장인 신용대출','DOC_04','근로소득원천징수영수증',TRUE,365, 'pdf',        200,'COMPANY',TRUE,1825),
  ('P002','주택담보대출',   'DOC_01','신분증 (주민증/면허증)',TRUE,NULL,'pdf,jpg,png',200,'GOV24',TRUE,1825),
  ('P002','주택담보대출',   'DOC_05','부동산 등기부등본',    TRUE,90,  'pdf',        200,'GOV24',TRUE,1825),
  ('P002','주택담보대출',   'DOC_06','매매계약서',           TRUE,NULL,'pdf',        200,'PRIVATE',FALSE,1825);

-- 서류 제출 운영 로그
CREATE TABLE loan_document_submission (
    submission_id      UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    application_id     VARCHAR(50)  NOT NULL,
    doc_code           VARCHAR(10)  NOT NULL,
    raw_object_key     VARCHAR(500),   -- MinIO/R2 원본 경로 (Vault Transit 암호화)
    masked_object_key  VARCHAR(500),   -- 마스킹본 경로
    forgery_score      NUMERIC(3,2),   -- 자동 산출 점수 (사람이 판정하는 게 아님)
    verify_status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
                                       -- PENDING|AUTO_PASS|NEEDS_RESUBMIT|HOLD|LOCKED|CLEARED
    reviewer_id        VARCHAR(50),
    human_review_status VARCHAR(20) DEFAULT 'NOT_REQUIRED',
                                       -- NOT_REQUIRED|PENDING|CLEARED|CONFIRMED_FORGERY
    retention_until    DATE,
    legal_hold         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_doc_submission_application ON loan_document_submission(application_id);
CREATE INDEX idx_doc_submission_status ON loan_document_submission(verify_status);
CREATE INDEX idx_doc_submission_retention ON loan_document_submission(retention_until)
    WHERE legal_hold = FALSE;

-- 위변조 시그널 로그 (향후 모델 학습 데이터)
CREATE TABLE loan_forgery_signal (
    signal_id     BIGSERIAL    PRIMARY KEY,
    submission_id UUID         NOT NULL REFERENCES loan_document_submission(submission_id),
    category      VARCHAR(20)  NOT NULL,  -- META|VISUAL|SEMANTIC|EXTERNAL
    signal_type   VARCHAR(50)  NOT NULL,  -- META_EDIT_TOOL|ELA_HIGH|COPY_MOVE|SSN_CHECKSUM_FAIL...
    score         NUMERIC(3,2) NOT NULL,
    evidence      JSONB,
    detected_at   TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_forgery_signal_submission ON loan_forgery_signal(submission_id);
CREATE INDEX idx_forgery_signal_category   ON loan_forgery_signal(category, signal_type);

-- ---- V2__identity_verify_cache.sql ----
-- doc-agent Phase D-3: 진위확인 결과 캐시 (TTL 1일)

CREATE TABLE identity_verify_cache (
    cache_key   VARCHAR(200) PRIMARY KEY,   -- SHA-256(api_type + params), PII 미포함
    result      VARCHAR(20)  NOT NULL,      -- VALID|INVALID|ERROR
    verified_at TIMESTAMP    NOT NULL DEFAULT now(),
    expires_at  TIMESTAMP    NOT NULL
);

CREATE INDEX idx_identity_verify_expires ON identity_verify_cache(expires_at);

-- 마스터 테이블 조회 성능 인덱스
CREATE INDEX idx_lpd_product_essential ON loan_product_documents(product_id, is_essential);

-- ---- V3__forgery_signal_score_double.sql ----
-- loan_forgery_signal.score: 엔티티(ForgerySignalEntity.score=double)와 타입 일치
-- 기존 NUMERIC(3,2) → DOUBLE PRECISION 으로 변경하여 Hibernate 스키마 검증 통과
ALTER TABLE loan_forgery_signal
    ALTER COLUMN score TYPE DOUBLE PRECISION;

-- ---- V4__status_history.sql ----
-- 공용 엔티티 com.bank.common.audit.StatusHistory 매핑 테이블.
-- 분산 보관 정책: 각 도메인 DB 가 자체 status_history 테이블을 보유한다.
-- (loan-service V1 의 status_history 와 동일 스키마)
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
