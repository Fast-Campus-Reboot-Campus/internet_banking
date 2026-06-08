# LLM / RAG 모니터링 가이드

> 대상 도구: **Langfuse**, **Arize Phoenix**
> 대상 독자: 개발팀 전원
> 환경: 로컬 Docker Compose 기준

---

## 핵심 모니터링 지표

LLM/RAG 모니터링에서 **반드시 확인해야 할 지표**를 도구별 · 서비스별로 정리합니다.

---

### Langfuse — 공통 (consultation-service + auto-loan-review)

| 지표 | 설명 | 정상 | 주의 | 위험 |
|------|------|------|------|------|
| **LLM 응답시간 P50** | 전체 LLM 호출의 중간값 | < 2초 | 2~5초 | 5초 초과 |
| **LLM 응답시간 P95** | 느린 상위 5% 기준 최대값 | < 5초 | 5~10초 | 10초 초과 |
| **LLM 오류율** | 호출 실패 비율 | 0% | < 5% | 10% 이상 |
| **시간당 토큰 비용** | 모델별 누적 비용 ($) | — | 전일 대비 2배 초과 | — |
| **트레이스 수 추이** | 시간대별 LLM 호출 건수 | — | 갑작스러운 급증/급감 | — |

#### consultation-service 추가 확인 지표

| 지표 | 설명 | 확인 방법 |
|------|------|---------|
| **태그 구분** | `consultation-service` 태그 필터로 서비스 분리 확인 | Traces → Filter → Tag |
| **llm-answer 응답 품질** | Input(프롬프트)과 Output(응답)이 문맥에 맞는지 | 트레이스 상세 → Input/Output |
| **llm-recommend 토큰 사용량** | 현금흐름 추천 시 과도한 토큰 사용 여부 | 트레이스 상세 → Usage |

#### auto-loan-review 추가 확인 지표

| 지표 | 설명 | 확인 방법 |
|------|------|---------|
| **심사 LLM 호출 흐름** | trace → span(RAG 검색) → generation(LLM) 계층 확인 | Traces → 상세 |
| **RAG 검색 스팬 지연** | 임베딩/검색 단계가 전체 지연의 주원인인지 | Traces → Span latency |
| **LLM 판단 근거** | 심사 요약 생성 시 입력 정책/데이터가 올바른지 | generation Input 전문 |

---

### Arize Phoenix — consultation-service 전용

| 지표 | 설명 | 정상 | 주의 |
|------|------|------|------|
| **ChatCompletion Latency P50** | OpenAI 호출 중간 응답시간 | < 2초 | 2초 초과 |
| **ChatCompletion Latency P99** | 최악의 경우 응답시간 | < 5초 | 5초 초과 |
| **Cumulative Tokens** | 스팬별 누적 토큰 사용량 | — | 특정 스팬에 집중 시 확인 |
| **Cumulative Cost** | 스팬별 누적 비용 | — | 비정상 급등 시 확인 |
| **Status: ERROR 스팬** | 오류 발생 스팬 수 | 0건 | 1건 이상 즉시 확인 |
| **RAG 스팬 비율** | 전체 트레이스 중 검색 스팬 비율 | — | 검색이 응답시간의 50% 초과 시 |

---

## 이 가이드는 무엇인가요?

Grafana는 서비스의 **인프라 상태**(HTTP 응답시간, JVM 메모리, DB 커넥션 등)를 봅니다.
하지만 LLM(언어 모델)과 RAG(검색 기반 응답)은 인프라 지표만으로는 판단하기 어렵습니다.

> "LLM이 왜 이상한 답을 했지?" "어떤 프롬프트가 들어갔지?" "토큰을 얼마나 썼지?"

이런 질문에 답하는 도구가 **Langfuse**와 **Arize Phoenix**입니다.

### 도구별 역할

| 도구 | 역할 | 주요 확인 사항 |
|------|------|--------------|
| **Grafana** | 인프라 메트릭 | HTTP 응답시간, JVM, DB 커넥션 등 서비스 상태 |
| **Langfuse** | LLM 호출 추적 | 프롬프트/응답 내용, 토큰 비용, 품질 평가 |
| **Arize Phoenix** | LLM/RAG 스팬 시각화 | 입력→검색→생성 파이프라인 흐름 |

> **LLM 관련 지표(토큰 수, 비용, RAG 검색 미스 등)는 Grafana가 아닌 Langfuse에서 확인합니다.**
>
> Grafana에 LLM 전용 대시보드(`auto-loan-review-llm.json`, `llm-overview.json`)를 만들어뒀었지만, 아래 이유로 현재는 비활성화 상태입니다.
> - LLM 트레이싱(프롬프트 내용, 토큰 사용, 비용 추적)은 이 목적에 특화된 Langfuse가 훨씬 적합
> - 챗봇과 ML 심사는 각자 전용 Grafana 대시보드가 있어 중복
>
> 나중에 필요하다면 `infra/grafana/disabled_dashboards/` 폴더에서 파일을 꺼내 provisioning 폴더로 옮기면 즉시 복원됩니다.

### 서비스별 연결 현황

| 서비스 | Grafana (인프라) | Langfuse | Phoenix |
|--------|----------------|----------|---------|
| consultation-service | ✅ 챗봇 대시보드 | ✅ | ✅ (별도 프로젝트) |
| auto-loan-review | ✅ ML 심사 대시보드 | ✅ | ❌ |

> **Phoenix는 consultation-service(Python)에서만 사용합니다.** Java 생태계의 Phoenix 지원이 아직 미성숙하여, auto-loan-review는 Langfuse로 LLM 모니터링을 커버합니다.

---

## 1. 접속 방법

| 도구 | URL | 계정 |
|------|-----|------|
| Langfuse | `http://localhost:3001` | 가입 후 사용 (최초 접속 시 회원가입) |
| Arize Phoenix | `http://localhost:6006` | 없음 (인증 없음) |

> 두 도구 모두 Docker Compose가 실행 중이어야 접속 가능합니다.
> ```powershell
> docker compose up -d langfuse phoenix
> ```

---

## 2. Langfuse 사용 가이드

### 2-1. Langfuse란?

LLM 호출 내역을 기록하고 분석하는 도구입니다. 서비스가 OpenAI GPT를 호출할 때마다 다음 정보가 자동으로 저장됩니다.

- 어떤 프롬프트를 보냈는지
- AI가 뭐라고 응답했는지
- 토큰을 몇 개 썼고 비용이 얼마인지
- 응답하는 데 얼마나 걸렸는지

### 2-2. 대시보드 접속

1. `http://localhost:3001` 접속
2. 로그인 후 **EXFul_Bank** 프로젝트 선택
3. 좌측 메뉴에서 확인할 섹션 선택

### 2-3. 주요 메뉴

#### Dashboard (첫 화면)
전체 현황을 한눈에 볼 수 있습니다.

| 항목 | 설명 |
|------|------|
| **Total Traces** | 총 LLM 호출 건수 |
| **Model costs** | 모델별 토큰 비용 ($) |
| **Trace latencies** | 트레이스별 응답시간 (P50, P95) |
| **Model latencies** | 모델별 응답시간 그래프 |

#### Tracing → Traces
LLM 호출 내역 목록입니다. 각 항목을 클릭하면 상세 내용을 볼 수 있습니다.

**서비스 구분 방법**: Tags 컬럼에서 서비스 이름을 확인합니다.

| Tag | 서비스 |
|-----|--------|
| `consultation-service` | 챗봇 상담 서비스 |
| (태그 없음) | auto-loan-review |

**필터 사용 방법**:
- 상단 필터에서 `Tag` → `consultation-service` 선택 시 챗봇 서비스만 보임
- `Name` 필터로 `llm-answer`, `llm-recommend` 등 특정 함수만 볼 수 있음

#### 트레이스 상세 보기
트레이스 하나를 클릭하면 다음을 확인할 수 있습니다.

| 항목 | 설명 |
|------|------|
| **Input** | LLM에 보낸 프롬프트 전문 |
| **Output** | LLM이 반환한 응답 전문 |
| **Usage** | 입력 토큰 수 / 출력 토큰 수 / 비용 |
| **Latency** | 응답 소요 시간 |
| **Tags** | 어느 서비스에서 호출했는지 |

### 2-4. 트레이스 이름 의미

| 트레이스 이름 | 발생 시점 |
|-------------|----------|
| `llm-answer` | 챗봇이 고객 질문에 GPT로 답변할 때 |
| `llm-recommend` | 챗봇이 현금흐름 분석 기반 상품을 추천할 때 |
| `auto-loan-review` | 대출 심사 AI가 LLM을 호출할 때 |

### 2-5. 정상 / 주의 기준

| 항목 | 정상 | 주의 | 위험 |
|------|------|------|------|
| LLM 응답시간 (P50) | < 2초 | 2~5초 | 5초 초과 |
| LLM 응답시간 (P95) | < 5초 | 5~10초 | 10초 초과 |
| 시간당 비용 | 서비스별 다름 | 전일 대비 2배 이상 증가 시 확인 | — |
| 오류 트레이스 | 없음 | 간헐적 | 지속 발생 |

### 2-6. 이상 징후별 확인 순서

#### LLM 응답이 갑자기 느려졌다
1. Langfuse **Dashboard** → `Trace latencies` P95 확인
2. 특정 트레이스 이름(`llm-answer`, `llm-recommend`)에서만 느린지 확인
3. **Traces** 목록에서 느린 트레이스 클릭 → Input 길이가 비정상적으로 길지 않은지 확인
4. OpenAI API 상태 페이지 확인

#### 비용이 갑자기 늘었다
1. **Dashboard** → `Model costs` 에서 어떤 모델이 급증했는지 확인
2. **Traces** 목록 → Usage 컬럼에서 토큰 수가 비정상적으로 큰 호출 탐색
3. 해당 트레이스 클릭 → Input 내용 확인 (프롬프트가 너무 길어진 건지)

#### LLM이 이상한 답변을 했다
1. **Traces** → 문제 시점 트레이스 클릭
2. **Input** 탭 → 어떤 프롬프트가 들어갔는지 확인
3. **Output** 탭 → AI 응답 전문 확인
4. 필요 시 Langfuse **Prompts** 메뉴에서 프롬프트 버전 이력 확인

---

## 3. Arize Phoenix 사용 가이드

> Phoenix는 **consultation-service(챗봇 서비스)** 전용입니다.

### 3-1. Phoenix란?

OpenTelemetry 기반의 LLM/RAG 파이프라인 추적 도구입니다. Langfuse가 "무슨 프롬프트를 보냈는가"에 집중한다면, Phoenix는 **"요청이 어떤 단계를 거쳐 처리됐는가"**를 스팬(Span) 단위로 보여줍니다.

RAG 흐름 예시:
```
사용자 질문 입력
    └─ 임베딩 생성 (Span)
        └─ 벡터 DB 검색 (Span)
            └─ 컨텍스트 조합
                └─ LLM 호출 (Span) → ChatCompletion
```

### 3-2. 대시보드 접속

1. `http://localhost:6006` 접속
2. Projects 화면에서 **consultation-service** 프로젝트 클릭
3. 상단 탭에서 **Spans** 또는 **Traces** 선택

> **default 프로젝트**에는 테스트 데이터가 있을 수 있습니다. 실제 모니터링은 **consultation-service 프로젝트**를 사용하세요.

### 3-3. 주요 탭

#### Spans 탭
모든 스팬(처리 단계) 목록입니다. 기본으로 `Root Spans`(최상위 스팬)만 보여줍니다.

| 컬럼 | 설명 |
|------|------|
| **kind** | 스팬 종류. `llm` = LLM 호출 |
| **name** | 처리 단계 이름. `ChatCompletion` = OpenAI 호출 |
| **input** | LLM에 보낸 메시지 (클릭하면 전문 확인 가능) |
| **output** | LLM 응답 |
| **latency** | 소요 시간 |
| **cumulative tokens** | 사용된 토큰 수 |
| **cumulative cost** | 해당 호출 비용 ($) |

**스팬 필터 사용 방법**:
검색창에 조건을 입력합니다.
```
span_kind == 'LLM'           # LLM 호출 스팬만 보기
latency > 5000               # 5초 초과 스팬만 보기
```

#### Traces 탭
여러 스팬을 하나의 요청 흐름으로 묶어서 보여줍니다.

#### Metrics 탭
집계 지표를 그래프로 보여줍니다.

| 지표 | 설명 |
|------|------|
| **Total Traces** | 총 트레이스 수 |
| **Latency P50/P99** | 응답시간 분포 |
| **Total Cost** | 총 LLM 비용 |

### 3-4. 스팬 상세 보기

스팬을 클릭하면 우측에 상세 패널이 열립니다.

| 항목 | 설명 |
|------|------|
| **Status** | OK = 정상, ERROR = 오류 발생 |
| **Total Cost** | 해당 스팬의 LLM 비용 |
| **Latency** | 소요 시간 |

상단의 `>>` 버튼을 클릭하면 전체 화면으로 확인할 수 있습니다.

### 3-5. 정상 / 주의 기준

| 항목 | 정상 | 주의 |
|------|------|------|
| Latency P50 | < 2초 | 2초 초과 |
| Latency P99 | < 5초 | 5초 초과 |
| Status | OK | ERROR 발생 시 즉시 확인 |

### 3-6. 이상 징후별 확인 순서

#### LLM 호출 오류가 발생했다
1. **Spans** 탭 → `status == 'ERROR'` 필터 입력
2. 오류 스팬 클릭 → 상세 내용에서 오류 메시지 확인
3. 동 시점 Langfuse에서도 동일 트레이스 확인

#### 특정 시점 이후 데이터가 안 보인다
1. 우측 상단 시간 범위 확인 (`Last 7 Days` 등)
2. consultation-service가 실행 중인지 확인
3. Phoenix 컨테이너 상태 확인: `docker ps | Select-String phoenix`

---

## 4. Langfuse와 Phoenix 함께 사용하기

두 도구는 **서로 보완적**입니다.

| 상황 | 확인할 도구 |
|------|-----------|
| LLM 비용이 갑자기 늘었다 | **Langfuse** (비용 대시보드) |
| AI 응답 품질이 이상하다 | **Langfuse** (프롬프트/응답 전문) |
| 요청이 어느 단계에서 오래 걸리는지 모르겠다 | **Phoenix** (스팬 단위 지연시간) |
| LLM 오류가 발생한 정확한 입력이 뭔지 | **Langfuse** (Input 전문) |
| RAG 검색 → LLM 호출 전체 흐름 확인 | **Phoenix** (Traces) |

---

## 5. 로컬 실행 방법

### 모니터링 도구 실행
```powershell
# Langfuse + Phoenix 실행
docker compose up -d langfuse langfuse-db phoenix
```

### consultation-service 실행
```powershell
cd "c:\Users\jaho3\OneDrive\바탕 화면\AX_FULL_Bank\internet_banking\services\consultation-service"
.\.venv\Scripts\python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 8087 --log-level info
```

> `.env` 파일에 다음이 설정되어 있어야 Langfuse/Phoenix가 활성화됩니다.
> ```
> CONSULTATION_LANGFUSE_ENABLED=true
> PHOENIX_ENABLED=true
> ```

### auto-loan-review 실행
```powershell
cd "c:\Users\jaho3\OneDrive\바탕 화면\AX_FULL_Bank\internet_banking"
.\gradlew :services:auto-loan-review:bootRun
```

> `services/auto-loan-review/.env` 파일에 다음이 설정되어 있어야 Langfuse가 활성화됩니다.
> ```
> LANGFUSE_ENABLED=true
> ```

### 테스트 데이터 생성 (consultation-service)

LLM 호출이 발생해야 Langfuse/Phoenix에 데이터가 쌓입니다. 아래 요청으로 테스트합니다.

```powershell
# 1. 상담 세션 시작
$session = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8087/chatbot/consultations/start" `
  -ContentType "application/json" `
  -Body '{"customer_no":"TEST001","entry_screen":"HOME","app_version":"1.0"}'

$id = $session.chatbot_consultation_id

# 2. LLM을 호출하는 자유 질문 전송 (키워드 매칭이 안 되는 질문)
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8087/chatbot/consultations/$id/messages" `
  -ContentType "application/json" `
  -Body '{"message":"만기 후 이자는 어떻게 받나요?"}'
```

요청 후 Langfuse와 Phoenix에서 새 트레이스가 생겼는지 확인합니다.

---

## 6. 모니터링 연결 검증 방법

처음 세팅하거나 환경이 바뀐 뒤에는 아래 체크리스트를 순서대로 따라가면 연결이 정상인지 확인할 수 있습니다.

---

### Step 1. 컨테이너 실행 확인

```powershell
docker ps | Select-String "langfuse|phoenix"
```

아래 두 컨테이너가 보여야 합니다.

| 컨테이너 | 확인 포트 |
|---------|---------|
| `ib-langfuse` | 3001 |
| `ib-phoenix` | 6006, 4317 |

보이지 않으면 실행합니다.
```powershell
docker compose up -d langfuse langfuse-db phoenix
```

---

### Step 2. 서비스 기동 및 연결 로그 확인

consultation-service를 실행하고 터미널 로그를 확인합니다.

```powershell
cd "c:\Users\jaho3\OneDrive\바탕 화면\AX_FULL_Bank\internet_banking\services\consultation-service"
.\.venv\Scripts\python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 8087 --log-level info
```

기동 시 아래 두 줄이 출력되면 연결 성공입니다.

```
INFO  app.main - [Langfuse] LLM 추적 활성화 → http://localhost:3001
INFO  app.main - [Phoenix] OTel 계측 활성화 → http://localhost:6006/v1/traces (project: consultation-service)
```

> 로그가 보이지 않으면 `.env` 파일에서 `CONSULTATION_LANGFUSE_ENABLED=true`, `PHOENIX_ENABLED=true` 설정을 확인하세요.

---

### Step 3. 테스트 요청 전송

LLM 호출이 있어야 Langfuse/Phoenix에 데이터가 쌓입니다. 아래 순서로 테스트합니다.

**핵심:** "키워드 매칭이 안 되는 자유 질문"을 보내야 합니다. 시나리오나 규칙으로 처리되는 질문은 LLM을 호출하지 않아서 트레이스가 생기지 않습니다.

```powershell
# 1단계: 상담 세션 시작
$session = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8087/chatbot/consultations/start" `
  -ContentType "application/json" `
  -Body '{"customer_no":"TEST001","entry_screen":"HOME","app_version":"1.0"}'

$id = $session.chatbot_consultation_id
Write-Host "세션 ID: $id"

# 2단계: LLM을 호출하는 자유 질문 전송
$response = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8087/chatbot/consultations/$id/messages" `
  -ContentType "application/json" `
  -Body '{"message":"만기 후 이자는 어떻게 받나요?"}'

Write-Host "처리 방식: $($response.process_method)"
```

응답에서 `process_method`가 `BP003_GPT`이면 LLM이 실제로 호출된 것입니다.

| process_method | LLM 호출 여부 | 트레이스 생성 |
|---------------|-------------|------------|
| `BP003_GPT` | ✅ 호출됨 | Langfuse + Phoenix에 기록 |
| `FEATURE_*`, `SCENARIO` | ❌ 호출 안 됨 | 기록 없음 |
| `AGENT_TRANSFER` | ❌ LLM 오류 | Langfuse에 오류 기록 |

> **LLM을 확실하게 호출하는 질문 예시**: "만기 후 이자는 어떻게 받나요?", "청약과 적금의 차이가 뭔가요?"

---

### Step 4. Langfuse에서 트레이스 확인

1. `http://localhost:3001` 접속
2. **Tracing → Traces** 클릭
3. 방금 보낸 요청의 트레이스가 목록에 나타나는지 확인

**확인 항목**:

| 항목 | 정상 상태 |
|------|---------|
| 트레이스 이름 | `llm-answer` 또는 `llm-recommend` |
| Tags | `consultation-service` |
| Status | 성공 (오류 없음) |
| Input | 보낸 메시지 내용 포함 |
| Output | AI 응답 텍스트 포함 |

---

### Step 5. Phoenix에서 스팬 확인

1. `http://localhost:6006` 접속
2. Projects 목록에서 **consultation-service** 클릭
3. **Spans** 탭에서 새 스팬이 생겼는지 확인

**확인 항목**:

| 항목 | 정상 상태 |
|------|---------|
| 스팬 kind | `llm` |
| 스팬 이름 | `ChatCompletion` |
| Status | OK |
| Input | 프롬프트 내용 포함 |
| Output | AI 응답 포함 |
| Latency | 수 초 (보통 1~3초) |

> **데이터가 바로 안 보일 때**: Phoenix는 배치(Batch) 방식으로 스팬을 전송하기 때문에 최대 수십 초 뒤에 나타날 수 있습니다. 잠시 기다린 뒤 새로고침하세요.

---

### Step 6. auto-loan-review Langfuse 확인

auto-loan-review는 Langfuse만 연결됩니다.

```powershell
# 루트 디렉토리에서 실행
cd "c:\Users\jaho3\OneDrive\바탕 화면\AX_FULL_Bank\internet_banking"
.\gradlew :services:auto-loan-review:bootRun
```

서비스가 뜨면 실제 대출 심사 요청을 보내야 Langfuse에 트레이스가 생깁니다. Langfuse **Tracing → Traces**에서 `auto-loan-review` 트레이스를 확인합니다.

---

### 최종 체크리스트

| 항목 | 확인 방법 | 결과 |
|------|---------|------|
| Langfuse 컨테이너 실행 | `docker ps` → `ib-langfuse` 확인 | ☐ |
| Phoenix 컨테이너 실행 | `docker ps` → `ib-phoenix` 확인 | ☐ |
| consultation-service 연결 로그 | 기동 시 `[Langfuse]`, `[Phoenix]` 로그 확인 | ☐ |
| LLM 호출 응답 | `process_method: BP003_GPT` 확인 | ☐ |
| Langfuse 트레이스 생성 | `llm-answer` 트레이스 + `consultation-service` 태그 | ☐ |
| Phoenix 스팬 생성 | `consultation-service` 프로젝트에 `ChatCompletion` 스팬 | ☐ |

---

## 7. "데이터가 안 보인다" 대처법

| 증상 | 원인 | 조치 |
|------|------|------|
| Langfuse에 트레이스가 없음 | `CONSULTATION_LANGFUSE_ENABLED=false` | `.env`에서 `true`로 변경 후 재시작 |
| Langfuse에 트레이스가 없음 | LLM 키 미설정 | `.env`에 `CONSULTATION_OPENAI_API_KEY` 확인 |
| Phoenix에 데이터가 없음 | `PHOENIX_ENABLED=false` | `.env`에서 `true`로 변경 후 재시작 |
| Phoenix에 데이터가 없음 | LLM 호출이 아직 없음 | 위 테스트 요청 전송 후 확인 |
| Langfuse 접속 안 됨 | 컨테이너 미실행 | `docker compose up -d langfuse langfuse-db` |
| Phoenix 접속 안 됨 | 컨테이너 미실행 | `docker compose up -d phoenix` |

### 서비스 로그에서 연결 상태 확인

consultation-service 기동 시 아래 로그가 찍히면 정상 연결된 것입니다.

```
[Langfuse] LLM 추적 활성화 → http://localhost:3001
[Phoenix] OTel 계측 활성화 → http://localhost:6006/v1/traces (project: consultation-service)
```

---

## 7. 환경 변수 정리

### consultation-service (`.env`)

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `CONSULTATION_LANGFUSE_ENABLED` | Langfuse 활성화 여부 | `false` |
| `LANGFUSE_SECRET_KEY` | Langfuse API Secret Key | — |
| `LANGFUSE_PUBLIC_KEY` | Langfuse API Public Key | — |
| `LANGFUSE_HOST` | Langfuse 서버 주소 | `http://localhost:3001` |
| `PHOENIX_ENABLED` | Phoenix 활성화 여부 | `false` |
| `PHOENIX_HTTP_ENDPOINT` | Phoenix OTLP 엔드포인트 | `http://localhost:6006/v1/traces` |

### auto-loan-review (`.env`)

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `LANGFUSE_ENABLED` | Langfuse 활성화 여부 | `false` |
| `LANGFUSE_SECRET_KEY` | Langfuse API Secret Key | — |
| `LANGFUSE_PUBLIC_KEY` | Langfuse API Public Key | — |
| `LANGFUSE_HOST` | Langfuse 서버 주소 | `http://localhost:3001` |

> **Langfuse API 키 발급 방법**:
> 1. `http://localhost:3001` 로그인
> 2. 우측 상단 프로젝트 설정 → **API Keys**
> 3. `Create new API key` → Secret Key / Public Key 복사

---

## 8. 관련 파일 위치

| 파일 | 역할 |
|------|------|
| `services/consultation-service/app/main.py` | Langfuse/Phoenix 초기화 (`_setup_langfuse`, `_setup_phoenix`) |
| `services/consultation-service/app/llm.py` | `@observe` 데코레이터 적용, Langfuse 태그 설정 |
| `services/consultation-service/app/config.py` | Langfuse 환경변수 설정 |
| `services/consultation-service/.env` | API 키 및 활성화 여부 |
| `services/auto-loan-review/src/main/java/.../LangfuseService.java` | Langfuse trace/span/generation 기록 |
| `services/auto-loan-review/src/main/java/.../GeminiOpenAiCompatLlmClient.java` | LLM 호출 시 Langfuse 자동 기록 |
| `services/auto-loan-review/.env` | API 키 및 활성화 여부 |

---

## 9. 관련 가이드

| 문서 | 내용 |
|------|------|
| [DASHBOARD_GUIDE.md](DASHBOARD_GUIDE.md) | Grafana 서비스 인프라 대시보드 |
| [CHATBOT_GUIDE.md](CHATBOT_GUIDE.md) | 챗봇 상담 Grafana 대시보드 (Prometheus 메트릭) |
| [ML_LOAN_REVIEW_GUIDE.md](ML_LOAN_REVIEW_GUIDE.md) | ML 대출 심사 모니터링 |
| [INFRA_VERSIONS.md](INFRA_VERSIONS.md) | 모니터링 인프라 버전 및 설정 파일 위치 |
