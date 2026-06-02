-- 공통 계좌 마스터 (common_account) — 공통 계좌 DB(common_db).
-- 여러 서비스가 공유하는 계좌 마스터. 수신/여신 계좌는 이 PK 를 공유(서브타입)한다.
--
-- 출처: deposit-service V5__full_erd_schema.sql 의 common_account 정의를 기준으로 하되,
-- common_db 는 독립 DB 이므로 customer/common_contract 등 타 테이블 FK 는 제거했다.
-- (서비스 간 참조는 FK 없이 값으로만 — 기존 cross-DB 관행과 동일)
--
-- account_type_cd: 계좌 종류. 일반(GENERAL) / 가상(VIRTUAL) / 시스템 수납·집행(SYSTEM) 등.
-- Phase 1 에서는 loan-service 가 본 스키마를 소유·마이그레이션하고,
-- Phase 2 에서 deposit-service 가 자기 common_account 를 본 DB 참조로 전환한다.
CREATE TABLE common_account (
    account_id              BIGSERIAL       PRIMARY KEY,
    account_no              VARCHAR(30)     UNIQUE,
    customer_id             BIGINT          NOT NULL,
    customer_no             VARCHAR(30),
    contract_id             BIGINT,
    account_type_cd         VARCHAR(30),
    bank_cd                 VARCHAR(10),
    account_nickname        VARCHAR(100),
    balance                 BIGINT,
    currency_cd             CHAR(3),
    account_password_hash   VARCHAR(255),
    daily_withdrawal_limit  BIGINT,
    daily_withdrawal_count  INT,
    suspic_account_yn       CHAR(1),
    account_status          VARCHAR(20),
    account_opened_at       CHAR(8),
    account_closed_at       CHAR(8),
    account_cancel_at       TIMESTAMPTZ(3),
    last_transaction_at     TIMESTAMPTZ(3),
    created_at              TIMESTAMPTZ(3)  NOT NULL DEFAULT now(),
    created_by              BIGINT,
    updated_at              TIMESTAMPTZ(3),
    updated_by              BIGINT
);

-- 계좌번호 조회 핫패스 (payment 가 account_no 로 조회·출금)
CREATE INDEX idx_common_account_no ON common_account (account_no);

-- 계좌 종류별 조회 (가상계좌 매칭 등)
CREATE INDEX idx_common_account_type ON common_account (account_type_cd);
