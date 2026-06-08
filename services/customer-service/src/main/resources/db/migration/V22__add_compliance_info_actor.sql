-- =============================================================================
-- V22: compliance_info 행위자 기록 — AML 위험도 평가 / KYC 완료 처리자
-- =============================================================================
-- compliance_info 에는 "언제"(aml_last_assessed_at, kyc_completed_at)는 있으나
-- "누가" 처리했는지가 없었다. 게이트웨이가 JWT 에서 주입하는 X-Employee-Id
-- (검증된 직원 employee_id)를 받아 마지막 처리 직원을 기록한다.
-- 시스템 자동 처리는 NULL. (감사 적재가 FK 로 막히면 안 되므로 FK 는 두지 않는다 —
-- customer_status_history.changed_by_employee_id 선례와 동일)

ALTER TABLE compliance_info
    ADD COLUMN aml_last_assessed_by_employee_id BIGINT;

ALTER TABLE compliance_info
    ADD COLUMN kyc_completed_by_employee_id BIGINT;
