ALTER TABLE loan_review
    ADD COLUMN rev_ai_track_cd  VARCHAR(20)   NULL,
    ADD COLUMN rev_ai_pd        DECIMAL(10,6) NULL,
    ADD COLUMN rev_ai_rationale TEXT          NULL;

COMMENT ON COLUMN loan_review.rev_ai_track_cd   IS 'AI 트랙 분기 결과 (TRACK_1/2/3)';
COMMENT ON COLUMN loan_review.rev_ai_pd         IS 'AI PD 스코어 (0~1)';
COMMENT ON COLUMN loan_review.rev_ai_rationale  IS 'AI 결정 근거 한 줄 요약';
