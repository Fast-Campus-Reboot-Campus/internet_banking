-- =============================================================================
-- 인증보안계 DDL (통합 최종본)
--
--   기준 문서 : docs/auth_security_ddl_design.md
--   DB        : PostgreSQL
--   범위      : 17개 테이블 (V2 15개 + V4/V6 신설 2개) — customer 는 고객계 외부 참조
--   정본 기준 : Flyway 마이그레이션 V2~V26 을 합친 "현재 스키마 스냅샷"
--
--   ※ 전제: 고객계 DDL(customer_ddl.sql) 적용 완료 후 실행 — customer.customer_id FK 기준점
--   ※ 본 파일은 신규 환경 일괄 적용용 참조 DDL. 운영 DB 는 Flyway 로 관리.
--   ※ V7 이 선반영한 otp_device/security_card/security_card_code/auth_token 은
--     V9 에서 철회되어 최종 스키마에 없으므로 본 파일에 포함하지 않는다.
--
--   순환 참조 (login_session ↔ api_token):
--     login_session.token_id → api_token.token_id      DEFERRABLE INITIALLY DEFERRED
--     api_token.session_id   → login_session.session_id  DEFERRABLE INITIALLY DEFERRED
--     → 동일 트랜잭션 내 세션 INSERT → 토큰 INSERT → 세션 token_id UPDATE → COMMIT
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
    fds_rule_risk_weight       INT            NOT NULL DEFAULT 50,   -- 0~100
    fds_rule_action_type_code  VARCHAR(20)    NOT NULL,              -- BLOCK / CHALLENGE / MONITOR
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
-- 2. credential  (계정자격증명, customer 1:1)
-- -----------------------------------------------------------------------------
CREATE TABLE credential (
    credential_id                    BIGINT         GENERATED ALWAYS AS IDENTITY,
    customer_id                      BIGINT         NOT NULL,
    login_id                         VARCHAR(50)    NOT NULL,
    password_hash                    VARCHAR(255)   NOT NULL,
    password_changed_at              TIMESTAMPTZ(3) NOT NULL,
    password_expiry_at               TIMESTAMPTZ(3),
    account_status_code              VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE/LOCKED/DORMANT/CLOSED
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
-- 3. registered_device  (등록기기, customer 1:N)
-- -----------------------------------------------------------------------------
CREATE TABLE registered_device (
    device_id               BIGINT         GENERATED ALWAYS AS IDENTITY,
    customer_id             BIGINT         NOT NULL,
    device_name             VARCHAR(100),
    device_type_code        VARCHAR(20)    NOT NULL,              -- MOBILE / PC / TABLET
    device_os_name          VARCHAR(50),
    device_os_version       VARCHAR(50),
    device_fingerprint_hash VARCHAR(255)   NOT NULL,
    trusted_device_yn       CHAR(1)        NOT NULL DEFAULT 'F',
    designated_pc_yn        CHAR(1)        NOT NULL DEFAULT 'F',
    device_registered_ip    VARCHAR(45)    NOT NULL,
    device_last_used_at     TIMESTAMPTZ(3),
    device_status_code      VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE/SUSPENDED/REVOKED
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
    CONSTRAINT chk_registered_device_type          CHECK (device_type_code IN ('MOBILE','PC','TABLET')),
    CONSTRAINT chk_registered_device_status        CHECK (device_status_code IN ('ACTIVE','SUSPENDED','REVOKED')),
    CONSTRAINT chk_registered_device_trusted       CHECK (trusted_device_yn IN ('T','F')),
    CONSTRAINT chk_registered_device_designated_pc CHECK (designated_pc_yn IN ('T','F'))
);

-- -----------------------------------------------------------------------------
-- 4. auth_method  (인증수단, customer 1:N)
--    타입 집합: V4 에서 CERT_AXFUL 추가, V9 에서 정본 집합으로 최종 복구
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
    CONSTRAINT chk_auth_method_type CHECK (auth_method_type_code IN (
        'SMS','PASS','CERT_FIN','CERT_COMMON','CERT_AXFUL','PIN','BIO_FACE','BIO_FINGER'
    )),
    CONSTRAINT chk_auth_method_primary CHECK (primary_auth_method_yn IN ('T','F'))
);

-- -----------------------------------------------------------------------------
-- 5. certificate  (금융인증서, customer 1:N / auth_method 1:N)
--    cert_pin_hash: 인증서 PIN 해시 — 로그인 비밀번호와 분리 (V5)
--    cert_login_failure_count / max_*: nullable (ERD 확정)
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
    certificate_purpose_code           VARCHAR(50)    NOT NULL,    -- LOGIN / TXN_SIGN / CONTRACT_SIGN
    certificate_issued_date            CHAR(8)        NOT NULL,
    certificate_expiry_date            CHAR(8)        NOT NULL,
    certificate_renewal_scheduled_date CHAR(8),
    certificate_status_code            VARCHAR(20)    NOT NULL,    -- ACTIVE/EXPIRED/REVOKED/SUSPENDED
    certificate_revoke_reason_code     VARCHAR(200),
    certificate_revoked_at             TIMESTAMPTZ(3),
    cert_pin_hash                      VARCHAR(255),               -- V5
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
    CONSTRAINT fk_certificate_customer    FOREIGN KEY (customer_id)    REFERENCES customer (customer_id),
    CONSTRAINT fk_certificate_auth_method FOREIGN KEY (auth_method_id) REFERENCES auth_method (auth_method_id),
    CONSTRAINT chk_certificate_status
        CHECK (certificate_status_code IN ('ACTIVE','EXPIRED','REVOKED','SUSPENDED'))
);

-- -----------------------------------------------------------------------------
-- 6. mobile_auth  (휴대폰인증요청, customer 1:N — 가입 전 NULL 허용)
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
    CONSTRAINT fk_mobile_auth_customer FOREIGN KEY (customer_id) REFERENCES customer (customer_id),
    CONSTRAINT chk_mobile_auth_verified CHECK (mobile_auth_verified_yn IN ('T','F'))
);

-- -----------------------------------------------------------------------------
-- 7. login_attempt  (로그인시도이력, customer/device nullable)
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
    CONSTRAINT fk_login_attempt_customer FOREIGN KEY (customer_id) REFERENCES customer (customer_id),
    CONSTRAINT fk_login_attempt_device   FOREIGN KEY (device_id)   REFERENCES registered_device (device_id),
    CONSTRAINT chk_login_attempt_success CHECK (login_attempt_success_yn IN ('T','F'))
);

-- -----------------------------------------------------------------------------
-- 8. login_session  (로그인세션, PK VARCHAR(64))
--    token_id FK(→ api_token)는 순환 참조 — api_token 생성 후 ALTER 로 추가(아래)
-- -----------------------------------------------------------------------------
CREATE TABLE login_session (
    session_id               VARCHAR(64)    NOT NULL,
    customer_id              BIGINT         NOT NULL,
    login_attempt_id         BIGINT         NOT NULL,
    device_id                BIGINT,
    token_id                 BIGINT         NOT NULL,
    session_issued_ip        VARCHAR(45)    NOT NULL,
    session_channel_code     VARCHAR(20)    NOT NULL,
    session_status_code      VARCHAR(20)    NOT NULL,   -- ACTIVE/EXPIRED/LOGGED_OUT/FORCED_OUT
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
    CONSTRAINT fk_login_session_customer      FOREIGN KEY (customer_id)      REFERENCES customer (customer_id),
    CONSTRAINT fk_login_session_login_attempt FOREIGN KEY (login_attempt_id) REFERENCES login_attempt (login_attempt_id),
    CONSTRAINT fk_login_session_device        FOREIGN KEY (device_id)        REFERENCES registered_device (device_id),
    CONSTRAINT chk_login_session_status CHECK (session_status_code IN ('ACTIVE','EXPIRED','LOGGED_OUT','FORCED_OUT')),
    CONSTRAINT chk_login_session_mfa    CHECK (session_mfa_completed_yn IN ('T','F'))
    -- fk_login_session_token 은 api_token 생성 후 ALTER TABLE 로 추가
);

-- -----------------------------------------------------------------------------
-- 9. api_token  (API토큰)
--    created_at / updated_at / updated_by: nullable 유지 (ERD 기준, NOT NULL 통일 팀 결정 대기)
--    session_id FK: 순환 참조 → DEFERRABLE INITIALLY DEFERRED
-- -----------------------------------------------------------------------------
CREATE TABLE api_token (
    token_id                  BIGINT         GENERATED ALWAYS AS IDENTITY,
    customer_id               BIGINT         NOT NULL,
    session_id                VARCHAR(64)    NOT NULL,
    token_type_code           VARCHAR(20)    NOT NULL,   -- ACCESS / REFRESH / OAUTH
    token_hash                VARCHAR(255)   NOT NULL,
    token_issued_channel_code VARCHAR(20)    NOT NULL,
    token_scope               VARCHAR(500),
    token_client_id           VARCHAR(50),
    token_issued_at           TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    token_expiry_at           TIMESTAMPTZ(3) NOT NULL,
    token_revoked_at          TIMESTAMPTZ(3),
    token_revoke_reason_code  VARCHAR(20),
    created_at                TIMESTAMPTZ(3) DEFAULT CURRENT_TIMESTAMP(3),   -- nullable (예외)
    created_by                BIGINT,
    updated_at                TIMESTAMPTZ(3),                                -- nullable (예외)
    updated_by                BIGINT,
    deleted_at                TIMESTAMPTZ(3),
    deleted_by                BIGINT,
    version                   INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_api_token PRIMARY KEY (token_id),
    CONSTRAINT fk_api_token_customer FOREIGN KEY (customer_id) REFERENCES customer (customer_id),
    CONSTRAINT fk_api_token_session  FOREIGN KEY (session_id)  REFERENCES login_session (session_id)
        DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT chk_api_token_type CHECK (token_type_code IN ('ACCESS','REFRESH','OAUTH'))
);

-- 순환 참조 FK 추가: login_session.token_id → api_token.token_id
ALTER TABLE login_session
    ADD CONSTRAINT fk_login_session_token
        FOREIGN KEY (token_id) REFERENCES api_token (token_id)
        DEFERRABLE INITIALLY DEFERRED;

-- -----------------------------------------------------------------------------
-- 10. pin  (간편비밀번호, customer/auth_method/device 1:N)
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
    CONSTRAINT fk_pin_customer    FOREIGN KEY (customer_id)    REFERENCES customer (customer_id),
    CONSTRAINT fk_pin_auth_method FOREIGN KEY (auth_method_id) REFERENCES auth_method (auth_method_id),
    CONSTRAINT fk_pin_device      FOREIGN KEY (device_id)      REFERENCES registered_device (device_id)
);

-- -----------------------------------------------------------------------------
-- 11. password_history  (비밀번호이력, credential/customer 1:N)
-- -----------------------------------------------------------------------------
CREATE TABLE password_history (
    password_history_id          BIGINT         GENERATED ALWAYS AS IDENTITY,
    credential_id                BIGINT         NOT NULL,
    customer_id                  BIGINT         NOT NULL,
    password_hash                VARCHAR(255)   NOT NULL,
    password_change_channel_code VARCHAR(20)    NOT NULL,
    password_change_reason_code  VARCHAR(200),
    password_change_ip           VARCHAR(45),
    created_at                   TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                   BIGINT,
    updated_at                   TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by                   BIGINT,
    deleted_at                   TIMESTAMPTZ(3),
    deleted_by                   BIGINT,
    version                      INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_password_history PRIMARY KEY (password_history_id),
    CONSTRAINT fk_password_history_credential FOREIGN KEY (credential_id) REFERENCES credential (credential_id),
    CONSTRAINT fk_password_history_customer   FOREIGN KEY (customer_id)   REFERENCES customer (customer_id)
);

-- -----------------------------------------------------------------------------
-- 12. fds_detection  (FDS탐지결과, customer/fds_rule 1:N)
--     fds_detection_event_reference_id: 거래계 이벤트 soft reference (FK 미설정)
-- -----------------------------------------------------------------------------
CREATE TABLE fds_detection (
    fds_detection_id                 BIGINT         GENERATED ALWAYS AS IDENTITY,
    customer_id                      BIGINT         NOT NULL,
    fds_rule_id                      BIGINT         NOT NULL,
    fds_detection_event_type_code    VARCHAR(30)    NOT NULL,
    fds_detection_event_reference_id BIGINT         NOT NULL,
    fds_detected_at                  TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    fds_detection_status_code        VARCHAR(20)    NOT NULL DEFAULT 'PENDING',  -- PENDING/CONFIRMED/FALSE_POSITIVE
    created_at                       TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                       BIGINT,
    updated_at                       TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by                       BIGINT,
    deleted_at                       TIMESTAMPTZ(3),
    deleted_by                       BIGINT,
    version                          INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_fds_detection PRIMARY KEY (fds_detection_id),
    CONSTRAINT fk_fds_detection_customer FOREIGN KEY (customer_id) REFERENCES customer (customer_id),
    CONSTRAINT fk_fds_detection_rule     FOREIGN KEY (fds_rule_id) REFERENCES fds_rule (fds_rule_id),
    CONSTRAINT chk_fds_detection_status
        CHECK (fds_detection_status_code IN ('PENDING','CONFIRMED','FALSE_POSITIVE'))
);

-- -----------------------------------------------------------------------------
-- 13. fds_incident  (FDS사고처리, fds_detection 1:N)
-- -----------------------------------------------------------------------------
CREATE TABLE fds_incident (
    fds_incident_id                  BIGINT         GENERATED ALWAYS AS IDENTITY,
    fds_detection_id                 BIGINT         NOT NULL,
    fds_incident_handler_employee_id BIGINT,                    -- soft ref
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
    CONSTRAINT fk_fds_incident_detection FOREIGN KEY (fds_detection_id) REFERENCES fds_detection (fds_detection_id),
    CONSTRAINT chk_fds_incident_fss_reported CHECK (fds_incident_fss_reported_yn IN ('T','F'))
);

-- -----------------------------------------------------------------------------
-- 14. identity_verification  (본인확인이력, customer nullable / mobile_auth 1:N)
--     V18: rrn_encrypted(주민번호 암호문, 평문 미보관) + consumed_*(가입 소비)
-- -----------------------------------------------------------------------------
CREATE TABLE identity_verification (
    identity_verification_id                    BIGINT         GENERATED ALWAYS AS IDENTITY,
    customer_id                                 BIGINT,
    mobile_auth_id                              BIGINT         NOT NULL,
    identity_verification_agency_code           VARCHAR(30)    NOT NULL,    -- NICE/KCB/SCI/PASS
    identity_verification_purpose_code          VARCHAR(30)    NOT NULL,
    identity_verification_ci_value              VARCHAR(88)    NOT NULL,
    identity_verification_name                  VARCHAR(50)    NOT NULL,
    identity_verification_birth_date            CHAR(8)        NOT NULL,
    identity_verification_gender_code           CHAR(1)        NOT NULL,
    identity_verification_nationality_type_code VARCHAR(20)    NOT NULL,
    identity_verification_telecom_carrier_code  VARCHAR(20),
    identity_verification_phone_number          VARCHAR(20),
    identity_verified_at                        TIMESTAMPTZ(3) NOT NULL,
    rrn_encrypted                               VARCHAR(255),               -- V18: AES-256 주민번호 암호문
    consumed_yn                                 CHAR(1)        NOT NULL DEFAULT 'F',  -- V18
    consumed_customer_id                        BIGINT,                     -- V18
    consumed_at                                 TIMESTAMPTZ(3),             -- V18
    created_at                                  TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                                  BIGINT,
    updated_at                                  TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by                                  BIGINT,
    deleted_at                                  TIMESTAMPTZ(3),
    deleted_by                                  BIGINT,
    version                                     INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_identity_verification PRIMARY KEY (identity_verification_id),
    CONSTRAINT fk_identity_verification_customer    FOREIGN KEY (customer_id)    REFERENCES customer (customer_id),
    CONSTRAINT fk_identity_verification_mobile_auth FOREIGN KEY (mobile_auth_id) REFERENCES mobile_auth (mobile_auth_id),
    CONSTRAINT chk_identity_verification_agency
        CHECK (identity_verification_agency_code IN ('NICE','KCB','SCI','PASS')),
    CONSTRAINT chk_identity_verification_consumed CHECK (consumed_yn IN ('T','F'))
);

-- -----------------------------------------------------------------------------
-- 15. certificate_use  (인증서사용이력, certificate/customer 1:N)
--     certificate_use_target_transaction_id: 거래계 미구축 → VARCHAR(50) 임시 soft ref
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
    CONSTRAINT fk_certificate_use_certificate FOREIGN KEY (certificate_id) REFERENCES certificate (certificate_id),
    CONSTRAINT fk_certificate_use_customer    FOREIGN KEY (customer_id)    REFERENCES customer (customer_id)
);

-- -----------------------------------------------------------------------------
-- 16. qr_login_token  (QR로그인토큰, customer/login_session FK) — V4
--     플로우: PC 생성(PENDING) → 모바일 스캔(SCANNED) → 승인(APPROVED) → PC 세션 발급
-- -----------------------------------------------------------------------------
CREATE TABLE qr_login_token (
    qr_token_id          BIGINT         GENERATED ALWAYS AS IDENTITY,
    qr_token_hash        VARCHAR(255)   NOT NULL,
    customer_id          BIGINT,                              -- 스캔 전 NULL
    session_id           VARCHAR(64),                         -- 승인 후 연결
    qr_status_code       VARCHAR(20)    NOT NULL DEFAULT 'PENDING',  -- PENDING/SCANNED/APPROVED/EXPIRED/CANCELLED
    request_ip           VARCHAR(45)    NOT NULL,
    request_channel_code VARCHAR(20)    NOT NULL DEFAULT 'WEB',
    issued_at            TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    expiry_at            TIMESTAMPTZ(3) NOT NULL,
    scanned_at           TIMESTAMPTZ(3),
    approved_at          TIMESTAMPTZ(3),
    created_at           TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by           BIGINT,
    updated_at           TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by           BIGINT,
    deleted_at           TIMESTAMPTZ(3),
    deleted_by           BIGINT,
    version              INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_qr_login_token PRIMARY KEY (qr_token_id),
    CONSTRAINT uq_qr_login_token_hash UNIQUE (qr_token_hash),
    CONSTRAINT fk_qr_login_token_customer FOREIGN KEY (customer_id) REFERENCES customer (customer_id),
    CONSTRAINT fk_qr_login_token_session  FOREIGN KEY (session_id)  REFERENCES login_session (session_id),
    CONSTRAINT chk_qr_login_token_status
        CHECK (qr_status_code IN ('PENDING','SCANNED','APPROVED','EXPIRED','CANCELLED'))
);

-- -----------------------------------------------------------------------------
-- 17. withdrawal_account  (출금계좌, customer 1:N) — V6
-- -----------------------------------------------------------------------------
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
    CONSTRAINT fk_withdrawal_account_customer FOREIGN KEY (customer_id) REFERENCES customer (customer_id)
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

-- QR 토큰 해시 조회 (PC 폴링용) — V4
CREATE INDEX idx_qr_login_token_hash_status
    ON qr_login_token (qr_token_hash, qr_status_code)
    WHERE deleted_at IS NULL;

-- 만료된 QR 토큰 정리 배치용 — V4
CREATE INDEX idx_qr_login_token_expiry
    ON qr_login_token (expiry_at)
    WHERE qr_status_code = 'PENDING' AND deleted_at IS NULL;

-- 동일 고객 활성 출금계좌 계좌번호 중복 금지 — V6
CREATE UNIQUE INDEX uq_withdrawal_account_active
    ON withdrawal_account (customer_id, account_number)
    WHERE deleted_at IS NULL;
