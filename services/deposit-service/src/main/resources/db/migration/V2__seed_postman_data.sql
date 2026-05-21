-- Seed data for local Postman testing (ERD 기준 테이블명 적용)

INSERT INTO deposit_departments (department_id, department_code, department_name, department_type, is_active)
VALUES
    (1, 'DEP-PRODUCT', '수신상품부', 'PRODUCT', TRUE),
    (2, 'DEP-SALES',   '수신영업부', 'SALES',   TRUE)
ON CONFLICT (department_id) DO NOTHING;

INSERT INTO deposit_banking_products (
    banking_product_id, deposit_product_type, deposit_product_name, description, department_id,
    base_interest_rate, min_join_amount, max_join_amount, min_period_month, max_period_month,
    is_early_termination_allowed, is_tax_benefit_available, is_auto_renewal_available,
    released_at, deposit_product_status
)
VALUES
    (1, 'DEPOSIT',      '포스트맨 정기예금', 'Postman 테스트용 정기예금 상품', 1, 3.20, 100000.00,  100000000.00, 6,  36,  TRUE,  TRUE, TRUE,  '20260101', 'SELLING'),
    (2, 'SAVINGS',      '포스트맨 자유적금', 'Postman 테스트용 자유적금 상품', 1, 3.60, 10000.00,   50000000.00,  6,  36,  TRUE,  TRUE, FALSE, '20260101', 'SELLING'),
    (3, 'SUBSCRIPTION', '포스트맨 청약저축', 'Postman 테스트용 청약 상품',     1, 2.80, 20000.00,   10000000.00,  12, 120, FALSE, TRUE, FALSE, '20260101', 'SELLING')
ON CONFLICT (banking_product_id) DO NOTHING;

INSERT INTO banking_deposit_products (deposit_product_id, banking_product_id, deposit_type, is_compound_interest)
VALUES (1, 1, 'TERM', FALSE)
ON CONFLICT (deposit_product_id) DO NOTHING;

INSERT INTO deposit_savings_products (savings_product_id, banking_product_id, saving_type, monthly_payment_min_amount, monthly_payment_max_amount)
VALUES (1, 2, 'FREE', 10000.00, 1000000.00)
ON CONFLICT (savings_product_id) DO NOTHING;

INSERT INTO deposit_subscription_products (banking_product_id, monthly_payment_amount, min_monthly_payment, max_monthly_payment, max_recognized_payment_amount)
VALUES (3, 100000.00, 20000.00, 500000.00, 10000000.00)
ON CONFLICT (banking_product_id) DO NOTHING;

INSERT INTO deposit_target_groups (target_group_id, target_group_name, description, is_active)
VALUES
    (1, '개인고객', '개인 인터넷뱅킹 고객', TRUE),
    (2, '직장인',   '급여소득이 있는 고객', TRUE)
ON CONFLICT (target_group_id) DO NOTHING;

INSERT INTO banking_deposit_product_target_groups (banking_product_id, target_group_id)
VALUES (1, 1), (2, 1), (2, 2), (3, 1)
ON CONFLICT (banking_product_id, target_group_id) DO NOTHING;

INSERT INTO banking_deposit_product_join_channels (channel_id, banking_product_id, join_channel_code)
VALUES
    (1, 1, 'WEB'),
    (2, 1, 'MOBILE'),
    (3, 2, 'WEB'),
    (4, 2, 'MOBILE'),
    (5, 3, 'BRANCH')
ON CONFLICT (channel_id) DO NOTHING;

INSERT INTO banking_deposit_product_interest_rates (
    rate_id, banking_product_id, rate_type, minimum_contract_period, maximum_contract_period,
    minimum_join_amount, maximum_join_amount, rate, condition_description,
    effective_start_date, effective_end_date, is_active
)
VALUES
    (1, 1, 'BASE',         6,  36, 100000.00, 100000000.00, 3.20, '정기예금 기본 금리',   '20260101', NULL, TRUE),
    (2, 1, 'PREFERENTIAL', 12, 36, 1000000.00, NULL,         0.30, '비대면 가입 우대',     '20260101', NULL, TRUE),
    (3, 2, 'BASE',         6,  36, 10000.00,  50000000.00,  3.60, '자유적금 기본 금리',   '20260101', NULL, TRUE),
    (4, 3, 'BASE',         12, 120,20000.00,  10000000.00,  2.80, '청약저축 기본 금리',   '20260101', NULL, TRUE)
ON CONFLICT (rate_id) DO NOTHING;

INSERT INTO deposit_special_terms (
    special_term_id, special_term_name, special_term_content, special_term_summary,
    is_required, is_electronic_agreement_allowed, special_term_version, started_at, status
)
VALUES
    (1, '비대면 가입 특약',   '비대면 채널로 가입하는 경우 적용되는 테스트 특약입니다.', '비대면 가입 우대 조건', TRUE,  TRUE, '1.0', '20260101', 'ACTIVE'),
    (2, '자동이체 우대 특약', '자동이체를 설정한 경우 적용되는 테스트 특약입니다.',     '자동이체 우대 조건',   FALSE, TRUE, '1.0', '20260101', 'ACTIVE')
ON CONFLICT (special_term_id) DO NOTHING;

INSERT INTO banking_deposit_product_special_terms (deposit_product_special_term_id, banking_product_id, special_term_id, is_required)
VALUES
    (1, 1, 1, TRUE),
    (2, 2, 2, FALSE)
ON CONFLICT (deposit_product_special_term_id) DO NOTHING;

INSERT INTO deposit_contracts (
    contract_id, contract_number, customer_id, banking_product_id, join_amount,
    contract_interest_rate, total_preferential_rate, final_interest_rate,
    tax_benefit_type, contract_period_month, started_at, maturity_at,
    is_auto_renewal, auto_transfer_enabled, auto_transfer_day,
    contract_status, join_channel
)
VALUES
    (1, 'CTR-20260519-0001', 'CUST001',        1, 1000000.00, 3.20, 0.30, 3.50, 'GENERAL', 12, '20260519', '20270519', TRUE,  FALSE, NULL, 'ACTIVE', 'WEB'),
    (2, 'CTR-20260519-0002', 'CUST001',        2, 50000.00,   3.60, 0.00, 3.60, 'GENERAL', 24, '20260519', '20280519', FALSE, TRUE,  25,   'ACTIVE', 'MOBILE'),
    (3, 'CTR-20260519-0003', 'CUST_NO_ACCOUNT',1, 300000.00,  3.20, 0.00, 3.20, 'GENERAL', 12, '20260519', '20270519', FALSE, FALSE, NULL, 'ACTIVE', 'WEB')
ON CONFLICT (contract_id) DO NOTHING;

INSERT INTO deposit_accounts (
    account_id, account_number, customer_id, contract_id, account_type, saving_type,
    account_alias, balance, total_paid_amount, total_interest_amount,
    account_password, daily_withdraw_limit, daily_withdraw_count_limit, atm_withdraw_limit,
    is_withdrawable, is_online_banking_enabled, is_mobile_banking_enabled,
    account_status, opened_at, maturity_at
)
VALUES
    (1, 'ACC-20260519-0001', 'CUST001', 1, 'DEPOSIT', NULL,   '포스트맨 예금계좌', 1000000.00, 1000000.00, 0.00, '1234', 10000000.00, 10, 1000000.00, TRUE, TRUE, TRUE, 'ACTIVE', '20260519', '20270519'),
    (2, 'ACC-20260519-0002', 'CUST001', 2, 'SAVINGS', 'FREE', '포스트맨 적금계좌',  100000.00,  100000.00, 0.00, '1234', 5000000.00,  10,  500000.00, TRUE, TRUE, TRUE, 'ACTIVE', '20260519', '20280519')
ON CONFLICT (account_id) DO NOTHING;

INSERT INTO deposit_contract_applied_rates (applied_rate_id, contract_id, rate_id, applied_rate, condition_verified_yn)
VALUES
    (1, 1, 1, 3.20, TRUE),
    (2, 1, 2, 0.30, TRUE),
    (3, 2, 3, 3.60, TRUE)
ON CONFLICT (applied_rate_id) DO NOTHING;

INSERT INTO deposit_contract_special_term_agreements (
    special_agreement_id, contract_id, special_term_id, is_agreed,
    agreed_at, agreement_ip_address, agreement_device_info, is_electronic_signed
)
VALUES
    (1, 1, 1, TRUE, '20260519', '127.0.0.1', 'Postman', TRUE),
    (2, 2, 2, TRUE, '20260519', '127.0.0.1', 'Postman', TRUE)
ON CONFLICT (special_agreement_id) DO NOTHING;

INSERT INTO deposit_interest_history (
    interest_id, contract_id, account_id, applied_interest_rate,
    interest_calculation_start_date, interest_calculation_end_date,
    interest_occurred_at, interest_amount, tax_benefit_type, applied_tax_rate,
    interest_before_tax, interest_tax_amount, local_income_tax_amount,
    interest_after_tax, interest_reason, interest_paid_at
)
VALUES
    (1, 1, 1, 3.50, '20260519', '20260619', NOW(), 2468.00, 'GENERAL', 15.4000, 2917.00, 404.00, 45.00, 2468.00, 'REGULAR_INTEREST', NOW())
ON CONFLICT (interest_id) DO NOTHING;

INSERT INTO deposit_transactions (
    transaction_id, transaction_number, account_id, contract_id, transaction_type,
    direction_type, amount, balance_before, balance_after, available_balance_after,
    channel_type, transaction_memo, transaction_summary, transaction_at, posted_at,
    payment_round
)
VALUES
    (1, 'DEP-20260519-0001', 1, NULL, 'DEPOSIT',         'IN', 1000000.00, 0.00,       1000000.00, 1000000.00, 'SYSTEM', '초기 테스트 입금', '초기 입금',    NOW(), NOW(), NULL),
    (2, 'SAV-20260519-0001', 2, 2,    'SAVINGS_PAYMENT', 'IN',  100000.00, 0.00,        100000.00,  100000.00, 'SYSTEM', '초기 적금 납입',   '초기 적금 납입', NOW(), NOW(), 1)
ON CONFLICT (transaction_id) DO NOTHING;

-- 시퀀스 동기화
SELECT setval(pg_get_serial_sequence('deposit_departments',                           'department_id'),               COALESCE((SELECT MAX(department_id)               FROM deposit_departments),                           1), TRUE);
SELECT setval(pg_get_serial_sequence('deposit_banking_products',                      'banking_product_id'),          COALESCE((SELECT MAX(banking_product_id)           FROM deposit_banking_products),                      1), TRUE);
SELECT setval(pg_get_serial_sequence('banking_deposit_products',                      'deposit_product_id'),          COALESCE((SELECT MAX(deposit_product_id)           FROM banking_deposit_products),                      1), TRUE);
SELECT setval(pg_get_serial_sequence('deposit_savings_products',                      'savings_product_id'),          COALESCE((SELECT MAX(savings_product_id)           FROM deposit_savings_products),                      1), TRUE);
SELECT setval(pg_get_serial_sequence('banking_deposit_product_join_channels',         'channel_id'),                  COALESCE((SELECT MAX(channel_id)                   FROM banking_deposit_product_join_channels),          1), TRUE);
SELECT setval(pg_get_serial_sequence('banking_deposit_product_interest_rates',        'rate_id'),                     COALESCE((SELECT MAX(rate_id)                      FROM banking_deposit_product_interest_rates),         1), TRUE);
SELECT setval(pg_get_serial_sequence('deposit_target_groups',                         'target_group_id'),             COALESCE((SELECT MAX(target_group_id)              FROM deposit_target_groups),                          1), TRUE);
SELECT setval(pg_get_serial_sequence('deposit_special_terms',                         'special_term_id'),             COALESCE((SELECT MAX(special_term_id)              FROM deposit_special_terms),                          1), TRUE);
SELECT setval(pg_get_serial_sequence('banking_deposit_product_special_terms',         'deposit_product_special_term_id'), COALESCE((SELECT MAX(deposit_product_special_term_id) FROM banking_deposit_product_special_terms),     1), TRUE);
SELECT setval(pg_get_serial_sequence('deposit_contracts',                             'contract_id'),                 COALESCE((SELECT MAX(contract_id)                  FROM deposit_contracts),                              1), TRUE);
SELECT setval(pg_get_serial_sequence('deposit_accounts',                              'account_id'),                  COALESCE((SELECT MAX(account_id)                   FROM deposit_accounts),                               1), TRUE);
SELECT setval(pg_get_serial_sequence('deposit_contract_applied_rates',                'applied_rate_id'),             COALESCE((SELECT MAX(applied_rate_id)              FROM deposit_contract_applied_rates),                 1), TRUE);
SELECT setval(pg_get_serial_sequence('deposit_contract_special_term_agreements',      'special_agreement_id'),        COALESCE((SELECT MAX(special_agreement_id)         FROM deposit_contract_special_term_agreements),       1), TRUE);
SELECT setval(pg_get_serial_sequence('deposit_interest_history',                      'interest_id'),                 COALESCE((SELECT MAX(interest_id)                  FROM deposit_interest_history),                       1), TRUE);
SELECT setval(pg_get_serial_sequence('deposit_transactions',                          'transaction_id'),              COALESCE((SELECT MAX(transaction_id)               FROM deposit_transactions),                           1), TRUE);
