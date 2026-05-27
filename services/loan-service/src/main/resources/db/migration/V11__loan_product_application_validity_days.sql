-- Plan 09: 신청 승인 유효기간 상품별 차등
-- NULL = 시스템 기본 14일, 1~90 사이 값 허용
ALTER TABLE loan_product
    ADD COLUMN application_validity_days INT;
