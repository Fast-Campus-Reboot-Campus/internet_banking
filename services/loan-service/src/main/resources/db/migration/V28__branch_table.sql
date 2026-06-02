-- 지점 마스터. MVP는 시드 3개로 시작.
CREATE TABLE branch (
    branch_id   VARCHAR(10)  NOT NULL,
    branch_name VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_branch PRIMARY KEY (branch_id)
);

INSERT INTO branch (branch_id, branch_name) VALUES
    ('0001', '강남지점'),
    ('0002', '종로지점'),
    ('HQ',   '본사');
