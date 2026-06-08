-- ============================================================
-- V39: 상환 라이프사이클 데모 시드
--
-- 목적: 계약 이후 운영(서비싱) 화면이 비어있는 공백을 메운다.
--       "완전히 서비싱 중인 계약 1건"을 풀체인으로 구성:
--         신청(CONTRACTED) → 심사 → 계약(ACTIVE) → 대출실행(DONE)
--         → 상환계좌(VERIFIED, 자동이체) → 상환스케줄 12회차 → 납부거래 3건
--       스케줄은 납부완료(PAID)·연체(OVERDUE)·예정(DUE)을 섞어 상환 화면을 채운다.
-- 대역: 9201~ (V37=90xx, V38=91xx 와 비충돌). customer_id=1006 가상 고객.
-- 멱등: 모든 INSERT 는 ON CONFLICT ... DO NOTHING.
-- FK 순서: contract → execution / account / schedule → transaction.
-- ============================================================

-- ------------------------------------------------------------
-- 1. 신청 / 심사 / 계약 (CONTRACTED 라인)
-- ------------------------------------------------------------
INSERT INTO loan_application (
    appl_id, appl_no, customer_id, prod_id, channel_cd,
    requested_amount, requested_period_mo, loan_purpose_cd,
    repayment_method_cd, estimated_income_amt, employment_type_cd,
    appl_status_cd, applied_at, idempotency_key,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9201, 'DEMO-2025-201', 1006, 9001, 'MOBILE',
 12000000, 12, 'LIVING_EXPENSE',
 'EQUAL_PRINCIPAL', 45000000, 'EMPLOYED',
 'CONTRACTED', '2025-03-02 09:00:00+09', 'DEMO-IDEM-2025-201',
 '2025-03-02 09:00:00+09', 0, '2025-03-10 16:00:00+09', 0, 0)
ON CONFLICT (appl_id) DO NOTHING;

INSERT INTO credit_evaluation (
    ceval_id, appl_id, customer_id,
    ceval_engine, ceval_engine_version,
    ceval_grade, ceval_score, pd_bps,
    ceval_decision_cd, eval_limit_amount, eval_rate_bps,
    ceval_status_cd, ceval_factors, evaluated_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9201, 9201, 1006,
 'KCB', 'v2.1',
 'A', 790, 45,
 'APPROVED', 15000000, 470,
 'COMPLETED', '{"main_factor":"credit_history","detail":"우량 신용 이력"}', '2025-03-03 09:00:00+09',
 '2025-03-03 09:00:00+09', 0, '2025-03-03 09:00:00+09', 0, 0)
ON CONFLICT (ceval_id) DO NOTHING;

INSERT INTO dsr_calculation (
    dsr_id, appl_id, customer_id,
    annual_income_amt, existing_principal_total, existing_annual_repay_amt,
    new_annual_repay_amt, total_annual_repay_amt,
    dsr_ratio_bps, dsr_limit_bps, dsr_status_cd,
    dsr_reg_type_cd, calculated_at, calc_engine_version,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9201, 9201, 1006,
 45000000, 0, 0,
 12480000, 12480000,
 2773, 4000, 'PASS',
 'STANDARD', '2025-03-03 09:05:00+09', 'v1.3',
 '2025-03-03 09:05:00+09', 0, '2025-03-03 09:05:00+09', 0, 0)
ON CONFLICT (dsr_id) DO NOTHING;

INSERT INTO loan_review (
    rev_id, appl_id, rev_type_cd, rev_status_cd, rev_decision_cd,
    approved_amount, approved_rate_bps, approved_period_mo,
    reject_reason_cd, reviewer_id, reviewed_at, approved_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9201, 9201, 'MANUAL', 'COMPLETED', 'APPROVED',
 12000000, 470, 12,
 NULL, 9002, '2025-03-04 11:00:00+09', '2025-03-05 09:00:00+09',
 '2025-03-04 11:00:00+09', 0, '2025-03-05 09:00:00+09', 0, 0)
ON CONFLICT (rev_id) DO NOTHING;

INSERT INTO loan_contract (
    cntr_id, cntr_no, appl_id, rev_id,
    customer_id, prod_id,
    contracted_amount, currency_cd, contracted_period_mo,
    total_rate_bps, base_rate_bps, spread_bps, preferential_rate_bps,
    rate_type_cd, repayment_method_cd,
    cntr_status_cd, cntr_start_date, cntr_end_date,
    signed_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9201, 'DEMO-CNTR-2025-201', 9201, 9201,
 1006, 9001,
 12000000, 'KRW', 12,
 470, 450, 20, 0,
 'FIXED', 'EQUAL_PRINCIPAL',
 'ACTIVE', '20250310', '20260310',
 '2025-03-10 10:00:00+09',
 '2025-03-10 10:00:00+09', 0, '2025-03-10 10:00:00+09', 0, 0)
ON CONFLICT (cntr_id) DO NOTHING;

-- ------------------------------------------------------------
-- 2. 대출 실행 (loan_execution) — 인출 완료(DONE)
-- ------------------------------------------------------------
INSERT INTO loan_execution (
    exec_id, cntr_id, executed_amount, currency_cd, exec_status_cd,
    executed_at, value_date, fee_amount, idempotency_key,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9201, 9201, 12000000, 'KRW', 'DONE',
 '2025-03-10 10:30:00+09', '20250310', 0, 'EXEC-IDEM-2025-201',
 '2025-03-10 10:30:00+09', 0, '2025-03-10 10:30:00+09', 0, 0)
ON CONFLICT (exec_id) DO NOTHING;

-- ------------------------------------------------------------
-- 3. 상환 계좌 (repayment_account) — 검증완료, 자동이체 매월 10일
-- ------------------------------------------------------------
INSERT INTO repayment_account (
    racct_id, cntr_id, account_no_masked, bank_cd, holder_name_masked,
    racct_status_cd, auto_debit_yn, debit_day, verified_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9201, 9201, '123-**-****56', '004', '홍**',
 'VERIFIED', 'Y', 10, '2025-03-10 10:40:00+09',
 '2025-03-10 10:40:00+09', 0, '2025-03-10 10:40:00+09', 0, 0)
ON CONFLICT (racct_id) DO NOTHING;

-- ------------------------------------------------------------
-- 4. 상환 스케줄 (repayment_schedule) — 12회차 원금균등(원금 100만/월)
--    1~3회: PAID, 4회: OVERDUE(미납), 5~12회: DUE(예정)
-- ------------------------------------------------------------
INSERT INTO repayment_schedule (
    rsch_id, cntr_id, installment_no, due_date,
    scheduled_principal, scheduled_interest, scheduled_total, remaining_balance,
    applied_rate_bps, rsch_status_cd, rsch_version_cd, holiday_adjusted_yn,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9201, 9201,  1, '20250410', 1000000, 47000, 1047000, 11000000, 470, 'PAID',    'V1', 'N', '2025-03-10 10:45:00+09', 0, '2025-04-10 10:00:00+09', 0, 0),
(9202, 9201,  2, '20250510', 1000000, 43000, 1043000, 10000000, 470, 'PAID',    'V1', 'N', '2025-03-10 10:45:00+09', 0, '2025-05-10 10:00:00+09', 0, 0),
(9203, 9201,  3, '20250610', 1000000, 39000, 1039000,  9000000, 470, 'PAID',    'V1', 'N', '2025-03-10 10:45:00+09', 0, '2025-06-10 10:00:00+09', 0, 0),
(9204, 9201,  4, '20250710', 1000000, 35000, 1035000,  8000000, 470, 'OVERDUE', 'V1', 'N', '2025-03-10 10:45:00+09', 0, '2025-07-11 00:00:00+09', 0, 0),
(9205, 9201,  5, '20250810', 1000000, 31000, 1031000,  7000000, 470, 'DUE',     'V1', 'N', '2025-03-10 10:45:00+09', 0, '2025-03-10 10:45:00+09', 0, 0),
(9206, 9201,  6, '20250910', 1000000, 27000, 1027000,  6000000, 470, 'DUE',     'V1', 'N', '2025-03-10 10:45:00+09', 0, '2025-03-10 10:45:00+09', 0, 0),
(9207, 9201,  7, '20251010', 1000000, 24000, 1024000,  5000000, 470, 'DUE',     'V1', 'N', '2025-03-10 10:45:00+09', 0, '2025-03-10 10:45:00+09', 0, 0),
(9208, 9201,  8, '20251110', 1000000, 20000, 1020000,  4000000, 470, 'DUE',     'V1', 'N', '2025-03-10 10:45:00+09', 0, '2025-03-10 10:45:00+09', 0, 0),
(9209, 9201,  9, '20251210', 1000000, 16000, 1016000,  3000000, 470, 'DUE',     'V1', 'N', '2025-03-10 10:45:00+09', 0, '2025-03-10 10:45:00+09', 0, 0),
(9210, 9201, 10, '20260110', 1000000, 12000, 1012000,  2000000, 470, 'DUE',     'V1', 'N', '2025-03-10 10:45:00+09', 0, '2025-03-10 10:45:00+09', 0, 0),
(9211, 9201, 11, '20260210', 1000000,  8000, 1008000,  1000000, 470, 'DUE',     'V1', 'N', '2025-03-10 10:45:00+09', 0, '2025-03-10 10:45:00+09', 0, 0),
(9212, 9201, 12, '20260310', 1000000,  4000, 1004000,        0, 470, 'DUE',     'V1', 'N', '2025-03-10 10:45:00+09', 0, '2025-03-10 10:45:00+09', 0, 0)
ON CONFLICT (rsch_id) DO NOTHING;

-- ------------------------------------------------------------
-- 5. 상환 거래 (repayment_transaction) — 1~3회차 정상 납부(SUCCESS, 자동이체)
-- ------------------------------------------------------------
INSERT INTO repayment_transaction (
    rtx_id, cntr_id, rsch_id, rtx_type_cd,
    total_amount, principal_amount, interest_amount, overdue_interest_amount, fee_amount,
    currency_cd, channel_cd, rtx_status_cd, paid_at, value_date, balance_after,
    idempotency_key, reversal_yn,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9201, 9201, 9201, 'SCHEDULED',
 1047000, 1000000, 47000, 0, 0,
 'KRW', 'AUTO_DEBIT', 'SUCCESS', '2025-04-10 06:00:00+09', '20250410', 11000000,
 'RTX-IDEM-2025-201', 'N',
 '2025-04-10 06:00:00+09', 0, '2025-04-10 06:00:00+09', 0, 0),
(9202, 9201, 9202, 'SCHEDULED',
 1043000, 1000000, 43000, 0, 0,
 'KRW', 'AUTO_DEBIT', 'SUCCESS', '2025-05-10 06:00:00+09', '20250510', 10000000,
 'RTX-IDEM-2025-202', 'N',
 '2025-05-10 06:00:00+09', 0, '2025-05-10 06:00:00+09', 0, 0),
(9203, 9201, 9203, 'SCHEDULED',
 1039000, 1000000, 39000, 0, 0,
 'KRW', 'AUTO_DEBIT', 'SUCCESS', '2025-06-10 06:00:00+09', '20250610', 9000000,
 'RTX-IDEM-2025-203', 'N',
 '2025-06-10 06:00:00+09', 0, '2025-06-10 06:00:00+09', 0, 0)
ON CONFLICT (rtx_id) DO NOTHING;

-- ============================================================
-- 끝.
-- ============================================================
