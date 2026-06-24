# LLM / RAG 모니터링 가이드

> 대상 도구: **Langfuse** (auto-loan-review · consultation-service · goal-agent · review-ai-gateway), **Prometheus + Grafana** (advisory-service RAG)
> 대상 독자: 개발팀 전원
> 환경: 로컬 Docker Compose 기준

---

## Advisory RAG 메트릭 (Prometheus 직접 노출)

advisory-service 는 pgvector 자체 검색이므로 Langfuse 없이 **Prometheus 메트릭**으로 관찰합니다.
`http://localhost:8080/actuator/prometheus` 에서 확인 가능합니다.

### 검색 메트릭

| 메트릭 | 종류 | 태그 | 설명 |
|--------|------|------|------|
| `advisory_rag_search_duration_seconds` | Timer | `kind`, `status` | 코사인 검색 지연시간. kind = `POLICY_CITATION` \| `SIMILAR_CASE`, status = `success` \| `error` |
| `advisory_rag_search_results` | DistributionSummary | `kind` | 검색 호출당 반환 결과 건수 분포 |

**권장 PromQL:**
```promql
# p95 검색 지연시간
histogram_quantile(0.95,
  sum(rate(advisory_rag_search_duration_seconds_bucket{status="success"}[5m])) by (kind, le)
)

# 검색 실패율
sum(rate(advisory_rag_search_duration_seconds_count{status="error"}[5m]))
/ sum(rate(advisory_rag_search_duration_seconds_count[5m]))
```

### 임베딩 메트릭

| 메트릭 | 종류 | 태그 | 설명 |
|--------|------|------|------|
| `advisory_rag_embedding_duration_seconds` | Timer | `model`, `status` | OpenAI 임베딩 API 호출 지연시간. model = `OPENAI_3S`, status = `success` \| `error` |
| `advisory_rag_embedding_calls_total` | Counter | `model`, `status` | 임베딩 API 누적 호출 수 |

**권장 PromQL:**
```promql
# 임베딩 p95 지연시간
histogram_quantile(0.95,
  sum(rate(advisory_rag_embedding_duration_seconds_bucket{status="success"}[5m])) by (model, le)
)

# 임베딩 오류율
sum(rate(advisory_rag_embedding_calls_total{status="error"}[5m]))
/ sum(rate(advisory_rag_embedding_calls_total[5m]))
```

### Prometheus Alert 규칙 (`infra/prometheus/alerts.yml`)

| Alert | 조건 | 심각도 |
|-------|------|--------|
| `AdvisoryRagSearchFailRateHigh` | 검색 실패율 > 5% / 5분 지속 | critical |
| `AdvisoryRagEmbeddingLatencySlow` | 임베딩 p95 > 2초 / 5분 지속 | warning |

### Grafana 패널 구성 (권장)

Grafana에서 **"Advisory RAG"** 섹션을 별도 Row로 구성할 것을 권장합니다.

| 패널 | 시각화 | PromQL |
|------|--------|--------|
| 검색 지연시간 (p50/p95/p99) | Time series | `histogram_quantile(0.95, ...)` by kind |
| 검색 실패율 | Stat / Time series | `rate(count{status="error"}) / rate(count)` |
| 검색 결과 건수 평균 | Time series | `rate(advisory_rag_search_results_sum[5m]) / rate(advisory_rag_search_results_count[5m])` |
| 임베딩 지연시간 (p95) | Time series | `histogram_quantile(0.95, ...)` by model |
| 임베딩 오류율 | Stat | `rate(calls{status="error"}) / rate(calls)` |
| 백필 처리 속도 | Time series | `rate(advisory_rag_backfill_processed_total[5m])` |

---

## 핵심 모니터링 지표

LLM/RAG 모니터링에서 **반드시 확인해야 할 지표**를 도구별 · 서비스별로 정리합니다.

---

### Langfuse — auto-loan-review

| 지표 | 설명 | 정상 | 주의 | 위험 |
|------|------|------|------|------|
| **LLM 응답시간 P50** | 전체 LLM 호출의 중간값 | < 2초 | 2~5초 | 5초 초과 |
| **LLM 응답시간 P95** | 느린 상위 5% 기준 최대값 | < 5초 | 5~10초 | 10초 초과 |
| **LLM 오류율** | 호출 실패 비율 | 0% | < 5% | 10% 이상 |
| **시간당 토큰 비용** | 모델별 누적 비용 ($) | — | 전일 대비 2배 초과 | — |
| **트레이스 수 추이** | 시간대별 LLM 호출 건수 | — | 갑작스러운 급증/급감 | — |

#### auto-loan-review 확인 지표

| 지표 | 설명 | 확인 방법 |
|------|------|---------|
| **심사 LLM 호출 흐름** | trace → span(RAG 검색) → generation(LLM) 계층 확인 | Traces → 상세 |
| **RAG 검색 스팬 지연** | 임베딩/검색 단계가 전체 지연의 주원인인지 | Traces → Span latency |
| **LLM 판단 근거** | 심사 요약 생성 시 입력 정책/데이터가 올바른지 | generation Input 전문 |

---

### Arize Phoenix — 현재 미구현

> Phoenix 패키지는 consultation-service에 설치되어 있으나, `app/main.py`에 초기화 코드가 없어 **현재 데이터 수집이 되지 않습니다.** consultation-service 담당자가 초기화 코드를 추가해야 합니다.

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
| **Prometheus** | Advisory RAG 메트릭 | 검색 지연시간, 임베딩 오류율 (가이드 상단 참고) |

> **LLM 관련 지표(토큰 수, 비용, RAG 검색 미스 등)는 Grafana가 아닌 Langfuse에서 확인합니다.**
>
> Grafana에 LLM 전용 대시보드(`auto-loan-review-llm.json`, `llm-overview.json`)를 만들어뒀었지만, 아래 이유로 현재는 비활성화 상태입니다.
> - LLM 트레이싱(프롬프트 내용, 토큰 사용, 비용 추적)은 이 목적에 특화된 Langfuse가 훨씬 적합
> - 챗봇과 ML 심사는 각자 전용 Grafana 대시보드가 있어 중복
>
> 나중에 필요하다면 `infra/grafana/disabled_dashboards/` 폴더에서 파일을 꺼내 provisioning 폴더로 옮기면 즉시 복원됩니다.

### 서비스별 LLM/RAG 연결 현황

| 서비스 | LLM 모델 | Langfuse | Prometheus RAG | 비고 |
|--------|---------|----------|----------------|------|
| auto-loan-review | Gemini (OpenAI compat) | ⚠️ 비활성 기본 | — | `LANGFUSE_ENABLED=true` 설정 시 활성화 |
| consultation-service | OpenAI GPT | ⚠️ 비활성 기본 | — | `CONSULTATION_LANGFUSE_ENABLED=true` 설정 시 활성화 |
| advisory-service | — (RAG only) | ❌ | ✅ 검색·임베딩 메트릭 | Langfuse 없이 Prometheus로 관찰 |
| goal-agent | Claude claude-opus-4-8 | ⚠️ 비활성 기본 | — | `LANGFUSE_ENABLED=true` 설정 시 활성화. API 키 없으면 Mock 자동 전환 |
| review-ai-gateway | Claude claude-opus-4-8 | ⚠️ 비활성 기본 | — | `LANGFUSE_ENABLED=true` + `LLM_PROVIDER=claude` 설정 시 활성화 |

---

## 1. 접속 방법

| 도구 | URL | 계정 |
|------|-----|------|
| Langfuse | `http://localhost:3001` | 가입 후 사용 (최초 접속 시 회원가입) |
| Arize Phoenix | `http://localhost:6006` | 없음 (인증 없음) — **현재 미구현, 데이터 없음** |

> Langfuse는 Docker Compose가 실행 중이어야 접속 가능합니다.
> ```powershell
> docker compose up -d langfuse langfuse-db
> ```

---

## 2. Langfuse 사용 가이드

### 2-1. Langfuse란?

LLM 호출 내역을 기록하고 분석하는 도구입니다. 서비스가 LLM을 호출할 때마다 다음 정보가 자동으로 저장됩니다.

- 어떤 프롬프트를 보냈는지
- AI가 뭐라고 응답했는지
- 토큰을 몇 개 썼고 비용이 얼마인지
- 응답하는 데 얼마나 걸렸는지

### 2-2. 서비스별 Langfuse 활성화 상태

Langfuse는 환경변수로 활성화합니다. 기본값은 **비활성**입니다.

| 서비스 | 상태 | 활성화 조건 |
|--------|------|------------|
| auto-loan-review | ⚠️ 비활성 기본 | `services/auto-loan-review/.env`에 `LANGFUSE_ENABLED=true` |
| consultation-service | ⚠️ 비활성 기본 | `.env`에 `CONSULTATION_LANGFUSE_ENABLED=true` |
| goal-agent | ⚠️ 비활성 기본 | 환경변수 `LANGFUSE_ENABLED=true` |
| review-ai-gateway | ⚠️ 비활성 기본 | `LANGFUSE_ENABLED=true` + `LLM_PROVIDER=claude` (기본값은 mock LLM) |

> **auto-loan-review**: `LangfuseService.java`가 `@ConditionalOnProperty(name = "langfuse.enabled", havingValue = "true")`로 조건부 빈 등록. `.env`에 `LANGFUSE_ENABLED=true` 추가 후 재기동하면 대출 심사 LLM 호출이 Langfuse에 기록됩니다.
>
> **consultation-service**: `rag.py`에 `@observe(name="rag-search")` 데코레이터가 이미 있어, `CONSULTATION_LANGFUSE_ENABLED=true` 설정만 하면 RAG 검색 스팬이 자동으로 Langfuse에 기록됩니다.
>
> **goal-agent**: `agent_goal_chat.py`, `agent_maturity_chat.py`, `agent_spending_chat.py`의 핵심 함수에 `@observe` 데코레이터 적용. `LANGFUSE_ENABLED=true` + `ANTHROPIC_API_KEY` 설정 시 활성화. API 키 없으면 Mock 자동 전환(트레이스 없음).
>
> **review-ai-gateway**: `ClaudeLlmClient.java`에 `LangfuseService` 주입. `LANGFUSE_ENABLED=true` 설정 시 조건부 활성화되지만, 기본 LLM이 Mock이므로 `LLM_PROVIDER=claude` + `CLAUDE_API_KEY`도 함께 설정해야 실제 트레이스가 쌓입니다.

### 2-3. 대시보드 접속

1. `http://localhost:3001` 접속
2. 로그인 후 **EXFul_Bank** 프로젝트 선택
3. 좌측 메뉴에서 확인할 섹션 선택

### 2-4. 주요 메뉴

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

**서비스 구분 방법**: Tags 컬럼 또는 Name 컬럼에서 서비스를 구분합니다.

| Name / Tag | 서비스 |
|-----|--------|
| `auto-loan-review` (tag) | auto-loan-review LLM 호출 |
| `rag-search` (name) | consultation-service RAG 검색 |
| `goal-agent` (name) | goal-agent 목표 달성 플래너 |
| `maturity-agent` (name) | goal-agent 만기 재투자 에이전트 |
| `spending-agent` (name) | goal-agent 지출 패턴 에이전트 |
| `audit-analysis` (tag), `completeWithTools` (name) | review-ai-gateway Claude 호출 |

**필터 사용 방법**:
- `Name` 필터로 특정 함수만 볼 수 있음 (예: `goal-agent`, `rag-search`)

#### 트레이스 상세 보기
트레이스 하나를 클릭하면 다음을 확인할 수 있습니다.

| 항목 | 설명 |
|------|------|
| **Input** | LLM에 보낸 프롬프트 전문 |
| **Output** | LLM이 반환한 응답 전문 |
| **Usage** | 입력 토큰 수 / 출력 토큰 수 / 비용 |
| **Latency** | 응답 소요 시간 |
| **Tags** | 어느 서비스에서 호출했는지 |

### 2-5. 트레이스 이름 의미

| 트레이스 이름 | 발생 시점 |
|-------------|----------|
| `auto-loan-review` | 대출 심사 AI가 LLM을 호출할 때 |
| `rag-search` | consultation-service RAG 검색 스팬 |
| `goal-agent` | 목표 달성 플래너 에이전트 실행 시 |
| `maturity-agent` | 만기 재투자 에이전트 실행 시 |
| `spending-agent` | 지출 패턴 관리 에이전트 실행 시 |
| `audit-analysis` | review-ai-gateway 감사 분석 요청 시 |

### 2-6. 정상 / 주의 기준

| 항목 | 정상 | 주의 | 위험 |
|------|------|------|------|
| LLM 응답시간 (P50) | < 2초 | 2~5초 | 5초 초과 |
| LLM 응답시간 (P95) | < 5초 | 5~10초 | 10초 초과 |
| 시간당 비용 | 서비스별 다름 | 전일 대비 2배 이상 증가 시 확인 | — |
| 오류 트레이스 | 없음 | 간헐적 | 지속 발생 |

### 2-7. 이상 징후별 확인 순서

#### LLM 응답이 갑자기 느려졌다
1. Langfuse **Dashboard** → `Trace latencies` P95 확인
2. 특정 트레이스 이름에서만 느린지 확인
3. **Traces** 목록에서 느린 트레이스 클릭 → Input 길이가 비정상적으로 길지 않은지 확인
4. LLM API 상태 페이지 확인

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

> **현재 미구현 상태입니다.** `arize-phoenix-otel` 패키지는 consultation-service에 설치되어 있으나, `services/consultation-service/app/main.py`에 Phoenix 클라이언트 초기화 코드가 없어 `http://localhost:6006`에 데이터가 수집되지 않습니다.

활성화하려면 consultation-service 담당자가 `main.py`에 Phoenix OTel 초기화 코드를 추가해야 합니다. 관련 패키지(`arize-phoenix-otel`, `openinference-instrumentation-openai`)는 `requirements.txt`에 이미 포함되어 있습니다.

---

## 4. 로컬 실행 방법

### 모니터링 도구 실행
```powershell
# Langfuse 실행
docker compose up -d langfuse langfuse-db
```

### 서비스별 Langfuse 활성화 환경변수

아래는 각 서비스의 `.env` 또는 환경변수에 추가할 내용입니다.

**auto-loan-review** (`services/auto-loan-review/.env`):
```
LANGFUSE_ENABLED=true
LANGFUSE_SECRET_KEY=<your-secret-key>
LANGFUSE_PUBLIC_KEY=<your-public-key>
LANGFUSE_HOST=http://localhost:3001
```

**consultation-service** (`services/consultation-service/.env`):
```
CONSULTATION_LANGFUSE_ENABLED=true
CONSULTATION_LANGFUSE_SECRET_KEY=<your-secret-key>
CONSULTATION_LANGFUSE_PUBLIC_KEY=<your-public-key>
CONSULTATION_LANGFUSE_HOST=http://localhost:3001
```

**goal-agent** (환경변수 또는 `.env`):
```
LANGFUSE_ENABLED=true
LANGFUSE_SECRET_KEY=<your-secret-key>
LANGFUSE_PUBLIC_KEY=<your-public-key>
LANGFUSE_HOST=http://localhost:3001
```

**review-ai-gateway** (`services/review-ai-gateway/.env` 또는 docker-compose):
```
LANGFUSE_ENABLED=true
LANGFUSE_SECRET_KEY=<your-secret-key>
LANGFUSE_PUBLIC_KEY=<your-public-key>
LANGFUSE_HOST=http://localhost:3001
LLM_PROVIDER=claude        # 기본값 mock → Claude 실 호출로 전환
CLAUDE_API_KEY=<your-api-key>
```

> Secret Key / Public Key는 Langfuse UI → 프로젝트 설정 → API Keys에서 발급합니다.

### 테스트 데이터 생성

LLM 호출이 발생해야 Langfuse에 데이터가 쌓입니다. 각 서비스의 실제 엔드포인트를 호출하세요.

---

## 5. 모니터링 연결 검증 방법

처음 세팅하거나 환경이 바뀐 뒤에는 아래 체크리스트를 순서대로 따라가면 연결이 정상인지 확인할 수 있습니다.

---

### Step 1. 컨테이너 실행 확인

```powershell
docker ps | Select-String "langfuse"
```

아래 컨테이너가 보여야 합니다.

| 컨테이너 | 확인 포트 |
|---------|---------|
| `ib-langfuse` | 3001 |
| `ib-langfuse-db` | — (내부) |

보이지 않으면 실행합니다.
```powershell
docker compose up -d langfuse langfuse-db
```

---

### Step 2. Langfuse 활성화 확인

auto-loan-review 서비스 기동 시 터미널 로그를 확인합니다.

```powershell
cd "c:\Users\jaho3\OneDrive\바탕 화면\AX_FULL_Bank\internet_banking"
.\gradlew :services:auto-loan-review:bootRun
```

`langfuse.enabled=true` 설정 시 아래 로그가 출력됩니다.

```
INFO  LangfuseService - [Langfuse] LLM 추적 활성화 → http://localhost:3001
```

> 로그가 보이지 않으면 `services/auto-loan-review/.env` 파일에서 `LANGFUSE_ENABLED=true` 설정을 확인하세요.

---

### Step 3. 테스트 요청 전송

실제 대출 심사 요청을 발생시켜야 Langfuse에 트레이스가 생깁니다. auto-loan-review 서비스에 심사 요청을 보내고 Langfuse **Tracing → Traces**에서 새 트레이스가 생겼는지 확인합니다.

---

### Step 4. Langfuse에서 트레이스 확인

1. `http://localhost:3001` 접속
2. **Tracing → Traces** 클릭
3. 방금 보낸 요청의 트레이스가 목록에 나타나는지 확인

**확인 항목**:

| 항목 | 정상 상태 |
|------|---------|
| 트레이스 이름 | `auto-loan-review` |
| Status | 성공 (오류 없음) |
| Input | 심사 입력 데이터 포함 |
| Output | LLM 응답 포함 |

---

### 최종 체크리스트

| 항목 | 확인 방법 | 결과 |
|------|---------|------|
| Langfuse 컨테이너 실행 | `docker ps` → `ib-langfuse` 확인 | ☐ |
| Langfuse 활성화 로그 | 기동 시 `[Langfuse] LLM 추적 활성화` 로그 확인 | ☐ |
| Langfuse 트레이스 생성 | `auto-loan-review` 트레이스 생성 확인 | ☐ |

---

## 6. "데이터가 안 보인다" 대처법

| 증상 | 원인 | 조치 |
|------|------|------|
| Langfuse에 트레이스가 없음 | `LANGFUSE_ENABLED=false` | `.env`에서 `true`로 변경 후 재시작 |
| Langfuse에 트레이스가 없음 | LLM 호출이 아직 없음 | 대출 심사 요청 전송 후 확인 |
| Langfuse 접속 안 됨 | 컨테이너 미실행 | `docker compose up -d langfuse langfuse-db` |

---

## 7. 환경 변수 정리

### auto-loan-review (`services/auto-loan-review/.env`)

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
| `services/auto-loan-review/src/main/java/.../LangfuseService.java` | Langfuse trace/span/generation 기록 |
| `services/auto-loan-review/src/main/java/.../GeminiOpenAiCompatLlmClient.java` | LLM 호출 시 Langfuse 자동 기록 |
| `services/auto-loan-review/src/main/java/.../RagSearchService.java` | RAG 검색 시 Langfuse span 기록 |
| `services/auto-loan-review/.env` | Langfuse API 키 및 활성화 여부 |
| `services/consultation-service/app/rag.py` | `@observe` 데코레이터 위치 (Langfuse 미초기화로 현재 no-op) |
| `services/advisory-service/.../AdvisoryMetrics.java` | RAG 검색/임베딩 Prometheus 메트릭 |

---

## 9. 관련 가이드

| 문서 | 내용 |
|------|------|
| [INTERNET_BANKING_SERVICE_OVERVIEW_GUIDE.md](INTERNET_BANKING_SERVICE_OVERVIEW_GUIDE.md) | Grafana 서비스 인프라 대시보드 |
| [CHATBOT_GUIDE.md](CHATBOT_GUIDE.md) | 챗봇 상담 Grafana 대시보드 (Prometheus 메트릭) |
| [ML_LOAN_REVIEW_GUIDE.md](ML_LOAN_REVIEW_GUIDE.md) | ML 대출 심사 모니터링 |
| [INFRA_PORTS.md](INFRA_PORTS.md) | 모니터링 인프라 포트 및 설정 파일 위치 |
