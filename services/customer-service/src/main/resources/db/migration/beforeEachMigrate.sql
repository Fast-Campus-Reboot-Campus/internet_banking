-- =============================================================================
-- Flyway 콜백: beforeEachMigrate — 각 마이그레이션 실행 직전 호출된다.
--
-- V4(add_axful_cert_and_qr_login)가 customer_id=1 기준으로 auth_method/certificate
-- 테스트 시드를 넣지만, 해당 고객(customer_id=1)을 생성하는 마이그레이션이 없어
-- fresh 설치 시 FK 위반(fk_auth_method_customer)으로 V4 가 실패한다.
--
-- 이 콜백은 customer 스키마가 준비된 시점부터 테스트 고객(customer_id=1)을
-- 멱등(ON CONFLICT DO NOTHING)으로 보장해 위 실패를 막는다.
--
-- 주의:
--   - 콜백은 schema_history 에 체크섬으로 기록되지 않으므로 기존 DB 의 V4 검증에 영향이 없다.
--   - 이미 마이그레이션이 완료된 DB(대기 마이그레이션 없음)에서는 호출되지 않는다.
--   - V4 를 수정하지 않으므로 팀원의 기존 DB 는 리셋/repair 가 불필요하다.
-- =============================================================================
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'customer'
    ) THEN
        INSERT INTO party (party_id, party_type_code, party_name, party_status_code, version)
        OVERRIDING SYSTEM VALUE
        VALUES (1, 'PERSON', '테스트고객', 'ACTIVE', 0)
        ON CONFLICT DO NOTHING;

        INSERT INTO party_person (party_id, is_pep_yn, version)
        VALUES (1, 'F', 0)
        ON CONFLICT DO NOTHING;

        INSERT INTO customer (customer_id, party_id, customer_status_code, main_customer_yn,
                              sms_receive_yn, email_receive_yn, postal_receive_yn,
                              joined_at, created_at, updated_at, version)
        OVERRIDING SYSTEM VALUE
        VALUES (1, 1, 'ACTIVE', 'T', 'F', 'F', 'F', NOW(), NOW(), NOW(), 0)
        ON CONFLICT DO NOTHING;

        -- 시퀀스 동기화: OVERRIDING SYSTEM VALUE 로 명시 id 를 seed 하면 IDENTITY 시퀀스가
        -- 올라가지 않아, 앱이 생성하는 id 가 seed된 id(1, 9001 등)와 충돌한다(pk 중복).
        -- seed(V3 직원 + 본 콜백) 이후 시퀀스를 최댓값으로 맞춰 충돌을 막는다.
        PERFORM setval(pg_get_serial_sequence('party', 'party_id'),
                       COALESCE((SELECT MAX(party_id) FROM party), 1), true);
        PERFORM setval(pg_get_serial_sequence('customer', 'customer_id'),
                       COALESCE((SELECT MAX(customer_id) FROM customer), 1), true);
    END IF;
END $$;
