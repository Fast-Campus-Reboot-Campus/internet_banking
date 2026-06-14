-- V30: customer 1 데모 인증서 로그인 실패 7건 시드 (상대시각)
--
-- 목적: Fraud Investigation Agent 의 get_auth_events 실연동 데모(Stage 7)에서
--   GET /api/v1/internal/auth/1/events 가 "최근 24h 인증실패"를 의미 있는 값(7회)으로
--   반환하게 한다. 이 값으로 에이전트가 계정탈취(H2)를 확정한다.
--
-- 왜 이 마이그레이션이 필요한가:
--   - 기존 데모 실패 이력은 마이그레이션이 아니라 런타임으로 생성돼 fresh DB 에는 없다.
--   - 카운트 쿼리(InternalAuthEventsController)는 `certificate_used_at >= now() - 24h` 의
--     슬라이딩 윈도우라, 고정 시각으로 박은 시드는 하루만 지나도 윈도우 밖으로 늙는다.
--   → 여기서 now() 상대시각으로 삽입해, "마이그레이션이 도는 순간 기준 최근"을 보장한다.
--
-- 주의(상대시각의 한계): now() 는 이 마이그레이션이 적용되는 시점에 한 번 평가된다.
--   따라서 적용 후 24h 가 지나면 다시 윈도우 밖으로 늙는다. 항상 최근으로 보려면
--   fresh DB(또는 이 시드 재적용)로 데모를 띄우는 것을 전제로 한다.
--
-- 멱등: 데모 마커(CERT_FAIL_BLOCK) 행을 지우고 다시 넣어 중복을 방지한다.
-- FK: certificate_use.certificate_id 는 임의의 유효 인증서를 참조한다(카운트 쿼리는
--   cert_use.customer_id 만 보므로 소유자 일치는 불필요 — 기존 데모 데이터와 동일한 방식).

DO $$
DECLARE
    v_cert_id BIGINT;
BEGIN
    -- 전제: customer 1 + 인증서 1건 이상 존재(둘 다 선행 마이그레이션 시드). 없으면 스킵.
    IF NOT EXISTS (SELECT 1 FROM customer WHERE customer_id = 1) THEN
        RAISE NOTICE 'V30 skip: customer 1 not found';
        RETURN;
    END IF;

    SELECT certificate_id INTO v_cert_id FROM certificate ORDER BY certificate_id LIMIT 1;
    IF v_cert_id IS NULL THEN
        RAISE NOTICE 'V30 skip: no certificate to reference';
        RETURN;
    END IF;

    DELETE FROM certificate_use
     WHERE customer_id = 1
       AND certificate_use_failure_reason_code = 'CERT_FAIL_BLOCK';

    INSERT INTO certificate_use (
        certificate_id, customer_id, purpose_code,
        certificate_use_signed_data_hash, certificate_use_signature_value,
        certificate_use_verification_result_code, certificate_use_failure_reason_code,
        certificate_use_request_ip, certificate_use_request_channel_code,
        certificate_used_at
    )
    SELECT
        v_cert_id, 1, 'LOGIN',
        'demo-cert-fail-hash-' || g, 'demo-sig',
        'FAIL', 'CERT_FAIL_BLOCK',
        '203.0.113.' || (10 + g), 'MOBILE',
        now() - (g * interval '1 minute')   -- 상대시각: 1~7분 전
    FROM generate_series(1, 7) AS g;

    RAISE NOTICE 'V30: seeded 7 cert failures for customer 1 (cert_id=%)', v_cert_id;
END $$;
