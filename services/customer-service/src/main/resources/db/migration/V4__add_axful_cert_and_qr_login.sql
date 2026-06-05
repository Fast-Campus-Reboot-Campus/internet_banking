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
