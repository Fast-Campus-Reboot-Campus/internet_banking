-- ============================================================
-- V41: 담보·LTV·보증인·보증보험 데모 시드 (origination 보강)
--
-- 목적: 담보/보증 분기 화면 공백을 메운다.
--         9401 주택담보대출: 담보(APPROVED) + LTV 산정(PASS) + 계약 + 보증보험(ISSUED)
--         9402 신용대출    : 보증인(SIGNED, 연대보증)
-- 대역: 9401~ (V37~V40 의 90xx~93xx 와 비충돌). customer_id=1012~1013 가상 고객.
-- 멱등: 모든 INSERT 는 ON CONFLICT ... DO NOTHING.
-- FK 순서: application → ceval/dsr → review → contract → collateral/ltv/guarantee_insurance;
--          guarantor_master → guarantor_agreement.
-- ============================================================

-- ------------------------------------------------------------
-- 1. 신청 (loan_application) — 9401 주택담보, 9402 보증부 신용
-- ------------------------------------------------------------
INSERT INTO loan_application (
    appl_id, appl_no, customer_id, prod_id, channel_cd,
    requested_amount, requested_period_mo, loan_purpose_cd,
    repayment_method_cd, estimated_income_amt, employment_type_cd,
    appl_status_cd, applied_at, idempotency_key,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9401, 'DEMO-2025-401', 1012, 9002, 'BRANCH',
 150000000, 240, 'HOUSE_PURCHASE', 'EQUAL_PRINCIPAL_INTEREST', 90000000, 'EMPLOYED',
 'CONTRACTED', '2025-04-15 10:00:00+09', 'DEMO-IDEM-2025-401',
 '2025-04-15 10:00:00+09', 0, '2025-04-22 16:00:00+09', 0, 0),
(9402, 'DEMO-2025-402', 1013, 9001, 'INTERNET',
 20000000, 36, 'LIVING_EXPENSE', 'EQUAL_PRINCIPAL_INTEREST', 35000000, 'EMPLOYED',
 'REVIEWING', '2025-04-18 11:00:00+09', 'DEMO-IDEM-2025-402',
 '2025-04-18 11:00:00+09', 0, '2025-04-19 09:00:00+09', 0, 0)
ON CONFLICT (appl_id) DO NOTHING;

-- ------------------------------------------------------------
-- 2. 신용평가 / DSR (화면 결합용 최소 1건씩)
-- ------------------------------------------------------------
INSERT INTO credit_evaluation (
    ceval_id, appl_id, customer_id, ceval_engine, ceval_engine_version,
    ceval_grade, ceval_score, pd_bps, ceval_decision_cd, eval_limit_amount, eval_rate_bps,
    ceval_status_cd, ceval_factors, evaluated_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9401, 9401, 1012, 'KCB', 'v2.1', 'A', 800, 40, 'APPROVED', 160000000, 380,
 'COMPLETED', '{"main_factor":"collateral_backed","detail":"담보부 우량"}', '2025-04-16 09:00:00+09',
 '2025-04-16 09:00:00+09', 0, '2025-04-16 09:00:00+09', 0, 0),
(9402, 9402, 1013, 'KCB', 'v2.1', 'C', 640, 200, 'REVIEW', 20000000, 720,
 'COMPLETED', '{"main_factor":"guarantor_required","detail":"보증인 보강 필요"}', '2025-04-18 11:30:00+09',
 '2025-04-18 11:30:00+09', 0, '2025-04-18 11:30:00+09', 0, 0)
ON CONFLICT (ceval_id) DO NOTHING;

INSERT INTO dsr_calculation (
    dsr_id, appl_id, customer_id, annual_income_amt, existing_principal_total, existing_annual_repay_amt,
    new_annual_repay_amt, total_annual_repay_amt, dsr_ratio_bps, dsr_limit_bps, dsr_status_cd,
    dsr_reg_type_cd, calculated_at, calc_engine_version,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9401, 9401, 1012, 90000000, 0, 0, 9000000, 9000000, 1000, 4000, 'PASS', 'STANDARD', '2025-04-16 09:05:00+09', 'v1.3', '2025-04-16 09:05:00+09', 0, '2025-04-16 09:05:00+09', 0, 0),
(9402, 9402, 1013, 35000000, 3000000, 1500000, 7200000, 8700000, 2485, 4000, 'PASS', 'STANDARD', '2025-04-18 11:35:00+09', 'v1.3', '2025-04-18 11:35:00+09', 0, '2025-04-18 11:35:00+09', 0, 0)
ON CONFLICT (dsr_id) DO NOTHING;

-- ------------------------------------------------------------
-- 3. 본심사 + 계약 (9401 주택담보, 보증보험 부착 대상)
-- ------------------------------------------------------------
INSERT INTO loan_review (
    rev_id, appl_id, rev_type_cd, rev_status_cd, rev_decision_cd,
    approved_amount, approved_rate_bps, approved_period_mo, reject_reason_cd,
    reviewer_id, reviewed_at, approved_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9401, 9401, 'MANUAL', 'COMPLETED', 'APPROVED',
 150000000, 380, 240, NULL,
 9002, '2025-04-20 14:00:00+09', '2025-04-21 10:00:00+09',
 '2025-04-20 14:00:00+09', 0, '2025-04-21 10:00:00+09', 0, 0)
ON CONFLICT (rev_id) DO NOTHING;

INSERT INTO loan_contract (
    cntr_id, cntr_no, appl_id, rev_id, customer_id, prod_id,
    contracted_amount, currency_cd, contracted_period_mo,
    total_rate_bps, base_rate_bps, spread_bps, preferential_rate_bps,
    rate_type_cd, repayment_method_cd, cntr_status_cd, cntr_start_date, cntr_end_date, signed_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9401, 'DEMO-CNTR-2025-401', 9401, 9401, 1012, 9002,
 150000000, 'KRW', 240,
 380, 350, 30, 0,
 'VARIABLE', 'EQUAL_PRINCIPAL_INTEREST', 'ACTIVE', '20250422', '20450422', '2025-04-22 10:00:00+09',
 '2025-04-22 10:00:00+09', 0, '2025-04-22 10:00:00+09', 0, 0)
ON CONFLICT (cntr_id) DO NOTHING;

-- ------------------------------------------------------------
-- 4. 담보 (collateral) — 아파트, 선순위 있음, APPROVED
-- ------------------------------------------------------------
INSERT INTO collateral (
    col_id, appl_id, col_type_cd, col_status_cd, col_no,
    col_name, col_address, col_registry_no, declared_value, currency_cd,
    ownership_type_cd, senior_lien_yn, senior_lien_amount,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9401, 9401, 'APARTMENT', 'APPROVED', 'DEMO-COL-9401',
 '데모아파트 101동 1001호', '서울 강남구 데모로 1', '1146-1996-012345', 300000000, 'KRW',
 'SOLE', 'Y', 50000000,
 '2025-04-16 10:00:00+09', 0, '2025-04-19 10:00:00+09', 0, 0)
ON CONFLICT (col_id) DO NOTHING;

-- ------------------------------------------------------------
-- 5. LTV 산정 (ltv_calculation) — 한도 70%, PASS
--    max_loan = 감정가 3억 × 70% - 선순위 5천만 = 1.6억 ≥ 요청 1.5억 → PASS
--    ltv_ratio = 요청 1.5억 / 감정가 3억 = 5000bps
-- ------------------------------------------------------------
INSERT INTO ltv_calculation (
    ltv_id, appl_id, col_id, applied_col_value, senior_lien_amount, requested_amount,
    ltv_ratio_bps, ltv_limit_bps, max_loan_amount, ltv_status_cd, calculated_at, calc_engine_version,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9401, 9401, 9401, 300000000, 50000000, 150000000,
 5000, 7000, 160000000, 'PASS', '2025-04-17 09:00:00+09', 'v1.1',
 '2025-04-17 09:00:00+09', 0, '2025-04-17 09:00:00+09', 0, 0)
ON CONFLICT (ltv_id) DO NOTHING;

-- ------------------------------------------------------------
-- 6. 보증인 마스터 (guarantor_master) — PII enc 는 placeholder(NOT NULL 충족용)
-- ------------------------------------------------------------
INSERT INTO guarantor_master (
    gmst_id, guarantor_name_enc, guarantor_name_masked, guarantor_ci_hash,
    relation_type_cd, mobile_no_masked,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9401, '\x00'::bytea, '김**', 'demo-ci-hash-9401',
 'FAMILY', '010-****-1234',
 '2025-04-18 12:00:00+09', 0, '2025-04-18 12:00:00+09', 0, 0)
ON CONFLICT (gmst_id) DO NOTHING;

-- ------------------------------------------------------------
-- 7. 보증인 약정 (guarantor_agreement) — 연대보증(JOINT), SIGNED
-- ------------------------------------------------------------
INSERT INTO guarantor_agreement (
    gagr_id, appl_id, gmst_id, gagr_type_cd, guarantee_amount, guarantee_ratio_bps,
    gagr_status_cd, consented_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9401, 9402, 9401, 'JOINT', 20000000, 10000,
 'SIGNED', '2025-04-19 09:30:00+09',
 '2025-04-18 12:10:00+09', 0, '2025-04-19 09:30:00+09', 0, 0)
ON CONFLICT (gagr_id) DO NOTHING;

-- ------------------------------------------------------------
-- 8. 보증보험 (guarantee_insurance) — 계약 9401 에 발급(ISSUED)
-- ------------------------------------------------------------
INSERT INTO guarantee_insurance (
    gins_id, cntr_id, gins_agency_cd, gins_policy_no, guarantee_amount, guarantee_ratio_bps,
    premium_amount, gins_status_cd, gins_start_date, gins_end_date, issued_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9401, 9401, 'SGI', 'DEMO-GINS-9401', 150000000, 10000,
 450000, 'ISSUED', '20250422', '20450422', '2025-04-22 10:30:00+09',
 '2025-04-22 10:30:00+09', 0, '2025-04-22 10:30:00+09', 0, 0)
ON CONFLICT (gins_id) DO NOTHING;

-- ============================================================
-- 끝.
-- ============================================================
