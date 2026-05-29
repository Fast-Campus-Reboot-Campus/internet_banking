-- doc-agent 서류 검증 결과 수신 이력 테이블
-- doc-agent.routed 토픽에서 수신한 AUTO_PASS / NEEDS_RESUBMIT / HOLD 결과를 적재하고,
-- 심사원 수동 승인(REVIEWER_PASS) 처리를 위한 reviewed_by / reviewed_at 컬럼도 포함.

CREATE TABLE loan_document_submission (
    submission_id   VARCHAR(36)  NOT NULL,
    appl_id         BIGINT       NOT NULL,
    doc_code        VARCHAR(50)  NOT NULL,
    verify_status   VARCHAR(50),
    occurred_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL,
    reviewed_by     BIGINT,
    reviewed_at     TIMESTAMPTZ,

    CONSTRAINT pk_loan_document_submission PRIMARY KEY (submission_id)
);

CREATE INDEX idx_loan_doc_sub_appl_id ON loan_document_submission (appl_id);
CREATE INDEX idx_loan_doc_sub_appl_doc ON loan_document_submission (appl_id, doc_code);
