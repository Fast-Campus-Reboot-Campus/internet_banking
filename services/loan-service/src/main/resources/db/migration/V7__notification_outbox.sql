-- 알림 outbox.
-- listener (신청/약정/실행/상환/심사) 가 이벤트별로 row 를 적재하면 dispatch 배치가 채널 어댑터로 송신한다.
-- 멱등 키 (event_type_cd + reference_id + channel_cd) 로 동일 이벤트 재발행 차단.

CREATE TABLE notification_outbox (
    outbox_id         BIGSERIAL     PRIMARY KEY,
    event_type_cd     VARCHAR(50)   NOT NULL,
    reference_id      BIGINT        NOT NULL,
    channel_cd        VARCHAR(50)   NOT NULL,
    payload           JSONB,
    status            VARCHAR(50)   NOT NULL,
    attempt_no        INT           NOT NULL DEFAULT 0,
    max_attempt       INT           NOT NULL DEFAULT 5,
    next_attempt_at   TIMESTAMPTZ   NOT NULL,
    last_error        VARCHAR(500),
    sent_at           TIMESTAMPTZ,
    idempotency_key   VARCHAR(200)  NOT NULL UNIQUE,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by        BIGINT        NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by        BIGINT        NOT NULL,
    deleted_at        TIMESTAMPTZ,
    deleted_by        BIGINT,
    version           INT           NOT NULL DEFAULT 0
);

-- dispatch 핫패스
CREATE INDEX idx_notification_outbox_dispatch
    ON notification_outbox (status, next_attempt_at)
    WHERE deleted_at IS NULL;

-- 운영자 조회
CREATE INDEX idx_notification_outbox_event_ref
    ON notification_outbox (event_type_cd, reference_id)
    WHERE deleted_at IS NULL;
