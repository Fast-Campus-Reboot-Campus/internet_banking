-- =============================================================================
-- V18: identity_verification 에 주민번호 암호문·소비 컬럼 추가
--
--  주민번호 기반 본인확인 강화. 검증 시점에 주민번호를 AES-256 암호문으로 박제하고
--  (평문 미보관), 가입(RegisterService)이 이 검증 1건을 1회 소비해 party 로 이전한다.
--  - rrn_encrypted        : 주민등록번호 AES-256 암호문 (가입 시 party_person.rrn_encrypted 로 복사)
--  - consumed_yn/customer : 가입에 소비됐는지 + 연결된 고객 (검증 1건 = 가입 1건)
-- =============================================================================

ALTER TABLE identity_verification
    ADD COLUMN rrn_encrypted        VARCHAR(255),
    ADD COLUMN consumed_yn          CHAR(1)        NOT NULL DEFAULT 'F',
    ADD COLUMN consumed_customer_id BIGINT,
    ADD COLUMN consumed_at          TIMESTAMPTZ(3);

ALTER TABLE identity_verification
    ADD CONSTRAINT chk_identity_verification_consumed CHECK (consumed_yn IN ('T','F'));
