-- =============================================================================
-- V27: PEP 유형코드 정본화 — 관계 축으로 정정
--
--  배경: pep_type_code 는 "고객과 PEP의 관계"(FATF: 본인/가족/측근)를 담는 컬럼이다.
--    국내/해외 구분은 별도 컬럼 pep_country_code(ISO 국가코드)가 이미 담당한다.
--    V24 시드가 8010에 pep_type_code='DOMESTIC'(국내/해외 축)을 넣은 것은 컬럼 의미를
--    잘못 쓴 것이자 pep_country_code 와 정보가 중복된다.
--
--  정정:
--    - pep_type_code 'DOMESTIC' → 'SELF'  (해당 party는 "고위공직자 본인" → SELF)
--    - 국내 PEP 라는 사실은 pep_country_code='KOR' 로 옮겨 보존
--
--  카탈로그(code_master) PEP_TYPE 정본: SELF / FAMILY / CLOSE_ASSOC
--
--  제약(chk_party_person_pep): is_pep_yn='T' 행은 pep_type_code NOT NULL 필요 — SELF로 충족.
--  값 기준 UPDATE라 데모 시드가 없는 환경에서는 0건 처리(no-op)된다.
-- =============================================================================

UPDATE party_person
   SET pep_type_code    = 'SELF',
       pep_country_code = COALESCE(pep_country_code, 'KOR'),
       updated_at       = NOW()
 WHERE pep_type_code = 'DOMESTIC';
