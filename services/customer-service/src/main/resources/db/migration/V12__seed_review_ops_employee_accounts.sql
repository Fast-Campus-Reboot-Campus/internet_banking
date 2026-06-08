-- =============================================================================
-- V12: 대출 본심사 매트릭스 시연용 직원 계정 보강
--
--  V11 이 시드한 관리자 7계정에는 DEPUTY_MANAGER(심사역)·OPS(운영팀) 직급이 없어
--  '수동 심사 실행·확정', '자동 심사', 'EOD' 등 매트릭스 액션을 로그인으로 시연할 수 없다.
--  본 마이그레이션은 두 직급의 직원 계정을 추가한다. (V11 은 이미 적용된 DB 의 체크섬
--  보호를 위해 수정하지 않고 새 버전으로 분리한다.)
--
--  customer_id 9010(deputy01)·9011(ops01). password: Employee1234! (V3/V11 과 동일 해시)
-- =============================================================================

INSERT INTO party (party_id, party_type_code, party_name, party_status_code, version)
OVERRIDING SYSTEM VALUE VALUES
    (9010, 'PERSON', '심사대리', 'ACTIVE', 0),
    (9011, 'PERSON', '운영담당', 'ACTIVE', 0);

INSERT INTO party_person (party_id, is_pep_yn, version)
VALUES (9010,'F',0),(9011,'F',0);

INSERT INTO customer (customer_id, party_id, customer_status_code, main_customer_yn,
                      sms_receive_yn, email_receive_yn, postal_receive_yn,
                      joined_at, created_at, updated_at, version)
OVERRIDING SYSTEM VALUE VALUES
    (9010, 9010, 'ACTIVE', 'F', 'F', 'F', 'F', NOW(), NOW(), NOW(), 0),
    (9011, 9011, 'ACTIVE', 'F', 'F', 'F', 'F', NOW(), NOW(), NOW(), 0);

-- password: Employee1234!  (BCrypt strength 10)
INSERT INTO credential (customer_id, login_id, password_hash,
                        password_changed_at, account_status_code,
                        created_at, updated_at, version)
VALUES
    (9010, 'deputy01', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', NOW(), 'ACTIVE', NOW(), NOW(), 0),
    (9011, 'ops01',    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', NOW(), 'ACTIVE', NOW(), NOW(), 0);

-- EMPLOYEE party_role (직원 게이트)
INSERT INTO party_role (party_id, role_type_code, role_status_code, role_start_date, version)
VALUES
    (9010, 'EMPLOYEE', 'ACTIVE', '20260101', 0),
    (9011, 'EMPLOYEE', 'ACTIVE', '20260101', 0);

-- employee 디테일 — grade_code = BankRole 이름
--   9010 → DEPUTY_MANAGER (수동 심사 실행·확정·편향확인)
--   9011 → OPS            (자동 심사·운영·EOD)
INSERT INTO employee (party_id, branch_code, grade_code, status_code, version)
VALUES
    (9010, '0000', 'DEPUTY_MANAGER', 'ACTIVE', 0),
    (9011, '0000', 'OPS',            'ACTIVE', 0);

-- 시퀀스 동기화 (OVERRIDING SYSTEM VALUE 시드 후 IDENTITY 충돌 방지)
SELECT setval(pg_get_serial_sequence('party', 'party_id'),
              COALESCE((SELECT MAX(party_id) FROM party), 1), true);
SELECT setval(pg_get_serial_sequence('customer', 'customer_id'),
              COALESCE((SELECT MAX(customer_id) FROM customer), 1), true);
