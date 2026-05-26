-- 자동 발화 멱등 가드 확장.
-- submit 직후 상태가 SENT 였던 stub 동작이 outbox 도입(plan 02 step 4)으로 REQUESTED 로 바뀌었다.
-- 따라서 PENDING 단계의 중복 발화도 차단하도록 REQUESTED 를 UNIQUE 대상에 포함시킨다.
-- FAILED/DEAD 는 운영자/배치가 재시도 결정을 내리므로 제외 — 재발화 자유.

DROP INDEX IF EXISTS uk_credit_info_report_dlq_idem;

CREATE UNIQUE INDEX uk_credit_info_report_dlq_idem
    ON credit_info_report (cntr_id, dlq_id, crpt_type_cd, report_reason_cd)
    WHERE dlq_id IS NOT NULL AND crpt_status_cd IN ('REQUESTED', 'SENT', 'ACKED');
