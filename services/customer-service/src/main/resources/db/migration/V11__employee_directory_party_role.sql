-- =============================================================================
-- V11: 직원 디렉토리를 party_role 기반 DB 모델로 이관
--
--  기존: 직원 판정(branch/grade/roles)을 application.yml `employee-directory` 가 담당.
--  변경: party 패턴 정석대로 직원 party 에 EMPLOYEE 역할(party_role)을 부여하고,
--        직급·지점 등 직원 전용 속성은 신규 employee 테이블(party_person 과 같은
--        서브타입 디테일 테이블)에 저장한다. JWT roles 는 grade_code(=BankRole 값)에서 파생.
--
--  grade_code 는 common BankRole enum 의 이름을 그대로 쓴다 (예: HQ_RISK, BRANCH_MANAGER).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. employee  (직원, party_person 1:1 서브타입과 같은 위치)
-- -----------------------------------------------------------------------------
CREATE TABLE employee (
    employee_id  BIGINT         GENERATED ALWAYS AS IDENTITY,
    party_id     BIGINT         NOT NULL,
    branch_code  VARCHAR(10)    NOT NULL,
    grade_code   VARCHAR(30)    NOT NULL,
    status_code  VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by   BIGINT,
    updated_at   TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by   BIGINT,
    deleted_at   TIMESTAMPTZ(3),
    deleted_by   BIGINT,
    version      INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_employee PRIMARY KEY (employee_id),
    CONSTRAINT fk_employee_party FOREIGN KEY (party_id) REFERENCES party (party_id),
    CONSTRAINT uq_employee_party UNIQUE (party_id),
    CONSTRAINT chk_employee_status CHECK (status_code IN ('ACTIVE','CLOSED'))
);
CREATE INDEX idx_employee_party ON employee (party_id);

-- -----------------------------------------------------------------------------
-- 2. 관리자 콘솔 직원 7계정 시드 (party / party_person / customer / credential)
--    customer_id 9003~9009. password: Employee1234! (V3 와 동일 BCrypt 해시)
-- -----------------------------------------------------------------------------
INSERT INTO party (party_id, party_type_code, party_name, party_status_code, version)
OVERRIDING SYSTEM VALUE VALUES
    (9003, 'PERSON', '김감사',   'ACTIVE', 0),
    (9004, 'PERSON', '이심사',   'ACTIVE', 0),
    (9005, 'PERSON', '박리스크', 'ACTIVE', 0),
    (9006, 'PERSON', '최마케팅', 'ACTIVE', 0),
    (9007, 'PERSON', '정담당',   'ACTIVE', 0),
    (9008, 'PERSON', '한직원',   'ACTIVE', 0),
    (9009, 'PERSON', '오타지점', 'ACTIVE', 0);

INSERT INTO party_person (party_id, is_pep_yn, version)
VALUES (9003,'F',0),(9004,'F',0),(9005,'F',0),(9006,'F',0),(9007,'F',0),(9008,'F',0),(9009,'F',0);

INSERT INTO customer (customer_id, party_id, customer_status_code, main_customer_yn,
                      sms_receive_yn, email_receive_yn, postal_receive_yn,
                      joined_at, created_at, updated_at, version)
OVERRIDING SYSTEM VALUE VALUES
    (9003, 9003, 'ACTIVE', 'F', 'F', 'F', 'F', NOW(), NOW(), NOW(), 0),
    (9004, 9004, 'ACTIVE', 'F', 'F', 'F', 'F', NOW(), NOW(), NOW(), 0),
    (9005, 9005, 'ACTIVE', 'F', 'F', 'F', 'F', NOW(), NOW(), NOW(), 0),
    (9006, 9006, 'ACTIVE', 'F', 'F', 'F', 'F', NOW(), NOW(), NOW(), 0),
    (9007, 9007, 'ACTIVE', 'F', 'F', 'F', 'F', NOW(), NOW(), NOW(), 0),
    (9008, 9008, 'ACTIVE', 'F', 'F', 'F', 'F', NOW(), NOW(), NOW(), 0),
    (9009, 9009, 'ACTIVE', 'F', 'F', 'F', 'F', NOW(), NOW(), NOW(), 0);

-- password: Employee1234!  (BCrypt strength 10) — V3 와 동일 해시
INSERT INTO credential (customer_id, login_id, password_hash,
                        password_changed_at, account_status_code,
                        created_at, updated_at, version)
VALUES
    (9003, 'audit01',  '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', NOW(), 'ACTIVE', NOW(), NOW(), 0),
    (9004, 'review01', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', NOW(), 'ACTIVE', NOW(), NOW(), 0),
    (9005, 'risk01',   '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', NOW(), 'ACTIVE', NOW(), NOW(), 0),
    (9006, 'mkt01',    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', NOW(), 'ACTIVE', NOW(), NOW(), 0),
    (9007, 'owner01',  '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', NOW(), 'ACTIVE', NOW(), NOW(), 0),
    (9008, 'staff01',  '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', NOW(), 'ACTIVE', NOW(), NOW(), 0),
    (9009, 'other01',  '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', NOW(), 'ACTIVE', NOW(), NOW(), 0);

-- -----------------------------------------------------------------------------
-- 3. EMPLOYEE party_role — 기존 직원(9001/9002) + 신규 7계정(9003~9009)
--    "이 party 는 직원 역할을 한다"는 정식 표시. 인증 디렉토리의 게이트.
-- -----------------------------------------------------------------------------
INSERT INTO party_role (party_id, role_type_code, role_status_code, role_start_date, version)
VALUES
    (9001, 'EMPLOYEE', 'ACTIVE', '20260101', 0),
    (9002, 'EMPLOYEE', 'ACTIVE', '20260101', 0),
    (9003, 'EMPLOYEE', 'ACTIVE', '20260101', 0),
    (9004, 'EMPLOYEE', 'ACTIVE', '20260101', 0),
    (9005, 'EMPLOYEE', 'ACTIVE', '20260101', 0),
    (9006, 'EMPLOYEE', 'ACTIVE', '20260101', 0),
    (9007, 'EMPLOYEE', 'ACTIVE', '20260101', 0),
    (9008, 'EMPLOYEE', 'ACTIVE', '20260101', 0),
    (9009, 'EMPLOYEE', 'ACTIVE', '20260101', 0);

-- -----------------------------------------------------------------------------
-- 4. employee 디테일 — branch_code / grade_code(=BankRole)
--    9001/9002 는 yaml employee-directory 에서 이관, 9003~9009 는 관리자 7역할 매핑.
--    관리자 7역할 → BankRole:
--      감사→COMPLIANCE, 심사→HQ_REVIEWER, 리스크→HQ_RISK, 마케팅→HQ_MARKETING,
--      담당→BRANCH_MANAGER, 지점직원→TELLER.
--    (PRIMARY_OWNER/OTHER_BRANCH 는 정적 직급이 아니라 담당·지점 비교로 판정하는 동적 개념)
-- -----------------------------------------------------------------------------
INSERT INTO employee (party_id, branch_code, grade_code, status_code, version)
VALUES
    (9001, '0001', 'BRANCH_MANAGER', 'ACTIVE', 0),
    (9002, '0001', 'DEPUTY_MANAGER', 'ACTIVE', 0),
    (9003, '0000', 'COMPLIANCE',     'ACTIVE', 0),
    (9004, '0000', 'HQ_REVIEWER',    'ACTIVE', 0),
    (9005, '0000', 'HQ_RISK',        'ACTIVE', 0),
    (9006, '0000', 'HQ_MARKETING',   'ACTIVE', 0),
    (9007, '0001', 'BRANCH_MANAGER', 'ACTIVE', 0),
    (9008, '0001', 'TELLER',         'ACTIVE', 0),
    (9009, '0002', 'TELLER',         'ACTIVE', 0);

-- 시퀀스 동기화: OVERRIDING SYSTEM VALUE 로 명시 id 시드 시 IDENTITY 시퀀스가 올라가지 않아
-- 앱 생성 id 와 충돌한다. beforeEachMigrate 콜백도 맞추지만 본 마이그레이션에서도 보정한다.
SELECT setval(pg_get_serial_sequence('party', 'party_id'),
              COALESCE((SELECT MAX(party_id) FROM party), 1), true);
SELECT setval(pg_get_serial_sequence('customer', 'customer_id'),
              COALESCE((SELECT MAX(customer_id) FROM customer), 1), true);
