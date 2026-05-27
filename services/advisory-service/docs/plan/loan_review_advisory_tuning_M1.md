# 📐 어드바이저리 1개월차 임계치 튜닝 보고서

> **상태**: 템플릿 (운영 1개월 데이터 누적 후 작성)
> **작성 예정일**: 운영 개시 + 30일
> **연관**: [도입 계획](./loan_review_advisory.md) · [진척도](./loan_review_advisory_progress.md) · [운영 룬북](../runbook/advisory.md)

본 문서는 운영 1개월 시점에 누적된 어드바이저리 데이터를 기반으로 룰 임계치·매칭 윈도우를 보정하기 위한 보고서다. **현재는 자리(placeholder)** 만 잡고, 실측은 운영 개시 후 채운다.

---

## 1. 보고서 목적

- Phase 2~3 에 도입된 5 룰의 *false positive / false negative* 비율을 실측
- BIAS 룰의 표본 임계(현재 30) 와 σ 임계(현재 ±2.0) 가 운영에서 적정한지 검증
- PEER 룰의 매칭 윈도우(±5점 / DSR/LTV ±500bps / 90일) 적정성 (plan §10-2 미해결 이슈)
- 결과로 `REVIEW_ADVISORY_RULE.rule_params` JSON 또는 코드 상수의 보정안 도출

---

## 2. 측정 지표 (실측 자리)

| 지표 | 출처 | 1개월 누적 값 | 비고 |
|---|---|---|---|
| 총 리포트 발행 | `advisory_report_published_total` | _TBD_ | rule_cd 별 |
| ack 응답 분포 | `advisory_ack_response_total` | _TBD_ | MAINTAIN / OVERTURN / ESCALATE / NEEDS_MORE_INFO |
| CRITICAL 게이트 차단 | `advisory_critical_gate_blocked_total` | _TBD_ | 차단된 약정 건수 |
| 평균 평가 지연 | `advisory_evaluate_duration_seconds` p50/p99 | _TBD_ | SYNC / BATCH 별 |
| 미해결 누적 (월말) | `advisory_open_reports` gauge | _TBD_ | severity 별 |

### 2.1 룰별 발행 vs ack 분포 매트릭스 (자리)

| rule_cd | 발행 수 | MAINTAIN | OVERTURN | ESCALATE | NEEDS_MORE_INFO | OVERTURN 비율 |
|---|---|---|---|---|---|---|
| DSR_THRESHOLD_OVERRIDE | _N_ | _N_ | _N_ | _N_ | _N_ | _%_ |
| LTV_THRESHOLD_OVERRIDE | _N_ | _N_ | _N_ | _N_ | _N_ | _%_ |
| BIAS_REJECT_RATE_DEVIATION | _N_ | _N_ | _N_ | _N_ | _N_ | _%_ |
| BIAS_APPROVAL_RATE_DEVIATION | _N_ | _N_ | _N_ | _N_ | _N_ | _%_ |
| PEER_DECISION_DIVERGENCE | _N_ | _N_ | _N_ | _N_ | _N_ | _%_ |

**해석 기준**:
- OVERTURN 비율 > 30% → 룰이 *유의미* (결정 번복으로 이어짐) → 유지 또는 강화
- OVERTURN 비율 < 5% → 룰이 *피로 유발* (대부분 무시) → 임계치 완화 또는 비활성 검토
- MAINTAIN > 80% + 리포트 폭증 → false positive 의심

---

## 3. 룰별 보정안 (실측 후 작성)

### 3.1 DSR_THRESHOLD_OVERRIDE / LTV_THRESHOLD_OVERRIDE
- **현재**: STATUS=FAIL 인지 단순 검사
- **검토**: 사전조건이 DSR/LTV PASS 만 통과시키므로 *정상 흐름* 에서는 트리거되지 않음. 트리거 시는 정정(`revise`) 또는 정책 예외 경로.
- **보정 후보**:
  - 트리거 자체는 유지 (CRITICAL 게이트 동작 의미 있음)
  - 정정 경로 통계 추가 — `revise()` 호출 후 트리거 비율을 별도 지표로 분리

### 3.2 BIAS_REJECT_RATE_DEVIATION / BIAS_APPROVAL_RATE_DEVIATION
- **현재**: σ 임계 ±2.0, 표본 임계 30
- **검토**:
  - 표본 30 미만 코호트가 노이즈 양산? → 50/100 상향 검토
  - σ ±2.0 가 너무 엄격해 거의 발화 안 되는 경우 → ±1.5 검토
- **보정 후보**: `MIN_SAMPLE = 30 → 50`, `SIGMA_THRESHOLD = 2.0 → 1.8` (실측 결과 따라)

### 3.3 PEER_DECISION_DIVERGENCE (plan §10-2 미해결 이슈)
- **현재**: 신용점수 ±5점, DSR ±500bps, LTV ±500bps, 90일, 70:30 분기, 표본 ≥ 10
- **검토 항목**:
  | 파라미터 | 현재 | 좁히기 (정밀) | 넓히기 (표본 확보) |
  |---|---|---|---|
  | 신용점수 윈도우 | ±5 점 | ±3 | ±10 |
  | DSR 윈도우 | ±500bps | ±300 | ±800 |
  | LTV 윈도우 | ±500bps | ±300 | ±800 |
  | 룩백 일수 | 90일 | 60 | 120 |
  | 다수 임계 | 70% | 75% (엄격) | 65% (완화) |
- **결정 기준**:
  - 평균 매칭 표본 < 5 건 → 윈도우 넓히기
  - 평균 매칭 표본 > 30 건 + OVERTURN < 5% → 의미 약함, 윈도우 좁히기

---

## 4. rule_params 마이그레이션 자리

운영 보정안이 확정되면 `REVIEW_ADVISORY_RULE.rule_params` JSON 으로 룰 외부화. 현재는 코드 상수.

### 4.1 (TBD) 코드 상수 → JSON 이전
```sql
-- 예시: BIAS_REJECT_RATE_DEVIATION 표본 임계 + σ 임계 외부화
UPDATE review_advisory_rule
   SET rule_params = '{"minSample": 50, "sigmaThreshold": 1.8}',
       rule_version = 'v1.1',
       updated_at = now(),
       updated_by = 0
 WHERE rule_cd = 'BIAS_REJECT_RATE_DEVIATION'
   AND deleted_at IS NULL;
```

룰 클래스 갱신 — `BiasRejectRateDeviationRule` 의 상수 대신 `master.getRuleParams()` JSON 파싱.

### 4.2 룰 변경 감사
모든 임계치 변경은 `PUT /api/advisory/rules/{ruleId}` 로 진행 → `STATUS_HISTORY` 자동 적재. 직접 SQL UPDATE 는 운영 사고 시에만 (감사 로그 누락 위험).

---

## 5. 신규 룰 후보 (실측 후 검토)

1개월 운영 중 발견된 패턴 — *별도 룰* 로 추가가 의미 있는 경우:

| 후보 | 트리거 조건 | severity | 우선순위 |
|---|---|---|---|
| (TBD) 동일 customerId 중복 신청 | 30일 내 동일 고객 N건 이상 | WARN | _TBD_ |
| (TBD) 단기 거절율 급변 | 본인 거절율 vs 본인 30일 평균 +3σ | WARN | _TBD_ |
| (TBD) 약정 후 즉시 정정 | 약정 1일 내 revise 발생 | INFO | _TBD_ |

---

## 6. 후속 조치

- [ ] 본 보고서를 운영 개시 +30일 시점에 채우기 (담당: 운영팀 + 도메인 리드)
- [ ] 보정안 → `rule_params` JSON 마이그레이션 + 룰 클래스 변경
- [ ] Phase 5 종료 + Phase 6 (RAG) 진입 의사결정
- [ ] §10-1 (`COHORT_DIM` 민감속성 검토) / §10-5 (결정 변경 시 리포트 처리) 와 통합 검토

---

## 7. 미해결 이슈 (plan 의 §10 항목 진척)

| 항목 | plan §10 | 1개월차 상태 | 결정 방향 |
|---|---|---|---|
| 코호트 차원 확정 | §10-1 | EMPLOYMENT_TYPE/LOAN_PURPOSE 2종만 가동 | customer-service 연동 후 AGE_BAND/REGION 추가 (Phase 6 이후) |
| PEER 매칭 윈도우 N | §10-2 | ±5점 / ±500bps / 90일 / 70% | 1개월 실측 후 §3.3 표 기반 확정 |
| ack 미이행 SLA | §10-3 | 없음 | 별도 결정 |
| CRITICAL 게이트 우회 | §10-4 | 불가 (룬북 §1.4) | 1개월 후 재검토 |
| 결정 변경 시 리포트 처리 | §10-5 | ✅ **완료** — `revise()` 훅 → 열린 리포트 일괄 RESOLVED (`3c4b55c`) | 결정 완료 |
| 신청인 통지 의무 | §10-6 | 룬북 §4 체크리스트 (법무·CS 합의 미정) | 이의신청 채널 설계 시 |
