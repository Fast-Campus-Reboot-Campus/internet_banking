# 📊 LON 심사관 어드바이저리 도입 진척도

> **상태**: **Phase 6 완료** (RAG 보강 포함 전 기능 구현) / §10-5 완료
> **갱신일**: 2026-05-23
> **연관 문서**: [loan_review_advisory.md](./loan_review_advisory.md) (도입 계획서)

---

## Phase 요약

| Phase | 범위 | 기간(plan) | 커밋 수 | 상태 |
|---|---|---|---|---|
| 1 | 기반 스키마 & 도메인 | 1주 | 7 | ✅ 완료 |
| 2 | 룰 엔진 코어 & 동기 트리거 | 1.5주 | 8 | ✅ 완료 |
| 3 | 비동기 배치 룰 & 코호트 분석 | 1.5주 | 9 | ✅ 완료 |
| 4 | 외부 API & 권한 가드 & 운영 도구 | 1주 | 7 | ✅ 완료 |
| 5 | 관측·튜닝·문서화 | 0.5주 | 4 | ✅ 완료 |
| 6 | RAG 보강 (유사 사례 + 정책 인용) | 2주 | 6 | ✅ 완료 |

---

## Phase 1 — 기반 스키마 & 도메인 (✅ 완료, 7 commits)

| Task | 산출물 | 커밋 | 상태 |
|---|---|---|---|
| 1-1 | `AdvisoryCodeSeeder` (master-service, 9 그룹 × 30 코드) | `78fe612` `feat(master)` | ✅ |
| (plan) | `loan_review_advisory.md` 초안 | `732dca1` `docs(loan)` | ✅ |
| 1-2 | `V2__advisory_tables.sql` (5 테이블 + 인덱스 + FK) | `285de24` `feat(loan)` | ✅ |
| 1-3 | 엔티티 5종 + Repository 5종 (`com.bank.loan.advisory.{domain,repository}`) | `39b5b92` `feat(loan)` | ✅ |
| 1-4 | `loan_erd.md` STAGE 2.5 섹션 신설 + plan STAGE 표기 정정 | `3f06270` `docs(loan)` | ✅ |
| 1-5 | `AdvisoryRepositoryPersistenceTest` (8 케이스) | `9cd5a74` `test(loan)` | ✅ |
| 1-5b | OptimisticLock 픽스 (`saveAndFlush` 반환값 재할당) | `958dd5e` `test(loan)` | ✅ |

### plan 대비 변경 의사결정
1. **코드 시드 위치**: `loan-service` Flyway → `master-service` ApplicationRunner (master_db/loan_db 분리)
2. **마이그레이션 번호**: 테이블 `V3→V2`, RAG `V4→V3` (V1 연속성)
3. **ERD STAGE**: `5.5 → 2.5` (loan_erd.md 의 본심사 위치는 STAGE 2)
4. **테스트 베이스**: `@DataJpaTest` 슬라이스 → `AbstractLoanIntegrationTest` 통합 (프로젝트 관행)

### DoD
- [x] 5 테이블 DDL + 인덱스 (Flyway V2)
- [x] 9 code_group_cd × 30 code_cd 시드 (master-service 부트스트랩)
- [x] 엔티티 5종 + JPA `@JdbcTypeCode(SqlTypes.JSON)` JSONB 매핑 3종
- [x] 영속성 테스트 8/8 통과 (BUILD SUCCESSFUL in 51s)
- [x] `compileJava` 통과 (master + loan)

---

## Phase 2 — 룰 엔진 코어 & 동기 트리거 (✅ 완료, 8 commits)

| Task | 산출물 | 커밋 | 상태 |
|---|---|---|---|
| 2-1 | 룰 엔진 코어 (`AdvisoryRule`/`RuleContext`/`SignalSpec`/`RuleResult`/`EvaluationMode`/`AdvisoryEvaluator`) | `e3e5a2d` `feat(loan)` | ✅ |
| 2-2 | `DsrThresholdOverrideRule` + `AdvisoryRuleSeeder` (DSR 시드) | `4fa6d8c` `feat(loan)` | ✅ |
| 2-3 | `LtvThresholdOverrideRule` + `LtvCalculationRepository.findByApplId...` + Seeder LTV | `b3b4b79` `feat(loan)` | ✅ |
| 2-4 | `LoanReviewService.run()` & `LoanReviewReviseService.revise()` 동기 평가 훅 | `a958ebc` `feat(loan)` | ✅ |
| 2-5 | `AdvisoryAckService` + `findUnresolvedCriticalByRevId` + `LOAN_190/191/192` | `eefaee8` `feat(loan)` | ✅ |
| 2-6 | `LoanContractService.create()` `validateAdvisoryAckGate` (`LOAN_192` 409) | `2a249d6` `feat(loan)` | ✅ |
| 2-7 | `AdvisoryRuleAndGateUnitTest` (9 케이스: DSR/LTV 분기 + evaluator + ack 게이트 해제) | `1e93c8c` `test(loan)` | ✅ |
| 2-8 | `AdvisoryFlowIntegrationTest` (1 풀 시나리오: 본심사 → CRITICAL → 약정 409 → ack → 약정 201) | `6855c44` `test(loan)` | ✅ |

### 룰 카탈로그 (Phase 2 활성)

| `rule_cd` | severity | mode | 트리거 조건 |
|---|---|---|---|
| `DSR_THRESHOLD_OVERRIDE` | CRITICAL | SYNC | DSR FAIL(=OVER_LIMIT) × 본심사 APPROVED |
| `LTV_THRESHOLD_OVERRIDE` | CRITICAL | SYNC | LTV FAIL(=OVER_LIMIT) × 본심사 APPROVED |

### DoD
- [x] DSR/LTV 룰 동기 평가 → CRITICAL 리포트 + signal 자동 적재
- [x] 미해결 CRITICAL 있으면 `POST /api/loan-contracts` 409 `LOAN_192`
- [x] `AdvisoryAckService.acknowledge()` 후 리포트 ACKED + ack row 적재
- [x] ack 후 약정 생성 성공 (201)
- [x] 단위 테스트 9/9, 통합 테스트 1/1 통과 (BUILD SUCCESSFUL)
- [x] 정상 흐름(DSR/LTV PASS) 회귀 없음 (사전조건이 룰 트리거를 자연 차단)

### 알려진 한계
- 정상 본심사 흐름은 사전조건에서 DSR/LTV PASS 만 통과 → CRITICAL 룰은 *정상 운영에서는 트리거 안 됨*. 트리거되는 경우는 본심사 정정(`revise`) 후 결정 변경 + 별도 경로의 DSR/LTV FAIL 상태 (정책 예외 처리 등)
- `POST /advisory/reports/{id}/ack` 외부 API 는 Phase 4 산출물. 현재 ack 는 service-level 호출만 가능

---

## Phase 3 — 비동기 배치 룰 & 코호트 분석 (✅ 완료, 9 commits)

| Task | 산출물 | 커밋 | 상태 |
|---|---|---|---|
| 3-1 | `ReviewerDecisionSnapshotService` — 코호트(EMPLOYMENT_TYPE/LOAN_PURPOSE) × reviewer 집계, peer σ 산출, 멱등 INSERT | `dfa509d` `feat(loan)` | ✅ |
| 3-2 | `InternalAdvisoryBatchController` — `POST /api/internal/advisory/snapshot?baseDate=` | `73ac552` `feat(loan)` | ✅ |
| 3-3 | `BiasRejectRateDeviationRule` (+2σ, sample≥30) + repo `findBySnapshotDate` + Seeder | `974ef65` `feat(loan)` | ✅ |
| 3-4 | `BiasApprovalRateDeviationRule` (-2σ, approve_rate peer σ 자체 계산) + Seeder | `ff4ab23` `feat(loan)` | ✅ |
| 3-5 | `SimilarApplicantFinder` (credit ±5점 / DSR/LTV ±500bps, in-memory) | `050c142` `feat(loan)` | ✅ |
| 3-6 | `PeerDecisionDivergenceRule` (BATCH, 90일 윈도우, 70:30 분기 + 본 건 소수) + Seeder | `41a2330` `feat(loan)` | ✅ |
| 3-7 | `AdvisoryBatchEvaluationService` + `AdvisoryReportPublishedEvent` + `/batch-evaluate` | `81b55c5` `feat(loan)` | ✅ |
| 3-8 | `AdvisoryBiasBatchFlowTest` — 5 reviewer × 30 본심사 시드, A 거절율 +2σ → 30 WARN (2040) | `31bbfe3` `test(loan)` | ✅ |
| 3-9 | `AdvisoryPeerDivergenceFlowTest` — 7:3 분기 + 본 건 소수 결정 시 WARN (2050) | `6b4d46c` `test(loan)` | ✅ |

### Phase 3 활성 룰 (누적 5/5)

| `rule_cd` | severity | mode | 트리거 조건 |
|---|---|---|---|
| `DSR_THRESHOLD_OVERRIDE` | CRITICAL | SYNC | DSR FAIL × 본심사 APPROVED |
| `LTV_THRESHOLD_OVERRIDE` | CRITICAL | SYNC | LTV FAIL × 본심사 APPROVED |
| `BIAS_REJECT_RATE_DEVIATION` | WARN | BATCH | 코호트 거절율 +2σ 초과 (sample ≥ 30) |
| `BIAS_APPROVAL_RATE_DEVIATION` | WARN | BATCH | 코호트 승인율 -2σ 미만 (sample ≥ 30) |
| `PEER_DECISION_DIVERGENCE` | WARN | BATCH | 유사 신청자 90일 70:30 분기 + 본 건 소수 |

### plan 대비 변경 의사결정
1. **코호트 차원**: `AGE_BAND/EMPLOYMENT_TYPE/LOAN_PURPOSE/REGION` 4종 → `EMPLOYMENT_TYPE/LOAN_PURPOSE` 2종 (AGE/REGION 은 customer-service 호출 필요 → §10-1 미해결 이슈)
2. **Spring Batch**: plan 의 Spring Batch 잡 → 단순 `@Service` 메서드 + `@RequestParam` (프로젝트 관행, batch starter 의존성 없음)
3. **approval 룰 peer σ**: snapshot 의 `deviation_sigma` 는 reject 기준이라 approve 룰은 자체 in-memory 계산 (snapshot 컬럼 추가 회피)

### DoD
- [x] `POST /api/internal/advisory/snapshot?baseDate=YYYYMMDD` → 코호트별 row 적재 + peer 평균/σ 산출
- [x] 편향 패턴 시드 → BIAS 룰 트리거 → 일자 본심사 각각에 리포트 1건씩
- [x] PEER 분기 시드 → 소수 결정 본 건에 WARN 리포트
- [x] 민감속성(성별/국적/종교/혼인상태) `cohort_dimension_cd` 입력 미사용 (코드/쿼리 검증)

---

## Phase 4 — 외부 API & 권한 가드 (✅ 완료, 7 commits)

| Task | 산출물 | 커밋 | 상태 |
|---|---|---|---|
| 4-1 | DTO 11종 (Report Summary/List/Detail, Signal, Ack req/res/history, Rule list/update, Stats) | `06867c4` `feat(loan)` | ✅ |
| 4-2 | `AdvisoryReportQueryService` (role 분기 본인 필터) + `AdvisoryReportController` 4 endpoint + `AdvisoryViewerRole` | `787c011` `feat(loan)` | ✅ |
| 4-3 | `AdvisoryRuleAdminService` + `AdvisoryRuleController` 2 endpoint + STATUS_HISTORY BEFORE/AFTER 적재 | `189188f` `feat(loan)` | ✅ |
| 4-4 | `AdvisoryStatsService` + `AdvisoryStatsController` (ack 분포 / 룰별 트리거) + ack repo batch 조회 | `fbacc93` `feat(loan)` | ✅ |
| 4-5 | `AdvisoryRoleGuard` 빈 추출 (X-Actor-Role 헤더 일원화, COMMON_403 throw) | `6a8f6e6` `feat(loan)` | ✅ |
| 4-6 | `AdvisoryExternalApiFlowTest` 13 케이스 (happy path + 권한 거부 + 본인 필터) — 2060 | `e181410` `test(loan)` | ✅ |
| 4-7 | `docs/api/advisory.md` 운영 가이드 (8 endpoint + 권한 매트릭스 + 에러코드 + 운영 시나리오) | `c4d396f` `docs(loan)` | ✅ |

### plan 대비 변경 의사결정
1. **권한 가드 메커니즘**: Spring Security `@PreAuthorize` → `X-Actor-Role` 헤더 + `AdvisoryRoleGuard` 빈
   - 사유: 본 프로젝트에 `spring-boot-starter-security` 미설치. 풀 도입은 모든 endpoint 영향 + 회귀 위험. Spring Security 본 도입은 별도 PR로
2. **OpenAPI 산출물**: `docs/api/advisory.yaml` → springdoc 어노테이션 유지 + `docs/api/advisory.md` 운영 가이드 추가
   - 사유: springdoc 자동 노출(swagger-ui) + 운영자/감사 시나리오 정리 가이드가 더 가치 큼

### DoD
- [x] 8 endpoint 모두 200/4xx 정상 응답 (13 testcase BUILD SUCCESSFUL)
- [x] REVIEWER 가 본인 ack 미처리 리포트만 조회 가능 (타인 GET 시 404 LOAN_190)
- [x] ADMIN 만 룰 임계치 변경 가능, 변경 시 STATUS_HISTORY row 적재 (`PUT /rules` 4-3)
- [x] OpenAPI/운영 가이드에 8 endpoint + 요청/응답 스키마 + 권한·에러 매트릭스 등재

---

## Phase 5 — 관측·튜닝·문서화 (✅ 완료, 4 commits)

| Task | 산출물 | 커밋 | 상태 |
|---|---|---|---|
| 5-1 | `AdvisoryMetrics` — published/ack/gate_blocked counter + open_reports gauge + evaluate_duration timer | `ceee2ef` `feat(loan)` | ✅ |
| 5-2 | Grafana 대시보드 `infra/grafana/provisioning/dashboards/advisory.json` (4 패널) + dashboards provider yaml | `e02dde1` `feat(loan)` | ✅ |
| 5-3 | 운영 룬북 `docs/runbook/advisory.md` (8 섹션 — Cheat sheet, CRITICAL 차단, 폭증, 일배치 실패, 신청인 통지, 관측 이상, 감사, 에스컬레이션) | `9b64240` `docs(loan)` | ✅ |
| 5-4 | 1개월차 튜닝 보고서 자리 `docs/plan/loan_review_advisory_tuning_M1.md` (실측 자리 + §10 미해결 이슈 진척 표) | `9e3ae69` `docs(loan)` | ✅ |

### plan 대비 변경 의사결정
1. **Grafana 대시보드 위치**: `infra/grafana/dashboards/advisory.json` → `infra/grafana/provisioning/dashboards/advisory.json`
   - 사유: docker-compose 가 `provisioning` 디렉터리만 마운트. provisioning 안으로 옮겨 자동 로드
2. **튜닝 보고서**: 운영 실측 후 작성하는 본문 대신 *템플릿 + 채워야 할 자리* 로 작성
   - 사유: 1개월 실측 데이터 없음. 운영 개시 +30일 시점에 채우는 가이드만 제공

### DoD
- [x] `/actuator/prometheus` 에 5 지표 모두 노출 (코드 회귀 검증 BUILD SUCCESSFUL)
- [x] Grafana 대시보드 4 패널 정의 + 자동 로딩 provisioning 적용
- [x] 룬북 §1.4 (CRITICAL 게이트 우회 권한 결정사항: *불가*) 반영 — plan §10-4
- [x] 튜닝 보고서가 PEER 윈도우 N 값 검토 항목 포함 — plan §10-2

---

## §10 미해결 이슈 진척

| 항목 | plan §10 | 상태 | 커밋 |
|---|---|---|---|
| 코호트 차원 확정 | §10-1 | ⏳ EMPLOYMENT_TYPE/LOAN_PURPOSE 2종 가동 (AGE_BAND/REGION 은 Phase 6 이후) | — |
| PEER 매칭 윈도우 N | §10-2 | ⏳ 1개월 실측 후 확정 예정 | — |
| ack 미이행 SLA | §10-3 | ⏳ 결정 보류 | — |
| CRITICAL 게이트 우회 | §10-4 | ✅ **불가**로 확정 (룬북 §1.4 반영) | `9b64240` |
| 결정 변경 시 리포트 처리 | §10-5 | ✅ **완료** — 본심사 결정 변경(`revise`) 시 해당 신청의 열린 ADVISORY 리포트 일괄 RESOLVED 종결 | `3c4b55c` |
| 신청인 통지 의무 | §10-6 | ⏳ 룬북 §4 체크리스트 수준 (법무·CS 합의 미정) | — |

### §10-5 완료 상세 (2026-05-23)

- **구현**: `LoanReviewReviseService.revise()` 결정 변경 훅 → `AdvisoryReportService.resolveAllOpen(applId, reason)` 호출
- **동작**: `OPEN` 상태인 해당 신청의 ADVISORY 리포트를 `RESOLVED` 일괄 전이 + 종결 사유 기록
- **부가**: `V3__advisory_rag_tables.sql` (pgvector + 4 RAG 테이블) DDL 선행 적재 (Phase 6 재개 시 즉시 사용 가능)
- **인프라**: AbstractLoanIntegrationTest pgvector 이미지(`pgvector/pgvector:pg16`) 교체 완료 — V3 vector 확장 지원 (`3027fc8`)

---

## Phase 6 — RAG 보강 (유사 사례 + 정책 인용) (✅ 완료, 6 commits)

| Task | 산출물 | 커밋 | 상태 |
|---|---|---|---|
| 6-1 | `V3__advisory_rag_tables.sql` (pgvector + 4 테이블: advisory_document/chunk/case_index/retrieval_log) | `3c4b55c` `feat(loan)` | ✅ |
| 6-2 | `EmbeddingClient` 인터페이스 + `StubEmbeddingClient` (결정론적 해시 L2-정규화 1536-dim) + 4 RAG 엔티티 + 4 Repository + `AdvisoryCodeSeeder` RAG 코드 추가 | `3808a4f` `feat(loan)` | ✅ |
| 6-3 | `DocumentIngestionService` (정책문서 청크 분할 800자/100 overlap + JdbcTemplate `CAST(? AS vector)` 적재) | `01e225a` `feat(loan)` | ✅ |
| 6-4 | `CaseIndexingService` (과거 사례 PII 마스킹 후 임베딩 적재) + `PiiMaskingUtil` (RRN/ACCT/PHONE/NAME) | `01e225a` `feat(loan)` | ✅ |
| 6-5 | `SimilarCaseRetriever` (cosine `<=>` top-K + RETRIEVAL_LOG 감사) | `01e225a` `feat(loan)` | ✅ |
| 6-6 | `PolicyCitationRetriever` (active 문서 필터 cosine top-K + RETRIEVAL_LOG 감사) | `01e225a` `feat(loan)` | ✅ |
| 6-7 | `AdvisoryEvaluator.attachCitations()` — CRITICAL 룰 발화 시 `advr_payload.citations` 자동 첨부 | `01e225a` `feat(loan)` | ✅ |
| 6-8 | `AdvisoryRagController` (`/similar-cases`, `/citations`) + `InternalAdvisoryRagController` (`/documents`, `/index/cases`) | `01e225a` `feat(loan)` | ✅ |
| 6-9 | `AdvisoryRagFlowTest` 6 케이스 (연도 2070) + `init-pgvector.sql` + embedding DDL 엔티티 픽스 | `d4e8872` `fix(loan)` + `899b579` `test(loan)` | ✅ |
| 6-10 | 진척 문서 Phase 6 완료 갱신 | 이 커밋 `docs(loan)` | ✅ |

### Phase 6 구현 상세

- **임베딩 전략**: `StubEmbeddingClient` — 외부 모델 없이 결정론적 해시 기반 1536-dim 단위 벡터. 동일 텍스트 → 동일 벡터(cosine=1.0). 실 모델 교체 시 `EmbeddingClient` 구현체만 교체.
- **벡터 저장**: JPA DML 우회 (`insertable=false, updatable=false`) + JdbcTemplate `CAST(? AS vector)`. Hibernate DDL은 `columnDefinition="vector(1536)"` 로 컬럼 생성.
- **유사도 검색**: PostgreSQL `embedding <=> CAST(? AS vector)` (cosine distance) + `ORDER BY` + `LIMIT topK`.
- **PII 보호**: `PiiMaskingUtil` — summary_text 저장 전 주민번호/계좌/전화/이름 패턴 마스킹.
- **감사 로그**: 모든 검색(유사 사례·정책 인용)은 `ADVISORY_RETRIEVAL_LOG` append-only 적재.
- **인용 캐싱**: CRITICAL 룰 발화 시 `advr_payload.citations` 에 JSON 저장 → GET `/citations` 는 재검색 없이 캐시 반환.
- **테스트 격리**: 연도 2070, `init-pgvector.sql` (`CREATE EXTENSION IF NOT EXISTS vector`) pgvector 확장 활성화.

### DoD
- [x] POST `/api/internal/advisory/documents` → 201, 청크 생성 + active_yn='Y' 확인
- [x] PUT `/api/internal/advisory/documents/{id}/activate?active=false/true` → 비활성화/재활성화
- [x] POST `/api/internal/advisory/index/cases?revId=` → 사례 인덱싱 + RETRIEVAL_LOG KIND_SIMILAR_CASE
- [x] GET `/api/advisory/reports/{id}/similar-cases` → totalCount ≥ 0 (200)
- [x] GET `/api/advisory/reports/{id}/citations` → 200, citations 배열 확인
- [x] `PiiMaskingUtil` — RRN/ACCT/PHONE/NAME 마스킹, 비PII 보존 단위 검증
- [x] 6/6 테스트 케이스 BUILD SUCCESSFUL (AdvisoryRagFlowTest)

---

## 누적 통계 (2026-05-23 기준)

| 지표 | 값 |
|---|---|
| 완료 Phase | **6 / 6** (Phase 1~5 MVP + Phase 6 RAG) · §10-5 완료 |
| 완료 Task | 46 / 46 (Phase 1~5: 36 + Phase 6: 10) + §10-5 |
| 누적 커밋 | 43 (Phase 1~5: 35 + §10-5/pgvector/docs: 3 + Phase 6: 5) |
| 신규 테이블 | 9 (V2: RULE/REPORT/SIGNAL/ACK/SNAPSHOT + V3: DOCUMENT/DOCUMENT_CHUNK/CASE_INDEX/RETRIEVAL_LOG) |
| 신규 코드 그룹 | 13 (Phase 1~5: 9 + Phase 6: DOC_CATEGORY/EMBEDDING_MODEL/RETRIEVAL_KIND/SCORE_BAND 4) |
| 신규 에러코드 | 3 (LOAN_190/191/192) |
| 활성 룰 | 5 / 5 (DSR/LTV CRITICAL + BIAS reject/approve + PEER WARN) |
| 신규 internal API | 5 (`/snapshot`, `/batch-evaluate` + RAG: `/documents`, `/documents/{id}/activate`, `/index/cases`) |
| 신규 외부 API | 9 (`/reports` 4 + `/rules` 2 + `/stats` 1 + RAG: `/similar-cases`, `/citations`) |
| 신규 DTO | 15 (Phase 1~5: 11 + Phase 6: DocumentRegister req/res, SimilarCaseResponse, PolicyCitationResponse) |
| 신규 Prometheus 지표 | 5 (published/ack/gate_blocked/open_reports/evaluate_duration) |
| 신규 테스트 케이스 | 39 (Phase 1~5: 33 + Phase 6 RAG: 6) |

### 테스트 격리 연도 사용 현황
| 연도 | 사용 테스트 |
|---|---|
| 2030 | `AdvisoryFlowIntegrationTest` (Phase 2) |
| 2040 | `AdvisoryRepositoryPersistenceTest` snapshot, `AdvisoryBiasBatchFlowTest` (Phase 3) |
| 2050 | `AdvisoryPeerDivergenceFlowTest` (Phase 3) |
| 2060 | `AdvisoryExternalApiFlowTest` (Phase 4) |
| 2070 | `AdvisoryRagFlowTest` (Phase 6 RAG) |
