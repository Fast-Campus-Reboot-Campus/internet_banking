ALTER TABLE loan_prescreening
    ADD COLUMN ai_track_cd VARCHAR(20) NULL;

COMMENT ON COLUMN loan_prescreening.ai_track_cd IS 'AI 트랙 분기 결과 (TRACK_1/2/3) — PASS 건만 저장';
