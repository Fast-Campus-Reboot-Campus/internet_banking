-- =============================================================================
-- V15: 제재 스크리닝 Hit 검토 워크플로
-- =============================================================================
-- compliance_info의 제재 플래그(is_*_sanctioned_yn)는 "현재 제재대상 여부" 상태일 뿐,
-- 스크리닝 탐지 건별(일치율·Hit유형·검토상태·검토자) 정보를 담지 못한다.
-- 제재대상 Hit 검토 화면(/admin/screening)의 검토 큐를 위해 hit 단위 테이블을 신설한다.

CREATE TABLE sanction_screening_hit (
    sanction_screening_hit_id  BIGINT         GENERATED ALWAYS AS IDENTITY,
    party_id                   BIGINT         NOT NULL,
    hit_type_code              VARCHAR(30)    NOT NULL,   -- OFAC_SDN / KR_PEP / UN / EU
    match_rate                 INT            NOT NULL,   -- 0~100 유사도(%)
    screening_status_code      VARCHAR(20)    NOT NULL,   -- PENDING / CLEARED(동명이인) / CONFIRMED(제재확정)
    reviewer_employee_id       BIGINT,
    review_comment             VARCHAR(500),
    detected_at                TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    reviewed_at                TIMESTAMPTZ(3),
    created_at                 TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by                 BIGINT,
    updated_at                 TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by                 BIGINT,
    deleted_at                 TIMESTAMPTZ(3),
    deleted_by                 BIGINT,
    version                    INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_sanction_screening_hit PRIMARY KEY (sanction_screening_hit_id),
    CONSTRAINT fk_sanction_screening_hit_party FOREIGN KEY (party_id) REFERENCES party (party_id),
    CONSTRAINT chk_sanction_screening_hit_rate CHECK (match_rate BETWEEN 0 AND 100)
);

-- 검토 대기 큐 조회
CREATE INDEX idx_sanction_screening_hit_status
    ON sanction_screening_hit (screening_status_code)
    WHERE deleted_at IS NULL;
