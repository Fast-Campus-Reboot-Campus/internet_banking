-- 조회·열람·break-glass 접근 이벤트 전용 감사 로그. append-only.
-- 상태 전이 이력(status_history)과 분리: 접근 행위 자체를 기록한다.
CREATE TABLE access_audit_log (
    log_id             BIGINT GENERATED ALWAYS AS IDENTITY,
    actor_id           BIGINT         NOT NULL,
    target_type        VARCHAR(50)    NOT NULL,
    target_id          BIGINT         NOT NULL,
    action_cd          VARCHAR(30)    NOT NULL,
    branch_id          VARCHAR(10),
    break_glass_reason TEXT,
    logged_at          TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_access_audit_log PRIMARY KEY (log_id),
    CONSTRAINT chk_aal_action_cd CHECK (action_cd IN ('VIEW', 'UNMASK', 'BREAK_GLASS')),
    CONSTRAINT chk_aal_target_type CHECK (target_type IN ('LOAN_APPLICATION', 'LOAN_REVIEW', 'DOCUMENT'))
);

CREATE INDEX idx_aal_actor    ON access_audit_log (actor_id, logged_at DESC);
CREATE INDEX idx_aal_target   ON access_audit_log (target_type, target_id, logged_at DESC);
