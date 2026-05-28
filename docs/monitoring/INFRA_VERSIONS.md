# 모니터링 인프라 버전 관리

> Last updated: 2026-05-28
> 로컬 설치 기준. Docker 사용 시 docker-compose.yml 참고.

---

## 현재 사용 버전

| 도구 | 버전 | 설치 위치 (로컬) | 포트 |
|---|---|---|---|
| Prometheus | 3.12.0-rc.0 | `C:\bank_project\prometheus-3.12.0-rc.0.windows-amd64` | 9090 |
| Grafana | 13.0.1 | Windows MSI 설치 | 3000 |

### 애플리케이션 서비스 포트 (로컬 직접 실행)

| 서비스 | 포트 | 비고 |
|--------|------|------|
| gateway-service | 8080 | Spring Boot |
| customer-service | 8081 | Spring Boot |
| deposit-service | 8082 | Spring Boot |
| loan-service | 8083 | Spring Boot |
| payment-service | 8084 | Spring Boot (로컬 IDE, application-local.yml) |
| master-service | 8085 | Spring Boot |
| ai-service / auto-loan-review | 8086 | Spring Boot |
| consultation-service | **8087** | Python FastAPI |
| review-ai-gateway / advisory-service | 8088 | Spring Boot |
| inference-server (ai-service) | 8090 | Python (별도 기동) |
| Kafka UI (payment docker) | 8090 | Docker, `${KAFKA_UI_PORT:-8090}` |

> 포트 변수 전체 목록: 루트 `.env.sample` 참고

---

### Kafka 모니터링 컴포넌트 (Docker)

> `services/payment-service/docker-compose-kafka.yml` 에서 함께 기동됨.

| 도구 | 이미지 | 포트 | 담당 클러스터 |
|------|--------|------|--------------|
| kafka-exporter (KFTC) | `danielqsj/kafka-exporter:latest` | 9308 | kftc-kafka:29092 |
| kafka-exporter (BOK) | `danielqsj/kafka-exporter:latest` | 9309 | bok-kafka:29093 |
| kafka-exporter (Internal) | `danielqsj/kafka-exporter:latest` | 9310 | internal-kafka:29094 |
| Mock Responder | `python:3.12-alpine` | — | KFTC + BOK 자동 응답 (테스트 전용) |

> Mock Responder는 `docker-compose-mock.yml` 로 별도 기동. `SUCCESS_RATE` 환경변수로 성공률 조정 가능.

---

## 설정 파일 위치

| 도구 | 설정 파일 |
|---|---|
| Prometheus | `infra/prometheus/prometheus.yml` |
| Prometheus 알림 규칙 | `infra/prometheus/alerts.yml` |
| Grafana datasource | `infra/grafana/provisioning/datasources/datasource.yml` |
| Grafana dashboard | `infra/grafana/provisioning/dashboards/dashboard.yml` |
| Grafana dashboard JSON (서비스 현황) | `infra/grafana/provisioning/dashboards/internet-banking.json` |
| Grafana dashboard JSON (Kafka Payment) | `infra/grafana/provisioning/dashboards/kafka-payment.json` |
| Grafana dashboard JSON (챗봇 상담) | `infra/grafana/provisioning/dashboards/chatbot.json` |

---

## 버전 업그레이드 시

1. 본 문서의 버전 정보 업데이트
2. `docker-compose.yml` 이미지 태그도 함께 업데이트
