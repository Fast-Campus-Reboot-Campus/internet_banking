-- 공통 거래원장 (common_transaction) — 공통 계좌 DB(common_db).
-- 은행 공통 거래/분개 원장. 수신/여신/결제 거래가 공유하는 거래 마스터.
-- 여신 거래(loan_execution / repayment_transaction)는 자기 DB 에서 transaction_id 값으로 참조한다.
--
-- 출처: deposit-service V5__full_erd_schema.sql 의 common_transaction 정의 기준.
-- common_db 내부 FK(common_account, common_contract, 자기참조)만 유지, 타 서비스 FK 는 없음(값 참조).
-- deposit V5 대비 교정:
--   - original_transaction_id BIGINT NOT NULL -> nullable (역분개 row 만 원거래를 가리킴)
--   - created_at/updated_at NOT NULL DEFAULT now()
CREATE TABLE common_transaction (
    transaction_id              BIGSERIAL       PRIMARY KEY,
    transaction_no              VARCHAR(50),
    account_id                  BIGINT,
    contract_id                 BIGINT,
    transaction_type_cd         VARCHAR(30),
    debit_credit_type           VARCHAR(10),
    transaction_amount          BIGINT,
    balance_before              BIGINT,
    balance_after               BIGINT,
    fee_amount                  BIGINT,
    channel_cd                  VARCHAR(30),
    counterparty_bank_cd        VARCHAR(10),
    counterparty_bank_name      VARCHAR(100),
    counterparty_account_no     VARCHAR(30),
    counterparty_name           VARCHAR(100),
    counterparty_customer_id    BIGINT,
    counterparty_account_id     BIGINT,
    counterparty_name_verified_yn CHAR(1),
    original_transaction_id     BIGINT,
    transaction_memo            VARCHAR(255),
    transaction_status          VARCHAR(20),
    transacted_at               TIMESTAMPTZ(3),
    currency_cd                 CHAR(3),
    available_balance           BIGINT,
    transaction_summary         VARCHAR(100),
    transfer_type_cd            VARCHAR(30),
    transfer_requested_at       TIMESTAMPTZ(3),
    transfer_completed_at       TIMESTAMPTZ(3),
    transfer_failed_yn          CHAR(1),
    payment_method_code         VARCHAR(30),
    card_payment_yn             CHAR(1),
    payment_failed_yn           CHAR(1),
    merchant_no                 VARCHAR(50),
    merchant_name               VARCHAR(100),
    failure_type_cd             VARCHAR(30),
    failure_reason_cd           VARCHAR(50),
    failure_cause_cd            VARCHAR(50),
    failed_at                   TIMESTAMPTZ(3),
    retry_count                 INT,
    approval_no                 VARCHAR(50),
    external_transaction_no     VARCHAR(100),
    terminal_id                 VARCHAR(50),
    client_ip                   VARCHAR(45),
    transaction_location        VARCHAR(100),
    ledger_posted_at            TIMESTAMPTZ(3),
    cancelled_at                TIMESTAMPTZ(3),
    created_at                  TIMESTAMPTZ(3)  NOT NULL DEFAULT now(),
    created_by                  BIGINT,
    updated_at                  TIMESTAMPTZ(3)  NOT NULL DEFAULT now(),
    updated_by                  BIGINT,
    CONSTRAINT fk_common_tx_account
        FOREIGN KEY (account_id)  REFERENCES common_account (account_id),
    CONSTRAINT fk_common_tx_contract
        FOREIGN KEY (contract_id) REFERENCES common_contract (contract_id),
    CONSTRAINT fk_common_tx_original
        FOREIGN KEY (original_transaction_id) REFERENCES common_transaction (transaction_id)
        DEFERRABLE INITIALLY DEFERRED
);

-- 계좌별 거래 조회 핫패스 (최신순)
CREATE INDEX idx_common_tx_account  ON common_transaction (account_id, transacted_at DESC);
-- 계약별 거래 조회 (최신순)
CREATE INDEX idx_common_tx_contract ON common_transaction (contract_id, transacted_at DESC)
    WHERE contract_id IS NOT NULL;
-- write-through 멱등 dedupe 후보키(서비스가 transaction_no 를 자연키로 세팅)
CREATE INDEX idx_common_tx_no       ON common_transaction (transaction_no) WHERE transaction_no IS NOT NULL;
