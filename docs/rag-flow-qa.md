# 여신 심사 RAG 플로우 — 면접 대비 Q&A

> 작성 목적: 여신 심사 AI Agent의 RAG 파이프라인에 대한 예상 면접 질문 정리.
> 모든 답변은 레포 코드·설계문서 근거. "구현됨 / 설계만 됨 / 안 함"을 구분해 솔직하게 작성.
> 근거 파일은 각 항목에 표기.

---

## 0. 큰 그림 — RAG가 1개가 아니라 3개 + 2단계 진화

| 구분 | 서비스 | 검색 백엔드 |
|------|--------|------------|
| **자동심사** | `auto-loan-review` | Phase E → **Elasticsearch** |
| **자문** | `advisory-service` / `loan-service` | Phase D → **pgvector** |
| **통합** | `doc-agent` / `ai-service` | 공통 규정 단일 인덱스(진행중) |

- 검색 백엔드가 **Phase D(pgvector + FTS 가중합) → Phase E(ES + nori + RRF)** 로 전환 중.
- 질문 대부분이 이 전환축에 걸려 있음.

근거: `docs/plan/rag-corpora.md`, `docs/plan/rag-regulation-consolidation-plan.md`(Phase E), `docs/plan/13_rag_operationalization.md`

---

## 1. 저장 위치 — PostgreSQL vs 벡터 DB

**핵심: pgvector = PostgreSQL의 확장(extension)이지 별도 DB가 아니다.**

- **원본 데이터**는 항상 일반 PostgreSQL/파일에 존재:
  - 유사사례 원천 = `LOAN_REVIEW`(실제 심사기록) → loan_db(PostgreSQL)
  - 정책 원문 = 파일(`.md` / `.pdf` / `.yml`)
- **RAG용 사본**(청킹 + 임베딩)은 별도로 생성·저장:
  - Phase D → pgvector = **PostgreSQL 안** (`ai_embedding` 테이블, `corpus` 컬럼으로 구분)
  - Phase E → **Elasticsearch** (PostgreSQL 밖. 인덱스 `kb_policy_v1` / `kb_similar_cases_v1` / `kb_internal_faq_v1` + alias)

> 정리: 원본은 PostgreSQL에 그대로 두고, **검색용으로 한 벌 더 임베딩 복제**한다. 그 복제본이 pgvector면 PostgreSQL 안, ES면 PostgreSQL 밖에 들어간다.

근거: `rag-corpora.md §2`, `rag-regulation-consolidation-plan.md §7`

---

## 2. 데이터셋 · 크기 · 용어

### 유사사례 데이터셋
- 운영 원천 = 누적 `LOAN_REVIEW`(실제 심사결과)
- cold-start 보강 = **합성 1만 건(HMDA + Home Credit 변환)**, 페르소나 = **Nemotron-Personas-Korea**

근거: `rag-corpora.md §1·§15`, `DATASETS.md`

### 용어 정리
- **Cold-start**: 서비스 초기에 누적 심사기록이 0건이라 유사사례 검색이 빈 결과를 내는 문제. → 진짜 데이터가 쌓이기 전까지 합성 데이터 1만 건을 미리 채워 운영 첫날부터 검색이 동작하게 함.
- **Home Credit**: Kaggle 공개 대출 채무불이행 예측 데이터셋(신청자 정보 + 12개월 내 연체 라벨). 한국 신용 정형데이터 부재로 한국 도메인 적응 후 PD 모델 reference + 합성 유사사례 seed로 사용.
- **Nemotron slim**: NVIDIA의 한국 인구통계 기반 합성 페르소나 100만 명 데이터셋(`Nemotron-Personas-Korea`)을, 원본 1.9GB/26컬럼에서 필요한 **14컬럼만 추려 65.7MB로 줄인 가공본**. 가짜 대출 신청자 생성의 인구 분포 씨앗.
- **코퍼스 chunk**: 청킹해서 나온 조각 1개. "100K~1M chunks"는 용량(MB)이 아니라 **조각 개수**. (정책 1K~5K, 유사케이스 100K~1M, FAQ 0.5K~2K)

### 데이터셋 크기(레포 명시)
- Nemotron slim: 65.7MB / 1,000,000 rows / 14 cols
- home-credit-stability: 522,596행 / home-credit-default: 307,511행 / german_credit: 1,000행 / payments_delinquency: 500,000행 / KOSIS: 178,890행

근거: `DATASETS.md`

### 페르소나 — 삭제되지 않았음 (문서 수정 불필요)
- `data-tools/src/synthesize/persona_sampler.py` = "Layer 1 — Nemotron 페르소나 샘플링" (합성데이터 1단계, 신청자 분포 reweighting)
- `auto-loan-review/.../prompts/purpose_analysis_v1.yml` = LLM 프롬프트에 `페르소나: {{personaSummary}}` 직접 사용
- DATASETS.md의 "12개 컬럼 삭제"는 페르소나 삭제가 아니라 **장황한 서브 설명 12개**(`sports_persona`, `travel_persona`, `hobbies` 등)만 제거. **핵심 `persona` 요약 1줄은 유지**.

근거: `persona_sampler.py:1,16`, `purpose_analysis_v1.yml:19`, `DATASETS.md §1`

---

## 3. 청킹 전략

| 코퍼스 | 청킹 단위 | overlap | 왜 |
|--------|-----------|---------|-----|
| 정책/규정 | **조항 단위**(`제\d+조`/`§`/`\d+\.\d+` 정규식, 항 분리) | 없음 | 법·정책 인용의 최소 **자족 단위 = 조항**. 인용(grounding)이 그 자체로 성립해야 하고, 조항 중간이 잘리면 근거가 깨짐 |
| 유사사례 | **심사 1건 = 1청크**(페르소나+신청+지표+결정+사유 템플릿) | 없음 | 비교 단위가 "과거 1건 전체". 쪼개면 신청과 결정이 분리돼 "유사 사례" 의미가 사라짐 |
| 일반 정책문서(advisory 인제스트) | **800자 / overlap 100자** 슬라이딩 | 100자 | 구조 없는 산문은 의미 단위가 없어 고정 사이즈 + overlap으로 경계 손실 방지 |

- **왜 사이즈 기반이 아니냐**: 구조가 있는 코퍼스(조항/1건)는 의미 경계가 명확해서 고정 길이로 자르면 경계가 깨짐(조항 절단, 두 케이스 혼합). → "구조 있으면 의미 단위, 구조 없으면 고정 사이즈"로 이원화. 공통 상한 512토큰 권장, 초과 시 의미 단위 split.

근거: `rag-corpora.md §4.1/§4.2/§4.4`, `13_rag_operationalization.md §1-1`

---

## 4. 임베딩 모델 — 자동심사 vs 자문이 다른 이유

코드 확인 결과:
- **자동심사**(`auto-loan-review/application.yml:48,59`): Vertex **`text-embedding-005` / 768d**
- **자문**(`loan-service/application.yml:153-154`): OpenAI **`text-embedding-3-small` / 1536d**

**왜 다른가**: 의도적으로 다른 모델을 고른 게 아니라 **백엔드·도입 시점이 달라서** 갈라짐.
- 자문 = pgvector + OpenAI 3-small(1536d)로 먼저 운영(DDL 변경 불필요)
- 자동심사 = ES 전환하며 ES `dense_vector`(int8_hnsw)에 맞춰 768d Vertex 005로
- 통합 계획에서 이 **벡터 공간 불일치(1536 vs 768)** 를 리스크 C1로 명시 → "텍스트 in 검색 API"(호출측은 텍스트만, 임베딩은 doc-agent 수행)로 모델 무관하게 통합 결정

**운영시 bge-m3로 바꾸는 이유**:
1. 한국어/다국어 강점(금융·법률 한국어)
2. 1024d — 품질/저장비용 균형
3. MIT 라이선스 — 상업 사용 자유
4. 자체 ONNX 호스팅 → per-call 비용 0, RPM 무제한, p99<50ms(CPU), 모델 버전 통제 → 외부 API의 쿼터·비용·PII 외부전송 거버넌스 회피
5. dense+sparse+multi-vector(rerank 베이스) 동시 산출

근거: `auto-loan-review/application.yml:48,59`, `loan-service/application.yml:153-154`, `rag-regulation-consolidation-plan.md §3.3 C1`, `rag-corpora.md §3.1/§3.2/§16`

---

## 5. 저장소 인프라 (IVFFlat · 메타필터)

### PostgreSQL vs pgvector
- PostgreSQL = 관계형 DBMS. pgvector = 그 위에 `vector` 타입 + 유사도 연산자(`<=>`) + 벡터 인덱스(IVFFlat/HNSW)를 더한 **확장**. 별개 제품이 아니라 "PostgreSQL에 벡터검색을 붙인 것".
- 장점: 기존 RDB와 한 운영/한 트랜잭션, 메타필터(SQL WHERE)+벡터 결합 쉬움(MRM 단순). 한계: 한국어 BM25·대규모에서 전용엔진보다 약함 → ES 전환 이유.

### IVFFlat이란
- pgvector의 **근사최근접(ANN) 벡터 인덱스**. 전체 벡터를 `lists`개 군집(centroid)으로 미리 나눠두고(Inverted File), 검색 시 질의와 가까운 일부 군집(`probes`개)만 스캔 → 전수비교보다 빠름.
- 근사라 recall 손실 가능, lists·probes로 정확도/속도 trade-off. lists 권장: `rows≤1M → rows/1000`, `>1M → sqrt(rows)`. 현재 lists=100(1만 건 가정). 100만 초과 or p99>200ms면 HNSW로 교체.

### 규모별 인덱스 전략 (exact → IVFFlat → HNSW)
- **소규모 실측/오프라인 평가 = exact(전수비교)**: 문서 10개 수준의 검색전략 검증(§7 `HybridSearchOfflineEvalTest`)에선 ANN 인덱스를 쓰지 않고 전수비교(exact)로 채점 — 인덱스 근사오차를 배제해야 "검색 전략 자체"의 품질을 정확히 비교할 수 있고, 이 규모에선 전수비교가 충분히 빠름.
- **운영 1만~ = IVFFlat**: cold-start 합성 1만 건 기준 lists=100. 근사지만 recall/속도 균형이 맞음.
- **대규모 = HNSW 고려**: 100만 건 초과 또는 p99>200ms 시 HNSW로 교체 설계. 그래프 기반이라 고차원·대용량에서 IVFFlat보다 recall/지연이 우수하나 빌드 비용·메모리↑.
- → 요약: **"작을 땐 exact로 정확히 재고, 운영 규모는 IVFFlat, 대규모는 HNSW"** — 규모에 맞춰 정확도-속도-비용을 단계적으로 트레이드.

### 메타필터를 왜 저장하나
1. **범위 선차단** — `product_code`·`segment`·`corpus`로 후보를 먼저 좁혀 노이즈 제거 + 속도↑
2. **시점 정확성(컴플라이언스)** — `effective_date <= now < expiry_date`로 폐지·미래 정책 인용 차단
3. **룰 결과 자동 인용 매칭** — RuleEngine `(product, segment)` 분기를 `matrix_coord`로 자동 주입 → 룰 결정과 인용 정책 자동 정합
4. **grounding/감사** — 인용 청크의 출처·조항번호 추적
- ES에선 BM25·kNN 양쪽 retriever에 동일하게 `bool.filter` 주입(`EsHybridSearchService.java:151`)

근거: `rag-corpora.md §0.1/§2.2/§2.3/§7`, `13_rag_operationalization.md §6-1`

---

## 6. ES + nori + RRF 전환

**왜 전환**:
- **한국어**: pgvector FTS는 `simple`/`pg_trgm`(n-gram)이라 형태소 인식 X → nori 형태소로 정확도↑
- **하이브리드**: pgvector는 cosine+FTS를 SQL 가중합(α=0.7)으로 섞어 스케일 정규화 필요 → ES RRF는 rank 기반 융합이라 BM25/cosine 스케일 차 무관, α 튜닝 불필요
- 네이티브 HNSW, 재랭커 빌트인(`text_similarity_reranker`) 경로 확보
- 트레이드오프: 별도 클러스터 + Kafka Connect → 운영비↑

**RRF 점수** = `Σ 1/(rank_constant + rank_i)` — BM25/cosine 스케일 정규화 불필요.

근거: `rag-regulation-consolidation-plan.md §6/§8`, `EsHybridSearchService.java`

---

## 7. 검색 전략 오프라인 평가 (HybridSearchOfflineEvalTest)

**"검색 방법 3개 중 뭐가 제일 잘 찾나"를 자동 채점한 시험.**

- 비교 대상: **BM25**(키워드) vs **kNN**(의미벡터) vs **RRF**(둘 합침)
- 방법: 문서 10개 + 정답 아는 질문 12개. 질문 3종류 — exact(원문 그대로), keyword(핵심단어만 겹침), semantic(뜻 같고 단어 다름)
- 채점: **MRR@5**(정답이 상위 5개 중 몇 번째 — 1등 1점/2등 0.5점…), **Recall@5**(상위 5개 안에 정답 들어온 비율)
- 어설션(자동 통과 조건):
  1. RRF가 셋 중 최악 단일 전략보다 20% 이상 나빠지지 않아야 함(합쳐서 망가지지 않음을 보장)
  2. exact 질문은 RRF가 반드시 top-3 안에 정답을 찾아야 함
- 주의: `StubEmbeddingClient`(SHA-256→768d) 사용이라 semantic은 실 임베딩이 아님

→ 요약: **"두 검색법을 RRF로 합치는 게 안전하고 손해 없다"를 숫자로 증명하는 자동 테스트.**

근거: `auto-loan-review/.../HybridSearchOfflineEvalTest.java`

---

## 8. 프롬프트 관리

- ❌ **안 하는 것 = few-shot**: 프롬프트에 예시 Q/A 샘플을 끼워넣는 기법은 안 씀
  - 이유: structured output(`BeanOutputConverter`) 스키마 강제로 형식 일탈 차단이 더 확실 / 핵심은 grounding(근거 주입+출력 검증) / few-shot은 입력토큰↑(비용) + 트랙별 톤 분기라 유지보수 부담
- ✅ **하는 것 = 프롬프트 버전 관리**: YAML 파일로 두고 고쳐서 저장 + 버전 매김
  - `version`, `changelog`, `output_schema`, `temperature` 관리(`purpose_analysis_v1.yml`)
  - 변경 시 `version`+1 → `changelog` 기록 → `PROMPT_REGISTRY.md` 이력 추가 → git 추적
  - `prompt_hash`(sha256)+model+version을 audit에 저장(재현성), StubLLM에 promptId별 결정론 응답(CI), v1(inline)/v2(rag tools) 분기

→ 요약: **프롬프트는 코드처럼 파일로 관리·버전업한다. 다만 few-shot 기법은 안 쓴다.**

근거: `purpose_analysis_v1.yml`, `docs/ai/PROMPT_REGISTRY.md`, `docs/plan/banking-review-llm.md §3.1`

---

## 9. 쿼리 재작성 / 캐싱

- **둘 다 현재 안 함**. 명시적 비-목표: "임베딩 캐시/배치 큐 — 동기 처리 유지, 부하 보고 후 판단"(`13_op §2`). 쿼리 재작성(HyDE/multi-query)도 구현 없음.
- 안 한 이유: MVP 비용/복잡도 통제 / 품질은 하이브리드(RRF)+메타필터+grounding으로 우선 확보 / 정책·케이스가 자주 갱신이라 캐시 stale 위험 + 트래픽 적어 효용 낮음 / 재작성은 LLM 추가 호출 = 비용·지연·또 다른 환각원

근거: `13_rag_operationalization.md §2`

---

## 10. 리랭킹 / 요약 / 출력 캐싱

> cross-encoder 리랭킹은 **현재 운영 코드에 미구현**.
- 자문(pgvector): 1-stage cosine+FTS 가중합(α=0.7) + threshold 0.5 컷. "재랭킹 없음"이 명시 비-목표
- 자동심사(ES): RRF — 엄밀히는 reranking이 아니라 rank fusion
- 계획된 리랭킹(미구현): ① bge-reranker-v2-m3 cross-encoder(1차 top-50→top-5, Track3 한정) ② ES `text_similarity_reranker` 빌트인
- 방식 설명: bi-encoder(임베딩 코사인)는 빠르나 상호작용 약함 / cross-encoder는 (query,doc) 함께 인코딩→정확하나 느림 → 1차로 후보 좁히고 2차만 cross-encoder. RRF는 모델이 아니라 순위 융합 공식.

**요약 방식**: 청크마다 `chunk_summary`(짧은 1줄)를 인제스트 때 미리 생성·저장 → LLM 입력 토큰 절약. advisory는 LLM 전달 직전 점수순 상한(maxChunks)+문자수 절단(maxContentChars)으로 컨텍스트 압축(`AuditFairnessAgent.extractRagChunks`).

**출력 전 캐싱**: 별도 응답 캐시 없음. LLM 출력은 audit 테이블에 영속 + `prompt_hash` 저장(재현성/감사). 임베딩·검색 캐시도 비-목표.

근거: `rag-corpora.md §6`, `rag-regulation-consolidation-plan.md §12`, `13_rag_operationalization.md §2`, `AuditFairnessAgent.java:157-162`

---

## 11. 나중에 해야 할 것 (TODO)

### 비교 테스트 (아직 미수행)
- [ ] **임베딩 모델 간 검색품질 비교 테스트** — 768d Vertex 005 vs OpenAI 3-small 1536d vs bge-m3
  - 동일 코퍼스·동일 쿼리셋으로 MRR/Recall 측정. 현재는 후보 비교 계획 표만 존재(`13_op §3`, `rag-corpora §15`).
- [ ] **pgvector(cosine + pg_trgm) vs ES(RRF) 직접 정량 비교**
  - 현재는 ES 내부 BM25/kNN/RRF 비교만 있음(`HybridSearchOfflineEvalTest`). 엔진 간 비교는 설계 근거 기반 결정 상태.
- [ ] **리랭킹 방식 비교** — 기존(1-stage) vs map-reduce vs refine
  - 토큰·지연·환각 차이를 정량 측정.

### 측정해야 하는 것 (런타임 수치 미기록)
- [ ] **pgvector → Elasticsearch 이관 소요 시간**
  - 임베딩이 1536d → 768d로 바뀌므로 **전량 재임베딩 + 재색인** 필요. `rag-corpora §15`에 "수 시간" 추정만 있고 실측치 없음.
  - 측정 수단(이미 존재): `rag.embedding.duration{model,status}`, `rag.search.duration`, `rag_index_lag_seconds`(Grafana).
