-- 수신상품 테이블
CREATE TABLE banking_products (
    banking_product_id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL UNIQUE,
    banking_product_type VARCHAR(50) NOT NULL,
    min_join_amount NUMERIC(18,2),
    max_join_amount NUMERIC(18,2),
    min_contract_period_month INT,
    max_contract_period_month INT,
    auto_transfer_available_yn BOOLEAN NOT NULL DEFAULT FALSE,
    auto_renewal_available_yn BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT fk_banking_products_product
        FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
);

-- 가입방식 테이블
CREATE TABLE join_methods (
    join_method_id BIGSERIAL PRIMARY KEY,
    join_method_name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

-- 우대금리 condition_name 컬럼 추가 및 rate_id nullable 처리
ALTER TABLE contract_applied_rates
    ADD COLUMN IF NOT EXISTS condition_name VARCHAR(200);

ALTER TABLE contract_applied_rates
    ALTER COLUMN rate_id DROP NOT NULL;

ALTER TABLE contract_applied_rates
    DROP CONSTRAINT IF EXISTS fk_contract_applied_rates_rate;

ALTER TABLE contract_applied_rates
    DROP CONSTRAINT IF EXISTS uq_contract_applied_rates_contract_rate;
