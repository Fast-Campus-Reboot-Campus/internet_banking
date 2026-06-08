-- ============================================================
-- V5: doc-agent Admin 화면 데모 시드 데이터
--
-- 목적: 휴먼리뷰 큐(GET /api/documents/queue) 에 표시될 샘플 서류 제출 건.
-- 멱등: ON CONFLICT (submission_id) DO NOTHING
-- ============================================================

-- 위변조 의심 — HOLD + PENDING 리뷰 (큐에 표시됨)
INSERT INTO loan_document_submission (
    submission_id, application_id, doc_code,
    forgery_score, verify_status, human_review_status,
    legal_hold, created_at, updated_at
) VALUES
(
    'a1b2c3d4-0001-0000-0000-000000000001',
    'DEMO-2025-001', 'DOC_01',
    0.82, 'HOLD', 'PENDING',
    FALSE, '2025-01-10 10:05:00', '2025-01-10 10:10:00'
),
(
    'a1b2c3d4-0002-0000-0000-000000000002',
    'DEMO-2025-003', 'DOC_03',
    0.71, 'HOLD', 'PENDING',
    FALSE, '2025-03-01 11:10:00', '2025-03-01 11:15:00'
),
-- 리걸홀드 + 휴먼리뷰 대기
(
    'a1b2c3d4-0003-0000-0000-000000000003',
    'DEMO-2025-003', 'DOC_02',
    0.55, 'HOLD', 'PENDING',
    TRUE,  '2025-03-02 09:00:00', '2025-03-02 09:30:00'
)
ON CONFLICT (submission_id) DO NOTHING;

-- 정상 처리 건 (큐 미표시 — 참고용)
INSERT INTO loan_document_submission (
    submission_id, application_id, doc_code,
    forgery_score, verify_status, human_review_status,
    legal_hold, created_at, updated_at
) VALUES
(
    'b2c3d4e5-0001-0000-0000-000000000001',
    'DEMO-2025-001', 'DOC_02',
    0.12, 'AUTO_PASS', 'NOT_REQUIRED',
    FALSE, '2025-01-10 10:05:00', '2025-01-10 10:08:00'
),
(
    'b2c3d4e5-0002-0000-0000-000000000002',
    'DEMO-2025-002', 'DOC_01',
    0.08, 'AUTO_PASS', 'NOT_REQUIRED',
    FALSE, '2025-02-05 09:05:00', '2025-02-05 09:07:00'
)
ON CONFLICT (submission_id) DO NOTHING;

-- ============================================================
-- 끝.
-- ============================================================
