-- 상환 스케줄 회차에 휴일 보정 여부 플래그 추가.
-- 신규 약정부터 생성 시 due_date 가 비영업일이면 다음 영업일로 이동(`following`), 그 경우 'Y' 로 기록.
-- 이미 생성된 회차는 'N' 유지 (마이그레이션 시 backfill 안 함 — 하드 갱신 금지).

ALTER TABLE repayment_schedule
    ADD COLUMN holiday_adjusted_yn CHAR(1) NOT NULL DEFAULT 'N';
