-- =============================================================================
-- V20: 직원 계정 비밀번호 해시 교정
--
--  V3/V11/V12 가 시드한 직원 11계정의 password_hash 가 placeholder 였고
--  실제 'Employee1234!' 와 BCrypt 매칭되지 않아 관리자 로그인이 모두 401 로 거부됐다.
--  (시드 주석의 "교체 필요 시 갱신" 이 반영되지 않은 채 머지됨)
--
--  V3/V11/V12 는 이미 적용된 DB 의 체크섬 보호를 위해 수정하지 않고 본 버전에서 교정한다.
--  새 해시는 BCryptPasswordEncoder(10).encode("Employee1234!") 검증 완료(strength 10, $2a).
-- =============================================================================

UPDATE credential
   SET password_hash = '$2a$10$cLdYthdLyRkMgrSGeVwSBOrLmExEFpFvgwXqt.SFAovzo34fDvWRS',
       updated_at    = NOW()
 WHERE password_hash = '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG';
