-- 대출상품 상태 코드 정규화.
-- 일부 상품이 도메인에 없는 'ON_SALE' 값으로 적재되어 신청 검증(STATUS_ACTIVE)에서
-- LOAN_010("판매 중인 상품이 아닙니다")으로 거절되었다.
-- 도메인 표준(DRAFT/ACTIVE/DISCONTINUED)에 맞춰 ON_SALE -> ACTIVE 로 정렬한다.
UPDATE loan_product
   SET prod_status_cd = 'ACTIVE'
 WHERE prod_status_cd = 'ON_SALE';
