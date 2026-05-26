-- ai-db 초기화: pgvector 확장 활성화
-- docker-entrypoint-initdb.d 로 마운트되어 ai-db 컨테이너 최초 기동 시 1회 실행됨
-- (볼륨 ai-db-data 가 비어있을 때만 자동 실행되므로, 이미 생성된 DB 에서는 수동 실행 필요)

CREATE EXTENSION IF NOT EXISTS vector;
