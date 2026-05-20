-- PostgreSQL DDL for the current ERD
-- Baseline: ERD_도메인구조.md / teamproject.drawio, 22 tables

BEGIN;

CREATE TYPE product_type_enum AS ENUM ('DEPOSIT', 'SAVINGS', 'SUBSCRIPTION');
CREATE TYPE product_status_enum AS ENUM ('SELLING', 'SUSPENDED', 'EXPIRED');
CREATE TYPE deposit_type_enum AS ENUM ('TERM', 'DEMAND');
CREATE TYPE saving_type_enum AS ENUM ('REGULAR', 'FREE');
CREATE TYPE join_channel_enum AS ENUM ('BRANCH', 'WEB', 'MOBILE', 'TELL', 'RECRUITER', 'ETC');
CREATE TYPE rate_type_enum AS ENUM ('BASE', 'PERIOD_BASE', 'PREFERENTIAL', 'EARLY_TERMINATION');
CREATE TYPE contract_status_enum AS ENUM ('ACTIVE', 'MATURED', 'TERMINATED', 'SUSPENDED');
CREATE TYPE tax_benefit_type_enum AS ENUM ('GENERAL', 'NON_TAXABLE', 'REDUCED_TAX');
CREATE TYPE account_status_enum AS ENUM ('ACTIVE', 'DORMANT', 'SUSPENDED', 'CLOSED');
CREATE TYPE transaction_type_enum AS ENUM ('DEPOSIT', 'WITHDRAW', 'TRANSFER', 'INTEREST', 'SAVINGS_PAYMENT', 'PAYMENT', 'REVERSAL');
CREATE TYPE direction_type_enum AS ENUM ('IN', 'OUT');
CREATE TYPE transaction_status_enum AS ENUM ('SUCCESS', 'FAILED', 'CANCELED', 'PENDING');
CREATE TYPE transaction_channel_enum AS ENUM ('BRANCH', 'ATM', 'INTERNET', 'MOBILE', 'SYSTEM');
CREATE TYPE transfer_type_enum AS ENUM ('INTERNAL', 'EXTERNAL', 'AUTO', 'SCHEDULED');
CREATE TYPE payment_method_enum AS ENUM ('CARD', 'ACCOUNT_TRANSFER', 'EASY_PAY');
CREATE TYPE failure_type_enum AS ENUM ('TRANSFER', 'CARD_PAYMENT', 'AUTH', 'LIMIT', 'SYSTEM');
CREATE TYPE failure_reason_code_enum AS ENUM ('INSUFFICIENT_BALANCE', 'LIMIT_EXCEEDED', 'INVALID_ACCOUNT', 'CARD_DECLINED', 'AUTH_FAILED', 'SYSTEM_ERROR');
CREATE TYPE interest_reason_enum AS ENUM ('REGULAR_INTEREST', 'MATURITY_INTEREST', 'BONUS_INTEREST');
CREATE TYPE special_term_status_enum AS ENUM ('ACTIVE', 'INACTIVE');
CREATE TYPE recognition_status_enum AS ENUM ('RECOGNIZED', 'PARTIAL', 'REJECTED', 'PENDING');
CREATE TYPE department_type_enum AS ENUM ('PRODUCT', 'SALES', 'OPERATION', 'RISK', 'IT');
CREATE TYPE employment_status_enum AS ENUM ('ACTIVE', 'ON_LEAVE', 'RESIGNED', 'DISMISSED');
CREATE TYPE employee_role_enum AS ENUM ('TELLER', 'MANAGER', 'ADMIN');
CREATE TYPE query_type_enum AS ENUM ('CUSTOMER', 'ACCOUNT', 'LOAN', 'CONSULTATION', 'TRANSACTION');

CREATE TABLE departments (
    department_id BIGSERIAL PRIMARY KEY,
    department_code VARCHAR(50) NOT NULL UNIQUE,
    department_name VARCHAR(100) NOT NULL,
    parent_department_id BIGINT,
    department_type VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT fk_departments_parent
        FOREIGN KEY (parent_department_id)
        REFERENCES departments (department_id)
        ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE TABLE branches (
    branch_id BIGSERIAL PRIMARY KEY,
    branch_code VARCHAR(20) NOT NULL UNIQUE,
    branch_name VARCHAR(100) NOT NULL,
    department_id BIGINT NOT NULL,
    address VARCHAR(300),
    phone_number VARCHAR(20),
    region VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    opened_at CHAR(8),
    closed_at CHAR(8),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT fk_branches_department
        FOREIGN KEY (department_id)
        REFERENCES departments (department_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_branches_opened_at_yyyymmdd CHECK (opened_at IS NULL OR opened_at ~ '^[0-9]{8}$'),
    CONSTRAINT ck_branches_closed_at_yyyymmdd CHECK (closed_at IS NULL OR closed_at ~ '^[0-9]{8}$')
);

CREATE TABLE employees (
    employee_id BIGSERIAL PRIMARY KEY,
    employee_number VARCHAR(20) NOT NULL UNIQUE,
    employee_name VARCHAR(100) NOT NULL,
    department_id BIGINT NOT NULL,
    branch_id BIGINT,
    position VARCHAR(50),
    role VARCHAR(20) NOT NULL,
    employment_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    phone_number VARCHAR(20),
    email VARCHAR(100),
    joined_at CHAR(8),
    resigned_at CHAR(8),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT fk_employees_department
        FOREIGN KEY (department_id)
        REFERENCES departments (department_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_employees_branch
        FOREIGN KEY (branch_id)
        REFERENCES branches (branch_id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT ck_employees_joined_at_yyyymmdd CHECK (joined_at IS NULL OR joined_at ~ '^[0-9]{8}$'),
    CONSTRAINT ck_employees_resigned_at_yyyymmdd CHECK (resigned_at IS NULL OR resigned_at ~ '^[0-9]{8}$')
);

CREATE TABLE products (
    product_id BIGSERIAL PRIMARY KEY,
    product_type VARCHAR(20) NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    description TEXT,
    department_id BIGINT,
    base_interest_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
    preferential_rate_condition TEXT,
    min_join_amount NUMERIC(18,2),
    max_join_amount NUMERIC(18,2),
    min_period_month INT,
    max_period_month INT,
    is_early_termination_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    early_termination_rate NUMERIC(5,2),
    is_tax_benefit_available BOOLEAN NOT NULL DEFAULT FALSE,
    is_auto_renewal_available BOOLEAN NOT NULL DEFAULT FALSE,
    is_passbook_issued BOOLEAN NOT NULL DEFAULT FALSE,
    released_at CHAR(8),
    ended_at CHAR(8),
    product_status VARCHAR(20) NOT NULL DEFAULT 'SELLING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT fk_products_department
        FOREIGN KEY (department_id)
        REFERENCES departments (department_id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT ck_products_base_rate CHECK (base_interest_rate >= 0),
    CONSTRAINT ck_products_early_rate CHECK (early_termination_rate IS NULL OR early_termination_rate >= 0),
    CONSTRAINT ck_products_join_amount_range CHECK (min_join_amount IS NULL OR max_join_amount IS NULL OR min_join_amount <= max_join_amount),
    CONSTRAINT ck_products_period_range CHECK (min_period_month IS NULL OR max_period_month IS NULL OR min_period_month <= max_period_month),
    CONSTRAINT ck_products_released_at_yyyymmdd CHECK (released_at IS NULL OR released_at ~ '^[0-9]{8}$'),
    CONSTRAINT ck_products_ended_at_yyyymmdd CHECK (ended_at IS NULL OR ended_at ~ '^[0-9]{8}$')
);

CREATE TABLE deposit_products (
    deposit_product_id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    deposit_type VARCHAR(20) NOT NULL,
    is_compound_interest BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT fk_deposit_products_product
        FOREIGN KEY (product_id)
        REFERENCES products (product_id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE savings_products (
    savings_product_id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    saving_type VARCHAR(20) NOT NULL,
    monthly_payment_min_amount NUMERIC(18,2),
    monthly_payment_max_amount NUMERIC(18,2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT fk_savings_products_product
        FOREIGN KEY (product_id)
        REFERENCES products (product_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT ck_savings_products_monthly_amount_range
        CHECK (monthly_payment_min_amount IS NULL OR monthly_payment_max_amount IS NULL OR monthly_payment_min_amount <= monthly_payment_max_amount)
);

CREATE TABLE subscription_products (
    product_id BIGINT PRIMARY KEY,
    monthly_payment_amount NUMERIC(18,2) NOT NULL,
    min_monthly_payment NUMERIC(18,2),
    max_monthly_payment NUMERIC(18,2),
    max_recognized_payment_amount NUMERIC(18,2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT fk_subscription_products_product
        FOREIGN KEY (product_id)
        REFERENCES products (product_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT ck_subscription_products_monthly_amount CHECK (monthly_payment_amount >= 0),
    CONSTRAINT ck_subscription_products_monthly_range
        CHECK (min_monthly_payment IS NULL OR max_monthly_payment IS NULL OR min_monthly_payment <= max_monthly_payment)
);

CREATE TABLE product_join_channels (
    product_join_channel_id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    join_channel_code VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_product_join_channels_product
        FOREIGN KEY (product_id)
        REFERENCES products (product_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT uq_product_join_channels_product_channel UNIQUE (product_id, join_channel_code)
);

CREATE TABLE target_groups (
    target_group_id BIGSERIAL PRIMARY KEY,
    target_group_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE TABLE product_target_groups (
    product_id BIGINT NOT NULL,
    target_group_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (product_id, target_group_id),
    CONSTRAINT fk_product_target_groups_product
        FOREIGN KEY (product_id)
        REFERENCES products (product_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_product_target_groups_target_group
        FOREIGN KEY (target_group_id)
        REFERENCES target_groups (target_group_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE product_interest_rates (
    rate_id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    rate_type VARCHAR(30) NOT NULL,
    minimum_contract_period INT,
    maximum_contract_period INT,
    minimum_join_amount NUMERIC(18,2),
    maximum_join_amount NUMERIC(18,2),
    rate NUMERIC(5,2) NOT NULL,
    condition_description TEXT,
    effective_start_date CHAR(8) NOT NULL,
    effective_end_date CHAR(8),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT fk_product_interest_rates_product
        FOREIGN KEY (product_id)
        REFERENCES products (product_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_product_interest_rates_rate CHECK (rate >= 0),
    CONSTRAINT ck_product_interest_rates_period_range
        CHECK (minimum_contract_period IS NULL OR maximum_contract_period IS NULL OR minimum_contract_period <= maximum_contract_period),
    CONSTRAINT ck_product_interest_rates_amount_range
        CHECK (minimum_join_amount IS NULL OR maximum_join_amount IS NULL OR minimum_join_amount <= maximum_join_amount),
    CONSTRAINT ck_product_interest_rates_start_yyyymmdd CHECK (effective_start_date ~ '^[0-9]{8}$'),
    CONSTRAINT ck_product_interest_rates_end_yyyymmdd CHECK (effective_end_date IS NULL OR effective_end_date ~ '^[0-9]{8}$'),
    CONSTRAINT ck_product_interest_rates_date_range CHECK (effective_end_date IS NULL OR effective_start_date <= effective_end_date)
);

CREATE TABLE special_terms (
    special_term_id BIGSERIAL PRIMARY KEY,
    special_term_name VARCHAR(200) NOT NULL,
    special_term_content TEXT NOT NULL,
    special_term_summary TEXT,
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    is_electronic_agreement_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    special_term_version VARCHAR(20) NOT NULL,
    started_at CHAR(8),
    ended_at CHAR(8),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    status_changed_at CHAR(8),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT uq_special_terms_name_version UNIQUE (special_term_name, special_term_version),
    CONSTRAINT ck_special_terms_started_at_yyyymmdd CHECK (started_at IS NULL OR started_at ~ '^[0-9]{8}$'),
    CONSTRAINT ck_special_terms_ended_at_yyyymmdd CHECK (ended_at IS NULL OR ended_at ~ '^[0-9]{8}$'),
    CONSTRAINT ck_special_terms_status_changed_at_yyyymmdd CHECK (status_changed_at IS NULL OR status_changed_at ~ '^[0-9]{8}$'),
    CONSTRAINT ck_special_terms_date_range CHECK (ended_at IS NULL OR started_at IS NULL OR started_at <= ended_at)
);

CREATE TABLE product_special_terms (
    product_special_term_id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    special_term_id BIGINT NOT NULL,
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT fk_product_special_terms_product
        FOREIGN KEY (product_id)
        REFERENCES products (product_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_product_special_terms_special_term
        FOREIGN KEY (special_term_id)
        REFERENCES special_terms (special_term_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT uq_product_special_terms_product_term UNIQUE (product_id, special_term_id)
);

CREATE TABLE special_term_history (
    history_id BIGSERIAL PRIMARY KEY,
    special_term_id BIGINT NOT NULL,
    previous_version VARCHAR(20),
    changed_version VARCHAR(20) NOT NULL,
    change_reason TEXT,
    changed_at CHAR(8) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_special_term_history_special_term
        FOREIGN KEY (special_term_id)
        REFERENCES special_terms (special_term_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_special_term_history_changed_at_yyyymmdd CHECK (changed_at ~ '^[0-9]{8}$')
);

CREATE TABLE contracts (
    contract_id BIGSERIAL PRIMARY KEY,
    contract_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id VARCHAR(30) NOT NULL,
    product_id BIGINT NOT NULL,
    is_monthly_payment BOOLEAN NOT NULL DEFAULT FALSE,
    payment_count_total INT,
    monthly_payment_day INT,
    join_amount NUMERIC(18,2) NOT NULL,
    contract_interest_rate NUMERIC(5,2) NOT NULL,
    total_preferential_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
    final_interest_rate NUMERIC(5,2) NOT NULL,
    tax_benefit_type VARCHAR(20) NOT NULL DEFAULT 'GENERAL',
    applied_tax_rate NUMERIC(5,2) NOT NULL DEFAULT 15.40,
    expected_interest_amount NUMERIC(18,2),
    contract_period_month INT NOT NULL,
    started_at CHAR(8) NOT NULL,
    maturity_at CHAR(8),
    terminated_at CHAR(8),
    termination_reason VARCHAR(200),
    is_auto_renewal BOOLEAN NOT NULL DEFAULT FALSE,
    auto_transfer_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    auto_transfer_day INT,
    contract_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    status_changed_at CHAR(8),
    join_channel VARCHAR(20) NOT NULL,
    branch_id BIGINT,
    branch_code VARCHAR(20),
    branch_name VARCHAR(100),
    manager_id BIGINT,
    manager_name VARCHAR(100),
    is_proxy_joined BOOLEAN NOT NULL DEFAULT FALSE,
    is_power_of_attorney_verified BOOLEAN NOT NULL DEFAULT FALSE,
    power_of_attorney_file_url VARCHAR(500),
    terms_file_url VARCHAR(500),
    contract_file_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT fk_contracts_product
        FOREIGN KEY (product_id)
        REFERENCES products (product_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_contracts_branch
        FOREIGN KEY (branch_id)
        REFERENCES branches (branch_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_contracts_manager
        FOREIGN KEY (manager_id)
        REFERENCES employees (employee_id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT ck_contracts_join_amount CHECK (join_amount >= 0),
    CONSTRAINT ck_contracts_rates CHECK (contract_interest_rate >= 0 AND total_preferential_rate >= 0 AND final_interest_rate >= 0),
    CONSTRAINT ck_contracts_tax_rate CHECK (applied_tax_rate >= 0),
    CONSTRAINT ck_contracts_expected_interest CHECK (expected_interest_amount IS NULL OR expected_interest_amount >= 0),
    CONSTRAINT ck_contracts_period CHECK (contract_period_month > 0),
    CONSTRAINT ck_contracts_payment_count CHECK (payment_count_total IS NULL OR payment_count_total > 0),
    CONSTRAINT ck_contracts_monthly_payment_day CHECK (monthly_payment_day IS NULL OR monthly_payment_day BETWEEN 1 AND 31),
    CONSTRAINT ck_contracts_auto_transfer_day CHECK (auto_transfer_day IS NULL OR auto_transfer_day BETWEEN 1 AND 31),
    CONSTRAINT ck_contracts_branch_required CHECK (join_channel <> 'BRANCH' OR branch_id IS NOT NULL),
    CONSTRAINT ck_contracts_power_of_attorney CHECK (is_proxy_joined = FALSE OR is_power_of_attorney_verified = TRUE),
    CONSTRAINT ck_contracts_started_at_yyyymmdd CHECK (started_at ~ '^[0-9]{8}$'),
    CONSTRAINT ck_contracts_maturity_at_yyyymmdd CHECK (maturity_at IS NULL OR maturity_at ~ '^[0-9]{8}$'),
    CONSTRAINT ck_contracts_terminated_at_yyyymmdd CHECK (terminated_at IS NULL OR terminated_at ~ '^[0-9]{8}$'),
    CONSTRAINT ck_contracts_status_changed_at_yyyymmdd CHECK (status_changed_at IS NULL OR status_changed_at ~ '^[0-9]{8}$'),
    CONSTRAINT ck_contracts_maturity_range CHECK (maturity_at IS NULL OR started_at <= maturity_at)
);

CREATE TABLE contract_applied_rates (
    applied_rate_id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    rate_id BIGINT NOT NULL,
    applied_rate NUMERIC(5,2) NOT NULL,
    condition_verified_yn BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT fk_contract_applied_rates_contract
        FOREIGN KEY (contract_id)
        REFERENCES contracts (contract_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_contract_applied_rates_rate
        FOREIGN KEY (rate_id)
        REFERENCES product_interest_rates (rate_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_contract_applied_rates_applied_rate CHECK (applied_rate >= 0),
    CONSTRAINT uq_contract_applied_rates_contract_rate UNIQUE (contract_id, rate_id)
);

CREATE TABLE contract_special_term_agreements (
    special_agreement_id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    special_term_id BIGINT NOT NULL,
    is_agreed BOOLEAN NOT NULL,
    agreed_at CHAR(8),
    agreement_ip_address VARCHAR(45),
    agreement_device_info VARCHAR(255),
    is_electronic_signed BOOLEAN NOT NULL DEFAULT FALSE,
    is_agreement_withdrawn BOOLEAN NOT NULL DEFAULT FALSE,
    agreement_withdrawn_at CHAR(8),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT fk_contract_special_term_agreements_contract
        FOREIGN KEY (contract_id)
        REFERENCES contracts (contract_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_contract_special_term_agreements_special_term
        FOREIGN KEY (special_term_id)
        REFERENCES special_terms (special_term_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT uq_contract_special_term_agreements_contract_term UNIQUE (contract_id, special_term_id),
    CONSTRAINT ck_contract_special_term_agreements_agreed_at CHECK (agreed_at IS NULL OR agreed_at ~ '^[0-9]{8}$'),
    CONSTRAINT ck_contract_special_term_agreements_withdrawn_at CHECK (agreement_withdrawn_at IS NULL OR agreement_withdrawn_at ~ '^[0-9]{8}$'),
    CONSTRAINT ck_contract_special_term_agreements_withdrawn
        CHECK (is_agreement_withdrawn = FALSE OR agreement_withdrawn_at IS NOT NULL)
);

CREATE TABLE subscription_payment_recognition_history (
    recognition_id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    payment_amount NUMERIC(18,2) NOT NULL,
    recognized_amount NUMERIC(18,2) NOT NULL,
    payment_month DATE NOT NULL,
    recognized_at TIMESTAMPTZ,
    recognition_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_subscription_payment_recognition_history_contract
        FOREIGN KEY (contract_id)
        REFERENCES contracts (contract_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_subscription_payment_recognition_amounts CHECK (payment_amount >= 0 AND recognized_amount >= 0),
    CONSTRAINT ck_subscription_payment_recognition_month_start CHECK (payment_month = DATE_TRUNC('month', payment_month)::DATE),
    CONSTRAINT uq_subscription_payment_recognition_contract_month UNIQUE (contract_id, payment_month)
);

CREATE TABLE accounts (
    account_id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(30) NOT NULL UNIQUE,
    customer_id VARCHAR(30) NOT NULL,
    contract_id BIGINT NOT NULL UNIQUE,
    account_type VARCHAR(20) NOT NULL,
    saving_type VARCHAR(20),
    bank_code VARCHAR(10) NOT NULL DEFAULT '001',
    account_alias VARCHAR(100),
    balance NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_paid_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_interest_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    last_transaction_at TIMESTAMPTZ,
    last_interest_paid_at TIMESTAMPTZ,
    currency CHAR(3) NOT NULL DEFAULT 'KRW',
    account_password VARCHAR(255) NOT NULL,
    daily_withdraw_limit NUMERIC(18,2),
    daily_withdraw_count_limit INT,
    atm_withdraw_limit NUMERIC(18,2),
    is_withdrawable BOOLEAN NOT NULL DEFAULT TRUE,
    is_online_banking_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    is_mobile_banking_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    is_phone_banking_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    opened_at CHAR(8) NOT NULL,
    maturity_at CHAR(8),
    dormant_at CHAR(8),
    dormant_released_at CHAR(8),
    closed_at CHAR(8),
    status_changed_at CHAR(8),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT fk_accounts_contract
        FOREIGN KEY (contract_id)
        REFERENCES contracts (contract_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_accounts_saving_type CHECK ((account_type = 'SAVINGS' AND saving_type IS NOT NULL) OR (account_type <> 'SAVINGS' AND saving_type IS NULL)),
    CONSTRAINT ck_accounts_amounts CHECK (balance >= 0 AND total_paid_amount >= 0 AND total_interest_amount >= 0),
    CONSTRAINT ck_accounts_limits CHECK (
        (daily_withdraw_limit IS NULL OR daily_withdraw_limit >= 0)
        AND (daily_withdraw_count_limit IS NULL OR daily_withdraw_count_limit >= 0)
        AND (atm_withdraw_limit IS NULL OR atm_withdraw_limit >= 0)
    ),
    CONSTRAINT ck_accounts_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_accounts_opened_at_yyyymmdd CHECK (opened_at ~ '^[0-9]{8}$'),
    CONSTRAINT ck_accounts_maturity_at_yyyymmdd CHECK (maturity_at IS NULL OR maturity_at ~ '^[0-9]{8}$'),
    CONSTRAINT ck_accounts_dormant_at_yyyymmdd CHECK (dormant_at IS NULL OR dormant_at ~ '^[0-9]{8}$'),
    CONSTRAINT ck_accounts_dormant_released_at_yyyymmdd CHECK (dormant_released_at IS NULL OR dormant_released_at ~ '^[0-9]{8}$'),
    CONSTRAINT ck_accounts_closed_at_yyyymmdd CHECK (closed_at IS NULL OR closed_at ~ '^[0-9]{8}$'),
    CONSTRAINT ck_accounts_status_changed_at_yyyymmdd CHECK (status_changed_at IS NULL OR status_changed_at ~ '^[0-9]{8}$')
);

CREATE TABLE interest_history (
    interest_id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    applied_interest_rate NUMERIC(5,2) NOT NULL,
    interest_calculation_start_date CHAR(8),
    interest_calculation_end_date CHAR(8),
    interest_occurred_at TIMESTAMPTZ,
    interest_amount NUMERIC(18,2) NOT NULL,
    tax_benefit_type VARCHAR(20) NOT NULL,
    applied_tax_rate NUMERIC(6,4) NOT NULL,
    interest_before_tax NUMERIC(18,2) NOT NULL,
    interest_tax_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    local_income_tax_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    interest_after_tax NUMERIC(18,2) NOT NULL,
    interest_reason VARCHAR(30) NOT NULL,
    interest_paid_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT fk_interest_history_contract
        FOREIGN KEY (contract_id)
        REFERENCES contracts (contract_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_interest_history_account
        FOREIGN KEY (account_id)
        REFERENCES accounts (account_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_interest_history_rates CHECK (applied_interest_rate >= 0 AND applied_tax_rate >= 0),
    CONSTRAINT ck_interest_history_amounts CHECK (
        interest_amount >= 0
        AND interest_before_tax >= 0
        AND interest_tax_amount >= 0
        AND local_income_tax_amount >= 0
        AND interest_after_tax >= 0
    ),
    CONSTRAINT ck_interest_history_calc_start_yyyymmdd CHECK (interest_calculation_start_date IS NULL OR interest_calculation_start_date ~ '^[0-9]{8}$'),
    CONSTRAINT ck_interest_history_calc_end_yyyymmdd CHECK (interest_calculation_end_date IS NULL OR interest_calculation_end_date ~ '^[0-9]{8}$'),
    CONSTRAINT ck_interest_history_calc_range CHECK (
        interest_calculation_start_date IS NULL
        OR interest_calculation_end_date IS NULL
        OR interest_calculation_start_date <= interest_calculation_end_date
    )
);

CREATE TABLE transactions (
    transaction_id BIGSERIAL PRIMARY KEY,
    transaction_number VARCHAR(50) NOT NULL UNIQUE,
    account_id BIGINT NOT NULL,
    contract_id BIGINT,
    transaction_type VARCHAR(30) NOT NULL,
    direction_type VARCHAR(10) NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    balance_before NUMERIC(18,2) NOT NULL,
    balance_after NUMERIC(18,2) NOT NULL,
    available_balance_after NUMERIC(18,2),
    fee_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    currency CHAR(3) NOT NULL DEFAULT 'KRW',
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    channel_type VARCHAR(20) NOT NULL,
    ip_address VARCHAR(45),
    terminal_id VARCHAR(50),
    transaction_location VARCHAR(100),
    transaction_memo VARCHAR(255),
    transaction_summary VARCHAR(100),
    transaction_at TIMESTAMPTZ NOT NULL,
    posted_at TIMESTAMPTZ,
    canceled_at TIMESTAMPTZ,
    depositor_customer_id VARCHAR(30),
    depositor_name VARCHAR(100),
    delegate_customer_id VARCHAR(30),
    delegate_customer_name VARCHAR(100),
    transfer_type VARCHAR(20),
    counterparty_bank_code VARCHAR(10),
    counterparty_bank_name VARCHAR(100),
    counterparty_account_no VARCHAR(30),
    counterparty_account_id BIGINT,
    counterparty_customer_id VARCHAR(30),
    counterparty_name VARCHAR(100),
    counterparty_name_verified_yn BOOLEAN,
    transfer_requested_at TIMESTAMPTZ,
    transfer_completed_at TIMESTAMPTZ,
    payment_method VARCHAR(30),
    merchant_id VARCHAR(50),
    merchant_name VARCHAR(100),
    approval_number VARCHAR(50),
    external_transaction_no VARCHAR(100),
    payment_round INT,
    original_transaction_id BIGINT,
    failure_type VARCHAR(30),
    failure_code VARCHAR(50),
    failure_reason_code VARCHAR(40),
    failure_at TIMESTAMPTZ,
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT fk_transactions_account
        FOREIGN KEY (account_id)
        REFERENCES accounts (account_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_transactions_contract
        FOREIGN KEY (contract_id)
        REFERENCES contracts (contract_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_transactions_counterparty_account
        FOREIGN KEY (counterparty_account_id)
        REFERENCES accounts (account_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_transactions_original_transaction
        FOREIGN KEY (original_transaction_id)
        REFERENCES transactions (transaction_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_transactions_amounts CHECK (
        amount > 0
        AND balance_before >= 0
        AND balance_after >= 0
        AND (available_balance_after IS NULL OR available_balance_after >= 0)
        AND fee_amount >= 0
    ),
    CONSTRAINT ck_transactions_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_transactions_contract_required CHECK (
        (transaction_type IN ('INTEREST', 'SAVINGS_PAYMENT') AND contract_id IS NOT NULL)
        OR (transaction_type NOT IN ('INTEREST', 'SAVINGS_PAYMENT') AND contract_id IS NULL)
    ),
    CONSTRAINT ck_transactions_transfer_fields CHECK (
        (transaction_type = 'TRANSFER' AND transfer_type IS NOT NULL)
        OR (transaction_type <> 'TRANSFER' AND transfer_type IS NULL)
    ),
    CONSTRAINT ck_transactions_internal_transfer_counterparty CHECK (
        transfer_type <> 'INTERNAL'
        OR counterparty_account_id IS NOT NULL
    ),
    CONSTRAINT ck_transactions_payment_fields CHECK (
        (transaction_type = 'PAYMENT' AND payment_method IS NOT NULL)
        OR (transaction_type <> 'PAYMENT' AND payment_method IS NULL)
    ),
    CONSTRAINT ck_transactions_savings_payment_round CHECK (
        (transaction_type = 'SAVINGS_PAYMENT' AND payment_round IS NOT NULL AND payment_round > 0)
        OR (transaction_type <> 'SAVINGS_PAYMENT' AND payment_round IS NULL)
    ),
    CONSTRAINT ck_transactions_reversal_original CHECK (
        (transaction_type = 'REVERSAL' AND original_transaction_id IS NOT NULL)
        OR (transaction_type <> 'REVERSAL')
    ),
    CONSTRAINT ck_transactions_failed_fields CHECK (
        (status = 'FAILED' AND failure_reason_code IS NOT NULL AND failure_at IS NOT NULL)
        OR (status <> 'FAILED')
    ),
    CONSTRAINT ck_transactions_retry_count CHECK (retry_count >= 0)
);

CREATE TABLE staff_query_logs (
    log_id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    customer_id VARCHAR(30) NOT NULL,
    query_type VARCHAR(30) NOT NULL,
    target_table VARCHAR(50) NOT NULL,
    target_id VARCHAR(50),
    ip_address VARCHAR(45),
    query_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    result_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT fk_staff_query_logs_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees (employee_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_staff_query_logs_result_count CHECK (result_count >= 0)
);

-- Indexes: products / dimensions
CREATE INDEX idx_products_department_status ON products (department_id, product_status);
CREATE INDEX idx_products_type_status ON products (product_type, product_status);
CREATE INDEX idx_deposit_products_product ON deposit_products (product_id);
CREATE INDEX idx_savings_products_product ON savings_products (product_id);
CREATE INDEX idx_product_interest_rates_product_type_active ON product_interest_rates (product_id, rate_type, is_active);
CREATE INDEX idx_product_interest_rates_effective_period ON product_interest_rates (product_id, effective_start_date, effective_end_date);
CREATE INDEX idx_product_target_groups_target_group ON product_target_groups (target_group_id);
CREATE INDEX idx_product_special_terms_special_term ON product_special_terms (special_term_id);

-- Indexes: contracts
CREATE INDEX idx_contracts_customer_status ON contracts (customer_id, contract_status);
CREATE INDEX idx_contracts_product_status ON contracts (product_id, contract_status);
CREATE INDEX idx_contracts_started_at ON contracts (started_at);
CREATE INDEX idx_contracts_maturity_at_active ON contracts (maturity_at) WHERE contract_status = 'ACTIVE';
CREATE INDEX idx_contracts_branch_started_at ON contracts (branch_id, started_at);
CREATE INDEX idx_contracts_manager_started_at ON contracts (manager_id, started_at);
CREATE INDEX idx_contract_applied_rates_contract ON contract_applied_rates (contract_id);
CREATE INDEX idx_contract_applied_rates_rate ON contract_applied_rates (rate_id);
CREATE INDEX idx_contract_special_term_agreements_contract ON contract_special_term_agreements (contract_id);
CREATE INDEX idx_subscription_payment_recognition_contract_month ON subscription_payment_recognition_history (contract_id, payment_month);
CREATE INDEX idx_subscription_payment_recognition_status ON subscription_payment_recognition_history (recognition_status);

-- Indexes: accounts
CREATE INDEX idx_accounts_customer_status ON accounts (customer_id, account_status);
CREATE INDEX idx_accounts_type_status ON accounts (account_type, account_status);

-- Indexes: interest_history
CREATE INDEX idx_interest_history_contract_paid_at ON interest_history (contract_id, interest_paid_at DESC);
CREATE INDEX idx_interest_history_account_paid_at ON interest_history (account_id, interest_paid_at DESC);
CREATE INDEX idx_interest_history_reason_paid_at ON interest_history (interest_reason, interest_paid_at DESC);
CREATE INDEX idx_interest_history_paid_at ON interest_history (interest_paid_at DESC);

-- Indexes: transactions
CREATE INDEX idx_transactions_account_at_desc ON transactions (account_id, transaction_at DESC);
CREATE INDEX idx_transactions_contract_at_desc ON transactions (contract_id, transaction_at DESC) WHERE contract_id IS NOT NULL;
CREATE INDEX idx_transactions_type_status_at ON transactions (transaction_type, status, transaction_at DESC);
CREATE INDEX idx_transactions_channel_at ON transactions (channel_type, transaction_at DESC);
CREATE INDEX idx_transactions_status_failed_at ON transactions (status, failure_at DESC) WHERE status = 'FAILED';
CREATE INDEX idx_transactions_original_transaction ON transactions (original_transaction_id) WHERE original_transaction_id IS NOT NULL;
CREATE INDEX idx_transactions_counterparty_account ON transactions (counterparty_account_id) WHERE counterparty_account_id IS NOT NULL;
CREATE INDEX idx_transactions_savings_contract_round ON transactions (contract_id, payment_round) WHERE transaction_type = 'SAVINGS_PAYMENT';
CREATE INDEX idx_transactions_interest_contract_at ON transactions (contract_id, transaction_at DESC) WHERE transaction_type = 'INTEREST';
CREATE INDEX idx_transactions_payment_merchant_at ON transactions (merchant_id, transaction_at DESC) WHERE transaction_type = 'PAYMENT';

-- Indexes: admin / audit
CREATE INDEX idx_departments_parent ON departments (parent_department_id);
CREATE INDEX idx_branches_department_active ON branches (department_id, is_active);
CREATE INDEX idx_employees_department_status ON employees (department_id, employment_status);
CREATE INDEX idx_employees_branch_status ON employees (branch_id, employment_status);
CREATE INDEX idx_staff_query_logs_employee_at ON staff_query_logs (employee_id, query_at DESC);
CREATE INDEX idx_staff_query_logs_customer_at ON staff_query_logs (customer_id, query_at DESC);
CREATE INDEX idx_staff_query_logs_target ON staff_query_logs (target_table, target_id);

COMMIT;

