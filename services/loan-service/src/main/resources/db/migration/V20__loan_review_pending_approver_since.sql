-- 승인자 대기 진입 시각 기록 컬럼 추가.
-- PENDING_APPROVER 타임아웃 배치(expire-pending-approver)의 cutoff 기준으로 사용.
-- 기존 PENDING_APPROVER 건은 NULL 허용 — 배치는 NULL 인 경우 updated_at 을 fallback 으로 처리.
ALTER TABLE loan_review
    ADD COLUMN IF NOT EXISTS pending_approver_since TIMESTAMPTZ;
