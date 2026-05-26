-- Plan 08: WRITE_OFF / SUBROGATION 종결 실로직 — 정산 외 사고종결 메타 컬럼
ALTER TABLE loan_closure
    ADD COLUMN write_off_amount        BIGINT,
    ADD COLUMN subrogation_amount      BIGINT,
    ADD COLUMN subrogation_party_ref   VARCHAR(200),
    ADD COLUMN write_off_reason_cd     VARCHAR(50);
