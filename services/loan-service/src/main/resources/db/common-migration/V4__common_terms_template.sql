-- 공통 약관 템플릿 (common_terms_template) — 공통 계좌 DB(common_db).
-- 수신/여신이 공유하는 약관 마스터. 약관 동의(common_terms_consent)가 본 템플릿을 참조한다.
--
-- 출처: deposit-service V5__full_erd_schema.sql 의 common_terms_template 정의 기준.
-- common_db 는 독립 DB 이므로 타 테이블 FK 는 두지 않는다(값 참조만).
-- 타입 정규화(AI_GUIDELINES): 날짜 CHAR(8) / 시각 TIMESTAMPTZ(3), soft-delete 컬럼 유지.
CREATE TABLE common_terms_template (
    terms_template_id   BIGSERIAL       PRIMARY KEY,
    terms_no            VARCHAR(50)     NOT NULL UNIQUE,
    terms_name          VARCHAR(200)    NOT NULL,
    terms_category_cd   VARCHAR(10)     NOT NULL,
    description         TEXT,
    required_yn         CHAR(1)         NOT NULL DEFAULT 'Y',
    biz_div_cd          VARCHAR(50)     NOT NULL,
    active_yn           CHAR(1)         NOT NULL DEFAULT 'Y',
    created_at          TIMESTAMPTZ(3)  NOT NULL DEFAULT now(),
    created_by          BIGINT,
    updated_at          TIMESTAMPTZ(3)  NOT NULL DEFAULT now(),
    updated_by          BIGINT,
    deleted_at          TIMESTAMPTZ(3),
    deleted_by          BIGINT
);

-- 업무구분·활성별 약관 목록 조회
CREATE INDEX idx_common_terms_template_biz ON common_terms_template (biz_div_cd, active_yn);
