-- =============================================================================
-- V24: 관리자 콘솔(고객계) 데모 고객 시드
--      (main 의 V23 = 대출 데모고객 시드와 번호 충돌 회피 → V24)
--
--  배경: 어드민 화면(/admin/customers·members·member-status·audit-log·join-stats·
--        agent·screening·duplicates·edd·fatca·minors)은 customer-service
--        /api/v1/internal/** 를 조회한다. 그러나 기존 시드는 직원 party(9001~9011)뿐이고
--        직원은 party_type_code='PERSON' 이라 고객 목록 쿼리(WHERE party_type_code='PERSONAL')
--        에서 제외돼, 어드민 고객 화면이 항상 비어 있었다.
--
--  목적: 권한·상태·등급·컴플라이언스가 다양한 "진짜 고객" party 를 시드해 어드민 고객계
--        전 화면과 검토 큐(대리인·제재·중복·EDD·FATCA·미성년·KYC만료)에 데이터가 뜨게 한다.
--
--  ID 범위: party_id / customer_id 8001~8016.
--    - 직원(9001~9011)·앱 생성(IDENTITY 시퀀스, 현재 9011 이후) 어느 쪽과도 충돌하지 않는다.
--    - GENERATED ALWAYS 라 OVERRIDING SYSTEM VALUE 로 명시 id 삽입, 끝에서 setval 보정.
--
--  주의: V11 선례(데모 직원 시드)와 동일 패턴. 운영 배포 시 제외하려면 별도 프로파일 분리 필요.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. party — 고객 14명(8001~8014) + 중복후보(8015) + 대리인(8016)
--    party_type_code='PERSONAL' 이어야 고객 목록에 노출된다(직원은 'PERSON').
-- -----------------------------------------------------------------------------
INSERT INTO party (party_id, party_type_code, party_name, party_status_code, version)
OVERRIDING SYSTEM VALUE VALUES
    (8001, 'PERSONAL', '김민준', 'ACTIVE', 0),
    (8002, 'PERSONAL', '이서연', 'ACTIVE', 0),
    (8003, 'PERSONAL', '박도윤', 'ACTIVE', 0),
    (8004, 'PERSONAL', '최지우', 'ACTIVE', 0),
    (8005, 'PERSONAL', '정하준', 'ACTIVE', 0),
    (8006, 'PERSONAL', '강서윤', 'ACTIVE', 0),
    (8007, 'PERSONAL', '윤지호', 'ACTIVE', 0),
    (8008, 'PERSONAL', '임하은', 'ACTIVE', 0),
    (8009, 'PERSONAL', '한지안', 'ACTIVE', 0),
    (8010, 'PERSONAL', '오시우', 'ACTIVE', 0),
    (8011, 'PERSONAL', '신아윤', 'ACTIVE', 0),
    (8012, 'PERSONAL', '권은우', 'ACTIVE', 0),
    (8013, 'PERSONAL', '황지율', 'ACTIVE', 0),
    (8014, 'PERSONAL', '안소율', 'ACTIVE', 0),
    (8015, 'PERSONAL', '김민준', 'ACTIVE', 0),   -- 8001 과 동명·동일생년 → 중복 검토 후보
    (8016, 'PERSONAL', '대리박', 'ACTIVE', 0);   -- 대리인(고객 아님, customer 미생성)

-- -----------------------------------------------------------------------------
-- 2. party_person — 생년월일·성별·국적·PEP. (PEP=T 는 pep_type_code 필수: chk_party_person_pep)
--    8014 안소율: 미성년(birth_date > now-19y) → 미성년 큐 노출.
-- -----------------------------------------------------------------------------
INSERT INTO party_person (party_id, birth_date, gender_code, nationality_code, is_pep_yn, pep_type_code, version)
VALUES
    (8001, '19850312', 'M', 'KOR', 'F', NULL,       0),
    (8002, '19900624', 'F', 'KOR', 'F', NULL,       0),
    (8003, '19780101', 'M', 'KOR', 'F', NULL,       0),
    (8004, '19951130', 'F', 'KOR', 'F', NULL,       0),
    (8005, '19820705', 'M', 'KOR', 'F', NULL,       0),
    (8006, '19931018', 'F', 'KOR', 'F', NULL,       0),
    (8007, '19880222', 'M', 'KOR', 'F', NULL,       0),
    (8008, '19970909', 'F', 'KOR', 'F', NULL,       0),
    (8009, '19751212', 'M', 'KOR', 'F', NULL,       0),
    (8010, '19700815', 'M', 'KOR', 'T', 'DOMESTIC', 0),   -- PEP(국내 고위공직자 등)
    (8011, '19861103', 'F', 'USA', 'F', NULL,       0),   -- 외국 국적 → FATCA/CRS 대상
    (8012, '19840417', 'M', 'KOR', 'F', NULL,       0),   -- 제재 스크리닝 Hit
    (8013, '19920529', 'F', 'KOR', 'F', NULL,       0),   -- EDD 대상
    (8014, '20100815', 'M', 'KOR', 'F', NULL,       0),   -- 미성년
    (8015, '19850312', 'M', 'KOR', 'F', NULL,       0),   -- 8001 과 동일 생년(중복 후보)
    (8016, '19800303', 'M', 'KOR', 'F', NULL,       0);   -- 대리인

-- -----------------------------------------------------------------------------
-- 3. compliance_info — 검토 큐 대상자만 적재(NOT NULL: aml/kyc/cdd/fatca/crs 코드).
--    EDD: edd_required_yn='T' / 제재: is_*_sanctioned_yn='T' / FATCA: *_reportable_yn='T'
--    KYC만료: kyc_status='COMPLETED' AND kyc_expiry_date <= 조회기준일.
-- -----------------------------------------------------------------------------
INSERT INTO compliance_info (
    party_id, aml_risk_level_code, kyc_status_code, cdd_level_code,
    fatca_status_code, crs_status_code,
    is_kr_sanctioned_yn, fatca_reportable_yn, crs_reportable_yn,
    edd_required_yn, edd_next_review_date, kyc_expiry_date, version)
VALUES
    -- 8010 PEP: 고위험 → EDD 대상
    (8010, 'HIGH', 'COMPLETED', 'EDD', 'NONE', 'NONE',
     'F', 'F', 'F', 'T', '20260731', '20270101', 0),
    -- 8011 외국적: FATCA·CRS 보고 대상
    (8011, 'MEDIUM', 'COMPLETED', 'CDD', 'REPORTABLE', 'REPORTABLE',
     'F', 'T', 'T', 'F', NULL, '20270301', 0),
    -- 8012 제재 대상(국내) + 스크리닝 Hit
    (8012, 'HIGH', 'COMPLETED', 'EDD', 'NONE', 'NONE',
     'T', 'F', 'F', 'F', NULL, '20270601', 0),
    -- 8013 EDD 대상 + KYC 만료 임박(과거일 → 어떤 기준일로 조회해도 노출)
    (8013, 'HIGH', 'COMPLETED', 'EDD', 'NONE', 'NONE',
     'F', 'F', 'F', 'T', '20260620', '20260601', 0);

-- -----------------------------------------------------------------------------
-- 4. customer — 상태/등급/연락처 다양화. 고객 목록·검색·상태관리·마스킹·가입통계의 데이터원.
--    상태별 생애주기 컬럼 필수(chk_customer_lifecycle):
--      DORMANT→dormant_at, SUSPENDED→suspended_at, CLOSED→closed_at+close_reason_code.
-- -----------------------------------------------------------------------------
INSERT INTO customer (customer_id, party_id, customer_grade_code, customer_status_code,
                      credit_rating_code, sms_receive_yn, email_receive_yn, postal_receive_yn,
                      email, phone, zip_code, address, address_detail,
                      join_channel_code, first_join_date, joined_at, last_transaction_at,
                      dormant_at, suspended_at, closed_at, close_reason_code,
                      created_at, updated_at, version)
OVERRIDING SYSTEM VALUE VALUES
    (8001, 8001, 'VIP',    'ACTIVE',    'AAA', 'T','T','F', 'minjun.kim@example.com',  '010-1001-2001', '06236','서울 강남구 테헤란로 1',  '101동 1001호', 'ONLINE','20200310', NOW() - INTERVAL '900 day', NOW() - INTERVAL '2 day',  NULL, NULL, NULL, NULL, NOW(), NOW(), 0),
    (8002, 8002, 'GOLD',   'ACTIVE',    'AA',  'T','F','F', 'seoyeon.lee@example.com', '010-1002-2002', '03187','서울 종로구 종로 2',     '5층',        'BRANCH','20210624', NOW() - INTERVAL '700 day', NOW() - INTERVAL '5 day',  NULL, NULL, NULL, NULL, NOW(), NOW(), 0),
    (8003, 8003, 'SILVER', 'ACTIVE',    'A',   'F','F','T', 'doyun.park@example.com',  '010-1003-2003', '48058','부산 해운대구 센텀1', '202호',       'ONLINE','20220101', NOW() - INTERVAL '500 day', NOW() - INTERVAL '20 day', NULL, NULL, NULL, NULL, NOW(), NOW(), 0),
    (8004, 8004, 'NORMAL', 'ACTIVE',    'BBB', 'T','T','F', 'jiwoo.choi@example.com',  '010-1004-2004', '13529','경기 성남시 분당구 3',  '301호',       'MOBILE','20230515', NOW() - INTERVAL '300 day', NOW() - INTERVAL '1 day',  NULL, NULL, NULL, NULL, NOW(), NOW(), 0),
    (8005, 8005, 'GOLD',   'DORMANT',   'AA',  'F','F','F', 'hajun.jung@example.com',  '010-1005-2005', '34126','대전 유성구 대학로 4',  '11층',        'ONLINE','20190705', NOW() - INTERVAL '1500 day', NOW() - INTERVAL '400 day', NOW() - INTERVAL '380 day', NULL, NULL, NULL, NOW(), NOW(), 0),
    (8006, 8006, 'NORMAL', 'DORMANT',   'BBB', 'F','F','F', 'seoyun.kang@example.com', '010-1006-2006', '61945','광주 서구 상무대로 5',  '8호',         'BRANCH','20200218', NOW() - INTERVAL '1300 day', NOW() - INTERVAL '420 day', NOW() - INTERVAL '400 day', NULL, NULL, NULL, NOW(), NOW(), 0),
    (8007, 8007, 'SILVER', 'SUSPENDED', 'BB',  'T','F','F', 'jiho.yoon@example.com',   '010-1007-2007', '05551','서울 송파구 올림픽로 6', '1503호',      'MOBILE','20210822', NOW() - INTERVAL '800 day', NOW() - INTERVAL '60 day', NULL, NOW() - INTERVAL '30 day', NULL, NULL, NOW(), NOW(), 0),
    (8008, 8008, 'NORMAL', 'SUSPENDED', 'CCC', 'T','T','F', 'haeun.lim@example.com',   '010-1008-2008', '21999','인천 연수구 송도과학로 7','704호',      'ONLINE','20220909', NOW() - INTERVAL '600 day', NOW() - INTERVAL '90 day', NULL, NOW() - INTERVAL '15 day', NULL, NULL, NOW(), NOW(), 0),
    (8009, 8009, 'NORMAL', 'CLOSED',    'B',   'F','F','F', 'jian.han@example.com',    '010-1009-2009', '44676','울산 남구 삼산로 8',    '9호',         'BRANCH','20180101', NOW() - INTERVAL '2000 day', NOW() - INTERVAL '200 day', NULL, NULL, NOW() - INTERVAL '120 day', 'CUST_REQ', NOW(), NOW(), 0),
    (8010, 8010, 'VIP',    'ACTIVE',    'AAA', 'T','T','T', 'siwoo.oh@example.com',    '010-1010-2010', '06164','서울 강남구 영동대로 9', 'PH',          'BRANCH','20170815', NOW() - INTERVAL '2500 day', NOW() - INTERVAL '3 day',  NULL, NULL, NULL, NULL, NOW(), NOW(), 0),
    (8011, 8011, 'GOLD',   'ACTIVE',    'AA',  'T','T','F', 'ayoon.shin@example.com',  '010-1011-2011', '04524','서울 중구 세종대로 10',  '20층',        'ONLINE','20211103', NOW() - INTERVAL '650 day', NOW() - INTERVAL '7 day',  NULL, NULL, NULL, NULL, NOW(), NOW(), 0),
    (8012, 8012, 'NORMAL', 'ACTIVE',    'CCC', 'F','F','F', 'eunwoo.kwon@example.com', '010-1012-2012', '07238','서울 영등포구 여의대로 11','30호',       'MOBILE','20230417', NOW() - INTERVAL '280 day', NOW() - INTERVAL '4 day',  NULL, NULL, NULL, NULL, NOW(), NOW(), 0),
    (8013, 8013, 'SILVER', 'ACTIVE',    'BBB', 'T','T','F', 'jiyul.hwang@example.com', '010-1013-2013', '34047','대전 대덕구 한밭대로 12','606호',       'ONLINE','20220529', NOW() - INTERVAL '470 day', NOW() - INTERVAL '10 day', NULL, NULL, NULL, NULL, NOW(), NOW(), 0),
    (8014, 8014, 'NORMAL', 'ACTIVE',    NULL,  'F','F','F', 'soyul.an@example.com',    '010-1014-2014', '16677','경기 수원시 영통구 13',  '1201호',      'BRANCH','20250815', NOW() - INTERVAL '120 day', NOW() - INTERVAL '12 day', NULL, NULL, NULL, NULL, NOW(), NOW(), 0),
    (8015, 8015, 'NORMAL', 'ACTIVE',    'BBB', 'F','F','F', 'minjun.kim2@example.com', '010-1015-2015', '06236','서울 강남구 테헤란로 1',  '101동 1002호','ONLINE','20260101', NOW() - INTERVAL '30 day',  NOW() - INTERVAL '6 day',  NULL, NULL, NULL, NULL, NOW(), NOW(), 0);
-- 8016(대리인)은 customer 를 만들지 않는다 — 고객이 아니라 위임 관계의 상대 party.

-- -----------------------------------------------------------------------------
-- 5. 대리인 위임장 검토 큐 — party_relation(review_status='PENDING')
--    최지우(8004) 가 대리박(8016) 에게 위임. /admin/agent 대기목록에 노출.
-- -----------------------------------------------------------------------------
INSERT INTO party_relation (from_party_id, to_party_id, relation_type_code, relation_detail_code,
                            representation_scope, proof_url, relation_start_date,
                            relation_review_status_code, version)
VALUES
    (8004, 8016, 'AGENT', 'DELEGATE', '전체 위임(조회·이체)', 'https://example.com/proxy/8004.pdf',
     '20260601', 'PENDING', 0);

-- -----------------------------------------------------------------------------
-- 6. 제재 스크리닝 Hit 검토 큐 — sanction_screening_hit(status='PENDING')
--    권은우(8012) OFAC SDN 유사 92%. /admin/screening 대기목록에 노출.
-- -----------------------------------------------------------------------------
INSERT INTO sanction_screening_hit (party_id, hit_type_code, match_rate, screening_status_code,
                                    detected_at, version)
VALUES
    (8012, 'OFAC_SDN', 92, 'PENDING', NOW() - INTERVAL '2 day', 0);

-- -----------------------------------------------------------------------------
-- 7. 중복고객 검토 큐 — duplicate_review_case(status='PENDING')
--    8015(신규 김민준) ↔ 8001(기존 김민준) 동명·동일생년. /admin/duplicates 대기목록에 노출.
-- -----------------------------------------------------------------------------
INSERT INTO duplicate_review_case (new_party_id, existing_party_id, match_type_code,
                                   review_status_code, detected_at, version)
VALUES
    (8015, 8001, 'NAME_BIRTH', 'PENDING', NOW() - INTERVAL '1 day', 0);

-- -----------------------------------------------------------------------------
-- 8. 조회 접근 감사로그 — /admin/audit-log 표시용 스냅샷 로그(FK 없음, append-only).
--    accessor_employee_id 는 직원 식별자(표시는 accessor_name 스냅샷 우선).
-- -----------------------------------------------------------------------------
INSERT INTO customer_access_log (accessor_employee_id, accessor_name, accessor_role, accessor_branch_code,
                                 target_customer_id, target_customer_name,
                                 access_action_code, access_reason, accessed_at)
VALUES
    (9003, '김감사',  'COMPLIANCE',     '0000', 8012, '권은우', 'CUSTOMER_DETAIL', '제재 스크리닝 Hit 검토',        NOW() - INTERVAL '3 hour'),
    (9008, '한직원',  'TELLER',         '0001', 8001, '김민준', 'CONTACT_VIEW',    '대출 상담 요청 연락',          NOW() - INTERVAL '5 hour'),
    (9007, '정담당',  'BRANCH_MANAGER', '0001', 8007, '윤지호', 'CUSTOMER_DETAIL', '이상거래 정지 사유 확인',       NOW() - INTERVAL '1 day'),
    (9004, '이심사',  'HQ_REVIEWER',    '0000', 8013, '황지율', 'CUSTOMER_DETAIL', 'EDD 심사 대상 검토',           NOW() - INTERVAL '1 day' - INTERVAL '2 hour'),
    (9008, '한직원',  'TELLER',         '0001', 8002, '이서연', 'CONTACT_VIEW',    '카드 발급 안내 연락',          NOW() - INTERVAL '2 day');

-- -----------------------------------------------------------------------------
-- 9. IDENTITY 시퀀스 보정 — OVERRIDING SYSTEM VALUE 로 명시 id 삽입했으므로
--    시퀀스를 현재 MAX 로 맞춰 앱 생성 id 와 충돌하지 않게 한다(V11 과 동일).
--    (직원 9011 이 더 크므로 실제 시퀀스 값은 변하지 않을 수 있다 — 안전한 no-op)
-- -----------------------------------------------------------------------------
SELECT setval(pg_get_serial_sequence('party', 'party_id'),
              COALESCE((SELECT MAX(party_id) FROM party), 1), true);
SELECT setval(pg_get_serial_sequence('customer', 'customer_id'),
              COALESCE((SELECT MAX(customer_id) FROM customer), 1), true);
