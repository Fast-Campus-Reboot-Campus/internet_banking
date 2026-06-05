-- 담당자(접수 텔러) ID.
ALTER TABLE loan_review
    ADD COLUMN IF NOT EXISTS owner_id BIGINT;

-- 본사 상신 타임스탬프. NULL = 정상 건, NOT NULL = 이상거래 상신 건.
ALTER TABLE loan_review
    ADD COLUMN IF NOT EXISTS escalated_at TIMESTAMPTZ(3);

-- 본사 상신 건 조회용 인덱스.
CREATE INDEX IF NOT EXISTS idx_loan_review_escalated ON loan_review (escalated_at)
    WHERE escalated_at IS NOT NULL;
