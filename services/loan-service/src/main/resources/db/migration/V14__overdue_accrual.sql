-- 연체 이자 일별 발생 (append-only)
-- UNIQUE (cntr_id, accrual_date) 로 동일 baseDate 재실행 시 멱등 보장

CREATE TABLE overdue_accrual (
    oa_id                      BIGSERIAL     PRIMARY KEY,
    cntr_id                    BIGINT        NOT NULL REFERENCES loan_contract(cntr_id),
    dlq_id                     BIGINT        NOT NULL REFERENCES delinquency(dlq_id),
    accrual_date               VARCHAR(8)    NOT NULL,
    overdue_principal          BIGINT        NOT NULL,
    overdue_rate_bps           INT           NOT NULL,
    dlq_days                   INT           NOT NULL,
    daily_overdue_interest     BIGINT        NOT NULL,
    cumulative_overdue_interest BIGINT       NOT NULL,
    oa_status_cd               VARCHAR(50)   NOT NULL,
    accrued_at                 TIMESTAMPTZ   NOT NULL,
    created_at                 TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by                 BIGINT        NOT NULL
);

CREATE UNIQUE INDEX uk_overdue_accrual_cntr_date
    ON overdue_accrual (cntr_id, accrual_date);

CREATE INDEX idx_overdue_accrual_dlq
    ON overdue_accrual (dlq_id, accrual_date);
