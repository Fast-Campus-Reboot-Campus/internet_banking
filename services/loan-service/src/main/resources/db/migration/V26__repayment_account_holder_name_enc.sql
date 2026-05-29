-- repayment_account 에 예금주명 암호화 컬럼 추가.
-- 역분개 환급 시 payment-service receiverHolderName 검증에 사용.
ALTER TABLE repayment_account
    ADD COLUMN holder_name_enc BYTEA;
