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
