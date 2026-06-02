-- 공통 약관 동의 (common_terms_consent) — 공통 계좌 DB(common_db).
-- 고객의 약관 동의 이력. 공통 약관 템플릿(common_terms_template)을 참조한다.
--
-- 출처: deposit-service V5__full_erd_schema.sql 의 common_terms_consent 정의 기준.
-- common_db 내부 FK(common_terms_template)만 유지, 타 서비스 소유(customer) FK 는 제거(값 참조).
-- 타입 정규화(AI_GUIDELINES): 날짜 CHAR(8) / 시각 TIMESTAMPTZ(3).
--   - retention_until VARCHAR(8) -> CHAR(8)
--   - created_at/updated_at NOT NULL DEFAULT now()
-- PK 는 deposit V5 그대로 복합키 (consent_id, customer_id).
CREATE TABLE common_terms_consent (
    consent_id          BIGSERIAL       NOT NULL,
    customer_id         BIGINT          NOT NULL,
    terms_template_id   BIGINT          NOT NULL,
    biz_div_cd          VARCHAR(10)     NOT NULL,
    consent_target_id   BIGINT,
    consent_status_cd   VARCHAR(10)     NOT NULL,
    agreed_yn           CHAR(1)         NOT NULL,
    agreed_at           CHAR(8)         NOT NULL,
    consent_method_cd   VARCHAR(10)     NOT NULL,
    consent_tool        VARCHAR(500),
    signed_doc_url      VARCHAR(500),
    signed_doc_hash     VARCHAR(64),
    client_ip           INET,
    withdrawn_yn        CHAR(1)         NOT NULL DEFAULT 'N',
    withdrawn_at        TIMESTAMPTZ(3),
    withdrawn_reason    VARCHAR(500),
    retention_until     CHAR(8),
    created_at          TIMESTAMPTZ(3)  NOT NULL DEFAULT now(),
    created_by          BIGINT,
    updated_at          TIMESTAMPTZ(3)  NOT NULL DEFAULT now(),
    updated_by          BIGINT,
    deleted_at          TIMESTAMPTZ(3),
    deleted_by          BIGINT,
    PRIMARY KEY (consent_id, customer_id),
    CONSTRAINT fk_common_terms_consent_template
        FOREIGN KEY (terms_template_id) REFERENCES common_terms_template (terms_template_id)
);

-- 고객별 동의 이력 조회
CREATE INDEX idx_common_terms_consent_customer ON common_terms_consent (customer_id);
-- 템플릿별 동의 조회
CREATE INDEX idx_common_terms_consent_template ON common_terms_consent (terms_template_id);
