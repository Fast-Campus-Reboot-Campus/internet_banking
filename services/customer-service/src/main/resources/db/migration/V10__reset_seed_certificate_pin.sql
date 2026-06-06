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
