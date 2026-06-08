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
