-- customer_id=1 (홍길동) 로그인 계정 시드
-- PIN: 123456 (BCrypt strength 10)
-- 금융인증서(CERT_FIN) 로그인 시 이 password_hash와 대조함

INSERT INTO credential (
    customer_id,
    login_id,
    password_hash,
    password_changed_at,
    account_status_code,
    created_at,
    updated_at,
    version
)
SELECT
    1,
    'hong',
    '$2b$10$KUwtJaiDrZvYKo78TM8GuOEW0aGCk5Si9ZG.qhZRNy2s.R55TcfG.',
    NOW(),
    'ACTIVE',
    NOW(),
    NOW(),
    0
WHERE NOT EXISTS (
    SELECT 1 FROM credential WHERE customer_id = 1 AND deleted_at IS NULL
);
