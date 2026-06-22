# 모니터링 인프라 포트 정리

> Last updated: 2026-06-19
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
| payment-service-b | 8180 | — | Spring Boot, 타행이체 중계 서비스 |
| master-service | 8085 | `MASTER_APP_PORT` | Spring Boot |
| auto-loan-review | 8089 | `AUTO_LOAN_REVIEW_APP_PORT` | Spring Boot, 대출 자동 심사 에이전트 (로컬 docker-compose에서는 호스트 포트 미노출 — 서버 docker-compose에서만 8089 노출) |
| consultation-service | 8087 | `CONSULTATION_APP_PORT` | Python FastAPI, 챗봇 상담 |
| review-ai-gateway | 8088 | `AIGATEWAY_APP_PORT` | Spring Boot, 감사 분석 에이전트 |
| web | 3000 | — | Next.js 프론트엔드 (서버 배포 전용 — 이 포트 때문에 Grafana가 3003으로 변경됨) |

### consultation-service 단독 실행 포트 (standalone docker-compose)

메인 docker-compose와 동시에 실행할 때 포트 충돌을 피하기 위해 아래 포트를 사용한다.

| 컨테이너 | 호스트 포트 | 비고 |
|----------|------------|------|
| consultation-postgres | 5440 | 기본값 5439에서 변경 (`ib-langfuse-db`와 충돌) |
| consultation-kafka | 9093 | 기본값 9092에서 변경 (`ib-kafka`와 충돌) |
| consultation-service | 8087 | 변경 없음 |

---

## 2. 모니터링 스택 포트

### 메트릭 (Prometheus + Grafana)

| 도구 | 포트 | 환경변수 | 용도 |
|------|------|----------|------|
| Prometheus | 9090 | `PROMETHEUS_PORT` | 메트릭 수집, Alert 평가 |
| Grafana | 3000 | `GRAFANA_PORT` | 대시보드 시각화 (서버에서는 3003 — web 포트 충돌로 변경됨) |
| Alertmanager | 9095 | `ALERTMANAGER_PORT` | Alert 라우팅 → Slack 전송. **9095는 호스트 노출 포트** (UI 접속, alert silence 용도). Prometheus가 내부에서 수집할 때는 컨테이너 내부 포트 9093 사용 — §4 참고 |
| Blackbox Exporter | 9115 | `BLACKBOX_PORT` | HTTP 프로브 (Langfuse 헬스체크) |

### 로그 수집 (Loki + Promtail)

| 도구 | 포트 | 환경변수 | 용도 |
|------|------|----------|------|
| Loki | 3100 | `LOKI_PORT` | 로그 저장소 |
| Promtail | — | — | 로그 파일 → Loki 전송 (포트 없음) |

### LLM 추적

| 도구 | 포트 | 환경변수 | 용도 |
|------|------|----------|------|
| Langfuse | 3001 | `LANGFUSE_PORT` | LLM/RAG 트레이싱 UI (서버 docker-compose 기본값은 3002이나 서버 .env에서 3001로 고정) |
| Langfuse DB | 5439 | `LANGFUSE_DB_PORT` | Langfuse 전용 PostgreSQL |
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
| payment-db-b | 5441 | `PAYMENT_DB_B_PORT` |
| master-db | 5436 | `MASTER_DB_PORT` |
| ai-db | 5437 | `AI_DB_PORT` |
| common-db | 5438 | `COMMON_DB_PORT` |
| langfuse-db | 5439 | `LANGFUSE_DB_PORT` |

### Kafka

| 도구 | 포트 | 환경변수 | 비고 |
|------|------|----------|------|
| Kafka (Internal) | 9092 | `KAFKA_PORT` | 내부 브로커 |
| schema-registry | 18081 | — | Confluent Schema Registry (항상 활성, 로컬 전용) |
| kafka-exporter (KFTC) | 9308 | `KAFKA_EXPORTER_KFTC_PORT` | KFTC 클러스터 메트릭 |
| kafka-exporter (BOK) | 9309 | `KAFKA_EXPORTER_BOK_PORT` | BOK 클러스터 메트릭 |
| kafka-exporter (Internal) | 9310 | `KAFKA_EXPORTER_INTERNAL_PORT` | Internal 클러스터 메트릭 |

### 기타

| 도구 | 포트 | 환경변수 | 비고 |
|------|------|----------|------|
| Redis | 6379 | `REDIS_PORT` | 세션 캐시 |
| Windows Exporter | 9182 | `WINDOWS_EXPORTER_PORT` | 호스트 OS 메트릭 (로컬 Windows 전용. 서버는 Linux라 수집 안 됨 — ServiceDown alert에서 제외 처리됨) |
| Node Exporter | 9100 | — | 호스트 OS 메트릭 (서버 Linux 전용. 로컬 Windows에는 없음 — ServiceDown alert에서 제외 처리됨) |

---

## 4. Prometheus Scrape 대상

`infra/prometheus/prometheus.yml` 기준.

> 여기서 포트는 **컨테이너 내부 포트** 기준이다. Prometheus는 Docker 내부 네트워크로 수집하므로 호스트 노출 포트(§1, §2)와 다를 수 있다.

| job_name | 포트 | 경로 | 비고 |
|----------|------|------|------|
| prometheus | 9090 | 기본 | |
| gateway-service | 8080 | `/actuator/prometheus` | |
| customer-service | 8081 | `/actuator/prometheus` | |
| deposit-service | 8082 | `/api/actuator/prometheus` | |
| loan-service | 8083 | `/actuator/prometheus` | |
| payment-service | 8084 | `/actuator/prometheus` | |
| master-service | 8085 | `/actuator/prometheus` | |
| auto-loan-review | 8089 | `/actuator/prometheus` | |
| consultation-service | 8087 | `/metrics` | |
| review-ai-gateway | 8088 | `/actuator/prometheus` | |
| windows-exporter | 9182 | 기본 | 로컬 Windows 전용 — 서버에서 DOWN 표시되나 ServiceDown alert 제외됨 |
| node-exporter | 9100 | 기본 | 서버(Linux) 전용 — 로컬에서 DOWN 표시되나 ServiceDown alert 제외됨 |
| kafka-exporter-kftc | 9308 | 기본 | |
| kafka-exporter-bok | 9309 | 기본 | |
| kafka-exporter-internal | 9310 | 기본 | |
| grafana | 3000 | `/metrics` | |
| loki | 3100 | `/metrics` | |
| alertmanager | 9093 | `/metrics` | 컨테이너 내부 포트. 외부 접속(UI)은 호스트 포트 9095 사용 — §2 참고 |
| monitoring-stack-probe | — | blackbox-exporter 경유 | |

---

## 5. 서버 접속 URL

NCP 서버(`axfulbank.store`)에서 운영 중인 모니터링 도구 접속 주소.

| 도구 | URL | 비고 |
|------|-----|------|
| Grafana | `https://axfulbank.store/grafana/` | 계정: admin / admin |
| Prometheus | `https://axfulbank.store/prometheus/` | 인증 없음 |
| Langfuse | `http://101.79.17.205:3001` | LLM/RAG 트레이싱 (서버 .env: LANGFUSE_PORT=3001) |
| Phoenix | `http://101.79.17.205:6006` | OTel 분산 추적 |

> 로컬에서 실행할 때는 포트로 직접 접속한다 (예: `http://localhost:3000` → Grafana).

---

## 6. 설정 파일 위치

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
| 서버 배포 설정 | `docker-compose.server.yml` |
