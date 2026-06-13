-- =============================================================================
-- V29: 직원 party_type 'PERSON'(비표준) → 'PERSONAL' 정본화
-- =============================================================================
-- party_type_code 는 개인(PERSONAL)/법인(ORGANIZATION) 축이다. 직원 여부는
-- party_role(role_type_code='EMPLOYEE')·employee 테이블이 정식으로 보유한다.
-- 그동안 직원 party 를 비표준값 'PERSON'으로 시드해, 고객목록 쿼리
-- (WHERE party_type_code='PERSONAL')가 직원을 "우연히" 걸러온 꼼수가 있었다.
-- 이 마이그레이션으로 직원 party_type 를 정본(PERSONAL)으로 교정하고,
-- 직원 제외는 CustomerRepository.searchCustomers 에서 party_role(EMPLOYEE)
-- NOT EXISTS 로 명시 전환한다(같은 PR — 데이터·쿼리 원자 변경).
--
-- 값 기준 UPDATE 라 'PERSON'이 없는 환경(운영)에서는 0건(no-op)이다.

UPDATE party
   SET party_type_code = 'PERSONAL',
       updated_at      = NOW()
 WHERE party_type_code = 'PERSON';
