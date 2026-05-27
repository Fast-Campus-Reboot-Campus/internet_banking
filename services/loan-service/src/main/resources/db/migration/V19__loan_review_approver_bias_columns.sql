-- 본심사 승인자 단계 + 편향 검증 상태 컬럼 추가.
-- 상태값: BIAS_REVIEWING (편향 검증 진행/대기), PENDING_APPROVER (승인자 대기)
-- bias_override_*: 상급자가 BLOCKED severity 를 우회 승인한 경우 기록.

ALTER TABLE loan_review
    ADD COLUMN approver_id          BIGINT,
    ADD COLUMN approved_decision_cd VARCHAR(50),
    ADD COLUMN override_reason_cd   VARCHAR(50),
    ADD COLUMN override_remark      VARCHAR(500),
    ADD COLUMN bias_severity_cd     VARCHAR(20),
    ADD COLUMN bias_override_by     BIGINT,
    ADD COLUMN bias_override_reason VARCHAR(500),
    ADD COLUMN bias_overridden_at   TIMESTAMPTZ;

CREATE INDEX ix_loan_review_status_bias
    ON loan_review (rev_status_cd)
    WHERE rev_status_cd IN ('BIAS_REVIEWING', 'PENDING_APPROVER');
