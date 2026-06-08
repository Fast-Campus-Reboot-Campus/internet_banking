-- 원 거래(rtx)당 활성 역분개(reversal)는 최대 1건만 허용한다.
-- 동시/중복 reverse() 요청이 existsActiveReversal SELECT 가드를 동시에 통과해
-- 이중 환급되는 것을 DB 레벨에서 차단한다(애플리케이션 단 가드는 빠른 사전 검사일 뿐).
CREATE UNIQUE INDEX IF NOT EXISTS ux_rtx_active_reversal_target
    ON repayment_transaction (reversal_target_rtx_id)
    WHERE reversal_yn = 'Y'
      AND rtx_status_cd = 'SUCCESS'
      AND deleted_at IS NULL;
