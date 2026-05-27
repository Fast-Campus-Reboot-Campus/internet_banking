-- 연체 자동 발화 멱등 가드.
-- 같은 (cntrId, dlqId, crptTypeCd, reportReasonCd) 신고가 SENT/ACKED 상태로 중복 적재되는 것을 차단한다.
-- dlqId 가 NULL 인 수동/약정/종결 신고는 본 제약 대상 외.

CREATE UNIQUE INDEX uk_credit_info_report_dlq_idem
    ON credit_info_report (cntr_id, dlq_id, crpt_type_cd, report_reason_cd)
    WHERE dlq_id IS NOT NULL AND crpt_status_cd IN ('SENT', 'ACKED');
