# 모니터링 인프라 버전 관리

> Last updated: 2026-05-22
> 로컬 설치 기준. Docker 사용 시 docker-compose.yml 참고.

---

## 현재 사용 버전

| 도구 | 버전 | 설치 위치 (로컬) | 포트 |
|---|---|---|---|
| Prometheus | 3.12.0-rc.0 | `C:\bank_project\prometheus-3.12.0-rc.0.windows-amd64` | 9090 |
| Grafana | 13.0.1 | Windows MSI 설치 | 3000 |

---

## 설정 파일 위치

| 도구 | 설정 파일 |
|---|---|
| Prometheus | `infra/prometheus/prometheus.yml` |
| Grafana datasource | `infra/grafana/provisioning/datasources/datasource.yml` |
| Grafana dashboard | `infra/grafana/provisioning/dashboards/dashboard.yml` |
| Grafana dashboard JSON | `infra/grafana/provisioning/dashboards/internet-banking.json` |

---

## 버전 업그레이드 시

1. 본 문서의 버전 정보 업데이트
2. `docker-compose.yml` 이미지 태그도 함께 업데이트
