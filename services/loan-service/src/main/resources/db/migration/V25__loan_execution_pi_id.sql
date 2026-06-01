-- loan_execution 에 payment-service 결제지시 ID(pi_id) 컬럼 추가.
-- 대출실행 출금 요청 결과를 추적하고 FAILED/CLEARING 재처리에 활용.
ALTER TABLE loan_execution
    ADD COLUMN pi_id VARCHAR(100);

CREATE INDEX idx_loan_execution_pi_id
    ON loan_execution (pi_id)
    WHERE pi_id IS NOT NULL;
