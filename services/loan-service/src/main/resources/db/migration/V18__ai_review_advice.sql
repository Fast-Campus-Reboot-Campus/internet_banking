-- AI 심사 조언 테이블.
-- 편향 검증 에이전트(BIAS_CHECK) 및 향후 LLM 보조 기능(SUMMARY, REJECTION_LETTER 등)의
-- 결과를 append-only 로 저장한다. 결정권은 사람에게 있으며 이 테이블은 보조 기록만 한다.
-- severity_cd: BLOCKED(명백한 규정위반) / HIGH / MEDIUM / LOW / NONE / null(비편향 advice 유형)

CREATE TABLE ai_review_advice (
    advice_id       BIGSERIAL     PRIMARY KEY,
    rev_id          BIGINT        NOT NULL REFERENCES loan_review(rev_id),
    advice_type_cd  VARCHAR(40)   NOT NULL,
    severity_cd     VARCHAR(20),
    advice_body     TEXT          NOT NULL,
    model           VARCHAR(80),
    model_version   VARCHAR(40),
    prompt_hash     CHAR(64),
    input_token     INT,
    output_token    INT,
    latency_ms      INT,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by      BIGINT
);

CREATE INDEX ix_ai_review_advice_rev_type_created
    ON ai_review_advice (rev_id, advice_type_cd, created_at DESC);
