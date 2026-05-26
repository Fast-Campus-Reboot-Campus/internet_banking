-- ============================================================
-- AI domain schema (PostgreSQL 16 + pgvector 0.8.x)
-- Notes:
--   - vector extension 은 infra/ai-db/init/01_pgvector.sql 에서 활성화됨
--   - Soft delete only (deleted_at IS NULL 필터 적용)
--   - 금액/금리 컬럼 없음, 날짜는 TIMESTAMPTZ(3)
-- ============================================================

SET TIME ZONE 'Asia/Seoul';

-- ============================================================
-- 1. RAG 원본 문서
-- ============================================================
CREATE TABLE rag_document (
    doc_id           BIGSERIAL        PRIMARY KEY,
    doc_type_cd      VARCHAR(50)      NOT NULL,
    title            VARCHAR(300)     NOT NULL,
    source_uri       VARCHAR(500)     NOT NULL,
    jurisdiction     VARCHAR(50)      NOT NULL DEFAULT 'KR',
    sensitivity_cd   VARCHAR(20)      NOT NULL DEFAULT 'PUBLIC',
    doc_version      VARCHAR(50),
    effective_from   CHAR(8),
    effective_to     CHAR(8),
    checksum         CHAR(64)         NOT NULL,
    ingested_at      TIMESTAMPTZ(3),
    created_at       TIMESTAMPTZ(3)   NOT NULL DEFAULT now(),
    created_by       BIGINT           NOT NULL DEFAULT 0,
    updated_at       TIMESTAMPTZ(3)   NOT NULL DEFAULT now(),
    updated_by       BIGINT           NOT NULL DEFAULT 0,
    deleted_at       TIMESTAMPTZ(3),
    deleted_by       BIGINT,
    version          INTEGER          NOT NULL DEFAULT 0
);

CREATE INDEX idx_rag_document_type     ON rag_document (doc_type_cd) WHERE deleted_at IS NULL;
CREATE INDEX idx_rag_document_eff      ON rag_document (effective_from, effective_to) WHERE deleted_at IS NULL;
CREATE INDEX idx_rag_document_checksum ON rag_document (checksum) WHERE deleted_at IS NULL;
COMMENT ON TABLE rag_document IS 'RAG 원본 문서 단위 (1 PDF = 1 row)';

-- ============================================================
-- 2. RAG 청크 (임베딩 단위)
-- ============================================================
CREATE TABLE rag_chunk (
    chunk_id    BIGSERIAL        PRIMARY KEY,
    doc_id      BIGINT           NOT NULL REFERENCES rag_document (doc_id),
    chunk_seq   INTEGER          NOT NULL,
    content     TEXT             NOT NULL,
    token_cnt   INTEGER,
    embedding   vector(1536),
    metadata    JSONB,
    created_at  TIMESTAMPTZ(3)   NOT NULL DEFAULT now()
);

CREATE INDEX idx_rag_chunk_doc   ON rag_chunk (doc_id);
-- 벡터 인덱스: 초기 데이터 적재 후 운영 규모 확인 뒤 생성 (ivfflat lists 튜닝 필요)
-- CREATE INDEX idx_rag_chunk_emb ON rag_chunk USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
COMMENT ON TABLE rag_chunk IS 'RAG 임베딩 단위 (1 문서 = N 청크)';

-- ============================================================
-- 3. RAG 임베딩 처리 이력
-- ============================================================
CREATE TABLE rag_ingestion_log (
    log_id       BIGSERIAL        PRIMARY KEY,
    doc_id       BIGINT           NOT NULL REFERENCES rag_document (doc_id),
    phase_cd     VARCHAR(30)      NOT NULL,
    status_cd    VARCHAR(20)      NOT NULL,
    chunk_cnt    INTEGER,
    model_name   VARCHAR(100),
    error_msg    VARCHAR(500),
    started_at   TIMESTAMPTZ(3)   NOT NULL DEFAULT now(),
    finished_at  TIMESTAMPTZ(3)
);

CREATE INDEX idx_rag_ingestion_log_doc ON rag_ingestion_log (doc_id, started_at DESC);
COMMENT ON TABLE rag_ingestion_log IS 'RAG 임베딩 파이프라인 감사 이력';
