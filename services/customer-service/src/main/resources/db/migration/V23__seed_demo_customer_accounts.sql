-- =============================================================================
-- V23: 데모용 고객(차주) 로그인 계정 시드 — user01 / user02 / user03
--
--  배경: 기존 고객 계정(user0N, customer_id 9111+)은 레포 밖 일회성 스크립트로
--        로컬 DB 에만 생성돼 있어, 신규 클론에서는 고객으로 로그인할 수 없었다.
--        (직원 계정은 V3/V11/V12 로 시드되지만 고객 계정 시드는 없었음)
--
--  본 마이그레이션은 고객 로그인 흐름(대출 신청 등) 재현을 위해 데모 고객 3명을 시드한다.
--    customer_id 9111(user01) / 9112(user02) / 9113(user03)
--    password: Employee1234!  (직원 데모와 동일 BCrypt 해시 재사용 — V20 검증 해시)
--    역할 행(party_role) 없음 → 로그인 시 기본 ROLE_CUSTOMER 부여.
--
--  멱등성: 이미 동일 id 가 존재하는 로컬 DB 에서도 안전하도록 ON CONFLICT / NOT EXISTS 가드.
-- =============================================================================

-- party
INSERT INTO party (party_id, party_type_code, party_name, party_status_code, version)
OVERRIDING SYSTEM VALUE VALUES
    (9111, 'PERSON', '테스트고객01', 'ACTIVE', 0),
    (9112, 'PERSON', '테스트고객02', 'ACTIVE', 0),
    (9113, 'PERSON', '테스트고객03', 'ACTIVE', 0)
ON CONFLICT (party_id) DO NOTHING;

-- party_person
INSERT INTO party_person (party_id, is_pep_yn, version)
VALUES
    (9111, 'F', 0),
    (9112, 'F', 0),
    (9113, 'F', 0)
ON CONFLICT (party_id) DO NOTHING;

-- customer
INSERT INTO customer (customer_id, party_id, customer_status_code, main_customer_yn,
                      sms_receive_yn, email_receive_yn, postal_receive_yn,
                      joined_at, created_at, updated_at, version)
OVERRIDING SYSTEM VALUE VALUES
    (9111, 9111, 'ACTIVE', 'T', 'T', 'T', 'F', NOW(), NOW(), NOW(), 0),
    (9112, 9112, 'ACTIVE', 'T', 'T', 'T', 'F', NOW(), NOW(), NOW(), 0),
    (9113, 9113, 'ACTIVE', 'T', 'T', 'T', 'F', NOW(), NOW(), NOW(), 0)
ON CONFLICT (customer_id) DO NOTHING;

-- credential (password: Employee1234! / BCrypt strength 10, V20 검증 해시)
INSERT INTO credential (customer_id, login_id, password_hash, password_changed_at,
                        account_status_code, created_at, updated_at, version)
SELECT v.customer_id, v.login_id,
       '$2a$10$cLdYthdLyRkMgrSGeVwSBOrLmExEFpFvgwXqt.SFAovzo34fDvWRS',
       NOW(), 'ACTIVE', NOW(), NOW(), 0
FROM (VALUES
        (9111, 'user01'),
        (9112, 'user02'),
        (9113, 'user03')
     ) AS v(customer_id, login_id)
WHERE NOT EXISTS (
    SELECT 1 FROM credential c WHERE c.login_id = v.login_id AND c.deleted_at IS NULL
);
