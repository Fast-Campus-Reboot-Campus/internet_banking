-- =============================================================================
-- V16: 중복고객 검토 케이스
-- =============================================================================
-- party_person.ci_value/이름+생년월일로 중복 후보를 탐지할 수는 있으나, 검토 결과(복본 확정/별개 확정)를
-- 담을 곳이 없다. 중복고객 검토 화면(/admin/duplicates)의 검토 큐를 위해 케이스 테이블을 신설한다.
-- 탐지 로직(배치/가입 시)이 신규 party와 기존 party를 묶어 PENDING 케이스로 적재하고, 직원이 복본/별개를 판정한다.

CREATE TABLE duplicate_review_case (
    duplicate_review_case_id  BIGINT         GENERATED ALWAYS AS IDENTITY,
    new_party_id              BIGINT         NOT NULL,   -- 신규(또는 후보) party
    existing_party_id         BIGINT         NOT NULL,   -- 기존 party
    match_type_code           VARCHAR(20)    NOT NULL,   -- CI / NAME_BIRTH
    review_status_code        VARCHAR(20)    NOT NULL,   -- PENDING / DUPLICATE(복본확정) / DISTINCT(별개확정)
    reviewer_employee_id      BIGINT,
    review_comment            VARCHAR(500),
    detected_at               TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    reviewed_at               TIMESTAMPTZ(3),
    created_at                TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                BIGINT,
    updated_at                TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by                BIGINT,
    deleted_at                TIMESTAMPTZ(3),
    deleted_by                BIGINT,
    version                   INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_duplicate_review_case PRIMARY KEY (duplicate_review_case_id),
    CONSTRAINT fk_duplicate_review_case_new      FOREIGN KEY (new_party_id)      REFERENCES party (party_id),
    CONSTRAINT fk_duplicate_review_case_existing FOREIGN KEY (existing_party_id) REFERENCES party (party_id),
    CONSTRAINT chk_duplicate_review_case_distinct_parties CHECK (new_party_id <> existing_party_id)
);

-- 검토 대기 큐 조회
CREATE INDEX idx_duplicate_review_case_status
    ON duplicate_review_case (review_status_code)
    WHERE deleted_at IS NULL;
