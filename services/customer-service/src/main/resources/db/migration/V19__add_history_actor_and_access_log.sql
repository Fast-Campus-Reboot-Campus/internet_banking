-- =============================================================================
-- V19: 직원 행위자 기록 — 변경 이력 actor + 조회 접근 감사로그
-- =============================================================================
-- 그동안 직원 admin API(상태/등급 변경, 고객 조회)의 "행위자"가 어디에도 남지 않았다.
-- (customer-service 는 CurrentActorProvider 를 오버라이드하지 않아 created_by 는 항상 0=SYSTEM)
-- 게이트웨이가 JWT 에서 주입하는 X-Employee-Id(검증된 직원 employee_id)를 받아 기록한다.

-- 1) 상태/등급 변경 이력에 변경자(직원) 컬럼 추가. 시스템 자동 전환은 NULL.
ALTER TABLE customer_status_history
    ADD COLUMN changed_by_employee_id BIGINT;

ALTER TABLE customer_grade_history
    ADD COLUMN changed_by_employee_id BIGINT;

-- 2) 조회 접근 감사로그 — "누가(직원) 누구를(고객) 무엇을(행위) 왜(사유) 언제" 조회했는가.
--    append-only 로그. FK 는 두지 않는다(감사 적재가 FK 로 막히면 안 됨, reviewer_employee_id 선례와 동일).
--    직원명·역할·지점·고객명은 조회 시점 스냅샷으로 적재한다 — 조회는 조인 없는 단순 SELECT 가 되고
--    이후 직원 전보·개명이 있어도 "그 시점의 사실"이 보존돼 감사상 정확하다.
CREATE TABLE customer_access_log (
    customer_access_log_id  BIGINT         GENERATED ALWAYS AS IDENTITY,
    accessor_employee_id    BIGINT         NOT NULL,   -- 조회한 직원 employee_id (X-Employee-Id)
    accessor_name           VARCHAR(100),              -- 직원명 스냅샷
    accessor_role           VARCHAR(40),               -- 직원 역할(BankRole) 스냅샷
    accessor_branch_code    VARCHAR(10),               -- 직원 지점 스냅샷(지점 범위 필터용)
    target_customer_id      BIGINT         NOT NULL,   -- 조회 대상 고객 customer_id
    target_customer_name    VARCHAR(100),              -- 고객명 스냅샷
    access_action_code      VARCHAR(40)    NOT NULL,   -- CUSTOMER_DETAIL / CONTACT_VIEW 등
    access_reason           VARCHAR(500),              -- 조회 사유(필요 역할만 입력)
    accessed_at             TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_customer_access_log PRIMARY KEY (customer_access_log_id)
);

-- 대상 고객별 접근 이력 조회
CREATE INDEX idx_customer_access_log_target   ON customer_access_log (target_customer_id);
-- 직원별 접근 이력 조회
CREATE INDEX idx_customer_access_log_accessor ON customer_access_log (accessor_employee_id);
-- 최근순 정렬(감사 화면 기본)
CREATE INDEX idx_customer_access_log_at       ON customer_access_log (accessed_at DESC);
