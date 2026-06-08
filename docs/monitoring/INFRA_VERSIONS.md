# 모니터링 인프라 버전 및 포트 정리

> Last updated: 2026-06-07
> Docker Compose 기준. 포트 변수 전체 목록은 루트 `.env.sample` 참고.

---

## 1. 애플리케이션 서비스 포트

| 서비스 | 포트 | 환경변수 | 비고 |
|--------|------|----------|------|
| gateway-service | 8080 | `GATEWAY_APP_PORT` | Spring Boot, 모든 외부 요청 진입점 |
| customer-service | 8081 | `CUSTOMER_APP_PORT` | Spring Boot |
| deposit-service | 8082 | `DEPOSIT_APP_PORT` | Spring Boot |
| loan-service | 8083 | `LOAN_APP_PORT` | Spring Boot |
| payment-service | 8084 | `PAYMENT_APP_PORT` | Spring Boot |
| master-service | 8085 | `MASTER_APP_PORT` | Spring Boot |
| auto-loan-review | 8086 | `AI_APP_PORT` | Spring Boot, 대출 자동 심사 에이전트 |
| consultation-service | 8087 | `CONSULTATION_APP_PORT` | Python FastAPI, 챗봇 상담 |
| review-ai-gateway | 8088 | `AIGATEWAY_APP_PORT` | Spring Boot, 감사 분석 에이전트 |
| inference-server | 8090 | — | Python, auto-loan-review에서 호출 (별도 기동) |

> **ai-service**: `services/ai-service/` 디렉토리 존재 (Spring Boot, `com.bank.ai` 패키지, RAG 기능 포함)하지만 Dockerfile 없고 docker-compose 미통합. 현재 실행 불가. 담당 팀원 통합 필요.

### consultation-service 단독 실행 포트 (standalone docker-compose)

메인 docker-compose와 동시에 실행할 때 포트 충돌을 피하기 위해 아래 포트를 사용한다.

| 컨테이너 | 호스트 포트 | 비고 |
|----------|------------|------|
| consultation-postgres | 5440 | 기본값 5439에서 변경 (`ib-common-db`와 충돌) |
| consultation-kafka | 9093 | 기본값 9092에서 변경 (`ib-kafka`와 충돌) |
| consultation-service | 8087 | 변경 없음 |

---

## 2. 모니터링 스택 포트

### 메트릭 (Prometheus + Grafana)

| 도구 | 포트 | 환경변수 | 용도 |
|------|------|----------|------|
| Prometheus | 9090 | `PROMETHEUS_PORT` | 메트릭 수집, Alert 평가 |
| Grafana | 3000 | `GRAFANA_PORT` | 대시보드 시각화 |
| Alertmanager | 9095 | `ALERTMANAGER_PORT` | Alert 라우팅 → Slack 전송 |
| Blackbox Exporter | 9115 | `BLACKBOX_PORT` | HTTP 프로브 (Langfuse 헬스체크) |

### 로그 수집 (Loki + Promtail)

| 도구 | 포트 | 환경변수 | 용도 |
|------|------|----------|------|
| Loki | 3100 | `LOKI_PORT` | 로그 저장소 |
| Promtail | — | — | 로그 파일 → Loki 전송 (포트 없음) |

### LLM 추적

| 도구 | 포트 | 환경변수 | 용도 |
|------|------|----------|------|
| Langfuse | 3001 | `LANGFUSE_PORT` | LLM/RAG 트레이싱 UI |
| Langfuse DB | 5438 | `LANGFUSE_DB_PORT` | Langfuse 전용 PostgreSQL |
| Phoenix (HTTP) | 6006 | `PHOENIX_PORT` | OTel 분산 추적 UI |
| Phoenix (gRPC) | 4317 | `PHOENIX_GRPC_PORT` | OTel 트레이스 수신 |

---

## 3. 인프라 포트

### Database

| 서비스 | 포트 | 환경변수 |
|--------|------|----------|
| customer-db | 5432 | `CUSTOMER_DB_PORT` |
| deposit-db | 5433 | `DEPOSIT_DB_PORT` |
| loan-db | 5434 | `LOAN_DB_PORT` |
| payment-db | 5435 | `PAYMENT_DB_PORT` |
| master-db | 5436 | `MASTER_DB_PORT` |
| ai-db | 5437 | `AI_DB_PORT` |
| langfuse-db | 5438 | `LANGFUSE_DB_PORT` |
| common-db | 5439 | `COMMON_DB_PORT` |

### Kafka

| 도구 | 포트 | 환경변수 | 비고 |
|------|------|----------|------|
| Kafka (Internal) | 9092 | `KAFKA_PORT` | 내부 브로커 |
| kafka-exporter (KFTC) | 9308 | `KAFKA_EXPORTER_KFTC_PORT` | KFTC 클러스터 메트릭 |
| kafka-exporter (BOK) | 9309 | `KAFKA_EXPORTER_BOK_PORT` | BOK 클러스터 메트릭 |
| kafka-exporter (Internal) | 9310 | `KAFKA_EXPORTER_INTERNAL_PORT` | Internal 클러스터 메트릭 |

### 기타

| 도구 | 포트 | 환경변수 | 비고 |
|------|------|----------|------|
| Redis | 6379 | `REDIS_PORT` | 세션 캐시 |
| Windows Exporter | 9182 | `WINDOWS_EXPORTER_PORT` | 호스트 OS 메트릭 |

---

## 4. Prometheus Scrape 대상

`infra/prometheus/prometheus.yml` 기준.

| job_name | 포트 | 경로 |
|----------|------|------|
| prometheus | 9090 | 기본 |
| gateway-service | 8080 | `/actuator/prometheus` |
| customer-service | 8081 | `/actuator/prometheus` |
| deposit-service | 8082 | `/actuator/prometheus` |
| loan-service | 8083 | `/actuator/prometheus` |
| payment-service | 8084 | `/actuator/prometheus` |
| master-service | 8085 | `/actuator/prometheus` |
| auto-loan-review | 8086 | `/actuator/prometheus` |
| consultation-service | 8087 | `/metrics` |
| review-ai-gateway | 8088 | `/actuator/prometheus` |
| windows-exporter | 9182 | 기본 |
| kafka-exporter-kftc | 9308 | 기본 |
| kafka-exporter-bok | 9309 | 기본 |
| kafka-exporter-internal | 9310 | 기본 |
| grafana | 3000 | `/metrics` |
| loki | 3100 | `/metrics` |
| alertmanager | 9093 | `/metrics` |
| monitoring-stack-probe | — | blackbox-exporter 경유 |

---

## 5. 설정 파일 위치

| 도구 | 설정 파일 |
|------|-----------|
| Prometheus | `infra/prometheus/prometheus.yml` |
| Prometheus Alert 규칙 | `infra/prometheus/alerts.yml` |
| Alertmanager | `infra/alertmanager/alertmanager.yml` |
| Blackbox Exporter | `infra/blackbox/blackbox.yml` |
| Grafana datasource | `infra/grafana/provisioning/datasources/datasource.yml` |
| Grafana dashboard 목록 | `infra/grafana/provisioning/dashboards/dashboard.yml` |
| Loki | `infra/loki/loki-config.yml` |
| Promtail | `infra/promtail/promtail-config.yml` |
| 포트 변수 | `.env.sample` |
