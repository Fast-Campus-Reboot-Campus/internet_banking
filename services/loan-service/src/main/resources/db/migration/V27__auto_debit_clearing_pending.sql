-- 자동이체 타행 청산(CLEARING) 대기 매핑.
-- payment.* Kafka 이벤트에는 piId 만 실려오므로(idempotencyKey 없음),
-- CLEARING 응답 시점에 piId ↔ 회차 정보를 미리 저장해 두고
-- 완결/실패 이벤트 수신 시 piId 로 조회해 상환을 완결한다.
CREATE TABLE auto_debit_clearing_pending (
    pending_id       BIGSERIAL    PRIMARY KEY,
    pi_id            VARCHAR(100) NOT NULL UNIQUE,
    cntr_id          BIGINT       NOT NULL,
    rsch_id          BIGINT       NOT NULL,
    installment_no   INT          NOT NULL,
    base_date        CHAR(8)      NOT NULL,
    idempotency_key  VARCHAR(100) NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    resolved_at      TIMESTAMPTZ,
    CONSTRAINT chk_adcp_status CHECK (status IN ('PENDING', 'DONE', 'FAILED'))
);

-- 미해소 대기건 조회 핫패스
CREATE INDEX idx_adcp_pending
    ON auto_debit_clearing_pending (status)
    WHERE status = 'PENDING';
