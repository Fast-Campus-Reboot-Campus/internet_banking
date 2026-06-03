-- 공통 계층(common_db) write-through outbox.
-- loan_db ↔ common_db 는 별도 datasource·XA 미구성이므로, 도메인 트랜잭션에서는 본 outbox row 만
-- 적재하고(loan_db), CommonSyncDispatchService 가 비동기로 common_db upsert + loan 브리지 컬럼 백필을 수행한다.
--
-- 흐름: 도메인 tx 가 PENDING 적재 → 디스패처가 common_db upsert(자연키 source_no 멱등)
--        → 생성된 common PK 를 common_id 에 기록 + loan 측 브리지 컬럼(product_id/contract_id/transaction_id) 백필 → DONE.
--
-- target_type_cd: PRODUCT(common_product) / CONTRACT(common_contract) / TRANSACTION(common_transaction)
-- source_id     : loan_db 원본 PK (prod_id / cntr_id / exec_id|rtx_id)
-- source_no     : common 자연키 (product_cd / contract_no / transaction_no) — upsert dedupe 키
-- common_id     : upsert 성공 후 common_db 가 채번한 PK (백필 대상 값)
-- 멱등 키 (target_type_cd + source_id) 로 동일 원본 중복 적재 차단.

CREATE TABLE common_sync_outbox (
    outbox_id         BIGSERIAL     PRIMARY KEY,
    target_type_cd    VARCHAR(20)   NOT NULL,
    source_id         BIGINT        NOT NULL,
    source_no         VARCHAR(50),
    payload           JSONB,
    common_id         BIGINT,
    status            VARCHAR(50)   NOT NULL,
    attempt_no        INT           NOT NULL DEFAULT 0,
    max_attempt       INT           NOT NULL DEFAULT 5,
    next_attempt_at   TIMESTAMPTZ   NOT NULL,
    last_error        VARCHAR(500),
    synced_at         TIMESTAMPTZ,
    idempotency_key   VARCHAR(100)  NOT NULL UNIQUE,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by        BIGINT        NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by        BIGINT        NOT NULL,
    deleted_at        TIMESTAMPTZ,
    deleted_by        BIGINT,
    version           INT           NOT NULL DEFAULT 0,
    CONSTRAINT chk_common_sync_outbox_target
        CHECK (target_type_cd IN ('PRODUCT', 'CONTRACT', 'TRANSACTION'))
);

-- dispatch 핫패스: 처리 대상 후보 픽업
CREATE INDEX idx_common_sync_outbox_dispatch
    ON common_sync_outbox (status, next_attempt_at)
    WHERE deleted_at IS NULL;

-- 운영자 조회: 원본 추적
CREATE INDEX idx_common_sync_outbox_source
    ON common_sync_outbox (target_type_cd, source_id)
    WHERE deleted_at IS NULL;
