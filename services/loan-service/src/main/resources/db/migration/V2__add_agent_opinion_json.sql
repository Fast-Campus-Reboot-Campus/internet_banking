-- ============================================================
-- A2 migration: loan_review 테이블에 사전 심사 에이전트 의견 컬럼 추가
--
-- pre-review-agent-plan.md 운영 대비책 §DB 마이그레이션
-- - JSONB NULL: 에이전트 미실행(Track 1 skip, fallback, 미도입 레거시) 허용
-- - 크기 CHECK: 단일 컬럼이 64KB 초과 시 저장 거부 (비정상 응답 방어)
-- - 인덱스 없음: 현 단계 — 필요시 GIN 인덱스 추가
-- ============================================================

ALTER TABLE loan_review
    ADD COLUMN IF NOT EXISTS agent_opinion_json JSONB NULL
        CONSTRAINT chk_agent_opinion_json_size
            CHECK (pg_column_size(agent_opinion_json) < 65536);

COMMENT ON COLUMN loan_review.agent_opinion_json
    IS 'Pre-Review Agent 의견 JSON (schema_version v1). NULL = 에이전트 미실행 또는 fallback.';
