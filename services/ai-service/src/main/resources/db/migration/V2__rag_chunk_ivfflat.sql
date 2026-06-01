-- ============================================================
-- Stage 4 — rag_chunk ivfflat 인덱스 활성화 (G4)
--
-- V1 에서 주석 처리됐던 벡터 인덱스를 규모 측정 후 생성.
--
-- lists 결정:
--   현재 예상 규모 ~500 rows → lists = max(10, ceil(500 / 1000)) = 10
--
-- 규모 증가 시 재조정:
--   SELECT count(*) FROM rag_chunk;
--   → rows > 5,000: REINDEX 전 DROP → CREATE WITH (lists = ceil(rows/1000))
--   → rows > 1,000,000: lists = ceil(sqrt(rows))
--
-- probes 권장: SET ivfflat.probes = 1;  (기본값, 정확도↑ 시 높임)
-- ============================================================

CREATE INDEX idx_rag_chunk_emb
    ON rag_chunk
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 10);
