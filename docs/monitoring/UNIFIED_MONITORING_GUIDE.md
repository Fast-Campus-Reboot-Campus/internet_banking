# 통합 모니터링 가이드

> 대상 대시보드: **Monitoring Stack Overview**
> 대상 독자: 개발팀 전원 (모니터링 담당 포함)
> 환경: Docker Compose 기준

---

## 이 가이드는 무엇인가요?

모니터링 도구들(Prometheus, Grafana, Loki, Alertmanager, Langfuse)이 제대로 작동하고 있는지 감시하는 구조입니다.

서비스 장애를 감지하는 모니터링이 정작 죽어있으면 아무것도 알 수 없습니다. 이 가이드는 **"모니터링 자체를 모니터링하는"** 구조와 그 해석 방법을 설명합니다.

---

## 1. 접속 방법

| 도구 | URL | 용도 |
|------|-----|------|
| Grafana | `http://localhost:3000` | 대시보드 |
| Prometheus | `http://localhost:9090` | 메트릭 쿼리 / Alert 상태 |
| Alertmanager | `http://localhost:9095` | Alert 라우팅 / 발송 현황 |
| healthchecks.io | `https://healthchecks.io` | Prometheus 자체 다운 감지 |

대시보드 경로: **Dashboards → Monitoring Stack Overview**

---

## 2. 전체 구조

```
애플리케이션 서비스들
    │ 메트릭 노출 (/actuator/prometheus, /metrics)
    ▼
Prometheus (메트릭 수집 + Alert 조건 평가)
    │
    ├─ Alert 발생 시 ──▶ Alertmanager ──▶ Slack DM
    │
    └─ Watchdog (1분마다 heartbeat) ──▶ healthchecks.io
                                              │
                                     heartbeat 끊기면
                                              ▼
                                         Slack DM
                                    (Prometheus 다운 알림)

로그 수집:  각 서비스 로그 파일 ──▶ Promtail ──▶ Loki

LLM 추적:  LLM/RAG 호출 ──▶ Langfuse
```

**핵심:** Prometheus가 죽으면 내부 Alert가 발송되지 않으므로, healthchecks.io가 외부에서 이를 감지하여 Slack으로 알립니다.

---

## 3. 대시보드 구성

대시보드는 6개 섹션으로 구성됩니다.

| 섹션 | 내용 |
|------|------|
| 전체 요약 | 전체 가용률, DOWN 서비스 수, 활성 Alert 수, Scrape 성공률 |
| Prometheus | UP/DOWN + Scrape 성공률 추이 + 수집 시계열 수 |
| Grafana | UP/DOWN + HTTP 요청률 + 5xx 오류율 |
| Loki | UP/DOWN + 로그 수집 라인수 + 수집 바이트 |
| Alertmanager | UP/DOWN + 알림 전송 건수 + 알림 전송 실패 |
| Langfuse | UP/DOWN + HTTP 프로브 응답시간 |

---

## 4. 전체 요약 섹션 해석

### 전체 서비스 가용률 (%)

전체 Prometheus scrape target 중 현재 응답 중인 비율입니다.

| 색상 | 범위 | 의미 |
|------|------|------|
| 초록 | 90% 이상 | 정상 |
| 노랑 | 70–90% | 일부 서비스 다운 확인 필요 |
| 빨강 | 70% 미만 | 다수 서비스 장애 의심 |

> 앱 서비스들이 실행 중이 아닌 개발 환경에서는 낮게 나올 수 있습니다.

### DOWN 서비스 수

현재 응답하지 않는 scrape target 수입니다.

| 색상 | 범위 | 의미 |
|------|------|------|
| 초록 | 0 | 모든 서비스 정상 |
| 노랑 | 1–2 | 일부 서비스 점검 필요 |
| 빨강 | 3 이상 | 즉시 확인 필요 |

### 활성 Alert 수

현재 FIRING 상태인 Alert 수입니다. 0이 정상입니다.

### 전체 Scrape 성공률

Prometheus가 각 target에서 메트릭을 정상적으로 수집하는 비율입니다. 낮으면 해당 서비스에서 메트릭을 노출하지 않고 있거나 서비스가 다운된 것입니다.

---

## 5. 도구별 섹션 해석

### Prometheus

| 패널 | 정상 | 주의 |
|------|------|------|
| UP/DOWN | UP (초록) | DOWN (빨강) → 메트릭 수집 전체 중단 |
| Scrape 성공률 추이 | 90% 이상 유지 | 급격히 하락 → 다수 서비스 장애 |
| 수집 중인 시계열 수 | 15,000~20,000개 수준 유지 | 급격히 감소 → scrape target 소실 / 급격히 증가 → 카디널리티 폭발 |

> **시계열 수**는 Prometheus가 현재 추적 중인 메트릭 데이터 포인트 수입니다. 서비스가 많아질수록 자연스럽게 증가하며, 급격한 변동이 없으면 정상입니다.

### Grafana

| 패널 | 정상 | 주의 |
|------|------|------|
| UP/DOWN | UP (초록) | DOWN → 대시보드 접근 불가 |
| HTTP 요청률 | 일정 수준 유지 | 0으로 떨어짐 → Grafana 응답 불가 |
| 5xx 오류율 | 0 | 증가 → Grafana 내부 오류 확인 필요 |

### Loki

| 패널 | 정상 | 주의 |
|------|------|------|
| UP/DOWN | UP (초록) | DOWN → 로그 수집 중단 |
| 로그 수집 라인수 | 꾸준히 들어옴 | 0 → Promtail 연결 끊김 또는 로그 파일 경로 오류 |
| 수집 바이트 | 꾸준히 들어옴 | 0 → 위와 동일 |

> Loki가 UP이어도 수집량이 0이면 `LokiNoLogsIngested` Alert가 5분 후 발동합니다.

### Alertmanager

| 패널 | 정상 | 주의 |
|------|------|------|
| UP/DOWN | UP (초록) | DOWN → Alert가 Slack으로 전송 안 됨 |
| 알림 전송 건수 | Alert 발생 시 증가 | 0 지속 → Prometheus 연결 확인 |
| 알림 전송 실패 | 0 | 증가 → Slack Webhook URL 유효성 확인 필요 |

> 알림 전송 패널은 **slack**, **webhook**(healthchecks.io) 두 채널만 표시합니다. slack은 일반 Alert 발송, webhook은 Watchdog heartbeat 전송에 사용됩니다.

### Langfuse

| 패널 | 정상 | 주의 |
|------|------|------|
| UP/DOWN | UP (초록) | DOWN → LLM 추적 데이터 유실 |
| 프로브 응답시간 | 500ms 이하 | 3초 이상 → Langfuse 컨테이너 부하 또는 DB 문제 |

> Langfuse는 Prometheus 형식 메트릭을 제공하지 않으므로 **Blackbox Exporter**가 HTTP 헬스체크(`/api/public/health`)로 생존 여부를 확인합니다. UP/DOWN은 이 프로브 결과입니다.

### Blackbox Exporter

Langfuse처럼 Prometheus 메트릭을 직접 노출하지 않는 서비스의 HTTP 엔드포인트를 주기적으로 호출해 응답 여부를 확인하는 도구입니다.

| 확인 항목 | 방법 |
|----------|------|
| 정상 작동 확인 | `http://localhost:9115` 접속 → 상태 페이지 표시 여부 |
| 프로브 직접 테스트 | `http://localhost:9115/probe?target=http://langfuse:3000/api/public/health&module=http_2xx` |

---

## 6. Alert 목록

### monitoring-infrastructure 그룹

| Alert | 조건 | 심각도 | 의미 |
|-------|------|--------|------|
| Watchdog | 항상 FIRING | info | Prometheus 생존 heartbeat. FIRING이 정상. |
| PrometheusDown | `up{job="prometheus"} == 0` 1분 | critical | Prometheus 다운 |
| GrafanaDown | `up{job="grafana"} == 0` 1분 | critical | Grafana 다운 |
| LokiDown | `up{job="loki"} == 0` 1분 | critical | Loki 다운 |
| AlertmanagerDown | `up{job="alertmanager"} == 0` 1분 | critical | Alertmanager 다운 → Alert Slack 전송 불가 |
| LangfuseDown | probe_success == 0 2분 | warning | Langfuse 응답 없음 |
| LokiNoLogsIngested | 수집량 == 0 5분 | warning | 로그 수집 중단 |
| PrometheusScrapeFailing | 실패율 > 30% 5분 | warning | 다수 scrape target 응답 없음 |

> **Watchdog**은 항상 FIRING 상태여야 합니다. INACTIVE로 바뀌면 Prometheus 또는 Alertmanager 문제입니다.

---

## 7. Slack 알림 해석

Alertmanager가 Slack으로 보내는 메시지 형식:

```
[FIRING] ServiceDown
• ai-service (host.docker.internal:8086): ...
• customer-service (host.docker.internal:8081): ...

[RESOLVED] ServiceDown
• ai-service (host.docker.internal:8086): ...
```

- `[FIRING]` — 현재 문제 발생 중
- `[RESOLVED]` — 문제 해결됨 (서비스 복구)

---

## 8. Dead Man's Switch (healthchecks.io)

Prometheus 자체가 다운되면 내부 Alert가 발송되지 않습니다. 이를 보완하기 위해 healthchecks.io를 사용합니다.

**동작 원리:**
1. Alertmanager가 1분마다 healthchecks.io에 핑 전송
2. 핑이 끊기면 (= Prometheus/Alertmanager 다운) healthchecks.io가 Slack으로 알림 전송

**정상 상태 확인:**
- `https://healthchecks.io` 접속 → `My First Check` 체크가 녹색이면 정상
- "Last Ping: X seconds/minutes ago" — 최근 핑 수신 시각

**이상 감지:**
- 체크가 빨간색으로 바뀌면 → Prometheus 또는 Alertmanager 다운 의심
- `docker-compose up -d prometheus alertmanager` 로 재시작

---

## 9. 알림이 오지 않을 때 체크리스트

1. **Prometheus 확인**: `http://localhost:9090/alerts` → Watchdog이 FIRING 상태인지
2. **Alertmanager 확인**: `http://localhost:9095` → alert가 수신되어 있는지
3. **Slack Webhook 확인**: `.env`의 `SLACK_WEBHOOK_URL` 값이 유효한지
4. **healthchecks.io 확인**: 체크가 녹색인지 (Prometheus가 살아있는지)
5. **컨테이너 상태 확인**: `docker ps | grep ib-alertmanager`

---

## 10. 관련 가이드

| 문서 | 내용 |
|------|------|
| [DASHBOARD_GUIDE.md](DASHBOARD_GUIDE.md) | 전체 서비스 대시보드 해석 |
| [KAFKA_PAYMENT_GUIDE.md](KAFKA_PAYMENT_GUIDE.md) | Kafka 결제 모니터링 |
| [LLM_RAG_MONITORING_GUIDE.md](LLM_RAG_MONITORING_GUIDE.md) | LLM/RAG 모니터링 (Langfuse + Phoenix) |
| [ML_LOAN_REVIEW_GUIDE.md](ML_LOAN_REVIEW_GUIDE.md) | ML 대출 심사 모니터링 |
| [CHATBOT_GUIDE.md](CHATBOT_GUIDE.md) | 챗봇 상담 모니터링 |
