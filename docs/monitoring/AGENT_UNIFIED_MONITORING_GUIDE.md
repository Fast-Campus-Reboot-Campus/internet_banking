# 에이전트 통합 모니터링 가이드

> 대상 대시보드: **Agent Unified Monitoring**
> 대상 독자: 개발팀 전원
> 환경: Docker Compose 기준

---

## 이 가이드는 무엇인가요?

이 프로젝트에는 AI 에이전트가 4종 있습니다. 에이전트는 일반 서비스와 다르게 **서비스가 살아있어도 내부적으로 제대로 동작하지 않는 경우**가 발생할 수 있습니다.

예를 들어:
- 서비스는 UP인데 LLM 호출이 계속 실패하고 있다
- 서비스는 UP인데 폴백률이 급증해서 실제 심사를 못 하고 있다
- 서비스는 UP인데 챗봇이 대부분 상담사에게 이관되고 있다

이 가이드는 **에이전트 4종이 정상적으로 동작하는지** 한눈에 확인하는 방법을 설명합니다.

---

## 목차

- [1. 접속 방법](#1-접속-방법)
- [2. 에이전트 전체 구조](#2-에이전트-전체-구조)
- [3. 대시보드 구성](#3-대시보드-구성)
- [4. 섹션별 해석](#4-섹션별-해석)
- [5. Alert 목록](#5-alert-목록)
- [6. 이상 징후 대응 절차](#6-이상-징후-대응-절차)
- [7. 관련 가이드](#7-관련-가이드)

---

## 1. 접속 방법

| 도구 | URL |
|------|-----|
| Grafana | `http://localhost:3000` |
| Prometheus | `http://localhost:9090` |

대시보드 경로: **Dashboards → Agent Unified Monitoring**

---

## 2. 에이전트 전체 구조

```
고객 대출 신청
  → auto-loan-review  (대출 심사 에이전트)
      → ai-service    (RAG 정책 검색)
      → LLM 호출      (심사 요약 생성)

감사 신호 발생
  → review-ai-gateway (감사 분석 에이전트, Claude tool-use)
      → ai-service    (RAG 정책 검색)
      → ai-service    (유사 사례 조회)

고객 상담 요청
  → consultation-service (챗봇 상담 에이전트)
      → LLM 호출          (답변 생성)
```

**핵심**: ai-service(RAG)가 느려지면 auto-loan-review와 review-ai-gateway 모두 영향을 받습니다. 공통 인프라 섹션을 함께 확인하세요.

---

## 3. 대시보드 구성

| 섹션 | 내용 |
|------|------|
| 전체 요약 | 에이전트 4종 UP/DOWN 상태 |
| auto-loan-review | 실행 횟수, 지연시간, 폴백, 하드 실패, 불일치 |
| review-ai-gateway | 분석 건수, 소요 시간, 결과 분포, 턴 초과 폴백 |
| consultation-service | 활성 세션, LLM 응답시간/오류, 이관률, 폴백 |
| 공통 인프라 | RAG 검색 지연시간/미스, ML 추론 응답시간/오류 |

---

## 4. 섹션별 해석

### 전체 요약

에이전트 4종의 UP/DOWN 상태를 한눈에 확인합니다.

| 색상 | 의미 |
|------|------|
| 초록 (UP) | 정상 응답 중 |
| 빨강 (DOWN) | 응답 없음 → 즉각 확인 필요 |

---

### auto-loan-review — 대출 심사 에이전트

**용어:**
- **outcome** — 에이전트 실행 결과. `SUCCESS` / `FALLBACK` / `ERROR`
- **폴백** — 에이전트가 정상 처리에 실패하여 대안 응답을 반환한 경우
- **하드 실패** — DSR/LTV 초과 등 규정상 절대 통과 불가 사유로 즉시 거절된 경우
- **불일치** — 에이전트 의견과 규칙엔진 판단이 다른 경우 (에이전트 로직 이상 신호)

| 패널 | 정상 | 주의 |
|------|------|------|
| 실행 횟수 (outcome별) | SUCCESS 비율 높음 | FALLBACK/ERROR 증가 → 에이전트 점검 |
| 실행 지연시간 p95 | 10초 이하 | 30초 이상 → LLM 또는 RAG 응답 지연 확인 |
| 폴백 횟수 | 0에 가까움 | 지속 증가 → 원인 분석 필요 |
| 하드 실패 횟수 | 0 | 발생 즉시 확인 (정상 심사 불가 상태) |
| 불일치 횟수 | 0에 가까움 | 30% 초과 → 에이전트 로직 점검 필요 |

---

### review-ai-gateway — 감사 분석 에이전트

**용어:**
- **BIAS_DETECTION** — 심사관 편향 탐지 분석
- **COMPLIANCE_VERIFICATION** — 규정 준수 검증 분석
- **conclusion** — 분석 결과: `BIAS_SUSPECTED` / `NO_BIAS_DETECTED` / `VIOLATION_SUSPECTED` / `COMPLIANT` / `INSUFFICIENT_DATA`
- **턴 초과 폴백** — Claude가 최대 5턴 내에 결론을 내리지 못해 `INSUFFICIENT_DATA`를 반환한 경우

| 패널 | 정상 | 주의 |
|------|------|------|
| 분석 건수 | 요청에 비례 | 0 지속 → 서비스 연결 확인 |
| 분석 소요 시간 p95 | 30초 이하 | 60초 이상 → LLM 응답 지연 확인 |
| 분석 결과 분포 | 결과 고르게 분포 | INSUFFICIENT_DATA 급증 → 턴 초과 확인 |
| 턴 초과 폴백 | 0 | 발생 시 → 도구 응답 지연 또는 LLM 문제 |

---

### consultation-service — 챗봇 상담 에이전트

**용어:**
- **활성 세션** — 현재 상담 중인 고객 수
- **이관** — 챗봇이 처리하지 못하고 상담사에게 넘긴 경우
- **폴백** — LLM 오류로 상담사 이관 안내로 대체된 응답

| 패널 | 정상 | 주의 |
|------|------|------|
| 활성 세션 수 | 트래픽에 비례 | 비정상적으로 높음 → 세션 누수 확인 |
| LLM 응답시간 p95 | 5초 이하 | 10초 이상 → LLM 서비스 지연 확인 |
| LLM 오류 횟수 | 0 | 증가 → API 키 또는 LLM 서비스 확인 |
| 상담사 이관 횟수 | 낮게 유지 | 50% 초과 → 챗봇 정상 동작 여부 확인 |
| 폴백 횟수 | 0에 가까움 | 증가 → LLM 호출 실패 원인 확인 |

---

### 공통 인프라 — ai-service (RAG + ML)

여러 에이전트가 공통으로 사용하는 서비스입니다. 여기서 문제가 생기면 auto-loan-review와 review-ai-gateway 모두 영향을 받습니다.

**용어:**
- **RAG 검색 미스** — 벡터 검색 결과가 없는 경우 (정책 코퍼스에 관련 내용이 없거나 검색 실패)
- **ML 추론** — 대출 승인/거절 확률을 계산하는 ML 모델 호출

| 패널 | 정상 | 주의 |
|------|------|------|
| RAG 검색 지연시간 p95 | 500ms 이하 | 2초 이상 → 벡터 DB 또는 ai-service 부하 확인 |
| RAG 검색 미스 횟수 | 낮게 유지 | 급증 → 코퍼스 업데이트 또는 검색 로직 점검 |
| ML 추론 응답시간 p95 | 1초 이하 | 5초 이상 → inference-server 상태 확인 |
| ML 추론 오류 횟수 | 0 | 발생 시 → inference-server 연결 확인 |

---

## 5. Alert 목록

| Alert | 조건 | 심각도 | 의미 |
|-------|------|--------|------|
| AgentHighFallbackRate | 폴백률 > 20% (5분) | warning | 에이전트가 정상 처리 못하는 비율 급증 |
| AgentHardFail | 하드 실패 발생 (1분) | critical | 즉각 확인 필요 |
| AgentHighDisagreementRate | 불일치율 > 30% (5분) | warning | 에이전트 로직 이상 의심 |
| ChatbotHighLlmErrorRate | LLM 오류율 > 10% (5분) | warning | 챗봇 LLM 호출 실패 급증 |
| ChatbotHighHandoffRate | 이관률 > 50% (5분) | warning | 챗봇이 대부분 상담사로 넘기는 상황 |
| GatewayLoopTimeoutHigh | 턴 초과 폴백 발생 (2분) | warning | 감사 에이전트가 결론 도출 실패 중 |

---

## 6. 이상 징후 대응 절차

### 에이전트 폴백률 급증 시
1. `http://localhost:9090/alerts` 에서 `AgentHighFallbackRate` 확인
2. 공통 인프라 섹션에서 RAG 검색 지연 여부 확인
3. LLM 호출 오류 여부 확인 (`ai.agent.llm.calls.total` outcome=ERROR)
4. `docker logs ib-auto-loan-review --tail 50` 로 로그 확인

### 챗봇 이관률 급증 시
1. LLM 오류 패널 확인 — LLM 호출 자체가 실패하는지
2. 폴백 패널 확인 — LlmHandoffAdapter로 대체되고 있는지
3. `docker logs ib-consultation-service --tail 50` 로 로그 확인

### RAG 검색 느려질 시
1. `docker ps | Select-String "ai-service"` 로 컨테이너 상태 확인
2. ai-service 로그 확인: `docker logs ib-ai-service --tail 50`
3. auto-loan-review, review-ai-gateway 지연시간도 함께 확인 (연쇄 영향)

---

## 7. 테스트 환경 구성

> 이 섹션은 에이전트 모니터링 대시보드가 실제 데이터로 동작하는지 검증하기 위한 테스트 환경 구성 방법을 설명합니다.

### 현재 상태 (2026-06-02 기준)

에이전트 모니터링 코드(대시보드, alert, 가이드)는 완성됐지만 실제 데이터로 테스트를 진행하지 못했습니다. 이유는 아래와 같습니다.

**이유 1 — 에이전트 서비스들이 단독 실행 구조가 아님**

auto-loan-review, review-ai-gateway, ai-service는 Dockerfile만 있고 docker-compose 설정이 없습니다. `docker compose up` 한 번으로 전체를 실행하는 구조가 아직 갖춰지지 않은 상태입니다.

**이유 2 — 서비스 간 의존성이 복잡함**

에이전트 서비스들은 단독으로 실행할 수 없고, 다른 서비스들이 먼저 실행되어 있어야 합니다.

```
auto-loan-review 실행하려면
  → loan-service, inference-server, Kafka 필요

review-ai-gateway 실행하려면
  → ai-service, loan-service, advisory-service, Kafka 필요
```

결국 프로젝트 전체를 띄워야 테스트가 가능합니다.

**이유 3 — consultation-service는 별도 협의 필요**

consultation-service는 자체 docker-compose를 가지고 있어 메인 인프라와 포트 충돌이 발생합니다. 담당 팀원과 협의 후 메인 docker-compose로 통합해야 합니다.

### 테스트 진행 순서

테스트를 진행하려면 아래 순서로 선행 작업이 필요합니다.

| 순서 | 작업 | 담당 |
|------|------|------|
| 1 | 서비스 전체를 메인 `docker-compose.yml` 하나로 통합 | 팀 전체 협의 |
| 2 | consultation-service 담당자와 docker-compose 통합 협의 | 담당 팀원 |
| 3 | `docker compose up` 으로 전체 서비스 실행 확인 | — |
| 4 | 에이전트 서비스에 실제 API 요청 발송 | — |
| 5 | 대시보드 패널에 데이터가 표시되는지 확인 | — |
| 6 | Alert 발동 시나리오 테스트 (폴백률, 하드 실패 등) | — |

---

## 8. 관련 가이드

| 문서 | 내용 |
|------|------|
| [UNIFIED_MONITORING_GUIDE.md](UNIFIED_MONITORING_GUIDE.md) | 전체 모니터링 스택 구조 및 초기 설정 |
| [ML_LOAN_REVIEW_GUIDE.md](ML_LOAN_REVIEW_GUIDE.md) | ML 대출 심사 모니터링 |
| [LLM_RAG_MONITORING_GUIDE.md](LLM_RAG_MONITORING_GUIDE.md) | Langfuse + Phoenix LLM 추적 |
| [CHATBOT_GUIDE.md](CHATBOT_GUIDE.md) | 챗봇 상담 모니터링 |
