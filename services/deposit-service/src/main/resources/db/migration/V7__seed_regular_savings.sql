-- 발표용 정기적금(REGULAR) 상품 seed 추가

-- target_group_id=1 이 없으면 삽입 (V2 가 미적용된 환경 대응)
INSERT INTO deposit_target_groups (target_group_id, target_group_name, description, is_active)
VALUES (1, '개인고객', '개인 인터넷뱅킹 고객', TRUE)
ON CONFLICT (target_group_id) DO NOTHING;

INSERT INTO deposit_banking_products (
    banking_product_id, deposit_product_type, deposit_product_name, description, department_id,
    base_interest_rate, min_join_amount, max_join_amount, min_period_month, max_period_month,
    is_early_termination_allowed, is_tax_benefit_available, is_auto_renewal_available,
    released_at, deposit_product_status
)
VALUES
    (4, 'SAVINGS', '포스트맨 정기적금(12개월)', 'Postman 테스트용 정기적금 상품 (12개월)', 9001, 3.00, 10000.00, 30000000.00, 12, 36, TRUE,  TRUE, FALSE, '20260101', 'SELLING'),
    (5, 'SAVINGS', '포스트맨 정기적금(24개월)', 'Postman 테스트용 정기적금 상품 (24개월)', 9001, 3.50, 30000.00, 50000000.00, 24, 60, TRUE,  TRUE, FALSE, '20260101', 'SELLING')
ON CONFLICT (banking_product_id) DO NOTHING;

INSERT INTO deposit_savings_products (savings_product_id, banking_product_id, saving_type, monthly_payment_min_amount, monthly_payment_max_amount)
VALUES
    (2, 4, 'REGULAR', 10000.00, 1000000.00),
    (3, 5, 'REGULAR', 30000.00, 3000000.00)
ON CONFLICT (savings_product_id) DO NOTHING;

INSERT INTO banking_deposit_product_interest_rates (
    rate_id, banking_product_id, rate_type, minimum_contract_period, maximum_contract_period,
    minimum_join_amount, maximum_join_amount, rate, condition_description,
    effective_start_date, effective_end_date, is_active
)
VALUES
    (5, 4, 'BASE', 12, 36, 10000.00, 30000000.00, 3.00, '정기적금 12개월 기본 금리', '20260101', NULL, TRUE),
    (6, 5, 'BASE', 24, 60, 30000.00, 50000000.00, 3.50, '정기적금 24개월 기본 금리', '20260101', NULL, TRUE)
ON CONFLICT (rate_id) DO NOTHING;

INSERT INTO banking_deposit_product_join_channels (channel_id, banking_product_id, join_channel_code)
VALUES
    (6, 4, 'WEB'),
    (7, 4, 'MOBILE'),
    (8, 5, 'WEB'),
    (9, 5, 'MOBILE')
ON CONFLICT (channel_id) DO NOTHING;

INSERT INTO banking_deposit_product_target_groups (banking_product_id, target_group_id)
VALUES (4, 1), (5, 1)
ON CONFLICT (banking_product_id, target_group_id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('deposit_banking_products',                 'banking_product_id'), COALESCE((SELECT MAX(banking_product_id) FROM deposit_banking_products), 1), TRUE);
SELECT setval(pg_get_serial_sequence('deposit_savings_products',                 'savings_product_id'), COALESCE((SELECT MAX(savings_product_id) FROM deposit_savings_products), 1), TRUE);
SELECT setval(pg_get_serial_sequence('banking_deposit_product_interest_rates',   'rate_id'),            COALESCE((SELECT MAX(rate_id)            FROM banking_deposit_product_interest_rates), 1), TRUE);
SELECT setval(pg_get_serial_sequence('banking_deposit_product_join_channels',    'channel_id'),         COALESCE((SELECT MAX(channel_id)         FROM banking_deposit_product_join_channels), 1), TRUE);
SELECT setval(pg_get_serial_sequence('deposit_target_groups',                    'target_group_id'),    COALESCE((SELECT MAX(target_group_id)    FROM deposit_target_groups), 1), TRUE);
