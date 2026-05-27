-- 월별 회계 요약 (EOM 산출)
-- summary_month = YYYYMM. UNIQUE 로 동일 baseMonth 재실행 시 멱등.

CREATE TABLE monthly_accounting_summary (
    mas_id                         BIGSERIAL     PRIMARY KEY,
    summary_month                  VARCHAR(6)    NOT NULL UNIQUE,
    base_month_start_date          VARCHAR(8)    NOT NULL,
    base_month_end_date            VARCHAR(8)    NOT NULL,

    -- 매출 (트랜잭션 합계)
    interest_revenue               BIGINT        NOT NULL DEFAULT 0,
    overdue_interest_revenue       BIGINT        NOT NULL DEFAULT 0,
    auto_debit_principal           BIGINT        NOT NULL DEFAULT 0,
    auto_debit_interest            BIGINT        NOT NULL DEFAULT 0,
    auto_debit_overdue_interest    BIGINT        NOT NULL DEFAULT 0,
    auto_debit_count               INT           NOT NULL DEFAULT 0,

    -- 신규 실행
    new_disbursed_amount           BIGINT        NOT NULL DEFAULT 0,
    new_disbursed_count            INT           NOT NULL DEFAULT 0,

    -- 월말 시점 잔액·연체 통계
    month_end_active_contracts     INT           NOT NULL DEFAULT 0,
    month_end_active_delinquencies INT           NOT NULL DEFAULT 0,
    month_end_npl_count            INT           NOT NULL DEFAULT 0,
    month_end_npl_principal        BIGINT        NOT NULL DEFAULT 0,

    summarized_at                  TIMESTAMPTZ   NOT NULL,
    created_at                     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by                     BIGINT        NOT NULL
);
