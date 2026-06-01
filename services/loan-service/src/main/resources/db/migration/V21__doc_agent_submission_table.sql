-- doc-agent 연동 마이그레이션
-- 1. loan_document_ocr 제거 — OCR은 doc-agent L3 파이프라인이 담당, loan-service 불필요
-- 2. loan_document_submission 추가 — doc-agent API 호출 이력 관리

DROP TABLE IF EXISTS loan_document_ocr;

CREATE TABLE loan_document_submission (
    submission_id     VARCHAR(36)    NOT NULL,
    doc_id            BIGINT         REFERENCES loan_document(doc_id),
    application_id    VARCHAR(30)    NOT NULL,
    doc_code          VARCHAR(50)    NOT NULL,
    verify_status     VARCHAR(50),
    confidence_score  NUMERIC(5, 4),
    occurred_at       TIMESTAMPTZ,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT pk_loan_document_submission PRIMARY KEY (submission_id)
);

CREATE INDEX idx_lds_doc_id        ON loan_document_submission (doc_id);
CREATE INDEX idx_lds_application_id ON loan_document_submission (application_id);
