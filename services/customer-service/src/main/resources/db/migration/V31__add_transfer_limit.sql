-- ============================================================
-- V31: 고객당 인터넷뱅킹 이체한도 (인증보안계)
--   계좌당 출금한도(deposit-service)와 구분되는, 고객 단위 인터넷뱅킹 1일/1회 이체한도.
--   온라인에서는 감액만 가능(증액은 영업점·본인인증). 행이 없으면 기본 100만원으로 간주한다.
-- ============================================================

CREATE TABLE transfer_limit (
    transfer_limit_id BIGINT          GENERATED ALWAYS AS IDENTITY,
    customer_id       BIGINT          NOT NULL,
    daily_limit       BIGINT          NOT NULL DEFAULT 1000000,
    once_limit        BIGINT          NOT NULL DEFAULT 1000000,
    created_at        TIMESTAMPTZ(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by        BIGINT,
    updated_at        TIMESTAMPTZ(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by        BIGINT,
    deleted_at        TIMESTAMPTZ(3),
    deleted_by        BIGINT,
    version           INT             NOT NULL DEFAULT 0,

    CONSTRAINT pk_transfer_limit PRIMARY KEY (transfer_limit_id),
    CONSTRAINT fk_transfer_limit_customer
        FOREIGN KEY (customer_id) REFERENCES customer(customer_id)
);

-- 고객당 활성(미삭제) 이체한도는 1건
CREATE UNIQUE INDEX uq_transfer_limit_customer
    ON transfer_limit (customer_id)
    WHERE deleted_at IS NULL;
