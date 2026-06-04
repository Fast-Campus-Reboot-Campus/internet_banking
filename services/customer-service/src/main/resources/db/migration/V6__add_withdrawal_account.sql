-- ============================================================
-- V6: 출금계좌 등록/삭제/순위변경 (인증보안계)
-- ============================================================

CREATE TABLE withdrawal_account (
    withdrawal_account_id BIGINT          GENERATED ALWAYS AS IDENTITY,
    customer_id           BIGINT          NOT NULL,
    account_number        VARCHAR(50)     NOT NULL,
    bank_code             VARCHAR(10)     NOT NULL,
    bank_name             VARCHAR(50)     NOT NULL,
    account_holder_name   VARCHAR(100),
    account_alias         VARCHAR(100),
    registration_type     VARCHAR(20)     NOT NULL DEFAULT 'ONLINE',
    priority_order        SMALLINT        NOT NULL DEFAULT 0,
    registered_at         TIMESTAMPTZ(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_at            TIMESTAMPTZ(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by            BIGINT,
    updated_at            TIMESTAMPTZ(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by            BIGINT,
    deleted_at            TIMESTAMPTZ(3),
    deleted_by            BIGINT,
    version               INT             NOT NULL DEFAULT 0,

    CONSTRAINT pk_withdrawal_account PRIMARY KEY (withdrawal_account_id),
    CONSTRAINT fk_withdrawal_account_customer
        FOREIGN KEY (customer_id) REFERENCES customer(customer_id)
);

-- 동일 고객의 활성(미삭제) 계좌는 계좌번호 중복 금지
CREATE UNIQUE INDEX uq_withdrawal_account_active
    ON withdrawal_account (customer_id, account_number)
    WHERE deleted_at IS NULL;
