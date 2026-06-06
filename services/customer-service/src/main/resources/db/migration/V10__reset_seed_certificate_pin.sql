-- Reset seed certificate PINs so the web demo financial certificate login works.
-- V5 added cert_pin_hash but never backfilled the V4 seed certs, leaving them NULL.
-- A NULL hash makes CertLoginService fall back to the account password (Employee1234!),
-- which can never match the 6-digit PIN the web cert login pad sends.
-- PIN: 123456
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
