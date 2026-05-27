-- IFRS9 ECL (Expected Credit Loss) 월별 산출 결과.
-- UNIQUE(cntr_id, summary_month) 로 동일 baseMonth 재실행 시 자연 멱등.
--
-- 본 단계 LGD 는 50% 고정 (담보·보증보험 차등은 후속).
-- PD 는 연체 stage 기반 단순 매핑 (외부 통계 모델 연동은 후속).

CREATE TABLE loan_ecl_summary (
    ecl_id          BIGSERIAL     PRIMARY KEY,
    cntr_id         BIGINT        NOT NULL REFERENCES loan_contract(cntr_id),
    summary_month   VARCHAR(6)    NOT NULL,
    ifrs_stage_cd   VARCHAR(50)   NOT NULL,      -- STAGE_1 / STAGE_2 / STAGE_3
    pd_bps          INT           NOT NULL,       -- bps (10000 bps = 100%)
    lgd_bps         INT           NOT NULL,
    ead             BIGINT        NOT NULL,
    ecl             BIGINT        NOT NULL,
    engine_version  VARCHAR(50)   NOT NULL,
    calculated_at   TIMESTAMPTZ   NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by      BIGINT        NOT NULL
);

CREATE UNIQUE INDEX uk_loan_ecl_summary_cntr_month
    ON loan_ecl_summary (cntr_id, summary_month);

CREATE INDEX idx_loan_ecl_summary_month_stage
    ON loan_ecl_summary (summary_month, ifrs_stage_cd);
