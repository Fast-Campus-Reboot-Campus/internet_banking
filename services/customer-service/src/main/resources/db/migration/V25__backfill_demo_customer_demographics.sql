-- =============================================================================
-- V25: 데모 고객(user01~user10) 생년월일·성별 백필
--
--  배경: V23 은 데모 고객 user01~03 을 시드하지만 party_person.birth_date /
--        gender_code 를 NULL 로 남겨 둔다. user04~10 은 레포 밖 런타임 스크립트로
--        로컬 DB 에만 생성되므로 fresh DB 에는 존재하지 않는다.
--
--  본 마이그레이션은 존재하는 데모 고객의 생년월일/성별만 백필한다(UPDATE).
--    - fresh DB: V23 으로 시드된 9111~9113(user01~03)만 갱신, 9114~9120 은 행이
--      없어 no-op.
--    - 런타임으로 user04~10 이 채워진 로컬 DB: 9111~9120 전원 갱신.
--
--  포맷: birth_date CHAR(8) YYYYMMDD, gender_code CHAR(1) 'M'/'F'.
--  멱등성: UPDATE … FROM (VALUES …) 로 반복 실행해도 동일 결과.
-- =============================================================================

UPDATE party_person AS p
SET birth_date  = v.birth_date,
    gender_code = v.gender_code,
    updated_at  = NOW()
FROM (VALUES
        (9111, '19880312', 'M'),  -- user01
        (9112, '19920724', 'F'),  -- user02
        (9113, '19851103', 'M'),  -- user03
        (9114, '19960518', 'F'),  -- user04
        (9115, '19790130', 'M'),  -- user05
        (9116, '19901008', 'F'),  -- user06
        (9117, '19830622', 'M'),  -- user07
        (9118, '19980914', 'F'),  -- user08
        (9119, '19751205', 'M'),  -- user09
        (9120, '19930427', 'F')   -- user10
     ) AS v(party_id, birth_date, gender_code)
WHERE p.party_id = v.party_id;
