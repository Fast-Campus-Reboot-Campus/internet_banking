-- LoanProduct 보증인 최소 수 정책 필드 추가.
-- guarantor_required_yn='Y' 인 경우 min_guarantor_count >= 1 이 강제됨 (서비스 레이어).
-- 기존 데이터는 0 으로 backfill — 보증 불필요(구 기본값)와 동일 의미.

ALTER TABLE loan_product
    ADD COLUMN min_guarantor_count INT NOT NULL DEFAULT 0;
