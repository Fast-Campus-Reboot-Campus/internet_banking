-- 대출 신청 접수 지점. 기존 행은 NULL 허용, 신규 신청부터 채움.
ALTER TABLE loan_application
    ADD COLUMN branch_id VARCHAR(10)
        REFERENCES branch(branch_id);
