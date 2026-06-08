-- Seed a 6-digit PIN for built-in test certificates.
-- PIN: 123456
UPDATE certificate
SET cert_pin_hash = '$2a$10$O1.JaZM1Uwq4GfH.NvzMwucOwzP4tul6ShBn/ApX6iwaWA3492N26',
    updated_at = NOW()
WHERE certificate_serial_number IN (
    'COMMON-TEST-2024-000001',
    'FINCERT-TEST-2024-000001',
    'AXFUL-TEST-2024-000001'
)
  AND cert_pin_hash IS NULL
  AND deleted_at IS NULL;
