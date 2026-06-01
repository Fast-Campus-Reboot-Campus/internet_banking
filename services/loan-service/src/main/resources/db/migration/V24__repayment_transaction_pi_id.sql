-- repayment_transaction 에 payment-service 결제지시 ID(pi_id) 컬럼 추가.
-- loan-payment-integration-spec §3 "paymentInstructionId 저장 권장" 반영.
-- CLEARING 콜백 수신 시 pi_id 로 거래를 조회하므로 인덱스도 함께 생성.
ALTER TABLE repayment_transaction
    ADD COLUMN pi_id VARCHAR(100);

CREATE INDEX idx_repayment_transaction_pi_id
    ON repayment_transaction (pi_id)
    WHERE pi_id IS NOT NULL;
