-- 연체 자동 발화 신고의 출처 dlq 추적 컬럼.
-- 자동 발화 외 신고(수동/약정 체결/종결)는 NULL.
-- 멱등 UNIQUE 인덱스는 step 3 (신고 멱등 가드) 마이그레이션에서 추가한다.

ALTER TABLE credit_info_report
    ADD COLUMN dlq_id BIGINT;

CREATE INDEX idx_credit_info_report_dlq_id
    ON credit_info_report (dlq_id)
    WHERE dlq_id IS NOT NULL;
