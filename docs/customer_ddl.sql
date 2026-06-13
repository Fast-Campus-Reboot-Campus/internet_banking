-- =============================================================================
-- 고객계 DDL (통합 최종본)
--
--   기준 문서 : docs/customer_ddl_design.md
--   DB        : PostgreSQL
--   범위      : 17개 테이블 (V1 13개 + V11/V15/V16/V19 신설 4개)
--   정본 기준 : Flyway 마이그레이션 V1~V26 을 합친 "현재 스키마 스냅샷"
--
--   ※ 본 파일은 신규 환경에 한 번에 적용하기 위한 참조용 통합 DDL 이다.
--     기존 운영 DB 는 Flyway 마이그레이션으로 관리되며 본 파일을 직접 재적용하지 않는다.
--   ※ 적용 순서: cust_code_master → party → (서브타입/속성) → customer → (이력)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. cust_code_master  (고객코드마스터, 전 도메인 공통 코드 — FK 미설정 soft reference)
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
    party_type_code    VARCHAR(20)    NOT NULL,   -- PERSONAL / ORGANIZATION (정본 = Party.java 상수)
    party_name         VARCHAR(100)   NOT NULL,
    party_english_name VARCHAR(200),
    party_status_code  VARCHAR(20)    NOT NULL,   -- ACTIVE / SUSPENDED / CLOSED
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
-- 3. party_person  (개인관계자, party 1:1 식별)
-- -----------------------------------------------------------------------------
CREATE TABLE party_person (
    party_id                  BIGINT         NOT NULL,
    rrn_encrypted             VARCHAR(255),                  -- 주민등록번호 AES-256
    ci_value                  VARCHAR(88),                   -- 본인확인 CI (uq_party_person_ci)
    nationality_type_code     VARCHAR(20),                   -- DOMESTIC / FOREIGN
    nationality_code          CHAR(3),                       -- ISO 3166
    birth_date                CHAR(8),
    gender_code               CHAR(1),                       -- M / F / U
    marital_status_code       VARCHAR(10),
    dependent_count           INT,
    occupation_code           VARCHAR(10),
    occupation_name           VARCHAR(100),
    workplace_name            VARCHAR(200),
    annual_income_amount      BIGINT,
    income_proof_code         VARCHAR(10),
    capacity_limit_type_code  VARCHAR(20),
    is_pep_yn                 CHAR(1)        NOT NULL DEFAULT 'F',
    pep_type_code             VARCHAR(10),                   -- SELF / FAMILY / CLOSE_ASSOC (관계 축)
    pep_country_code          CHAR(3),                       -- ISO 3166 (국내/해외 축)
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
-- 4. party_organization  (기업관계자, party 1:1 식별)
-- -----------------------------------------------------------------------------
CREATE TABLE party_organization (
    party_id                      BIGINT         NOT NULL,
    org_subtype_code              VARCHAR(20)    NOT NULL,   -- CORPORATION / NON_CORPORATION
    corp_reg_no                   CHAR(14),
    corp_formal_name              VARCHAR(200),
    corp_formal_english_name      VARCHAR(400),
    hq_country_code               CHAR(3),                   -- ISO 3166, 국내 = KOR
    foreign_corp_reg_no_encrypted VARCHAR(255),              -- AES-256 (외국 법인만)
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
-- 5. foreigner_info  (외국인정보, party_person 1:1 식별)
-- -----------------------------------------------------------------------------
CREATE TABLE foreigner_info (
    party_id                BIGINT         NOT NULL,
    foreigner_no_encrypted  VARCHAR(255),                    -- AES-256
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
-- 6. compliance_info  (컴플라이언스정보, party 1:1 식별)
--    is_sanctioned_yn: OFAC·UN·EU·KR OR 합산 GENERATED STORED
--    aml_last_assessed_by_employee_id / kyc_completed_by_employee_id: 행위자(V22, soft ref)
-- -----------------------------------------------------------------------------
CREATE TABLE compliance_info (
    party_id                          BIGINT         NOT NULL,
    aml_risk_level_code               VARCHAR(20)    NOT NULL,   -- LOW / MED / HIGH
    aml_last_assessed_at              TIMESTAMPTZ(3),
    aml_last_assessed_by_employee_id  BIGINT,                    -- V22 (soft ref, 시스템 자동 = NULL)
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
    kyc_status_code                   VARCHAR(20)    NOT NULL,   -- PENDING / COMPLETED / EXPIRED / FAILED
    kyc_completed_at                  TIMESTAMPTZ(3),
    kyc_completed_by_employee_id      BIGINT,                    -- V22 (soft ref, 시스템 자동 = NULL)
    kyc_expiry_date                   CHAR(8),
    kyc_next_review_date              CHAR(8),
    identity_verification_method_code VARCHAR(10),
    cdd_level_code                    VARCHAR(20)    NOT NULL,   -- SIMPLE / STANDARD / ENHANCED
    cdd_last_reviewed_at              TIMESTAMPTZ(3),
    cdd_next_review_date              CHAR(8),
    edd_required_yn                   CHAR(1)        NOT NULL DEFAULT 'F',
    edd_last_reviewed_at              TIMESTAMPTZ(3),
    edd_next_review_date              CHAR(8),
    fatca_status_code                 VARCHAR(20)    NOT NULL,   -- US_PERSON / NON_US / EXEMPT 등
    fatca_last_reviewed_at            TIMESTAMPTZ(3),
    fatca_next_review_date            CHAR(8),
    fatca_reportable_yn               CHAR(1)        NOT NULL DEFAULT 'F',
    crs_status_code                   VARCHAR(20)    NOT NULL,   -- REPORTABLE / NON_REPORTABLE / EXEMPT 등
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
    resident_type_code         VARCHAR(20)    NOT NULL,   -- RESIDENT / NON_RESIDENT
    tax_country_code           CHAR(3),
    foreign_tin                VARCHAR(50),
    withholding_rate_bps       INT,                       -- bps (14% = 1400)
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
-- 8. party_role  (관계자역할, party 1:N)
--    role_type_code: CUST/GRT/UBO/LGAR/EMPLOYEE/BEN/FAM/PROSP/AGT
-- -----------------------------------------------------------------------------
CREATE TABLE party_role (
    role_id              BIGINT         GENERATED ALWAYS AS IDENTITY,
    party_id             BIGINT         NOT NULL,
    role_type_code       VARCHAR(20)    NOT NULL,
    role_status_code     VARCHAR(20)    NOT NULL,   -- ACTIVE / SUSPENDED / CLOSED
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
--    relation_review_status_code: 대리인 위임 검토 큐 (V14, nullable)
-- -----------------------------------------------------------------------------
CREATE TABLE party_relation (
    relation_id                 BIGINT         GENERATED ALWAYS AS IDENTITY,
    from_party_id               BIGINT         NOT NULL,
    to_party_id                 BIGINT         NOT NULL,
    relation_type_code          VARCHAR(10)    NOT NULL,
    relation_detail_code        VARCHAR(10),
    equity_ratio_bps            INT,                       -- bps (UBO 25% = 2500)
    representation_scope        VARCHAR(200),
    proof_url                   VARCHAR(500),
    relation_review_status_code VARCHAR(20),               -- V14: PENDING / APPROVED / REJECTED
    relation_start_date         CHAR(8)        NOT NULL,
    relation_end_date           CHAR(8),
    relation_end_reason_code    VARCHAR(20),
    created_at                  TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                  BIGINT,
    updated_at                  TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by                  BIGINT,
    deleted_at                  TIMESTAMPTZ(3),
    deleted_by                  BIGINT,
    version                     INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_party_relation PRIMARY KEY (relation_id),
    CONSTRAINT fk_party_relation_from FOREIGN KEY (from_party_id) REFERENCES party (party_id),
    CONSTRAINT fk_party_relation_to   FOREIGN KEY (to_party_id)   REFERENCES party (party_id),
    CONSTRAINT chk_party_relation_no_self CHECK (from_party_id <> to_party_id)
);

-- -----------------------------------------------------------------------------
-- 10. business_info  (사업자정보, party 1:N)
-- -----------------------------------------------------------------------------
CREATE TABLE business_info (
    business_info_id   BIGINT         GENERATED ALWAYS AS IDENTITY,
    party_id           BIGINT         NOT NULL,
    biz_reg_no         CHAR(12)       NOT NULL,
    biz_status_code    VARCHAR(20)    NOT NULL,   -- CONTINUE / SUSPEND / CLOSE
    trade_name         VARCHAR(200)   NOT NULL,
    english_trade_name VARCHAR(400),
    opening_date       CHAR(8)        NOT NULL,
    closing_date       CHAR(8),
    nts_industry_code  CHAR(6)        NOT NULL,
    ksic_code          CHAR(5)        NOT NULL,
    biz_type_code      VARCHAR(10),
    biz_item_code      VARCHAR(10)    NOT NULL,
    tax_type_code      VARCHAR(10)    NOT NULL,   -- GENERAL / SIMPLIFIED / EXEMPT
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
-- 11. customer  (고객, party 1:N 비식별)
--    상태: ACTIVE / DORMANT / SUSPENDED(V13) / CLOSED
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
    suspended_at             TIMESTAMPTZ(3),                -- V13 (SUSPENDED 시 NOT NULL)
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
        (customer_status_code = 'CLOSED'    AND closed_at IS NOT NULL AND close_reason_code IS NOT NULL)
        OR (customer_status_code = 'DORMANT'   AND dormant_at IS NOT NULL)
        OR (customer_status_code = 'SUSPENDED' AND suspended_at IS NOT NULL)
        OR customer_status_code = 'ACTIVE'
    )
);

-- -----------------------------------------------------------------------------
-- 12. customer_status_history  (고객상태이력, 로그 — soft delete·version 미적용)
--     changed_by_employee_id: 변경 직원 (V19, soft ref)
-- -----------------------------------------------------------------------------
CREATE TABLE customer_status_history (
    customer_status_history_id           BIGINT         GENERATED ALWAYS AS IDENTITY,
    previous_customer_status_history_id  BIGINT,
    customer_id                          BIGINT         NOT NULL,
    customer_status_code                 VARCHAR(20)    NOT NULL,
    previous_customer_status_code        VARCHAR(20),
    customer_status_change_reason_code   VARCHAR(20)    NOT NULL,
    customer_status_change_reason_detail VARCHAR(500),
    customer_status_effective_start_at   TIMESTAMPTZ(3) NOT NULL,
    customer_status_effective_end_at     TIMESTAMPTZ(3),
    system_auto_triggered_yn             CHAR(1)        NOT NULL DEFAULT 'F',
    changed_by_employee_id               BIGINT,                    -- V19 (시스템 자동 = NULL)
    created_at                           TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                           BIGINT,
    CONSTRAINT pk_customer_status_history PRIMARY KEY (customer_status_history_id),
    CONSTRAINT fk_customer_status_history_customer
        FOREIGN KEY (customer_id) REFERENCES customer (customer_id),
    CONSTRAINT fk_customer_status_history_self
        FOREIGN KEY (previous_customer_status_history_id)
        REFERENCES customer_status_history (customer_status_history_id)
);

-- -----------------------------------------------------------------------------
-- 13. customer_grade_history  (고객등급이력, 로그 — soft delete·version 미적용)
--     changed_by_employee_id: 변경 직원 (V19, soft ref)
-- -----------------------------------------------------------------------------
CREATE TABLE customer_grade_history (
    customer_grade_history_id           BIGINT         GENERATED ALWAYS AS IDENTITY,
    previous_customer_grade_history_id  BIGINT,
    customer_id                         BIGINT         NOT NULL,
    customer_grade_code                 VARCHAR(10)    NOT NULL,
    previous_customer_grade_code        VARCHAR(10),
    customer_grade_change_reason_code   VARCHAR(20)    NOT NULL,
    customer_grade_change_reason_detail VARCHAR(500),
    customer_grade_effective_start_date CHAR(8)        NOT NULL,
    customer_grade_effective_end_date   CHAR(8),
    customer_grade_evaluated_at         TIMESTAMPTZ(3) NOT NULL,
    system_auto_triggered_yn            CHAR(1)        NOT NULL DEFAULT 'F',
    changed_by_employee_id              BIGINT,                    -- V19 (시스템 자동 = NULL)
    created_at                          TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                          BIGINT,
    CONSTRAINT pk_customer_grade_history PRIMARY KEY (customer_grade_history_id),
    CONSTRAINT fk_customer_grade_history_customer
        FOREIGN KEY (customer_id) REFERENCES customer (customer_id),
    CONSTRAINT fk_customer_grade_history_self
        FOREIGN KEY (previous_customer_grade_history_id)
        REFERENCES customer_grade_history (customer_grade_history_id)
);

-- -----------------------------------------------------------------------------
-- 14. employee  (직원, party 1:1 식별) — V11
--     grade_code = common BankRole enum 이름. JWT roles 는 grade_code 에서 파생.
-- -----------------------------------------------------------------------------
CREATE TABLE employee (
    employee_id  BIGINT         GENERATED ALWAYS AS IDENTITY,
    party_id     BIGINT         NOT NULL,
    branch_code  VARCHAR(10)    NOT NULL,   -- 본부 = 0000, 지점 = 0001..
    grade_code   VARCHAR(30)    NOT NULL,   -- BankRole enum 이름
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

-- -----------------------------------------------------------------------------
-- 15. sanction_screening_hit  (제재스크리닝 Hit 검토 큐, party 1:N) — V15
-- -----------------------------------------------------------------------------
CREATE TABLE sanction_screening_hit (
    sanction_screening_hit_id  BIGINT         GENERATED ALWAYS AS IDENTITY,
    party_id                   BIGINT         NOT NULL,
    hit_type_code              VARCHAR(30)    NOT NULL,   -- OFAC_SDN / KR_PEP / UN / EU
    match_rate                 INT            NOT NULL,   -- 0~100 유사도(%)
    screening_status_code      VARCHAR(20)    NOT NULL,   -- PENDING / CLEARED / CONFIRMED
    reviewer_employee_id       BIGINT,                    -- soft ref
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

-- -----------------------------------------------------------------------------
-- 16. duplicate_review_case  (중복고객 검토 케이스, party N:M self) — V16
-- -----------------------------------------------------------------------------
CREATE TABLE duplicate_review_case (
    duplicate_review_case_id  BIGINT         GENERATED ALWAYS AS IDENTITY,
    new_party_id              BIGINT         NOT NULL,   -- 신규(또는 후보)
    existing_party_id         BIGINT         NOT NULL,   -- 기존
    match_type_code           VARCHAR(20)    NOT NULL,   -- CI / NAME_BIRTH
    review_status_code        VARCHAR(20)    NOT NULL,   -- PENDING / DUPLICATE / DISTINCT
    reviewer_employee_id      BIGINT,                    -- soft ref
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

-- -----------------------------------------------------------------------------
-- 17. customer_access_log  (고객조회 감사로그, append-only) — V19
--     version·soft delete·updated_* 없음. FK 미설정(감사 적재가 FK 로 막히면 안 됨).
--     직원명·역할·지점·고객명은 조회 시점 스냅샷.
-- -----------------------------------------------------------------------------
CREATE TABLE customer_access_log (
    customer_access_log_id  BIGINT         GENERATED ALWAYS AS IDENTITY,
    accessor_employee_id    BIGINT         NOT NULL,   -- 조회 직원 (X-Employee-Id)
    accessor_name           VARCHAR(100),              -- 직원명 스냅샷
    accessor_role           VARCHAR(40),               -- BankRole 스냅샷
    accessor_branch_code    VARCHAR(10),               -- 직원 지점 스냅샷
    target_customer_id      BIGINT         NOT NULL,   -- 조회 대상 customer_id
    target_customer_name    VARCHAR(100),              -- 고객명 스냅샷
    access_action_code      VARCHAR(40)    NOT NULL,   -- CUSTOMER_DETAIL / CONTACT_VIEW 등
    access_reason           VARCHAR(500),
    accessed_at             TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_customer_access_log PRIMARY KEY (customer_access_log_id)
);

-- =============================================================================
-- 인덱스
-- =============================================================================

-- party 당 활성 고객 1건 제한 (CLOSED 제외, SUSPENDED 는 활성 슬롯 점유)
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

-- "한 사람(CI) = 한 party" 보장 (V17). CI 없는 레거시 row 제외 (부분 인덱스)
CREATE UNIQUE INDEX uq_party_person_ci
    ON party_person (ci_value)
    WHERE ci_value IS NOT NULL AND deleted_at IS NULL;

-- 직원 디렉토리 (V11)
CREATE INDEX idx_employee_party ON employee (party_id);

-- 제재 Hit 검토 대기 큐 (V15)
CREATE INDEX idx_sanction_screening_hit_status
    ON sanction_screening_hit (screening_status_code)
    WHERE deleted_at IS NULL;

-- 중복고객 검토 대기 큐 (V16)
CREATE INDEX idx_duplicate_review_case_status
    ON duplicate_review_case (review_status_code)
    WHERE deleted_at IS NULL;

-- 고객조회 감사로그 (V19)
CREATE INDEX idx_customer_access_log_target   ON customer_access_log (target_customer_id);
CREATE INDEX idx_customer_access_log_accessor ON customer_access_log (accessor_employee_id);
CREATE INDEX idx_customer_access_log_at       ON customer_access_log (accessed_at DESC);
