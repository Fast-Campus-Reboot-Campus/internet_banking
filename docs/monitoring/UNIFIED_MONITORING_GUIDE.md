# 통합 모니터링 가이드

> 대상 대시보드: **Monitoring Stack Overview**
> 대상 독자: 개발팀 전원 (모니터링 담당 포함)
> 환경: Docker Compose 기준

---

## 이 가이드는 무엇인가요?

모니터링 도구들(Prometheus, Grafana, Loki, Alertmanager, Langfuse)이 제대로 작동하고 있는지 감시하는 구조입니다.

서비스 장애를 감지하는 모니터링이 정작 죽어있으면 아무것도 알 수 없습니다. 이 가이드는 **"모니터링 자체를 모니터링하는"** 구조와 그 해석 방법을 설명합니다.

---

## 목차

- [1. 접속 방법](#1-접속-방법)
- [2. 전체 구조](#2-전체-구조)
- [3. 대시보드 구성](#3-대시보드-구성)
- [4. 전체 요약 섹션 해석](#4-전체-요약-섹션-해석)
- [5. 도구별 섹션 해석](#5-도구별-섹션-해석)
  - [Prometheus](#prometheus)
  - [Grafana](#grafana)
  - [Loki](#loki)
  - [Alertmanager](#alertmanager)
  - [Langfuse](#langfuse)
- [6. Alert 목록](#6-alert-목록)
- [7. Slack 알림 해석](#7-slack-알림-해석)
- [8. Dead Man's Switch](#8-dead-mans-switch-healthchecksio)
- [9. 알림이 오지 않을 때 체크리스트](#9-알림이-오지-않을-때-체크리스트)
- [10. 관련 가이드](#10-관련-가이드)

---

## 1. 접속 방법

| 도구 | URL | 용도 |
|------|-----|------|
| Grafana | `http://localhost:3000` | 대시보드 |
| Prometheus | `http://localhost:9090` | 메트릭 쿼리 / Alert 상태 |
| Alertmanager | `http://localhost:9095` | Alert 라우팅 / 발송 현황 |
| healthchecks.io | `https://healthchecks.io` | Prometheus 자체 다운 감지 |

대시보드 경로: **Dashboards → Monitoring Stack Overview**

### 최초 실행 전 필수 설정

Slack Webhook URL 등 민감 정보가 포함된 파일은 git에 올라가지 않습니다. 아래 순서대로 설정한 뒤 `docker compose up`을 실행하세요.

1. `.env.sample` → `.env` 복사
2. `infra/alertmanager/alertmanager.yml.sample` → `infra/alertmanager/alertmanager.yml` 복사
3. `.env`에서 아래 항목을 본인 값으로 직접 채웁니다:
   - `SLACK_WEBHOOK_URL` — Slack Incoming Webhooks에서 발급
   - `HEALTHCHECKS_PING_URL` — healthchecks.io 가입 후 체크 생성 시 발급
   - DB 비밀번호 (`*_DB_PASSWORD`) — 운영 환경에서만 변경 필요, 로컬은 기본값 사용
   - `LANGFUSE_SECRET_KEY`, `LANGFUSE_PUBLIC_KEY` — Langfuse UI → Settings → API Keys에서 발급

> **주의**: `alertmanager.yml`이 없으면 `docker compose up` 시 Alertmanager 컨테이너가 시작에 실패합니다.

### 설정 완료 후 정상 동작 확인

`docker compose up` 이후 아래 항목을 순서대로 확인하세요.

| 확인 항목 | 정상 상태 |
|-----------|-----------|
| `http://localhost:9090` | Prometheus 접속됨 |
| `http://localhost:9090/targets` | 수집 대상 목록에 `State: UP` 항목이 보임 |
| `http://localhost:9095` | Alertmanager 접속됨 |
| `http://localhost:9095/#/alerts` | `Watchdog` alert가 `FIRING` 상태로 표시됨 (정상) |
| `http://localhost:3000` | Grafana 접속됨 (admin / admin) |

> `Watchdog`이 `FIRING`이면 Prometheus → Alertmanager → healthchecks.io 연결이 모두 정상입니다.
> `Watchdog`이 보이지 않으면 `alertmanager.yml` 설정을 다시 확인하세요.

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

**용어:**
- **Scrape** — Prometheus가 각 서비스의 메트릭 엔드포인트를 주기적으로 호출해서 데이터를 가져오는 행위. 15초마다 수행됨.
- **Scrape Target** — Prometheus가 메트릭을 수집하는 대상 서비스. customer-service, grafana, loki 등 각각이 하나의 target.
- **Alert** — Prometheus가 정의된 조건(예: 서비스 다운, 오류율 급증)을 감지했을 때 발생시키는 알림.

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

- **FIRING** — Alert 조건이 충족되어 현재 발동 중인 상태.
- **PENDING** — 조건은 충족됐지만 `for` 시간이 지나지 않아 아직 발동 전인 상태.

### 전체 Scrape 성공률

Prometheus가 각 target에서 메트릭을 정상적으로 수집하는 비율입니다. 낮으면 해당 서비스가 다운됐거나 메트릭을 노출하지 않는 것입니다.

---

## 5. 도구별 섹션 해석

### Prometheus

**용어:**
- **시계열 (Time Series)** — 하나의 메트릭을 시간 순서대로 기록한 데이터. 예: customer-service의 HTTP 요청 수를 15초마다 기록한 것이 하나의 시계열.
- **수집 중인 시계열 수** — Prometheus가 현재 추적 중인 시계열의 총 개수. 서비스와 메트릭이 많을수록 늘어남.

| 패널 | 정상 | 주의 |
|------|------|------|
| UP/DOWN | UP (초록) | DOWN (빨강) → 메트릭 수집 전체 중단 |
| Scrape 성공률 추이 | 90% 이상 유지 | 급격히 하락 → 다수 서비스 장애 |
| 수집 중인 시계열 수 | 15,000~20,000개 수준 유지 | 급격히 감소 → scrape target 소실 / 급격히 증가 → 카디널리티 폭발 |

---

### Grafana

**용어:**
- **HTTP 요청률 (req/s)** — 초당 Grafana에 들어오는 HTTP 요청 수. 대시보드 조회, API 호출 등이 포함됨.
- **5xx 오류율** — Grafana가 500번대 오류(서버 내부 오류)를 반환한 비율.

| 패널 | 정상 | 주의 |
|------|------|------|
| UP/DOWN | UP (초록) | DOWN → 대시보드 접근 불가 |
| HTTP 요청률 | 일정 수준 유지 | 0으로 떨어짐 → Grafana 응답 불가 |
| 5xx 오류율 | 0 | 증가 → Grafana 내부 오류 확인 필요 |

---

### Loki

**용어:**
- **로그 수집 라인수 (lines/s)** — 초당 Loki에 수집되는 로그 라인 수. Promtail이 각 서비스의 로그 파일을 읽어 전송함.
- **수집 바이트 (bytes/s)** — 초당 수집되는 로그 데이터 크기.

| 패널 | 정상 | 주의 |
|------|------|------|
| UP/DOWN | UP (초록) | DOWN → 로그 수집 중단 |
| 로그 수집 라인수 | 꾸준히 들어옴 | 0 → Promtail 연결 끊김 또는 로그 파일 경로 오류 |
| 수집 바이트 | 꾸준히 들어옴 | 0 → 위와 동일 |

> Loki가 UP이어도 수집량이 0이면 `LokiNoLogsIngested` Alert가 5분 후 발동합니다.

---

### Alertmanager

**용어:**
- **slack** — Alertmanager가 Slack DM으로 alert를 전송하는 채널.
- **webhook** — Alertmanager가 healthchecks.io로 Watchdog heartbeat를 전송하는 채널.
- **알림 전송 실패** — Alertmanager가 Slack 또는 healthchecks.io로 전송을 시도했지만 실패한 건수.

| 패널 | 정상 | 주의 |
|------|------|------|
| UP/DOWN | UP (초록) | DOWN → Alert가 Slack으로 전송 안 됨 |
| 알림 전송 건수 | Alert 발생 시 증가 | 0 지속 → Prometheus 연결 확인 |
| 알림 전송 실패 | 0 | 증가 → Slack Webhook URL 유효성 확인 필요 |

> 알림 전송 패널은 **slack**, **webhook** 두 채널만 표시합니다.

---

### Langfuse

**용어:**
- **Blackbox Exporter** — HTTP 엔드포인트를 직접 호출해 응답 여부를 확인하는 도구. Langfuse처럼 Prometheus 메트릭을 노출하지 않는 서비스 감시에 사용.
- **probe_success** — Blackbox Exporter가 대상을 호출했을 때 성공 여부. 1이면 정상, 0이면 응답 없음.
- **프로브 응답시간** — Blackbox Exporter가 Langfuse 헬스체크 엔드포인트를 호출해서 응답받는 데 걸린 시간.

| 패널 | 정상 | 주의 |
|------|------|------|
| UP/DOWN | UP (초록) | DOWN → LLM 추적 데이터 유실 |
| 프로브 응답시간 | 500ms 이하 | 3초 이상 → Langfuse 컨테이너 부하 또는 DB 문제 |

> Langfuse는 Prometheus 형식 메트릭을 제공하지 않으므로 Blackbox Exporter가 `/api/public/health`를 주기적으로 호출해 생존 여부를 확인합니다.

---

## 6. Alert 목록

### monitoring-infrastructure 그룹

| Alert | 조건 | 심각도 | 의미 |
|-------|------|--------|------|
| Watchdog | 항상 FIRING | info | Prometheus 생존 heartbeat. FIRING이 정상. |
| PrometheusDown | `up{job="prometheus"} == 0` 1분 | critical | Prometheus가 자기 자신을 scrape하지 못하는 경우만 감지. Prometheus 프로세스 자체가 죽으면 이 alert는 발동하지 않음 — 그 역할은 Dead Man's Switch(healthchecks.io)가 담당 |
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

**용어:**
- **Dead Man's Switch** — "내가 살아있음"을 주기적으로 외부에 알리고, 신호가 끊기면 장애로 감지하는 패턴. Prometheus 자체 다운을 감지하기 위해 사용.

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

**현재 알림 수단 및 개선 여지:**

현재는 healthchecks.io의 이메일 integration만 등록되어 있어, Alertmanager 장애 시 **이메일로만** 알림을 받습니다.

healthchecks.io는 Slack integration도 지원합니다. 이를 추가하면 Alertmanager를 거치지 않고 healthchecks.io가 Slack으로 직접 알림을 보낼 수 있어 더 빠르게 감지할 수 있습니다.

> 개선 방법: healthchecks.io → Integrations → Slack 추가 → Slack webhook URL 등록

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
