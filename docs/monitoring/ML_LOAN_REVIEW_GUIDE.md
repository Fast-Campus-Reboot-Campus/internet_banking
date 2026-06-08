# ML 대출 심사 모니터링 가이드

> 대상 대시보드: **ML 대출 심사 모니터링**
> 대상 독자: 개발팀 전원
> 환경: 로컬 직접 설치 기준 (`ai-service` + Prometheus + Grafana)

> **⚠️ 포트 충돌 주의**: `ai-service`는 기본 포트 8086을 사용하는데, 메인 `docker-compose.yml`의 `auto-loan-review`도 동일한 8086 포트를 점유합니다. 두 서비스를 동시에 실행하면 충돌합니다. `ai-service`를 로컬로 실행할 때는 `docker compose stop auto-loan-review` 후 진행하거나, `AI_APP_PORT`를 다른 번호로 변경하세요.
> **참고**: `ai-service`(`services/ai-service/`)는 현재 docker-compose에 미통합 상태입니다. 로컬 Gradle 실행(`./gradlew :services:ai-service:bootRun`)으로만 기동 가능합니다.

---

## 이 가이드는 무엇인가요?

고객이 대출을 신청하면 ai-service는 ML 모델 서버(inference-server)에 심사 요청을 보내고, APPROVE / CONDITIONAL / REJECT 중 하나의 결정을 반환합니다. 이 대시보드는 그 과정이 지금 정상적으로 작동하고 있는지, 모델이 이상한 결과를 내고 있지는 않은지를 한눈에 확인할 수 있도록 만들어졌습니다.

---

## 1. 전체 흐름

```
대출 신청 요청
      │
      ▼
  ai-service (8086)
      │  입력 데이터 검증 및 전처리
      │  결측치 / 이상치 메트릭 기록
      │
      ▼
inference-server (8090)
      │  ML 모델 예측 실행
      │  APPROVE / CONDITIONAL / REJECT 반환
      │
      ▼
  ai-service
      │  응답시간 기록 (Timer)
      │  결정 결과 / 신뢰도 기록 (Counter, DistributionSummary)
      │
      ▼
Prometheus (9090) ← 15초마다 메트릭 수집
      │
      ▼
Grafana (3000) ← 30초마다 대시보드 갱신
```

inference-server에 연결이 안 되거나 모델이 오류를 반환하면 에러 메트릭이 기록되고 알림이 발생합니다.

---

## 2. 접속 방법

| 도구 | URL | 계정 |
|------|-----|------|
| Grafana (대시보드) | `http://localhost:3000` | admin / admin |
| Prometheus (알림 확인) | `http://localhost:9090/alerts` | 없음 |
| ai-service 메트릭 원본 | `http://localhost:8086/actuator/prometheus` | 없음 |

대시보드 경로: Grafana 접속 → 왼쪽 메뉴 **Dashboards** → **ML 대출 심사 모니터링**

> ai-service, inference-server, Prometheus가 모두 실행 중이어야 데이터가 표시됩니다.

---

## 3. 대시보드 구성

대시보드는 총 6개 섹션으로 구성됩니다.

| 섹션 | 무엇을 보는가 |
|------|--------------|
| 심사 처리량 · 결정 분포 | 지금 얼마나 많은 심사가 처리되고 있는지, 결과 분포는 어떤지 |
| 응답시간 (p50 / p95) | 심사에 걸리는 시간이 얼마나 되는지 |
| 모델 예측 신뢰도 (Score) | 모델이 결정에 얼마나 확신하는지 |
| 데이터 드리프트 감지 | 입력 데이터 분포가 정상 범위 안에 있는지 |
| 모델 버전별 결과 비교 | 새 모델 배포 후 결과가 달라졌는지 |
| 데이터 품질 (결측치 · 이상치) | 잘못된 입력 데이터가 들어오고 있지는 않은지 |

---

## 4. 심사 처리량 · 결정 분포 섹션

**용어:**
- **Inference (추론)** — ML 모델이 입력 데이터를 보고 결과를 예측하는 행위. 여기서는 심사 결과(APPROVE 등)를 내놓는 것.
- **inference-server** — 실제 ML 모델이 올라가 있는 별도 서버. ai-service가 HTTP로 요청을 보내면 예측 결과를 반환한다.

### 총 심사 건수 (1h)
- 최근 1시간 동안 처리된 자동 심사 건수입니다.
- 갑자기 0에 가까워지면 ai-service 또는 업스트림 서비스에 문제가 생긴 것입니다.

### 추론 실패 건수 (1h)
- inference-server 연결 실패 또는 오류 응답으로 인해 심사가 완료되지 못한 건수입니다.
- **정상이라면 항상 0이어야 합니다.**

| 등급 | 수치 |
|------|------|
| 정상 | 0 |
| 주의 | 1 ~ 5건 |
| 위험 | 5건 초과 (알림 발생) |

### REJECT 비율 (10m)
- 최근 10분간 심사 건수 중 REJECT 된 비율입니다.

| 등급 | 비율 |
|------|------|
| 정상 | 70% 미만 |
| 위험 | 70% 이상 (알림 발생) |

- REJECT 비율이 갑자기 급등하면 **데이터 드리프트** 또는 모델 이상 가능성이 있습니다.
- 업스트림(대출 신청 서비스)에서 비정상 요청이 대거 유입된 경우에도 이 값이 높아집니다.

### 추론 에러율 (5m)
- 최근 5분간 심사 요청 중 추론 실패 비율입니다.

| 등급 | 비율 |
|------|------|
| 정상 | 5% 미만 |
| 위험 | 5% 이상 (알림 발생) |

### 결정 분포 추이 (req/s)
- APPROVE / CONDITIONAL / REJECT 각각의 처리 속도를 시간축으로 보여줍니다.
- 특정 결과가 갑자기 급증하거나 사라지면 → 입력 데이터 변화 또는 모델 이상 신호입니다.

### 추론 에러 발생 추이
- 에러 발생이 시간적으로 몰려있으면 inference-server 장애일 가능성이 높습니다.
- 분산되어 발생하면 특정 입력 데이터 문제일 수 있습니다.

---

## 5. 응답시간 (p50 / p95) 섹션

**용어:**
- **p50 / p95** — 응답시간 분포. p95 = 100건 중 느린 5건을 제외한 나머지의 최대 응답시간. 낮을수록 빠름.

추론 요청을 보내고 결과를 받을 때까지 걸린 시간입니다.

| 등급 | p50 | p95 |
|------|-----|-----|
| 정상 | 1초 미만 | 3초 미만 |
| 주의 | 1 ~ 2초 | 3 ~ 5초 |
| 위험 | 2초 초과 | 5초 초과 (알림 발생) |

- **p50만 높고 p95도 높은 경우**: inference-server 전체가 느린 것 → 서버 부하 또는 모델 용량 문제.
- **p95만 특히 높은 경우**: 일부 요청(복잡한 입력)에서만 지연 → 특정 입력 패턴 확인 필요.
- 응답시간 추이 그래프에서 계단식 증가가 보이면 → 모델 재로딩 또는 inference-server 재기동 신호.

---

## 6. 모델 예측 신뢰도 (Score) 섹션

**용어:**
- **Score (신뢰도)** — 모델이 내린 결정에 대한 확신 정도. 0 ~ 1 사이 값. 0.9 이상이면 모델이 매우 확신하는 것.

모델이 내린 결정에 대한 확신 정도를 보여줍니다.

### 결정별 신뢰도 분포 (p50 / p95)
- APPROVE / REJECT 각각의 신뢰도 중간값과 상위 분포를 보여줍니다.
- 신뢰도가 갑자기 낮아지면(예: APPROVE인데 score=0.51) **모델이 확신하지 못하는 경계 케이스가 늘어난 것**입니다.

| 상황 | 의미 |
|------|------|
| APPROVE score > 0.8 | 모델이 명확하게 승인 판단 |
| APPROVE score 0.5 ~ 0.6 | 경계 케이스 증가 — 모델 재점검 필요 가능성 |
| 모든 score가 균일하게 낮아짐 | 드리프트 또는 모델 이상 |

> Score는 오프라인 정밀도(Precision/Recall)의 대리 지표입니다. 실제 심사 결과의 정확도를 판단하려면 심사 결과 추적 시스템의 사후 평가가 별도로 필요합니다.

---

## 7. 데이터 드리프트 감지 섹션

**용어:**
- **데이터 드리프트** — 실제로 들어오는 입력 데이터의 분포가 모델 학습 때와 달라지는 현상. 드리프트가 심해지면 모델 정확도가 떨어진다.
- **DSR (Debt Service Ratio)** — 연소득 대비 연간 부채 상환 비율. 값이 클수록 부채 부담이 크다. 정상 범위: 0.0 ~ 1.0.
- **신용점수 Proxy** — 내부 기준으로 산출한 신용도 점수. 0 ~ 1000 범위. 높을수록 신용도가 좋음.

입력 피처 분포가 변화하면 모델 정확도가 떨어질 수 있습니다. 이 섹션은 그 신호를 조기에 감지합니다.

### 입력 DSR 분포 추이
- 들어오는 요청의 DSR 값의 p50 / p95 분포를 시간축으로 보여줍니다.
- DSR p95가 갑자기 0.8 이상으로 올라가면: 고부채 고객 요청이 급증한 것.
- DSR 값이 0.0 근방으로 모이면: 업스트림에서 기본값을 넣고 있을 가능성.

### 입력 신용점수 분포 추이
- 신용점수 proxy 값의 p50 / p95 분포를 보여줍니다.
- 신용점수 p50이 갑자기 낮아지면: 저신용 요청이 급증한 것.
- 신용점수 값이 특정 값에 고정되면: 업스트림 데이터 파이프라인 오류 가능성.

### 두 지표를 함께 보는 방법
- **DSR 급등 + 신용점수 급락**: 고위험 대출 요청 급증 → 비즈니스 팀과 확인 필요
- **두 값이 동시에 특정 값으로 고정**: 업스트림 파이프라인에서 더미 데이터를 보내고 있을 가능성

---

## 8. 모델 버전별 결과 비교 섹션

**용어:**
- **모델 버전** — inference-server에 배포된 ML 모델의 버전 식별자 (예: v2). 버전 배포 후 지표 변화를 비교하는 데 사용.

새 모델을 배포했을 때 결과가 달라졌는지 확인하는 섹션입니다.

### 모델 버전별 결정 분포
- 버전별로 APPROVE / REJECT / CONDITIONAL 비율을 비교합니다.
- 배포 전후 REJECT 비율이 크게 달라졌으면 → 새 모델이 더 보수적이거나 공격적으로 바뀐 것입니다.

### 모델 버전별 응답시간 p95
- 새 모델이 이전 모델보다 응답시간이 현저히 길면 → 모델 크기 증가 또는 inference-server 리소스 부족.
- 두 버전이 동시에 나타나면 카나리 배포 중인 것입니다.

> 이 섹션은 inference-server가 응답에 `model_version` 필드를 포함해야 데이터가 나옵니다.

---

## 9. 데이터 품질 (결측치 · 이상치) 섹션

**용어:**
- **결측치** — 필수 입력 값이 없는 경우. DSR이 null로 들어오는 것이 대표적인 예.
- **이상치** — 정상 범위를 벗어난 값. DSR이 1.8로 들어오는 경우처럼 비현실적인 값.
- **LTV (Loan to Value)** — 자산 대비 대출 비율. 값이 클수록 담보 여력이 적다. 정상 범위: 0.0 ~ 1.5.

잘못된 입력이 들어오고 있는지 감지합니다.

### 핵심 필드 결측치 발생 추이
- DSR, LTV, 신용점수, 연소득, 신청금액, 총부채 중 null 값이 들어온 횟수입니다.

| 등급 | 5분간 결측치 수 |
|------|----------------|
| 정상 | 0 ~ 5건 |
| 주의 | 5 ~ 10건 |
| 위험 | 10건 초과 (알림 발생) |

- 결측치가 급증하면: 업스트림(대출 신청 UI 또는 API)에서 필드를 빠뜨리고 보내는 버그가 발생한 것.
- 특정 필드만 결측치가 많으면: 해당 필드를 수집하는 파이프라인만 오류인 것.

### 핵심 필드 이상치 발생 추이
- 정상 범위를 벗어난 값이 들어온 횟수입니다.

| 필드 | 정상 범위 | 이상치 기준 |
|------|----------|------------|
| DSR | 0.0 ~ 1.0 | 0 미만 또는 1.0 초과 |
| LTV | 0.0 ~ 1.5 | 0 미만 또는 1.5 초과 |
| 신용점수 proxy | 0 ~ 1000 | 0 미만 또는 1000 초과 |
| 나이 | 18 ~ 100 | 18 미만 또는 100 초과 |
| 연소득 | 0 이상 | 0 미만 |
| 신청금액 | 0 초과 | 0 이하 |

- 이상치 급증은 클라이언트 버그 또는 데이터 변환 오류가 원인인 경우가 많습니다.

---

## 10. 이상 징후 발생 시 확인 순서

### 추론 에러율이 급등했다
1. **inference-server 상태** 확인 — `http://localhost:8090/health` 응답 여부
2. **ai-service 로그** 확인 — `INFERENCE_UNAVAILABLE` 또는 `INFERENCE_FAILED` 오류 내용
3. 네트워크 연결 문제인지 모델 오류인지 구분
4. inference-server 재기동 또는 이전 버전으로 롤백

### REJECT 비율이 갑자기 70%를 넘었다
1. **입력 DSR / 신용점수 분포** 확인 — 드리프트가 있는지 확인
2. **결측치 / 이상치** 패널 확인 — 데이터 품질 문제가 아닌지 확인
3. 드리프트가 없는데 REJECT만 급등하면 → 모델 이상 또는 임계값 변경 여부 확인
4. 비즈니스 팀과 고위험 고객 요청 급증 여부 공유

### 응답시간 p95가 3초를 넘었다
1. **inference-server** CPU / 메모리 사용률 확인
2. 특정 모델 버전에서만 발생하는지 **모델 버전별 응답시간** 패널 확인
3. 동시 요청 수가 급증했는지 **총 심사 건수** 패널 확인
4. inference-server 스케일 업 또는 요청 큐 도입 검토

### 결측치 / 이상치가 급증했다
1. **업스트림 서비스** (대출 신청 API) 최근 배포 이력 확인
2. 결측치가 특정 필드에 집중되어 있는지 패널의 필드 태그 확인
3. 대출 신청 UI / API 입력 유효성 검사 코드 확인
4. 심각하면 해당 입력의 심사 결과를 임시 보류하고 수동 심사로 전환

---

## 11. 알림 규칙

이상 상황이 되면 자동으로 감지하는 규칙입니다. 알림 상태는 `http://localhost:9090/alerts` 에서 확인할 수 있습니다.

> Slack 알림 연동은 실제 배포 환경 구성 시 추가 예정입니다. 현재는 Prometheus UI에서만 확인 가능합니다.

| 알림 이름 | 조건 | 언제 발동 | 심각도 |
|-----------|------|----------|--------|
| ReviewResponseTimeSlow | p95 응답시간 > 3초 | 3분 지속 시 | ⚠️ warning |
| ReviewInferenceErrorHigh | 추론 실패율 > 5% | 2분 지속 시 | 🔴 critical |
| ReviewRejectRateHigh | REJECT 비율 > 70% | 5분 지속 시 | ⚠️ warning |
| ReviewInputMissingHigh | 5분간 결측치 > 10건 | 즉시 | ⚠️ warning |
| ReviewInputOutlierHigh | 5분간 이상치 > 10건 | 즉시 | ⚠️ warning |

> 즉시 발동하는 알림(결측치, 이상치)은 조건이 충족되는 순간 바로 발생합니다.
> 나머지는 일시적인 스파이크를 무시하고 지속될 때만 발생합니다.

---

## 12. "No data" 표시 시 대처법

### 특정 패널에만 No data
- **추론 실패 / 에러율**: 에러가 없으면 정상적으로 No data입니다.
- **REJECT 비율**: REJECT 결정이 없으면 No data입니다. 정상일 수 있습니다.
- **결측치 / 이상치**: 정상 데이터만 들어오면 No data입니다. 정상입니다.

### 전체 패널이 No data
1. **Prometheus Targets 확인**: `http://localhost:9090/targets` → `ai-service` 가 **UP** 인지 확인
2. **메트릭 원본 확인**: `http://localhost:8086/actuator/prometheus` 에서 `review_` 로 시작하는 메트릭이 있는지 확인
3. **datasource 확인**: Grafana → `http://localhost:3000/connections/datasources` → Prometheus → **Save & test**

### Prometheus는 UP인데 Grafana에 No data
- datasource UID 불일치 문제입니다.
- Grafana datasource URL에서 UID 확인 후 `infra/grafana/provisioning/dashboards/ml-loan-review.json` 의 datasource uid 값을 해당 UID로 교체한 뒤 Grafana 재시작합니다.

```powershell
# Grafana 재시작 (관리자 PowerShell)
Restart-Service -Name "Grafana"
```

---

## 13. 로컬 실행 방법

```powershell
# 1. ai-service DB 기동 (pgvector 포함 이미지 필수)
docker compose up -d ai-db

# 2. ai-service 기동 (프로젝트 루트에서)
.\gradlew.bat :services:ai-service:bootRun --args='--spring.profiles.active=local'

# 3. Mock inference-server 기동 (별도 PowerShell 창, 8090 포트)
$listener = [System.Net.HttpListener]::new()
$listener.Prefixes.Add("http://localhost:8090/")
$listener.Start()
while ($listener.IsListening) {
    $ctx = $listener.GetContext()
    $body = '{"model_version":"v2","predictions":[{"decision":"APPROVE","score":0.87,"proba":{"APPROVE":0.87,"CONDITIONAL":0.08,"REJECT":0.05}}]}'
    $buf = [System.Text.Encoding]::UTF8.GetBytes($body)
    $ctx.Response.ContentType = "application/json"
    $ctx.Response.OutputStream.Write($buf, 0, $buf.Length)
    $ctx.Response.Close()
}
```

> 실제 inference-server가 있으면 Mock 서버 없이 `ai.inference.base-url` 설정만 변경하면 됩니다.

### 테스트 요청 전송

```powershell
# 정상 요청
Invoke-RestMethod -Method Post -Uri "http://localhost:8086/api/ai/auto-review" `
  -ContentType "application/json" `
  -Body '{"gender":"M","age":35,"dsr":0.35,"ltv":0.5,"creditScoreProxy":720,"annualIncomeKw":60000,"requestedAmountKw":30000,"totalDebtKw":15000,"requestedPeriodMo":12,"purposeCd":"LIVING","purposeRedFlag":false}'

# 결측치 테스트 (dsr 없음)
Invoke-RestMethod -Method Post -Uri "http://localhost:8086/api/ai/auto-review" `
  -ContentType "application/json" `
  -Body '{"gender":"F","age":28,"ltv":0.4,"creditScoreProxy":680,"annualIncomeKw":45000,"requestedAmountKw":20000,"totalDebtKw":10000,"requestedPeriodMo":12,"purposeCd":"LIVING","purposeRedFlag":false}'

# 이상치 테스트 (dsr=1.8)
Invoke-RestMethod -Method Post -Uri "http://localhost:8086/api/ai/auto-review" `
  -ContentType "application/json" `
  -Body '{"gender":"M","age":45,"dsr":1.8,"ltv":0.9,"creditScoreProxy":400,"annualIncomeKw":30000,"requestedAmountKw":50000,"totalDebtKw":40000,"requestedPeriodMo":24,"purposeCd":"BUSINESS","purposeRedFlag":true}'
```

---

## 14. 관련 파일 위치

| 파일 | 역할 |
|------|------|
| `infra/prometheus/alerts.yml` | 알림 규칙 정의 (ml-loan-review 그룹) |
| `infra/prometheus/prometheus.yml` | 데이터 수집 대상 설정 (ai-service 포함) |
| `infra/grafana/provisioning/dashboards/ml-loan-review.json` | 대시보드 정의 파일 |
| `services/ai-service/src/main/java/com/bank/ai/review/observability/ReviewMetrics.java` | 메트릭 수집 코드 |
| `services/ai-service/src/main/java/com/bank/ai/review/service/AutoReviewService.java` | 심사 서비스 (메트릭 연동) |
| `services/ai-service/src/main/resources/application-local.yml` | 로컬 개발 설정 (Security 제외) |

---

## 15. 모니터링 가능 지표 vs 사후 평가 지표

이 대시보드로 볼 수 있는 것과 볼 수 없는 것을 명확히 구분합니다.

### 실시간 모니터링 가능 (이 대시보드에서 확인)

모델이 **지금 어떻게 작동하고 있는가**를 보여줍니다. 정답 데이터 없이 즉시 수집됩니다.

| 지표 | 설명 |
|------|------|
| 추론 응답시간 (p50/p95) | inference-server 호출 ~ 응답까지 걸린 시간 |
| 결정 분포 (APPROVE/CONDITIONAL/REJECT) | 심사 결과 비율 및 추이 |
| 예측 신뢰도 (Score) | 모델이 결정에 확신하는 정도 (0~1) |
| 추론 에러율 | inference-server 연결 실패 / 오류 응답 비율 |
| 입력 결측치 / 이상치 | 필수 필드 누락 및 정상 범위 이탈 건수 |
| 입력 피처 분포 (DSR, 신용점수) | 입력 데이터 분포 변화 — 드리프트 조기 감지 |
| 모델 버전별 결과 비교 | 새 모델 배포 전후 결정 분포 및 응답시간 비교 |

### 사후 평가 필요 (이 대시보드에서 확인 불가)

모델이 **옳은 판단을 했는가**를 검증하려면 실제 대출 상환 이력이 필요합니다. 대출 실행 후 수개월~수년이 지나야 정답 데이터가 확보되므로 실시간 수집이 불가능합니다.

| 지표 | 왜 실시간 불가한가 |
|------|-----------------|
| Precision / Recall / F1 | 실제 부실 여부(정답)를 알아야 계산 가능 |
| AUC-ROC | 전체 예측 분포와 실제 결과를 비교해야 함 |
| 오승인율 (False Positive) | 승인했는데 실제로 부실이 난 비율 — 사후에만 파악 가능 |
| 오거절율 (False Negative) | 거절했는데 실제로는 정상 상환했을 비율 — 반사실적 추론 필요 |
| 모델 정확도 | 정답 레이블이 있어야 산출 가능 |

> 사후 평가 지표는 별도의 오프라인 평가 파이프라인(배치 분석, 데이터 웨어하우스 등)을 통해 주기적으로 산출해야 합니다. 이 대시보드의 **REJECT 비율 급등**이나 **신뢰도 저하**는 사후 평가가 필요하다는 조기 신호로 활용할 수 있습니다.

---

## 16. 관련 가이드

| 문서 | 내용 |
|------|------|
| [DASHBOARD_GUIDE.md](DASHBOARD_GUIDE.md) | Service Overview 대시보드 — HTTP, JVM, DB, 인증/보안 해석 |
| [KAFKA_PAYMENT_GUIDE.md](KAFKA_PAYMENT_GUIDE.md) | Kafka Payment 대시보드 — Consumer Lag, Outbox, DLQ 해석 |
| [INFRA_VERSIONS.md](INFRA_VERSIONS.md) | 모니터링 인프라 버전 및 설정 파일 위치 |
