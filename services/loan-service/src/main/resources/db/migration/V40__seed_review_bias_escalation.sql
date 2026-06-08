-- ============================================================
-- V40: 본심사 분기(편향검증·본사 상신) 데모 시드
--
-- 목적: 권한/공정성 화면이 비어있는 공백을 메운다. 본심사 워크플로의
--       분기 상태 5종을 표본으로 만든다:
--         9301 BIAS_REVIEWING + HIGH      (편향 경고, 확인 대기)
--         9302 BIAS_REVIEWING + BLOCKED   (편향 차단, 미우회 — 정정/상급자 우회 필요)
--         9303 COMPLETED + 상급자 우회승인 (BLOCKED 를 지점장이 OVERRIDE_APPROVED)
--         9304 ESCALATED_TO_HQ           (이상거래 본사 상신)
--         9305 PENDING_APPROVER + NONE    (편향 없음, 승인자 결재 대기)
--       편향 상세는 ai_review_advice(BIAS_CHECK)에, 체크 이력은 review_check_log 에 적재.
-- 대역: 9301~ (V37=90xx, V38=91xx, V39=92xx 와 비충돌). customer_id=1007~1011 가상 고객.
-- 멱등: 모든 INSERT 는 ON CONFLICT ... DO NOTHING.
-- FK 순서: application → credit_evaluation/dsr_calculation → loan_review → ai_review_advice/review_check_log.
-- 행위자: reviewer=9002(부지점장), approver/override=9001(지점장). 4-eye(심사≠승인) 준수.
-- ============================================================

-- ------------------------------------------------------------
-- 1. 신청 (loan_application) — 9301~9305
-- ------------------------------------------------------------
INSERT INTO loan_application (
    appl_id, appl_no, customer_id, prod_id, channel_cd,
    requested_amount, requested_period_mo, loan_purpose_cd,
    repayment_method_cd, estimated_income_amt, employment_type_cd,
    appl_status_cd, applied_at, idempotency_key,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9301, 'DEMO-2025-301', 1007, 9001, 'INTERNET',
 22000000, 36, 'LIVING_EXPENSE', 'EQUAL_PRINCIPAL_INTEREST', 50000000, 'EMPLOYED',
 'REVIEWING', '2025-05-01 10:00:00+09', 'DEMO-IDEM-2025-301',
 '2025-05-01 10:00:00+09', 0, '2025-05-02 09:00:00+09', 0, 0),
(9302, 'DEMO-2025-302', 1008, 9001, 'INTERNET',
 18000000, 24, 'LIVING_EXPENSE', 'EQUAL_PRINCIPAL_INTEREST', 38000000, 'EMPLOYED',
 'REVIEWING', '2025-05-03 11:00:00+09', 'DEMO-IDEM-2025-302',
 '2025-05-03 11:00:00+09', 0, '2025-05-04 09:00:00+09', 0, 0),
(9303, 'DEMO-2025-303', 1009, 9001, 'BRANCH',
 20000000, 36, 'LIVING_EXPENSE', 'EQUAL_PRINCIPAL_INTEREST', 46000000, 'EMPLOYED',
 'APPROVED', '2025-05-05 14:00:00+09', 'DEMO-IDEM-2025-303',
 '2025-05-05 14:00:00+09', 0, '2025-05-08 16:00:00+09', 0, 0),
(9304, 'DEMO-2025-304', 1010, 9001, 'MOBILE',
 30000000, 48, 'LIVING_EXPENSE', 'EQUAL_PRINCIPAL_INTEREST', 55000000, 'EMPLOYED',
 'REVIEWING', '2025-05-07 09:30:00+09', 'DEMO-IDEM-2025-304',
 '2025-05-07 09:30:00+09', 0, '2025-05-07 15:00:00+09', 0, 0),
(9305, 'DEMO-2025-305', 1011, 9001, 'INTERNET',
 16000000, 24, 'LIVING_EXPENSE', 'EQUAL_PRINCIPAL_INTEREST', 41000000, 'EMPLOYED',
 'REVIEWING', '2025-05-09 13:00:00+09', 'DEMO-IDEM-2025-305',
 '2025-05-09 13:00:00+09', 0, '2025-05-10 09:00:00+09', 0, 0)
ON CONFLICT (appl_id) DO NOTHING;

-- ------------------------------------------------------------
-- 2. 신용평가 (credit_evaluation) — 9301~9305
-- ------------------------------------------------------------
INSERT INTO credit_evaluation (
    ceval_id, appl_id, customer_id,
    ceval_engine, ceval_engine_version,
    ceval_grade, ceval_score, pd_bps,
    ceval_decision_cd, eval_limit_amount, eval_rate_bps,
    ceval_status_cd, ceval_factors, evaluated_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9301, 9301, 1007, 'KCB', 'v2.1', 'B', 705, 100, 'REVIEW', 22000000, 560,
 'COMPLETED', '{"main_factor":"manual_review","detail":"편향 검증 대상"}', '2025-05-01 10:30:00+09',
 '2025-05-01 10:30:00+09', 0, '2025-05-01 10:30:00+09', 0, 0),
(9302, 9302, 1008, 'KCB', 'v2.1', 'C', 660, 170, 'REVIEW', 18000000, 690,
 'COMPLETED', '{"main_factor":"manual_review","detail":"편향 BLOCKED 대상"}', '2025-05-03 11:30:00+09',
 '2025-05-03 11:30:00+09', 0, '2025-05-03 11:30:00+09', 0, 0),
(9303, 9303, 1009, 'KCB', 'v2.1', 'B', 715, 90, 'REVIEW', 20000000, 530,
 'COMPLETED', '{"main_factor":"manual_review","detail":"BLOCKED 우회 승인 사례"}', '2025-05-05 14:30:00+09',
 '2025-05-05 14:30:00+09', 0, '2025-05-05 14:30:00+09', 0, 0),
(9304, 9304, 1010, 'KCB', 'v2.1', 'B', 700, 110, 'REVIEW', 30000000, 580,
 'COMPLETED', '{"main_factor":"fraud_signal","detail":"이상거래 의심 — 본사 상신"}', '2025-05-07 10:00:00+09',
 '2025-05-07 10:00:00+09', 0, '2025-05-07 10:00:00+09', 0, 0),
(9305, 9305, 1011, 'KCB', 'v2.1', 'A', 770, 55, 'APPROVED', 16000000, 480,
 'COMPLETED', '{"main_factor":"clean","detail":"편향 신호 없음"}', '2025-05-09 13:30:00+09',
 '2025-05-09 13:30:00+09', 0, '2025-05-09 13:30:00+09', 0, 0)
ON CONFLICT (ceval_id) DO NOTHING;

-- ------------------------------------------------------------
-- 3. DSR 계산 (dsr_calculation) — 9301~9305 (모두 PASS)
-- ------------------------------------------------------------
INSERT INTO dsr_calculation (
    dsr_id, appl_id, customer_id,
    annual_income_amt, existing_principal_total, existing_annual_repay_amt,
    new_annual_repay_amt, total_annual_repay_amt,
    dsr_ratio_bps, dsr_limit_bps, dsr_status_cd,
    dsr_reg_type_cd, calculated_at, calc_engine_version,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9301, 9301, 1007, 50000000, 4000000, 2000000, 8800000, 10800000, 2160, 4000, 'PASS', 'STANDARD', '2025-05-01 10:35:00+09', 'v1.3', '2025-05-01 10:35:00+09', 0, '2025-05-01 10:35:00+09', 0, 0),
(9302, 9302, 1008, 38000000, 5000000, 2400000, 8400000, 10800000, 2842, 4000, 'PASS', 'STANDARD', '2025-05-03 11:35:00+09', 'v1.3', '2025-05-03 11:35:00+09', 0, '2025-05-03 11:35:00+09', 0, 0),
(9303, 9303, 1009, 46000000, 3000000, 1500000, 8000000,  9500000, 2065, 4000, 'PASS', 'STANDARD', '2025-05-05 14:35:00+09', 'v1.3', '2025-05-05 14:35:00+09', 0, '2025-05-05 14:35:00+09', 0, 0),
(9304, 9304, 1010, 55000000, 6000000, 3000000, 9600000, 12600000, 2291, 4000, 'PASS', 'STANDARD', '2025-05-07 10:05:00+09', 'v1.3', '2025-05-07 10:05:00+09', 0, '2025-05-07 10:05:00+09', 0, 0),
(9305, 9305, 1011, 41000000, 0, 0, 8160000, 8160000, 1990, 4000, 'PASS', 'STANDARD', '2025-05-09 13:35:00+09', 'v1.3', '2025-05-09 13:35:00+09', 0, '2025-05-09 13:35:00+09', 0, 0)
ON CONFLICT (dsr_id) DO NOTHING;

-- ------------------------------------------------------------
-- 4. 본심사 (loan_review) — 분기 상태 5종
-- ------------------------------------------------------------
INSERT INTO loan_review (
    rev_id, appl_id, rev_type_cd, rev_status_cd, rev_decision_cd,
    approved_amount, approved_rate_bps, approved_period_mo, reject_reason_cd,
    reviewer_id, reviewed_at, approved_at,
    bias_severity_cd, bias_override_by, bias_override_reason, bias_overridden_at,
    approver_id, approved_decision_cd, override_reason_cd, override_remark,
    pending_approver_since, owner_id, escalated_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
-- 9301: 편향 HIGH, 확인 대기 (BIAS_REVIEWING)
(9301, 9301, 'MANUAL', 'BIAS_REVIEWING', NULL,
 NULL, NULL, NULL, NULL,
 9002, '2025-05-02 09:00:00+09', NULL,
 'HIGH', NULL, NULL, NULL,
 NULL, NULL, NULL, NULL,
 NULL, 9002, NULL,
 '2025-05-02 09:00:00+09', 0, '2025-05-02 09:00:00+09', 0, 0),
-- 9302: 편향 BLOCKED, 미우회 (정정 또는 상급자 우회 필요)
(9302, 9302, 'MANUAL', 'BIAS_REVIEWING', NULL,
 NULL, NULL, NULL, NULL,
 9002, '2025-05-04 09:00:00+09', NULL,
 'BLOCKED', NULL, NULL, NULL,
 NULL, NULL, NULL, NULL,
 NULL, 9002, NULL,
 '2025-05-04 09:00:00+09', 0, '2025-05-04 09:00:00+09', 0, 0),
-- 9303: BLOCKED 를 지점장이 우회 승인 → COMPLETED + OVERRIDE_APPROVED
(9303, 9303, 'MANUAL', 'COMPLETED', 'APPROVED',
 20000000, 530, 36, NULL,
 9002, '2025-05-06 10:00:00+09', '2025-05-08 16:00:00+09',
 'BLOCKED', 9001, '신용등급·소득 안정성 감안, 편향 경고는 표본 부족에 기인 — 예외 승인', '2025-05-08 15:30:00+09',
 9001, 'OVERRIDE_APPROVED', 'POLICY_EXCEPTION', '여신심사 기준서 §3.2 예외 요건 충족',
 '2025-05-06 10:00:00+09', 9002, NULL,
 '2025-05-06 10:00:00+09', 0, '2025-05-08 16:00:00+09', 0, 0),
-- 9304: 이상거래 본사 상신 (ESCALATED_TO_HQ)
(9304, 9304, 'MANUAL', 'ESCALATED_TO_HQ', NULL,
 NULL, NULL, NULL, NULL,
 9002, '2025-05-07 14:00:00+09', NULL,
 'MEDIUM', NULL, NULL, NULL,
 NULL, NULL, NULL, NULL,
 NULL, 9002, '2025-05-07 14:30:00+09',
 '2025-05-07 14:00:00+09', 0, '2025-05-07 14:30:00+09', 0, 0),
-- 9305: 편향 없음, 승인자 결재 대기 (PENDING_APPROVER)
(9305, 9305, 'MANUAL', 'PENDING_APPROVER', NULL,
 NULL, NULL, NULL, NULL,
 9002, '2025-05-10 09:00:00+09', NULL,
 'NONE', NULL, NULL, NULL,
 NULL, NULL, NULL, NULL,
 '2025-05-10 09:00:00+09', 9002, NULL,
 '2025-05-10 09:00:00+09', 0, '2025-05-10 09:00:00+09', 0, 0)
ON CONFLICT (rev_id) DO NOTHING;

-- ------------------------------------------------------------
-- 5. AI 편향 검증 조언 (ai_review_advice) — BIAS_CHECK
--    편향 분기 건(9301 HIGH / 9302 BLOCKED / 9303 BLOCKED)에 상세 사유 기록.
-- ------------------------------------------------------------
INSERT INTO ai_review_advice (
    advice_id, rev_id, advice_type_cd, severity_cd, advice_body,
    model, model_version, input_token, output_token, latency_ms,
    created_at, created_by
) OVERRIDING SYSTEM VALUE VALUES
(9301, 9301, 'BIAS_CHECK', 'HIGH',
 '동일 코호트(연령·소득 구간) 대비 거절 성향이 +1.8σ 로 관측됨. 결정 전 근거 보강 권고.',
 'claude', 'demo', 540, 120, 850, '2025-05-02 09:05:00+09', 0),
(9302, 9302, 'BIAS_CHECK', 'BLOCKED',
 '보호속성 추정 변수와 거절 간 통계적 연관(>임계) 탐지. 명백한 규정위반 가능 — 차단. 정정 또는 상급자 우회 필요.',
 'claude', 'demo', 610, 145, 910, '2025-05-04 09:05:00+09', 0),
(9303, 9303, 'BIAS_CHECK', 'BLOCKED',
 '9302 와 동일 패턴 차단. 단 표본 30 미만으로 신뢰구간 넓음 — 심사관 판단 여지 있음.',
 'claude', 'demo', 600, 138, 880, '2025-05-06 10:05:00+09', 0)
ON CONFLICT (advice_id) DO NOTHING;

-- ------------------------------------------------------------
-- 6. 심사 체크 이력 (review_check_log) — 완결 건(9303)의 자동 체크로그 5종
-- ------------------------------------------------------------
INSERT INTO review_check_log (
    rchk_id, rev_id, check_item_cd, check_result_cd, check_remark, checker_id, checked_at,
    created_at, created_by
) OVERRIDING SYSTEM VALUE VALUES
(9301, 9303, 'PRESCREEN_PASS', 'PASS', NULL, 9002, '2025-05-06 10:00:00+09', '2025-05-06 10:00:00+09', 0),
(9302, 9303, 'CB_DECISION',    'REVIEW', 'CB=REVIEW 수동 심사', 9002, '2025-05-06 10:00:00+09', '2025-05-06 10:00:00+09', 0),
(9303, 9303, 'DSR_CHECK',      'PASS', 'dsr=2065bps', 9002, '2025-05-06 10:00:00+09', '2025-05-06 10:00:00+09', 0),
(9304, 9303, 'LTV_CHECK',      'N_A', '신용대출(담보 없음)', 9002, '2025-05-06 10:00:00+09', '2025-05-06 10:00:00+09', 0),
(9305, 9303, 'FINAL_DECISION', 'PASS', 'OVERRIDE_APPROVED (편향 BLOCKED 우회)', 9001, '2025-05-08 16:00:00+09', '2025-05-08 16:00:00+09', 0)
ON CONFLICT (rchk_id) DO NOTHING;

-- ============================================================
-- 끝.
-- ============================================================
