-- =============================================================================
-- V28: 미사용 cust_code_master 정리
-- =============================================================================
-- cust_code_master 는 V1 에서 "전 도메인 공통코드 soft-reference" 테이블로 생성됐으나,
-- 검증(@ValidCode 0건)·런타임 어디에서도 사용되지 않는 죽은 테이블이다.
-- (코드 조회는 master-service 의 code_master 로 일원화됨. customer 의 /api/v1/codes
--  CodeController 는 프론트·타 서비스 어디서도 호출되지 않음.)
-- FK 미설정 soft reference 라 의존 객체 없음 → 안전하게 제거한다.
--
-- 동반 삭제(같은 PR): customer/code 패키지
--   CodeController · CustCodeMaster(domain) · CodeResponse(dto) · CustCodeMasterRepository

DROP TABLE IF EXISTS cust_code_master;
