-- =============================================================================
-- V21: 직원 역할 의미 정합 + 테스트 계정 실명화
--
--  1) owner01(9007 정담당): grade BRANCH_MANAGER → TELLER 로 정정.
--     "담당 직원(PRIMARY_OWNER)"은 정적 직급이 아니라 동적 관계(담당 고객 보유)로
--     판정해야 하는 개념이라, 직급 자체는 창구직원(TELLER)이 맞다. 이로써 0001 지점에
--     지점장이 둘(9001 + 9007)이던 모순도 해소된다. (대출 최종결재 데모는 9001 지점장이 담당)
--  2) 9001/9002 placeholder 이름(지점장테스트/부지점장테스트)을 실명으로 교체.
--     직급(BRANCH_MANAGER/DEPUTY_MANAGER)·지점(0001)은 유지.
--
--  V3/V11 은 이미 적용된 DB 의 체크섬 보호를 위해 수정하지 않고 본 버전에서 정정한다.
-- =============================================================================

-- 1) 담당직원 직급 정정 (지점장 → 창구직원)
UPDATE employee
   SET grade_code = 'TELLER',
       updated_at = NOW()
 WHERE party_id = 9007
   AND grade_code = 'BRANCH_MANAGER';

-- 2) 테스트 계정 실명화
UPDATE party SET party_name = '박상우' WHERE party_id = 9001 AND party_name = '지점장테스트';
UPDATE party SET party_name = '김다은' WHERE party_id = 9002 AND party_name = '부지점장테스트';
