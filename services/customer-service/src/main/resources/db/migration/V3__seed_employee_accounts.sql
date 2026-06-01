-- =============================================================================
-- V3: 테스트용 직원 계정 시드
-- customer_id 9001 (지점장), 9002 (부지점장)
-- loginId: employee01 / employee02
-- password: Employee1234!
--   BCrypt hash: $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG
--   (= BCryptPasswordEncoder(10).encode("Employee1234!") 결과로 교체 필요 시 갱신)
-- =============================================================================

-- party
INSERT INTO party (party_id, party_type_code, party_name, party_status_code, version)
OVERRIDING SYSTEM VALUE VALUES
    (9001, 'PERSON', '지점장테스트', 'ACTIVE', 0),
    (9002, 'PERSON', '부지점장테스트', 'ACTIVE', 0);

-- party_person
INSERT INTO party_person (party_id, is_pep_yn, version)
VALUES
    (9001, 'F', 0),
    (9002, 'F', 0);

-- customer
INSERT INTO customer (customer_id, party_id, customer_status_code, main_customer_yn,
                      sms_receive_yn, email_receive_yn, postal_receive_yn,
                      joined_at, created_at, updated_at, version)
OVERRIDING SYSTEM VALUE VALUES
    (9001, 9001, 'ACTIVE', 'F', 'F', 'F', 'F', NOW(), NOW(), NOW(), 0),
    (9002, 9002, 'ACTIVE', 'F', 'F', 'F', 'F', NOW(), NOW(), NOW(), 0);

-- credential
-- password: Employee1234!  (BCrypt strength 10)
INSERT INTO credential (customer_id, login_id, password_hash,
                        password_changed_at, account_status_code,
                        created_at, updated_at, version)
VALUES
    (9001, 'employee01', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
     NOW(), 'ACTIVE', NOW(), NOW(), 0),
    (9002, 'employee02', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
     NOW(), 'ACTIVE', NOW(), NOW(), 0);
