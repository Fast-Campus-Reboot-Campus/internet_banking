-- 공통 약관 연동 — 수신 서비스 측 연결 테이블 추가
-- common_terms_template / common_terms_version / common_terms_consent 은 공통 서비스 소유
-- 해당 테이블에 대한 FK는 공통 서비스 마이그레이션 완료 이후 유효

BEGIN;

-- ── 수신 상품(products) ↔ 공통 약관 템플릿 연결 테이블 ──────────────────────
CREATE TABLE product_common_terms (
    product_common_terms_id BIGSERIAL PRIMARY KEY,
    product_id              BIGINT  NOT NULL,
    terms_template_id       BIGINT  NOT NULL,
    is_required             BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              CHAR(8) NOT NULL,
    updated_at              CHAR(8),

    CONSTRAINT fk_product_common_terms_product
        FOREIGN KEY (product_id)
        REFERENCES products (product_id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    -- common_terms_template 은 공통 서비스 소유 (논리적 FK)
    -- CONSTRAINT fk_product_common_terms_template
    --     FOREIGN KEY (terms_template_id)
    --     REFERENCES common_terms_template (terms_template_id),

    CONSTRAINT uq_product_common_terms_product_template
        UNIQUE (product_id, terms_template_id)
);

CREATE INDEX idx_product_common_terms_product  ON product_common_terms (product_id);
CREATE INDEX idx_product_common_terms_template ON product_common_terms (terms_template_id);


-- ── 수신 계약(contracts) ↔ 공통 약관 동의 연결 테이블 ──────────────────────
CREATE TABLE contract_common_terms_consents (
    contract_common_terms_consents_id BIGSERIAL PRIMARY KEY,
    contract_id                       BIGINT NOT NULL,
    consent_id                        BIGINT NOT NULL,
    created_at                        CHAR(8) NOT NULL,

    CONSTRAINT fk_contract_common_terms_consents_contract
        FOREIGN KEY (contract_id)
        REFERENCES contracts (contract_id)
        ON DELETE RESTRICT ON UPDATE CASCADE

    -- common_terms_consent 은 공통 서비스 소유 (논리적 FK)
    -- CONSTRAINT fk_contract_common_terms_consents_consent
    --     FOREIGN KEY (consent_id)
    --     REFERENCES common_terms_consent (consent_id)
);

CREATE INDEX idx_contract_common_terms_consents_contract ON contract_common_terms_consents (contract_id);
CREATE INDEX idx_contract_common_terms_consents_consent  ON contract_common_terms_consents (consent_id);

COMMIT;
