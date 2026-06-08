-- ============================================================
-- V43: 자투리 데모 시드 — 가심사·신용동의·본인확인·서류OCR·증명서·자동이체 청산대기
--
-- 목적: 구현은 됐으나 시드가 없던 나머지 영역을 채운다. 기존 신청/계약/서류에 결합.
--   가심사   : 신청 9102·9103·9301 (PASS)
--   신용동의 : 신청 9103·9301 (CB 조회 동의)
--   본인확인 : 신청 9103·9301 (휴대폰 인증 DONE/PASS)
--   증명서   : 계약 9201(잔액·부채) / 9501(상환완료)
--   자동이체 : 계약 9201 5회차 청산 대기(PENDING)
-- 대역: PK 9601~ (기존과 비충돌). 멱등: ON CONFLICT ... DO NOTHING.
-- 참고: 결합 대상 신청/계약/서류는 V38/V39/V40/V42 에서 생성됨.
-- ============================================================

-- ------------------------------------------------------------
-- 1. 가심사 (loan_prescreening) — appl_id UNIQUE 이므로 가심사 미보유 신청에만
-- ------------------------------------------------------------
INSERT INTO loan_prescreening (
    presc_id, appl_id, presc_result_cd, estimated_limit_amt, estimated_rate_bps,
    estimated_grade, estimated_score, reject_reason_cd, presc_remark,
    prescreened_at, presc_engine_version,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9601, 9102, 'PASS', 80000000, 380, 'A', 760, NULL, '한도조회 통과',
 '2025-04-03 10:35:00+09', 'v1.2', '2025-04-03 10:35:00+09', 0, '2025-04-03 10:35:00+09', 0, 0),
(9602, 9103, 'PASS', 30000000, 540, 'B', 710, NULL, '가심사 통과',
 '2025-04-05 14:05:00+09', 'v1.2', '2025-04-05 14:05:00+09', 0, '2025-04-05 14:05:00+09', 0, 0),
(9603, 9301, 'PASS', 22000000, 560, 'B', 705, NULL, '가심사 통과(수동 심사 대상)',
 '2025-05-01 10:05:00+09', 'v1.2', '2025-05-01 10:05:00+09', 0, '2025-05-01 10:05:00+09', 0, 0)
ON CONFLICT (presc_id) DO NOTHING;

-- ------------------------------------------------------------
-- 2. 신용조회 동의 (credit_consent)
-- ------------------------------------------------------------
INSERT INTO credit_consent (
    csnt_id, appl_id, customer_id, consent_type_cd, consent_scope_cd, consent_target_cd,
    consent_yn, consented_at, consent_method_cd, retention_until, withdrawn_yn,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9601, 9103, 1004, 'CREDIT_INQUIRY', 'ALL', 'CB', 'Y', '2025-04-05 13:55:00+09', 'EFORM', '20300405', 'N',
 '2025-04-05 13:55:00+09', 0, '2025-04-05 13:55:00+09', 0, 0),
(9602, 9301, 1007, 'CREDIT_INQUIRY', 'ALL', 'CB', 'Y', '2025-05-01 09:55:00+09', 'EFORM', '20300501', 'N',
 '2025-05-01 09:55:00+09', 0, '2025-05-01 09:55:00+09', 0, 0)
ON CONFLICT (csnt_id) DO NOTHING;

-- ------------------------------------------------------------
-- 3. 본인확인 (loan_identity_verification) — 휴대폰 인증 완료. enc 컬럼은 NULL.
-- ------------------------------------------------------------
INSERT INTO loan_identity_verification (
    idv_id, appl_id, customer_id, idv_method_cd, idv_status_cd, idv_result_cd, idv_target_cd,
    ci_hash, mobile_no_masked, verified_at, external_tx_no,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9601, 9103, 1004, 'MOBILE_AUTH', 'DONE', 'PASS', 'APPLICANT',
 'demo-ci-9601', '010-****-3456', '2025-04-05 13:58:00+09', 'IDV-TX-9601',
 '2025-04-05 13:58:00+09', 0, '2025-04-05 13:58:00+09', 0, 0),
(9602, 9301, 1007, 'MOBILE_AUTH', 'DONE', 'PASS', 'APPLICANT',
 'demo-ci-9602', '010-****-7788', '2025-05-01 09:58:00+09', 'IDV-TX-9602',
 '2025-05-01 09:58:00+09', 0, '2025-05-01 09:58:00+09', 0, 0)
ON CONFLICT (idv_id) DO NOTHING;

-- (서류 OCR 은 V21 에서 loan_document_ocr 가 제거됨 — OCR 은 doc-agent L3 파이프라인 담당.
--  loan-service 시드 대상 아님.)

-- ------------------------------------------------------------
-- 4. 증명서 (loan_certificate) — 계약 9201(잔액·부채), 9501(상환완료)
-- ------------------------------------------------------------
INSERT INTO loan_certificate (
    cert_id, cntr_id, customer_id, cert_type_cd, cert_no, cert_status_cd,
    cert_purpose_cd, issue_channel_cd, issued_at, retention_until,
    created_at, created_by, updated_at, updated_by, version
) OVERRIDING SYSTEM VALUE VALUES
(9601, 9201, 1006, 'BALANCE',   'DEMO-CERT-9601', 'ISSUED', 'PROOF_OF_BALANCE', 'MOBILE',  '2025-06-01 10:00:00+09', '20300601', '2025-06-01 10:00:00+09', 0, '2025-06-01 10:00:00+09', 0, 0),
(9602, 9201, 1006, 'DEBT',      'DEMO-CERT-9602', 'ISSUED', 'LOAN_APPLICATION', 'COUNTER', '2025-06-02 11:00:00+09', '20300602', '2025-06-02 11:00:00+09', 0, '2025-06-02 11:00:00+09', 0, 0),
(9603, 9501, 1014, 'REPAYMENT', 'DEMO-CERT-9603', 'ISSUED', 'PROOF_OF_REPAYMENT', 'MOBILE', '2026-03-11 09:00:00+09', '20310311', '2026-03-11 09:00:00+09', 0, '2026-03-11 09:00:00+09', 0, 0)
ON CONFLICT (cert_id) DO NOTHING;

-- ------------------------------------------------------------
-- 6. 자동이체 청산 대기 (auto_debit_clearing_pending) — 계약 9201 5회차 진행중
--    (BaseEntity 미상속: created_by/updated_* 컬럼 없음. status CHECK: PENDING/DONE/FAILED)
-- ------------------------------------------------------------
INSERT INTO auto_debit_clearing_pending (
    pending_id, pi_id, cntr_id, rsch_id, installment_no, base_date, idempotency_key, status,
    created_at, resolved_at
) OVERRIDING SYSTEM VALUE VALUES
(9601, 'PI-DEMO-9601', 9201, 9205, 5, '20250810', 'AUTODEBIT-9201-5-20250810', 'PENDING',
 '2025-08-10 06:00:00+09', NULL)
ON CONFLICT (pending_id) DO NOTHING;

-- ============================================================
-- 끝.
-- ============================================================
