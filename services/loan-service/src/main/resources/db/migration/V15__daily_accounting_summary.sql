-- 일일 회계 요약 (EOD 산출)
-- 본격 복식부기 전표는 본 단계 범위 외 — 일별 합계만 적재한다.
-- UNIQUE(summary_date) 로 동일 baseDate 재실행 시 멱등.

CREATE TABLE daily_accounting_summary (
    das_id                       BIGSERIAL     PRIMARY KEY,
    summary_date                 VARCHAR(8)    NOT NULL UNIQUE,
    interest_revenue             BIGINT        NOT NULL DEFAULT 0,
    overdue_interest_revenue     BIGINT        NOT NULL DEFAULT 0,
    auto_debit_principal         BIGINT        NOT NULL DEFAULT 0,
    auto_debit_interest          BIGINT        NOT NULL DEFAULT 0,
    auto_debit_overdue_interest  BIGINT        NOT NULL DEFAULT 0,
    auto_debit_count             INT           NOT NULL DEFAULT 0,
    disbursed_amount             BIGINT        NOT NULL DEFAULT 0,
    disbursed_count              INT           NOT NULL DEFAULT 0,
    active_contract_count        INT           NOT NULL DEFAULT 0,
    active_delinquency_count     INT           NOT NULL DEFAULT 0,
    summarized_at                TIMESTAMPTZ   NOT NULL,
    created_at                   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by                   BIGINT        NOT NULL
);
