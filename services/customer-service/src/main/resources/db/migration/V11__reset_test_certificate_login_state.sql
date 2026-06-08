-- Reset lock state for built-in test certificates after seeding their PIN.
UPDATE certificate
SET cert_login_failure_count = 0,
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
