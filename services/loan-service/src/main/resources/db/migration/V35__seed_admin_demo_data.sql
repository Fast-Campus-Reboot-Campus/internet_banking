-- ============================================================
-- V35: Admin 화면 데모 시드 데이터
--
-- 목적: 관리자 화면(계약 모니터링, 어드바이저리, RAG 문서 등)에서
--       비어있지 않은 화면을 확인할 수 있는 최소한의 샘플 데이터 삽입.
-- 멱등: 모든 INSERT 는 ON CONFLICT ... DO NOTHING 또는 WHERE NOT EXISTS 사용.
-- 참조: customer_id=1001~1003 은 customer-service 에 등록된 가상 고객.
--       created_by=0 은 시스템(migration) 행위자.
-- ============================================================

-- ------------------------------------------------------------
-- 1. 대출 상품 (loan_product)
-- ------------------------------------------------------------
INSERT INTO loan_product (
    prod_id, prod_cd, prod_name, loan_type_cd, target_customer_cd,
    repayment_method_cd, rate_type_cd,
    base_rate_bps, min_rate_bps, max_rate_bps,
    min_amount, max_amount, min_period_mo, max_period_mo,
    collateral_required_yn, guarantor_required_yn,
    sale_start_date, prod_status_cd,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9001, 'DEMO_PERSONAL', '데모 개인신용대출', 'PERSONAL', 'INDIVIDUAL',
 'EQUAL_PRINCIPAL_INTEREST', 'FIXED',
 450, 350, 1500,
 1000000, 50000000, 6, 60,
 'N', 'N',
 '20250101', 'ACTIVE',
 '2025-01-01 00:00:00+09', 0, '2025-01-01 00:00:00+09', 0, 0),
(9002, 'DEMO_MORTGAGE', '데모 주택담보대출', 'MORTGAGE', 'INDIVIDUAL',
 'EQUAL_PRINCIPAL_INTEREST', 'VARIABLE',
 350, 280, 900,
 10000000, 500000000, 12, 360,
 'Y', 'N',
 '20250101', 'ACTIVE',
 '2025-01-01 00:00:00+09', 0, '2025-01-01 00:00:00+09', 0, 0)
ON CONFLICT (prod_id) DO NOTHING;

-- ------------------------------------------------------------
-- 2. 대출 신청 (loan_application)
--    9001/9002: 계약 완료(CONTRACTED)
--    9003    : 심사 중(REVIEWING) — 어드바이저리 대상
--    9004    : 거절(REJECTED)
-- ------------------------------------------------------------
INSERT INTO loan_application (
    appl_id, appl_no, customer_id, prod_id, channel_cd,
    requested_amount, requested_period_mo, loan_purpose_cd,
    repayment_method_cd, estimated_income_amt, employment_type_cd,
    appl_status_cd, applied_at, idempotency_key,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9001, 'DEMO-2025-001', 1001, 9001, 'INTERNET',
 20000000, 36, 'LIVING_EXPENSE',
 'EQUAL_PRINCIPAL_INTEREST', 48000000, 'EMPLOYED',
 'CONTRACTED', '2025-01-10 10:00:00+09', 'DEMO-IDEM-2025-001',
 '2025-01-10 10:00:00+09', 0, '2025-01-20 14:00:00+09', 0, 0),
(9002, 'DEMO-2025-002', 1001, 9002, 'BRANCH',
 100000000, 120, 'HOUSE_PURCHASE',
 'EQUAL_PRINCIPAL_INTEREST', 80000000, 'EMPLOYED',
 'CONTRACTED', '2025-02-05 09:30:00+09', 'DEMO-IDEM-2025-002',
 '2025-02-05 09:30:00+09', 0, '2025-02-15 15:00:00+09', 0, 0),
(9003, 'DEMO-2025-003', 1002, 9001, 'INTERNET',
 15000000, 24, 'LIVING_EXPENSE',
 'EQUAL_PRINCIPAL_INTEREST', 36000000, 'EMPLOYED',
 'REVIEWING', '2025-03-01 11:00:00+09', 'DEMO-IDEM-2025-003',
 '2025-03-01 11:00:00+09', 0, '2025-03-05 09:00:00+09', 0, 0),
(9004, 'DEMO-2025-004', 1003, 9001, 'MOBILE',
 10000000, 12, 'LIVING_EXPENSE',
 'EQUAL_PRINCIPAL_INTEREST', 30000000, 'SELF_EMPLOYED',
 'REJECTED', '2025-03-10 14:00:00+09', 'DEMO-IDEM-2025-004',
 '2025-03-10 14:00:00+09', 0, '2025-03-12 10:00:00+09', 0, 0)
ON CONFLICT (appl_id) DO NOTHING;

-- ------------------------------------------------------------
-- 3. 신용평가 (credit_evaluation)
-- ------------------------------------------------------------
INSERT INTO credit_evaluation (
    ceval_id, appl_id, customer_id,
    ceval_engine, ceval_engine_version,
    ceval_grade, ceval_score, pd_bps,
    ceval_decision_cd, eval_limit_amount, eval_rate_bps,
    ceval_status_cd, ceval_factors, evaluated_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9001, 9001, 1001,
 'KCB', 'v2.1',
 'B', 720, 85,
 'APPROVED', 25000000, 520,
 'COMPLETED', '{"main_factor":"income_stability","detail":"정규직 3년 이상"}', '2025-01-11 10:00:00+09',
 '2025-01-11 10:00:00+09', 0, '2025-01-11 10:00:00+09', 0, 0),
(9002, 9002, 1001,
 'KCB', 'v2.1',
 'A', 800, 40,
 'APPROVED', 120000000, 410,
 'COMPLETED', '{"main_factor":"credit_history","detail":"장기 우량 고객"}', '2025-02-06 09:00:00+09',
 '2025-02-06 09:00:00+09', 0, '2025-02-06 09:00:00+09', 0, 0),
(9003, 9003, 1002,
 'KCB', 'v2.1',
 'C', 650, 180,
 'APPROVED', 15000000, 680,
 'COMPLETED', '{"main_factor":"dsr_marginal","detail":"DSR 한도 소폭 초과 — 심사관 판단 요"}', '2025-03-02 09:00:00+09',
 '2025-03-02 09:00:00+09', 0, '2025-03-02 09:00:00+09', 0, 0),
(9004, 9004, 1003,
 'KCB', 'v2.1',
 'D', 580, 320,
 'REJECTED', 0, 0,
 'COMPLETED', '{"main_factor":"low_score","detail":"신용점수 580 미만 자동 거절"}', '2025-03-11 09:00:00+09',
 '2025-03-11 09:00:00+09', 0, '2025-03-11 09:00:00+09', 0, 0)
ON CONFLICT (ceval_id) DO NOTHING;

-- ------------------------------------------------------------
-- 4. DSR 계산 (dsr_calculation)
-- ------------------------------------------------------------
INSERT INTO dsr_calculation (
    dsr_id, appl_id, customer_id,
    annual_income_amt, existing_principal_total, existing_annual_repay_amt,
    new_annual_repay_amt, total_annual_repay_amt,
    dsr_ratio_bps, dsr_limit_bps, dsr_status_cd,
    dsr_reg_type_cd, calculated_at, calc_engine_version,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9001, 9001, 1001,
 48000000, 5000000, 2400000,
 7680000, 10080000,
 2100, 4000, 'PASS',
 'STANDARD', '2025-01-11 10:05:00+09', 'v1.3',
 '2025-01-11 10:05:00+09', 0, '2025-01-11 10:05:00+09', 0, 0),
(9002, 9002, 1001,
 80000000, 0, 0,
 14400000, 14400000,
 1800, 4000, 'PASS',
 'STANDARD', '2025-02-06 09:05:00+09', 'v1.3',
 '2025-02-06 09:05:00+09', 0, '2025-02-06 09:05:00+09', 0, 0),
(9003, 9003, 1002,
 36000000, 8000000, 4200000,
 9600000, 13800000,
 3833, 4000, 'FAIL',
 'STANDARD', '2025-03-02 09:05:00+09', 'v1.3',
 '2025-03-02 09:05:00+09', 0, '2025-03-02 09:05:00+09', 0, 0),
(9004, 9004, 1003,
 30000000, 12000000, 6000000,
 4800000, 10800000,
 3600, 4000, 'FAIL',
 'STANDARD', '2025-03-11 09:05:00+09', 'v1.3',
 '2025-03-11 09:05:00+09', 0, '2025-03-11 09:05:00+09', 0, 0)
ON CONFLICT (dsr_id) DO NOTHING;

-- ------------------------------------------------------------
-- 5. 본심사 (loan_review)
--    9001/9002: COMPLETED + APPROVED
--    9003    : PENDING_APPROVER (어드바이저리 발행됨)
--    9004    : COMPLETED + REJECTED
-- ------------------------------------------------------------
INSERT INTO loan_review (
    rev_id, appl_id, rev_type_cd, rev_status_cd, rev_decision_cd,
    approved_amount, approved_rate_bps, approved_period_mo,
    reject_reason_cd, reviewer_id, reviewed_at, approved_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9001, 9001, 'MANUAL', 'COMPLETED', 'APPROVED',
 20000000, 520, 36,
 NULL, 9002, '2025-01-12 14:00:00+09', '2025-01-13 10:00:00+09',
 '2025-01-12 14:00:00+09', 0, '2025-01-13 10:00:00+09', 0, 0),
(9002, 9002, 'MANUAL', 'COMPLETED', 'APPROVED',
 100000000, 410, 120,
 NULL, 9002, '2025-02-07 11:00:00+09', '2025-02-08 09:00:00+09',
 '2025-02-07 11:00:00+09', 0, '2025-02-08 09:00:00+09', 0, 0),
(9003, 9003, 'MANUAL', 'PENDING_APPROVER', NULL,
 NULL, NULL, NULL,
 NULL, 9002, '2025-03-03 15:00:00+09', NULL,
 '2025-03-03 15:00:00+09', 0, '2025-03-05 09:00:00+09', 0, 0),
(9004, 9004, 'AUTO', 'COMPLETED', 'REJECTED',
 NULL, NULL, NULL,
 'LOW_CREDIT_SCORE', 9002, '2025-03-11 09:10:00+09', NULL,
 '2025-03-11 09:10:00+09', 0, '2025-03-11 09:10:00+09', 0, 0)
ON CONFLICT (rev_id) DO NOTHING;

-- ------------------------------------------------------------
-- 6. 대출 계약 (loan_contract)
-- ------------------------------------------------------------
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
(9001, 'DEMO-CNTR-2025-001', 9001, 9001,
 1001, 9001,
 20000000, 'KRW', 36,
 520, 450, 70, 0,
 'FIXED', 'EQUAL_PRINCIPAL_INTEREST',
 'ACTIVE', '20250115', '20280115',
 '2025-01-15 10:00:00+09',
 '2025-01-15 10:00:00+09', 0, '2025-01-15 10:00:00+09', 0, 0),
(9002, 'DEMO-CNTR-2025-002', 9002, 9002,
 1001, 9002,
 100000000, 'KRW', 120,
 410, 350, 60, 0,
 'VARIABLE', 'EQUAL_PRINCIPAL_INTEREST',
 'ACTIVE', '20250210', '20350210',
 '2025-02-10 09:00:00+09',
 '2025-02-10 09:00:00+09', 0, '2025-02-10 09:00:00+09', 0, 0)
ON CONFLICT (cntr_id) DO NOTHING;

-- ------------------------------------------------------------
-- 7. 신용정보 신고 (credit_info_report)
-- ------------------------------------------------------------
INSERT INTO credit_info_report (
    crpt_id, cntr_id, customer_id,
    crpt_type_cd, crpt_agency_cd, crpt_status_cd,
    report_target_cd, report_reason_cd, report_payload,
    external_tx_no, reported_at, ack_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9001, 9001, 1001,
 'NEW_LOAN', 'KCB', 'ACKED',
 'NEW', 'NEW_LOAN_CONTRACTED',
 '{"contractedAmount":20000000,"period":36,"productCd":"DEMO_PERSONAL"}',
 'KCB-TX-2025-001', '2025-01-15 10:05:00+09', '2025-01-15 10:10:00+09',
 '2025-01-15 10:05:00+09', 0, '2025-01-15 10:10:00+09', 0, 0),
(9002, 9002, 1001,
 'NEW_LOAN', 'KCB', 'ACKED',
 'NEW', 'NEW_LOAN_CONTRACTED',
 '{"contractedAmount":100000000,"period":120,"productCd":"DEMO_MORTGAGE"}',
 'KCB-TX-2025-002', '2025-02-10 09:05:00+09', '2025-02-10 09:12:00+09',
 '2025-02-10 09:05:00+09', 0, '2025-02-10 09:12:00+09', 0, 0)
ON CONFLICT (crpt_id) DO NOTHING;

-- ------------------------------------------------------------
-- 8. 알림 발송함 (notification_outbox)
-- ------------------------------------------------------------
INSERT INTO notification_outbox (
    outbox_id, event_type_cd, reference_id, channel_cd,
    payload, status, attempt_no, max_attempt, next_attempt_at,
    idempotency_key, sent_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9001, 'LOAN_APPROVED', 9001, 'EMAIL',
 '{"customerId":1001,"applNo":"DEMO-2025-001","message":"대출 심사 승인 안내"}',
 'SENT', 1, 5, '2025-01-13 10:01:00+09',
 'DEMO:LOAN_APPROVED:9001:EMAIL', '2025-01-13 10:01:00+09',
 '2025-01-13 10:00:00+09', 0, '2025-01-13 10:01:00+09', 0, 0),
(9002, 'LOAN_CONTRACTED', 9001, 'SMS',
 '{"customerId":1001,"cntrNo":"DEMO-CNTR-2025-001","message":"대출 약정 완료 안내"}',
 'SENT', 1, 5, '2025-01-15 10:01:00+09',
 'DEMO:LOAN_CONTRACTED:9001:SMS', '2025-01-15 10:01:00+09',
 '2025-01-15 10:00:00+09', 0, '2025-01-15 10:01:00+09', 0, 0),
(9003, 'LOAN_APPROVED', 9002, 'EMAIL',
 '{"customerId":1001,"applNo":"DEMO-2025-002","message":"대출 심사 승인 안내"}',
 'SENT', 1, 5, '2025-02-08 09:01:00+09',
 'DEMO:LOAN_APPROVED:9002:EMAIL', '2025-02-08 09:01:00+09',
 '2025-02-08 09:00:00+09', 0, '2025-02-08 09:01:00+09', 0, 0),
(9004, 'LOAN_REVIEW_PENDING', 9003, 'EMAIL',
 '{"customerId":1002,"applNo":"DEMO-2025-003","message":"심사 결재 대기 중 안내"}',
 'SENT', 1, 5, '2025-03-05 09:01:00+09',
 'DEMO:LOAN_REVIEW_PENDING:9003:EMAIL', '2025-03-05 09:01:00+09',
 '2025-03-05 09:00:00+09', 0, '2025-03-05 09:01:00+09', 0, 0),
(9005, 'LOAN_REJECTED', 9004, 'EMAIL',
 '{"customerId":1003,"applNo":"DEMO-2025-004","message":"대출 심사 결과 안내"}',
 'SENT', 1, 5, '2025-03-11 09:11:00+09',
 'DEMO:LOAN_REJECTED:9004:EMAIL', '2025-03-11 09:11:00+09',
 '2025-03-11 09:10:00+09', 0, '2025-03-11 09:11:00+09', 0, 0)
ON CONFLICT (outbox_id) DO NOTHING;

-- ------------------------------------------------------------
-- 9. 어드바이저리 규칙 (review_advisory_rule)
--    AdvisoryRuleSeeder 가 앱 기동 시 동일 rule_cd 를 삽입하므로,
--    rule_cd 가 이미 존재하면 skip.
-- ------------------------------------------------------------
INSERT INTO review_advisory_rule (
    rule_cd, rule_name, advisory_type_cd, rule_category_cd, severity_cd,
    rule_params, rule_version, active_yn, rule_desc,
    created_at, created_by, updated_at, updated_by, version
)
SELECT 'DSR_THRESHOLD_OVERRIDE', 'DSR 한도 초과 승인',
       'REREVIEW_RECOMMEND', 'THRESHOLD_VIOLATION', 'CRITICAL',
       NULL, 'v1.0', 'Y', 'DSR_CALCULATION.dsr_status_cd=FAIL 인데 본심사가 승인된 경우',
       '2025-01-01 00:00:00+09', 0, '2025-01-01 00:00:00+09', 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM review_advisory_rule WHERE rule_cd='DSR_THRESHOLD_OVERRIDE' AND deleted_at IS NULL
);

INSERT INTO review_advisory_rule (
    rule_cd, rule_name, advisory_type_cd, rule_category_cd, severity_cd,
    rule_params, rule_version, active_yn, rule_desc,
    created_at, created_by, updated_at, updated_by, version
)
SELECT 'LTV_THRESHOLD_OVERRIDE', 'LTV 한도 초과 승인',
       'REREVIEW_RECOMMEND', 'THRESHOLD_VIOLATION', 'CRITICAL',
       NULL, 'v1.0', 'Y', 'LTV_CALCULATION.ltv_status_cd=FAIL 인데 본심사가 승인된 경우',
       '2025-01-01 00:00:00+09', 0, '2025-01-01 00:00:00+09', 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM review_advisory_rule WHERE rule_cd='LTV_THRESHOLD_OVERRIDE' AND deleted_at IS NULL
);

INSERT INTO review_advisory_rule (
    rule_cd, rule_name, advisory_type_cd, rule_category_cd, severity_cd,
    rule_params, rule_version, active_yn, rule_desc,
    created_at, created_by, updated_at, updated_by, version
)
SELECT 'BIAS_REJECT_RATE_DEVIATION', '심사관 거절율 편차',
       'BIAS_DETECTION', 'REVIEWER_DEVIATION', 'WARN',
       NULL, 'v1.0', 'Y', '코호트 거절율이 동료 평균 대비 +2σ 초과 (최소 표본 30)',
       '2025-01-01 00:00:00+09', 0, '2025-01-01 00:00:00+09', 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM review_advisory_rule WHERE rule_cd='BIAS_REJECT_RATE_DEVIATION' AND deleted_at IS NULL
);

INSERT INTO review_advisory_rule (
    rule_cd, rule_name, advisory_type_cd, rule_category_cd, severity_cd,
    rule_params, rule_version, active_yn, rule_desc,
    created_at, created_by, updated_at, updated_by, version
)
SELECT 'BIAS_APPROVAL_RATE_DEVIATION', '심사관 승인율 편차',
       'BIAS_DETECTION', 'REVIEWER_DEVIATION', 'WARN',
       NULL, 'v1.0', 'Y', '코호트 승인율이 동료 평균 대비 -2σ 미만 (최소 표본 30)',
       '2025-01-01 00:00:00+09', 0, '2025-01-01 00:00:00+09', 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM review_advisory_rule WHERE rule_cd='BIAS_APPROVAL_RATE_DEVIATION' AND deleted_at IS NULL
);

INSERT INTO review_advisory_rule (
    rule_cd, rule_name, advisory_type_cd, rule_category_cd, severity_cd,
    rule_params, rule_version, active_yn, rule_desc,
    created_at, created_by, updated_at, updated_by, version
)
SELECT 'PEER_DECISION_DIVERGENCE', '유사 신청자 결정 분기',
       'REREVIEW_RECOMMEND', 'PEER_DIVERGENCE', 'WARN',
       NULL, 'v1.0', 'Y', '유사 프로파일(신용±5점/DSR±500/LTV±500) 90일 그룹 70:30 분기에서 본 건이 소수 결정',
       '2025-01-01 00:00:00+09', 0, '2025-01-01 00:00:00+09', 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM review_advisory_rule WHERE rule_cd='PEER_DECISION_DIVERGENCE' AND deleted_at IS NULL
);

-- ------------------------------------------------------------
-- 10. 어드바이저리 리포트 (review_advisory_report)
--     rev_id=9003 (PENDING_APPROVER 건) 에 DSR 초과 승인 자문 1건.
--     rule_id 는 subquery 로 조회 (시드 또는 앱시더가 삽입한 행 참조).
-- ------------------------------------------------------------
INSERT INTO review_advisory_report (
    advr_id, rev_id, rule_id,
    advisory_type_cd, severity_cd, advr_status_cd,
    advr_title, advr_summary, advr_payload,
    target_reviewer_id, generated_at,
    created_at, created_by, updated_at, updated_by, version
)
OVERRIDING SYSTEM VALUE
SELECT
    9001, 9003, r.rule_id,
    'REREVIEW_RECOMMEND', 'CRITICAL', 'OPEN',
    'DSR 한도 초과 승인 감지',
    'dsr_ratio_bps=3833 이 한도(4000bps)에 근접하나 ceval_decision=APPROVED 로 처리됨. 재심사 권고.',
    '{"dsrRatioBps":3833,"dsrLimitBps":4000,"cevalGrade":"C","cevalScore":650}',
    9002, '2025-03-05 09:00:00+09',
    '2025-03-05 09:00:00+09', 0, '2025-03-05 09:00:00+09', 0, 0
FROM review_advisory_rule r
WHERE r.rule_cd = 'DSR_THRESHOLD_OVERRIDE' AND r.deleted_at IS NULL
LIMIT 1
ON CONFLICT (advr_id) DO NOTHING;

-- ------------------------------------------------------------
-- 11. AI 감사 의견 (ai_audit_opinion)
--     advr_id=9001 에 대한 편향 감지 의견.
-- ------------------------------------------------------------
INSERT INTO ai_audit_opinion (
    opinion_id, advr_id, rev_id, reviewer_id,
    analysis_type_cd, conclusion_cd,
    reasoning_summary, confidence_score,
    input_tokens, output_tokens, generated_at
) OVERRIDING SYSTEM VALUE VALUES
(9001, 9001, 9003, 9002,
 'BIAS_DETECTION', 'NO_BIAS_DETECTED',
 'DSR가 한도에 근접하나 심사관의 승인 결정은 신용등급(C) 및 고용안정성을 감안한 합리적 판단으로 보임. 통계적 편향 신호 미탐지.',
 0.7800,
 512, 128, '2025-03-05 09:05:00+09'),
(9002, 9001, 9003, 9002,
 'COMPLIANCE_VERIFICATION', 'COMPLIANT',
 '내부 여신심사 기준서 §3.2(DSR 예외 승인 요건) 충족. 예외 사유 기재 확인.',
 0.8500,
 480, 96, '2025-03-05 09:06:00+09')
ON CONFLICT (opinion_id) DO NOTHING;

-- ------------------------------------------------------------
-- 12. Advisory RAG 정책문서 (advisory_document)
--     임베딩(vector) 없이 마스터 행만 삽입.
--     실제 임베딩/청크는 앱 기동 후 /api/internal/advisory/documents/{id}/activate 호출.
-- ------------------------------------------------------------
INSERT INTO advisory_document (
    doc_id, doc_cd, doc_title, doc_category_cd, doc_version,
    effective_start_date, effective_end_date,
    active_yn, doc_desc,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9001, 'POL_DSR_EXCEPTION_V1', 'DSR 예외 승인 기준서', 'POLICY', '1.0',
 '20250101', NULL,
 'N', 'DSR 한도 초과 건에 대한 예외 승인 요건 및 절차',
 '2025-01-01 00:00:00+09', 0, '2025-01-01 00:00:00+09', 0, 0),
(9002, 'POL_BIAS_REVIEW_V1', '심사 공정성 관리 지침', 'POLICY', '1.0',
 '20250101', NULL,
 'N', '심사관 편향 감지 기준 및 자문 처리 절차',
 '2025-01-01 00:00:00+09', 0, '2025-01-01 00:00:00+09', 0, 0),
(9003, 'POL_CREDIT_GRADE_V2', '신용등급별 여신한도 기준표', 'POLICY', '2.0',
 '20240701', NULL,
 'N', '등급(A~F)별 최대 여신한도 및 금리 가산 기준',
 '2025-01-01 00:00:00+09', 0, '2025-01-01 00:00:00+09', 0, 0)
ON CONFLICT (doc_id) DO NOTHING;

-- ============================================================
-- 끝.
-- ============================================================
