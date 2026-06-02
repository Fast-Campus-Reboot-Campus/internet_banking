-- 유사 케이스 적재용 LOAN_REVIEW outbox (Phase E E3-3).
-- 결정 완료된 심사 건의 PII-free 케이스 청크를 동일 트랜잭션으로 적재하면,
-- polling worker 가 Kafka topic(loan-review.case-indexed.v1)으로 발행 후 status=SENT 로 전이한다.
-- 멱등 키(event_type_cd + aggregate_id) 로 동일 케이스 중복 발행을 차단.

CREATE TABLE loan_review_outbox (
    outbox_id         BIGSERIAL     PRIMARY KEY,
    aggregate_id      BIGINT        NOT NULL,           -- LOAN_REVIEW rev_id
    event_type_cd     VARCHAR(50)   NOT NULL,           -- 예: CASE_INDEXED
    payload           JSONB         NOT NULL,           -- PII 마스킹된 케이스 청크 페이로드
    status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING',  -- PENDING / SENT / FAILED
    attempt_no        INT           NOT NULL DEFAULT 0,
    max_attempt       INT           NOT NULL DEFAULT 5,
    next_attempt_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    last_error        VARCHAR(500),
    sent_at           TIMESTAMPTZ,
    idempotency_key   VARCHAR(200)  NOT NULL UNIQUE,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- polling worker 핫패스 (발행 대기 건 조회)
CREATE INDEX idx_loan_review_outbox_dispatch
    ON loan_review_outbox (status, next_attempt_at);

-- 운영자 조회 (특정 심사 건 추적)
CREATE INDEX idx_loan_review_outbox_aggregate
    ON loan_review_outbox (aggregate_id);
