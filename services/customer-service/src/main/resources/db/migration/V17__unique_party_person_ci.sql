-- =============================================================================
-- V17: party_person.ci_value 부분 유니크 인덱스
--
--  party 패턴 정석 — "한 사람(본인확인 CI) = 한 party" 를 DB 레벨에서 보장한다.
--  본인확인을 거친 가입은 CI 로 기존 party 를 찾아 역할만 추가하므로(RegisterService),
--  동일 CI 로 party 가 둘로 쪼개지는 신원 분리를 인덱스가 최종 방어한다.
--
--  CI 가 없는 기존/레거시 row(직원 시드 등)는 대상에서 제외(부분 인덱스).
-- =============================================================================

CREATE UNIQUE INDEX uq_party_person_ci
    ON party_person (ci_value)
    WHERE ci_value IS NOT NULL AND deleted_at IS NULL;
