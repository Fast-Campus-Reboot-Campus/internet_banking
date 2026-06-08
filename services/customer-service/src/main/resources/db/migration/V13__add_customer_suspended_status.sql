-- =============================================================================
-- V13: 회원 정지(SUSPENDED) 상태 도입
-- =============================================================================
-- 기존 chk_customer_lifecycle 는 ACTIVE/DORMANT/CLOSED 만 허용한다. 직원용 '회원 상태 관리'
-- 화면의 정지/해제 기능을 위해 Customer 생애주기에 SUSPENDED 를 추가한다.
-- 정지 시점 기록용 suspended_at 컬럼을 두고, CLOSED·DORMANT 와 동일하게 CHECK 불변식에 포함한다.
--
-- 활성 고객 1건 제한 인덱스(uq_customer_active_per_party, WHERE customer_status_code <> 'CLOSED')는
-- SUSPENDED 를 비-CLOSED 로 간주하므로 정지 고객도 활성 슬롯을 점유한다(정지 중 재가입 불가) — 의도된 동작.

ALTER TABLE customer ADD COLUMN suspended_at TIMESTAMPTZ(3);

ALTER TABLE customer DROP CONSTRAINT chk_customer_lifecycle;

ALTER TABLE customer ADD CONSTRAINT chk_customer_lifecycle CHECK (
    (customer_status_code = 'CLOSED'    AND closed_at IS NOT NULL AND close_reason_code IS NOT NULL)
    OR (customer_status_code = 'DORMANT'   AND dormant_at IS NOT NULL)
    OR (customer_status_code = 'SUSPENDED' AND suspended_at IS NOT NULL)
    OR customer_status_code = 'ACTIVE'
);
