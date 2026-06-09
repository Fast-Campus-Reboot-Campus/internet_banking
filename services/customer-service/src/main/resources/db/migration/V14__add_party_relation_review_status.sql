-- =============================================================================
-- V14: 관계자관계 검토상태(대리인 위임장 검토) 도입
-- =============================================================================
-- 대리인 위임장 검토 화면(/admin/agent)은 신규 등록된 관계(대리인 위임 등)를 직원이 검토해
-- 승인/거절하는 큐가 필요하다. 기존 party_relation에는 검토상태 컬럼이 없어 대기목록을 만들 수 없었다.
-- relation_review_status_code(PENDING/APPROVED/REJECTED)를 추가한다.
--
-- nullable로 둔다: 기존 관계(주주·대표이사 등 시드 데이터)는 NULL로 남아 검토 큐(='PENDING')에서 제외된다.
-- 신규 등록 관계는 서비스 레이어에서 PENDING으로 생성한다.

ALTER TABLE party_relation ADD COLUMN relation_review_status_code VARCHAR(20);
