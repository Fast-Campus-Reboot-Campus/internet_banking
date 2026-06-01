-- 공통 상품 마스터 (common_product) — 공통 계좌 DB(common_db).
-- 수신/여신 상품이 공유하는 상품 마스터의 부모. 각 서비스의 상품 서브타입
-- (deposit_product / loan_product)은 자기 DB 에서 product_id 값으로 참조한다(FK 없음).
--
-- 출처: deposit-service V5__full_erd_schema.sql 의 common_product 정의 기준.
-- common_db 는 독립 DB 이므로 타 테이블 FK 는 두지 않는다(값 참조만).
-- 타입 정규화(AI_GUIDELINES): 금액 BIGINT / 금리 bps INT / 날짜 CHAR(8) / 시각 TIMESTAMPTZ(3).
--   - deposit V5 의 created_at/updated_at 누락분은 NOT NULL DEFAULT now() 로 보완.
CREATE TABLE common_product (
    product_id                  BIGSERIAL       PRIMARY KEY,
    product_cd                  VARCHAR(30)     NOT NULL UNIQUE,
    biz_div_cd                  VARCHAR(10)     NOT NULL,
    product_name                VARCHAR(200)    NOT NULL,
    product_type_cd             VARCHAR(20),
    product_description         TEXT,
    target_type_cd              VARCHAR(50),
    channel_cd                  VARCHAR(50),
    currency_cd                 CHAR(3),
    policy_product_yn           CHAR(1),
    min_amount                  BIGINT,
    max_amount                  BIGINT,
    min_period_mo               INT,
    max_period_mo               INT,
    sale_yn                     CHAR(1),
    sale_start_date             CHAR(8),
    sale_end_date               CHAR(8),
    product_brochure_url        VARCHAR(500),
    financial_consumer_act_yn   CHAR(1),
    product_status              VARCHAR(50),
    created_at                  TIMESTAMPTZ(3)  NOT NULL DEFAULT now(),
    created_by                  BIGINT,
    updated_at                  TIMESTAMPTZ(3)  NOT NULL DEFAULT now(),
    updated_by                  BIGINT
);

-- 업무구분·상태별 조회 (수신/여신 상품 목록)
CREATE INDEX idx_common_product_biz ON common_product (biz_div_cd, product_status);
