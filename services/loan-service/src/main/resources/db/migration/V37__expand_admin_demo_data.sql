-- ============================================================
-- V37: Admin 화면 데모 시드 데이터 확장
--
-- 목적: V35 의 최소 시드를 보강하여 관리자 화면(심사 큐, 본심사,
--       신청서류, 어드바이저리)이 다양한 파이프라인 단계로 채워지도록 한다.
--       특히 신청서류는 검증 4상태(VERIFIED/REJECTED/HOLD/PENDING)를 모두 포함하여
--       doc-agent 미연결 시 검증 보류(PENDING) 강등 동작까지 화면에서 확인 가능하게 한다.
-- 멱등: 모든 INSERT 는 ON CONFLICT ... DO NOTHING.
-- 참조: customer_id=1002~1005 는 customer-service 의 가상 고객. created_by=0 은 시스템(migration) 행위자.
--       V35 가 9001~9004 를 점유하므로 본 파일은 9005 번대부터 사용한다.
-- ============================================================

-- ------------------------------------------------------------
-- 1. 대출 상품 추가 (loan_product) — 상품 목록 다양화
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
(9003, 'DEMO_JEONSE', '데모 전세자금대출', 'JEONSE', 'INDIVIDUAL',
 'EQUAL_PRINCIPAL_INTEREST', 'VARIABLE',
 380, 300, 1100,
 5000000, 300000000, 12, 240,
 'N', 'Y',
 '20250101', 'ACTIVE',
 '2025-01-01 00:00:00+09', 0, '2025-01-01 00:00:00+09', 0, 0),
(9004, 'DEMO_BIZ', '데모 개인사업자대출', 'BUSINESS', 'INDIVIDUAL',
 'EQUAL_PRINCIPAL_INTEREST', 'FIXED',
 600, 480, 1800,
 3000000, 100000000, 6, 84,
 'N', 'N',
 '20250101', 'ACTIVE',
 '2025-01-01 00:00:00+09', 0, '2025-01-01 00:00:00+09', 0, 0)
ON CONFLICT (prod_id) DO NOTHING;

-- ------------------------------------------------------------
-- 2. 대출 신청 (loan_application) — 파이프라인 전 단계 커버
--    9005: SUBMITTED        (접수, 서류 보완 필요)
--    9006: PRESCREENED      (가심사 통과, 서류 검증 보류 PENDING)
--    9007: REVIEWING        (본심사 진행 — 어드바이저리 대상)
--    9008: APPROVED         (심사 승인, 약정 전)
--    9009: CONTRACTED       (계약 완료)
-- ------------------------------------------------------------
INSERT INTO loan_application (
    appl_id, appl_no, customer_id, prod_id, channel_cd,
    requested_amount, requested_period_mo, loan_purpose_cd,
    repayment_method_cd, estimated_income_amt, employment_type_cd,
    appl_status_cd, applied_at, idempotency_key,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9005, 'DEMO-2025-005', 1002, 9001, 'MOBILE',
 12000000, 24, 'LIVING_EXPENSE',
 'EQUAL_PRINCIPAL_INTEREST', 42000000, 'EMPLOYED',
 'SUBMITTED', '2025-04-01 09:00:00+09', 'DEMO-IDEM-2025-005',
 '2025-04-01 09:00:00+09', 0, '2025-04-01 09:00:00+09', 0, 0),
(9006, 'DEMO-2025-006', 1004, 9003, 'INTERNET',
 80000000, 24, 'HOUSE_RENT',
 'EQUAL_PRINCIPAL_INTEREST', 55000000, 'EMPLOYED',
 'PRESCREENED', '2025-04-03 10:30:00+09', 'DEMO-IDEM-2025-006',
 '2025-04-03 10:30:00+09', 0, '2025-04-03 11:00:00+09', 0, 0),
(9007, 'DEMO-2025-007', 1004, 9001, 'INTERNET',
 30000000, 36, 'LIVING_EXPENSE',
 'EQUAL_PRINCIPAL_INTEREST', 52000000, 'EMPLOYED',
 'REVIEWING', '2025-04-05 14:00:00+09', 'DEMO-IDEM-2025-007',
 '2025-04-05 14:00:00+09', 0, '2025-04-07 09:00:00+09', 0, 0),
(9008, 'DEMO-2025-008', 1005, 9004, 'BRANCH',
 25000000, 48, 'BUSINESS_FUND',
 'EQUAL_PRINCIPAL_INTEREST', 60000000, 'SELF_EMPLOYED',
 'APPROVED', '2025-04-08 11:00:00+09', 'DEMO-IDEM-2025-008',
 '2025-04-08 11:00:00+09', 0, '2025-04-10 15:00:00+09', 0, 0),
(9009, 'DEMO-2025-009', 1005, 9001, 'MOBILE',
 18000000, 36, 'LIVING_EXPENSE',
 'EQUAL_PRINCIPAL_INTEREST', 47000000, 'EMPLOYED',
 'CONTRACTED', '2025-03-20 09:00:00+09', 'DEMO-IDEM-2025-009',
 '2025-03-20 09:00:00+09', 0, '2025-03-28 16:00:00+09', 0, 0)
ON CONFLICT (appl_id) DO NOTHING;

-- ------------------------------------------------------------
-- 3. 신용평가 (credit_evaluation) — 심사 단계 진입 건만
-- ------------------------------------------------------------
INSERT INTO credit_evaluation (
    ceval_id, appl_id, customer_id,
    ceval_engine, ceval_engine_version,
    ceval_grade, ceval_score, pd_bps,
    ceval_decision_cd, eval_limit_amount, eval_rate_bps,
    ceval_status_cd, ceval_factors, evaluated_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9007, 9007, 1004,
 'KCB', 'v2.1',
 'B', 710, 95,
 'REVIEW', 30000000, 540,
 'COMPLETED', '{"main_factor":"peer_divergence","detail":"유사군 결정 분기 — 수동 심사 권고"}', '2025-04-06 09:00:00+09',
 '2025-04-06 09:00:00+09', 0, '2025-04-06 09:00:00+09', 0, 0),
(9008, 9008, 1005,
 'KCB', 'v2.1',
 'B', 735, 70,
 'APPROVED', 25000000, 590,
 'COMPLETED', '{"main_factor":"business_cashflow","detail":"사업소득 안정"}', '2025-04-09 09:00:00+09',
 '2025-04-09 09:00:00+09', 0, '2025-04-09 09:00:00+09', 0, 0),
(9009, 9009, 1005,
 'KCB', 'v2.1',
 'A', 780, 50,
 'APPROVED', 20000000, 470,
 'COMPLETED', '{"main_factor":"credit_history","detail":"우량 신용 이력"}', '2025-03-21 09:00:00+09',
 '2025-03-21 09:00:00+09', 0, '2025-03-21 09:00:00+09', 0, 0)
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
(9007, 9007, 1004,
 52000000, 6000000, 3000000,
 10800000, 13800000,
 2654, 4000, 'PASS',
 'STANDARD', '2025-04-06 09:05:00+09', 'v1.3',
 '2025-04-06 09:05:00+09', 0, '2025-04-06 09:05:00+09', 0, 0),
(9008, 9008, 1005,
 60000000, 10000000, 4800000,
 7200000, 12000000,
 2000, 4000, 'PASS',
 'STANDARD', '2025-04-09 09:05:00+09', 'v1.3',
 '2025-04-09 09:05:00+09', 0, '2025-04-09 09:05:00+09', 0, 0),
(9009, 9009, 1005,
 47000000, 0, 0,
 6480000, 6480000,
 1378, 4000, 'PASS',
 'STANDARD', '2025-03-21 09:05:00+09', 'v1.3',
 '2025-03-21 09:05:00+09', 0, '2025-03-21 09:05:00+09', 0, 0)
ON CONFLICT (dsr_id) DO NOTHING;

-- ------------------------------------------------------------
-- 5. 본심사 (loan_review)
--    9007: PENDING_APPROVER (어드바이저리 발행 — 결재 대기)
--    9008: COMPLETED + APPROVED (약정 전)
--    9009: COMPLETED + APPROVED (계약 완료 건)
-- ------------------------------------------------------------
INSERT INTO loan_review (
    rev_id, appl_id, rev_type_cd, rev_status_cd, rev_decision_cd,
    approved_amount, approved_rate_bps, approved_period_mo,
    reject_reason_cd, reviewer_id, reviewed_at, approved_at,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9007, 9007, 'MANUAL', 'PENDING_APPROVER', NULL,
 NULL, NULL, NULL,
 NULL, 9002, '2025-04-06 15:00:00+09', NULL,
 '2025-04-06 15:00:00+09', 0, '2025-04-07 09:00:00+09', 0, 0),
(9008, 9008, 'MANUAL', 'COMPLETED', 'APPROVED',
 25000000, 590, 48,
 NULL, 9002, '2025-04-09 14:00:00+09', '2025-04-10 10:00:00+09',
 '2025-04-09 14:00:00+09', 0, '2025-04-10 10:00:00+09', 0, 0),
(9009, 9009, 'MANUAL', 'COMPLETED', 'APPROVED',
 18000000, 470, 36,
 NULL, 9002, '2025-03-22 11:00:00+09', '2025-03-23 09:00:00+09',
 '2025-03-22 11:00:00+09', 0, '2025-03-23 09:00:00+09', 0, 0)
ON CONFLICT (rev_id) DO NOTHING;

-- ------------------------------------------------------------
-- 6. 어드바이저리 리포트 (review_advisory_report)
--    rev_id=9007 에 유사군 결정 분기(PEER_DECISION_DIVERGENCE) 자문 1건.
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
    9002, 9007, r.rule_id,
    'REREVIEW_RECOMMEND', 'WARN', 'OPEN',
    '유사 신청자 결정 분기 감지',
    '유사 프로파일 90일 그룹이 70:30 으로 분기하며 본 건이 소수 결정 측에 속함. 재검토 권고.',
    '{"cohortApprovalRate":0.30,"peerSample":34,"cevalGrade":"B","cevalScore":710}',
    9002, '2025-04-07 09:00:00+09',
    '2025-04-07 09:00:00+09', 0, '2025-04-07 09:00:00+09', 0, 0
FROM review_advisory_rule r
WHERE r.rule_cd = 'PEER_DECISION_DIVERGENCE' AND r.deleted_at IS NULL
LIMIT 1
ON CONFLICT (advr_id) DO NOTHING;

-- ------------------------------------------------------------
-- 7. 신청서류 (loan_document) — 검증 4상태 모두 포함
--    VERIFIED(AUTO_PASS) / REJECTED(NEEDS_RESUBMIT) / UPLOADED(HOLD) /
--    UPLOADED(PENDING=doc-agent 미연결 검증 보류)
--    doc_url 에는 doc-agent submission_id 를 보존(보류 건은 NULL).
-- ------------------------------------------------------------
INSERT INTO loan_document (
    doc_id, appl_id, doc_type_cd, doc_status_cd, doc_source_cd,
    doc_name, doc_url, mime_type, file_size_bytes,
    submitted_at, verified_at, verify_result_cd,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
-- 9001 (CONTRACTED): 정상 검증 완료 서류
(9001, 9001, 'EMPLOYMENT_CERT', 'VERIFIED', 'MOBILE',
 'employment_cert.pdf', 'sub-demo-0001', 'application/pdf', 184320,
 '2025-01-10 10:20:00+09', '2025-01-10 10:21:00+09', 'AUTO_PASS',
 '2025-01-10 10:20:00+09', 0, '2025-01-10 10:21:00+09', 0, 0),
(9002, 9001, 'INCOME_PROOF', 'VERIFIED', 'MOBILE',
 'income_proof.pdf', 'sub-demo-0002', 'application/pdf', 220160,
 '2025-01-10 10:22:00+09', '2025-01-10 10:23:00+09', 'AUTO_PASS',
 '2025-01-10 10:22:00+09', 0, '2025-01-10 10:23:00+09', 0, 0),
-- 9003 (REVIEWING): 신분증 검증완료 + 통장사본 보류(HOLD)
(9003, 9003, 'ID_CARD', 'VERIFIED', 'INTERNET',
 'id_card.jpg', 'sub-demo-0003', 'image/jpeg', 98304,
 '2025-03-01 11:10:00+09', '2025-03-01 11:11:00+09', 'AUTO_PASS',
 '2025-03-01 11:10:00+09', 0, '2025-03-01 11:11:00+09', 0, 0),
(9004, 9003, 'BANKBOOK', 'UPLOADED', 'INTERNET',
 'bankbook.pdf', 'sub-demo-0004', 'application/pdf', 131072,
 '2025-03-01 11:12:00+09', NULL, 'HOLD',
 '2025-03-01 11:12:00+09', 0, '2025-03-01 11:12:00+09', 0, 0),
-- 9005 (SUBMITTED): 신분증 검증완료 + 소득증빙 재제출 필요(NEEDS_RESUBMIT)
(9005, 9005, 'ID_CARD', 'VERIFIED', 'MOBILE',
 'id_card.png', 'sub-demo-0005', 'image/png', 76800,
 '2025-04-01 09:10:00+09', '2025-04-01 09:11:00+09', 'AUTO_PASS',
 '2025-04-01 09:10:00+09', 0, '2025-04-01 09:11:00+09', 0, 0),
(9006, 9005, 'INCOME_PROOF', 'REJECTED', 'MOBILE',
 'income_blurry.jpg', 'sub-demo-0006', 'image/jpeg', 54200,
 '2025-04-01 09:12:00+09', NULL, 'NEEDS_RESUBMIT',
 '2025-04-01 09:12:00+09', 0, '2025-04-01 09:13:00+09', 0, 0),
-- 9006 (PRESCREENED): 재직증명 검증 보류(PENDING — doc-agent 미연결 강등). doc_url NULL.
(9007, 9006, 'EMPLOYMENT_CERT', 'UPLOADED', 'INTERNET',
 'employment_cert.pdf', NULL, 'application/pdf', 201728,
 '2025-04-03 10:40:00+09', NULL, 'PENDING',
 '2025-04-03 10:40:00+09', 0, '2025-04-03 10:40:00+09', 0, 0),
-- 9007 (REVIEWING): 정상 제출 서류 2종
(9008, 9007, 'ID_CARD', 'VERIFIED', 'INTERNET',
 'id_card.jpg', 'sub-demo-0008', 'image/jpeg', 91240,
 '2025-04-05 14:10:00+09', '2025-04-05 14:11:00+09', 'AUTO_PASS',
 '2025-04-05 14:10:00+09', 0, '2025-04-05 14:11:00+09', 0, 0),
(9009, 9007, 'INCOME_PROOF', 'VERIFIED', 'INTERNET',
 'income_proof.pdf', 'sub-demo-0009', 'application/pdf', 237568,
 '2025-04-05 14:12:00+09', '2025-04-05 14:13:00+09', 'AUTO_PASS',
 '2025-04-05 14:12:00+09', 0, '2025-04-05 14:13:00+09', 0, 0)
ON CONFLICT (doc_id) DO NOTHING;

-- ============================================================
-- 끝.
-- ============================================================
