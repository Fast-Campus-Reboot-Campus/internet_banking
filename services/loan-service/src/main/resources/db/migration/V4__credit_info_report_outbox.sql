-- 신용정보 신고 outbox.
-- submit() 시 신고 row 와 함께 적재되어 dispatch 배치가 외부 어댑터를 호출한다.

CREATE TABLE credit_info_report_outbox (
    outbox_id        BIGSERIAL     PRIMARY KEY,
    crpt_id          BIGINT        NOT NULL REFERENCES credit_info_report(crpt_id),
    status           VARCHAR(50)   NOT NULL,
    attempt_no       INT           NOT NULL DEFAULT 0,
    max_attempt      INT           NOT NULL DEFAULT 5,
    next_attempt_at  TIMESTAMPTZ   NOT NULL,
    last_error       VARCHAR(500),
    sent_at          TIMESTAMPTZ,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by       BIGINT        NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by       BIGINT        NOT NULL,
    deleted_at       TIMESTAMPTZ,
    deleted_by       BIGINT,
    version          INT           NOT NULL DEFAULT 0
);

-- dispatch 배치 핫패스: 처리 대상 row 후보 픽업.
CREATE INDEX idx_credit_info_report_outbox_dispatch
    ON credit_info_report_outbox (status, next_attempt_at)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_credit_info_report_outbox_crpt_id
    ON credit_info_report_outbox (crpt_id)
    WHERE deleted_at IS NULL;
