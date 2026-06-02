-- 공통 계약 (common_contract) — 공통 계좌 DB(common_db).
-- 수신/여신 계약이 공유하는 계약 마스터의 부모. 각 서비스의 계약 서브타입
-- (deposit_contract / loan_contract)은 자기 DB 에서 contract_id 값으로 참조한다.
--
-- 출처: deposit-service V5__full_erd_schema.sql 의 common_contract 정의 기준.
-- common_db 내부 FK(common_product)만 유지하고, 타 서비스 소유(customer) FK 는 제거(값 참조).
-- deposit V5 대비 교정:
--   - base_rate_bps VARCHAR(10) -> INT  (bps 는 정수)
--   - 정체불명 컬럼 "contract INT" 드롭 (ERD 아티팩트)
--   - created_at/updated_at NOT NULL DEFAULT now()
CREATE TABLE common_contract (
    contract_id                 BIGSERIAL       PRIMARY KEY,
    contract_no                 VARCHAR(50),
    customer_id                 BIGINT          NOT NULL,
    customer_no                 VARCHAR(30),
    product_id                  BIGINT          NOT NULL,
    biz_div_cd                  VARCHAR(10),
    contract_amount             BIGINT,
    rate_type_cd                VARCHAR(10),
    base_rate_bps               INT,
    spread_bps                  INT,
    preferential_bps            INT,
    total_rate_bps              INT,
    interest_amount_at_maturity BIGINT,
    contract_start_date         CHAR(8),
    contract_end_date           CHAR(8),
    contract_cancel_date        CHAR(8),
    contract_cancel_reason      VARCHAR(200),
    auto_transfer_yn            CHAR(1),
    auto_transfer_day           INT,
    signed_at                   TIMESTAMPTZ(3),
    contract_channel_cd         VARCHAR(20),
    spot_id                     BIGINT,
    spot_name                   VARCHAR(100),
    manager_id                  BIGINT,
    manager_name                VARCHAR(100),
    proxy_yn                    CHAR(1),
    contract_status             VARCHAR(20),
    term_url                    VARCHAR(500),
    term_hash                   VARCHAR(64),
    contract_url                VARCHAR(500),
    contract_hash               VARCHAR(64),
    created_at                  TIMESTAMPTZ(3)  NOT NULL DEFAULT now(),
    created_by                  BIGINT,
    updated_at                  TIMESTAMPTZ(3)  NOT NULL DEFAULT now(),
    updated_by                  BIGINT,
    CONSTRAINT fk_common_contract_product
        FOREIGN KEY (product_id) REFERENCES common_product (product_id)
);

-- 고객·상태별 계약 조회 핫패스
CREATE INDEX idx_common_contract_cust ON common_contract (customer_id, contract_status);
-- 상품별 계약 조회
CREATE INDEX idx_common_contract_prod ON common_contract (product_id);
-- write-through 멱등 dedupe 후보키(서비스가 contract_no 를 자연키로 세팅)
CREATE INDEX idx_common_contract_no   ON common_contract (contract_no) WHERE contract_no IS NOT NULL;
