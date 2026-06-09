-- ============================================================
-- V42: 연체·종결·만기 데모 시드 (서비싱 심화)
--
-- 목적: 대출 운영 후반 라이프사이클 화면 공백을 메운다.
--   연체: V39 의 계약 9201(4회차 OVERDUE)에 연체(ACTIVE/STAGE_1) + 연체이자 적립 부착
--   종결: 신규 계약 3건을 종결 — 9501 정상(NORMAL) / 9502 대손(WRITE_OFF) / 9503 대위변제(SUBROGATION)
--   만기: 9201 진행중(ACTIVE) / 9504 만기도래(MATURED) / 9505 기한연장(extension_count=1)
-- 대역: 9501~ 신규(계약/신청/심사), 도메인 PK 는 9201·9501~ 사용. customer_id=1014~1018.
-- 멱등: 모든 INSERT 는 ON CONFLICT ... DO NOTHING.
-- 계약 상태: CLOSED(종결) / ACTIVE(만기도래·연장은 계약은 ACTIVE, 만기상태는 maturity 테이블).
-- ============================================================

-- ------------------------------------------------------------
-- 1. 종결·만기용 신규 신청/심사/계약 (9501~9505)
-- ------------------------------------------------------------
INSERT INTO loan_application (
    appl_id, appl_no, customer_id, prod_id, channel_cd,
    requested_amount, requested_period_mo, loan_purpose_cd,
    repayment_method_cd, estimated_income_amt, employment_type_cd,
    appl_status_cd, applied_at, idempotency_key,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9501, 'DEMO-2025-501', 1014, 9001, 'MOBILE',   5000000, 12, 'LIVING_EXPENSE', 'EQUAL_PRINCIPAL', 40000000, 'EMPLOYED',      'CONTRACTED', '2025-03-01 09:00:00+09', 'DEMO-IDEM-2025-501', '2025-03-01 09:00:00+09', 0, '2025-03-10 10:00:00+09', 0, 0),
(9502, 'DEMO-2024-502', 1015, 9001, 'INTERNET', 8000000, 24, 'LIVING_EXPENSE', 'EQUAL_PRINCIPAL', 36000000, 'SELF_EMPLOYED', 'CONTRACTED', '2024-03-01 09:00:00+09', 'DEMO-IDEM-2024-502', '2024-03-01 09:00:00+09', 0, '2024-03-10 10:00:00+09', 0, 0),
(9503, 'DEMO-2024-503', 1016, 9001, 'BRANCH',  10000000, 24, 'LIVING_EXPENSE', 'EQUAL_PRINCIPAL', 38000000, 'EMPLOYED',      'CONTRACTED', '2024-06-01 09:00:00+09', 'DEMO-IDEM-2024-503', '2024-06-01 09:00:00+09', 0, '2024-06-10 10:00:00+09', 0, 0),
(9504, 'DEMO-2025-504', 1017, 9001, 'MOBILE',   6000000,  6, 'LIVING_EXPENSE', 'EQUAL_PRINCIPAL', 42000000, 'EMPLOYED',      'CONTRACTED', '2025-03-01 09:00:00+09', 'DEMO-IDEM-2025-504', '2025-03-01 09:00:00+09', 0, '2025-03-10 10:00:00+09', 0, 0),
(9505, 'DEMO-2025-505', 1018, 9001, 'INTERNET', 7000000, 12, 'LIVING_EXPENSE', 'EQUAL_PRINCIPAL', 44000000, 'EMPLOYED',      'CONTRACTED', '2025-03-01 09:00:00+09', 'DEMO-IDEM-2025-505', '2025-03-01 09:00:00+09', 0, '2025-03-10 10:00:00+09', 0, 0)
ON CONFLICT (appl_id) DO NOTHING;

INSERT INTO loan_review (
    rev_id, appl_id, rev_type_cd, rev_status_cd, rev_decision_cd,
    approved_amount, approved_rate_bps, approved_period_mo, reject_reason_cd,
    reviewer_id, reviewed_at, approved_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9501, 9501, 'AUTO', 'COMPLETED', 'APPROVED',  5000000, 470, 12, NULL, 9002, '2025-03-05 10:00:00+09', '2025-03-05 10:00:00+09', '2025-03-05 10:00:00+09', 0, '2025-03-05 10:00:00+09', 0, 0),
(9502, 9502, 'AUTO', 'COMPLETED', 'APPROVED',  8000000, 620, 24, NULL, 9002, '2024-03-05 10:00:00+09', '2024-03-05 10:00:00+09', '2024-03-05 10:00:00+09', 0, '2024-03-05 10:00:00+09', 0, 0),
(9503, 9503, 'MANUAL','COMPLETED', 'APPROVED', 10000000, 580, 24, NULL, 9002, '2024-06-05 10:00:00+09', '2024-06-05 10:00:00+09', '2024-06-05 10:00:00+09', 0, '2024-06-05 10:00:00+09', 0, 0),
(9504, 9504, 'AUTO', 'COMPLETED', 'APPROVED',  6000000, 460,  6, NULL, 9002, '2025-03-05 10:00:00+09', '2025-03-05 10:00:00+09', '2025-03-05 10:00:00+09', 0, '2025-03-05 10:00:00+09', 0, 0),
(9505, 9505, 'AUTO', 'COMPLETED', 'APPROVED',  7000000, 490, 12, NULL, 9002, '2025-03-05 10:00:00+09', '2025-03-05 10:00:00+09', '2025-03-05 10:00:00+09', 0, '2025-03-05 10:00:00+09', 0, 0)
ON CONFLICT (rev_id) DO NOTHING;

INSERT INTO loan_contract (
    cntr_id, cntr_no, appl_id, rev_id, customer_id, prod_id,
    contracted_amount, currency_cd, contracted_period_mo,
    total_rate_bps, base_rate_bps, spread_bps, preferential_rate_bps,
    rate_type_cd, repayment_method_cd, cntr_status_cd, cntr_start_date, cntr_end_date, signed_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9501, 'DEMO-CNTR-2025-501', 9501, 9501, 1014, 9001,  5000000, 'KRW', 12, 470, 450, 20, 0, 'FIXED', 'EQUAL_PRINCIPAL', 'CLOSED', '20250310', '20260310', '2025-03-10 10:00:00+09', '2025-03-10 10:00:00+09', 0, '2026-03-10 10:00:00+09', 0, 0),
(9502, 'DEMO-CNTR-2024-502', 9502, 9502, 1015, 9001,  8000000, 'KRW', 24, 620, 450, 170, 0, 'FIXED', 'EQUAL_PRINCIPAL', 'CLOSED', '20240310', '20260310', '2024-03-10 10:00:00+09', '2024-03-10 10:00:00+09', 0, '2025-11-15 10:00:00+09', 0, 0),
(9503, 'DEMO-CNTR-2024-503', 9503, 9503, 1016, 9001, 10000000, 'KRW', 24, 580, 450, 130, 0, 'FIXED', 'EQUAL_PRINCIPAL', 'CLOSED', '20240610', '20260610', '2024-06-10 10:00:00+09', '2024-06-10 10:00:00+09', 0, '2025-12-20 10:00:00+09', 0, 0),
(9504, 'DEMO-CNTR-2025-504', 9504, 9504, 1017, 9001,  6000000, 'KRW',  6, 460, 450, 10, 0, 'FIXED', 'EQUAL_PRINCIPAL', 'ACTIVE', '20250310', '20250910', '2025-03-10 10:00:00+09', '2025-03-10 10:00:00+09', 0, '2025-09-10 10:00:00+09', 0, 0),
(9505, 'DEMO-CNTR-2025-505', 9505, 9505, 1018, 9001,  7000000, 'KRW', 12, 490, 450, 40, 0, 'FIXED', 'EQUAL_PRINCIPAL', 'ACTIVE', '20250310', '20260310', '2025-03-10 10:00:00+09', '2025-03-10 10:00:00+09', 0, '2026-03-01 10:00:00+09', 0, 0)
ON CONFLICT (cntr_id) DO NOTHING;

-- ------------------------------------------------------------
-- 2. 연체 (delinquency) — 계약 9201 의 4회차 미납, STAGE_1, 20일 경과
-- ------------------------------------------------------------
INSERT INTO delinquency (
    dlq_id, cntr_id, dlq_status_cd, dlq_start_date, dlq_end_date, dlq_days,
    dlq_principal_amt, dlq_interest_amt, dlq_total_amt, overdue_rate_bps, dlq_stage_cd, resolved_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9201, 9201, 'ACTIVE', '20250711', NULL, 20,
 1000000, 35000, 1035000, 800, 'STAGE_1', NULL,
 '2025-07-11 06:00:00+09', 0, '2025-07-30 06:00:00+09', 0, 0)
ON CONFLICT (dlq_id) DO NOTHING;

-- 연체이자 적립 (overdue_accrual) — 적립일 기준 1건 (멱등 UNIQUE: cntr_id+accrual_date)
INSERT INTO overdue_accrual (
    oa_id, cntr_id, dlq_id, accrual_date, overdue_principal, overdue_rate_bps, dlq_days,
    daily_overdue_interest, cumulative_overdue_interest, oa_status_cd, accrued_at,
    created_at, created_by
) OVERRIDING SYSTEM VALUE VALUES
(9201, 9201, 9201, '20250730', 1000000, 800, 20,
 219, 4380, 'ACCRUED', '2025-07-30 06:00:00+09',
 '2025-07-30 06:00:00+09', 0)
ON CONFLICT (oa_id) DO NOTHING;

-- ------------------------------------------------------------
-- 3. 종결 (loan_closure) — 정상 / 대손 / 대위변제
-- ------------------------------------------------------------
INSERT INTO loan_closure (
    clos_id, cntr_id, clos_type_cd, clos_reason_cd, clos_status_cd,
    final_principal_amt, final_interest_amt, final_fee_amt, prepayment_fee_amt, total_settled_amt,
    clos_date, closed_at,
    write_off_amount, subrogation_amount, subrogation_party_ref, write_off_reason_cd,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
-- 정상 만기 종결 (잔액 0)
(9501, 9501, 'NORMAL', 'MATURITY_FULL_REPAY', 'COMPLETED',
 5000000, 150000, 0, 0, 5150000,
 '20260310', '2026-03-10 10:00:00+09',
 NULL, NULL, NULL, NULL,
 '2026-03-10 10:00:00+09', 0, '2026-03-10 10:00:00+09', 0, 0),
-- 대손 상각 (회수불능)
(9502, 9502, 'WRITE_OFF', 'UNCOLLECTIBLE', 'COMPLETED',
 0, 0, 0, 0, 0,
 '20251115', '2025-11-15 10:00:00+09',
 6500000, NULL, NULL, 'UNCOLLECTIBLE',
 '2025-11-15 10:00:00+09', 0, '2025-11-15 10:00:00+09', 0, 0),
-- 대위변제 (보증보험 대납)
(9503, 9503, 'SUBROGATION', 'GUARANTOR_PAID', 'COMPLETED',
 0, 0, 0, 0, 9000000,
 '20251220', '2025-12-20 10:00:00+09',
 NULL, 9000000, 'SGI보증보험(대위변제)', NULL,
 '2025-12-20 10:00:00+09', 0, '2025-12-20 10:00:00+09', 0, 0)
ON CONFLICT (clos_id) DO NOTHING;

-- ------------------------------------------------------------
-- 4. 만기 (maturity) — 진행중 / 만기도래 / 기한연장
-- ------------------------------------------------------------
INSERT INTO maturity (
    mat_id, cntr_id, original_maturity_date, current_maturity_date, mat_status_cd,
    extension_type_cd, extension_count, last_extended_date, extended_period_mo,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
-- 9201: 진행중 (V39 계약), 연장 없음
(9201, 9201, '20260310', '20260310', 'ACTIVE', NULL, 0, NULL, NULL,
 '2025-03-10 10:50:00+09', 0, '2025-03-10 10:50:00+09', 0, 0),
-- 9504: 만기 도래 (6개월 단기, 배치로 MATURED)
(9504, 9504, '20250910', '20250911', 'MATURED', NULL, 0, NULL, NULL,
 '2025-03-10 10:50:00+09', 0, '2025-09-11 04:00:00+09', 0, 0),
-- 9505: 기한 연장 1회 (+6개월)
(9505, 9505, '20260310', '20260910', 'ACTIVE', 'GRACE', 1, '20260301', 6,
 '2025-03-10 10:50:00+09', 0, '2026-03-01 11:00:00+09', 0, 0)
ON CONFLICT (mat_id) DO NOTHING;

-- ============================================================
-- 끝.
-- ============================================================
