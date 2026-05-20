# AI 학습/평가용 데이터셋 목록

본 문서는 ai-service의 ML/LLM 파이프라인에 사용되는 **모든 외부 데이터셋의 출처, 라이선스, 버전, 가공 내역**을 기록한다. 신규 데이터셋 추가 시 반드시 본 문서에 항목 추가.

---

## 1. nvidia/Nemotron-Personas-Korea

### 출처
- HuggingFace: <https://huggingface.co/datasets/nvidia/Nemotron-Personas-Korea>
- 제공자: NVIDIA
- 발행일: 2026-04-20 (last modified 2026-04-23)

### 버전 (재현성 pin)
- Revision (commit SHA): `d0a9272116a2ebf139b964ca72b8b8f604616689`
- Branch: `main`
- 다운로드 일자: 2026-05-19

### 라이선스: CC BY 4.0
- 상업/비상업 사용 자유
- **의무**: 저작자 표시(attribution), 변경 사항 표시
- 본 프로젝트는 교육/포트폴리오 목적. CC BY 4.0 준수 위해:
  - 본 문서에 출처 명시 (현재 항목)
  - 원본 README.md를 `data/synthetic/personas/README.md`로 보관
  - 가공본(slim) 사용 시 "Modified from NVIDIA Nemotron-Personas-Korea (CC BY 4.0)" 표기

### 원본 사양
- 총 1,000,000 records × 26 columns
- Parquet 9 샤드, 원본 1.9 GB
- 데이터 소스: KOSIS, 대법원, NHIS, KREI, NAVER Cloud
- 인구학적 분포: 한국 실통계 기반 (19세 이상 성인만)

### 본 프로젝트 가공본 (slim)
- 위치: `data/synthetic/personas/slim/` (`.gitignore` 처리됨, 레포 미포함)
- 형식: Parquet, zstd level 9 압축
- 크기: **65.7 MB** (원본 대비 96.5% 감소)
- 레코드: 1,000,000 그대로
- **사용 14 컬럼**:
  | 컬럼 | 타입 | 용도 |
  |------|------|------|
  | `uuid` | string | 페르소나 식별자 (PK) |
  | `sex` | string | 성별 (남자/여자) — 공정성 평가 |
  | `age` | int8 | 19~99, 다운캐스트 |
  | `marital_status` | string | 기혼/미혼/이혼/사별 |
  | `military_status` | string | 군필/미필/면제/해당없음 |
  | `family_type` | string | 39 카테고리 (배우자+자녀 등) |
  | `housing_type` | string | 아파트/단독주택 등 6종 |
  | `education_level` | string | 7단계 (초등~대학원) |
  | `bachelors_field` | string | 학사 전공 |
  | `occupation` | string | 직업 (KSCO 기반) |
  | `district` | string | 시군구 (252+ 종) |
  | `province` | string | 17개 광역시도 |
  | `country` | string | "대한민국" |
  | `persona` | string | 1~2문장 요약 (LLM narrative 입력용) |

- **삭제 컬럼 (12)**: `professional_persona`, `sports_persona`, `arts_persona`, `travel_persona`, `culinary_persona`, `family_persona`, `cultural_background`, `skills_and_expertise`, `skills_and_expertise_list`, `hobbies_and_interests`, `hobbies_and_interests_list`, `career_goals_and_ambitions`
- 삭제 사유: 자동심사 Phase 1.1 Layer 1(Persona Base)에는 인구학 + 요약만 필요. narrative는 Phase 1.1 Layer 3에서 LLM이 새로 생성. 추후 narrative 필요 시 원본 재다운로드(소요 ~15분).

### 한계 / 사용 주의
- **19세 이상 성인만** → 청소년 대출은 어차피 대상 외라 무관
- **기업 페르소나 없음** → 본 자동심사 범위(개인대출)에 무관
- **일반 인구 분포 vs 대출 신청자 분포 차이**:
  - 페르소나는 KOSIS 일반 인구 분포 기반
  - 실제 대출 신청자는 셀프 셀렉션(청년/자영업자 over-represented)
  - **대응**: 합성 파이프라인에 명시적 reweighting 함수 분리 — 실데이터 도입 시 이 단계만 교체
- **demographic 변수 독립성 가정** 일부 적용 → 페르소나끼리 직업↔거주지 등 상관관계는 보존되나, 변수 일부는 한계 분포만 매칭
- **합성 데이터**: 실존 인물과의 유사성은 우연. PII 아님

### 재다운로드 절차
```bash
# pip install -U huggingface_hub
hf download nvidia/Nemotron-Personas-Korea \
  --repo-type dataset \
  --revision d0a9272116a2ebf139b964ca72b8b8f604616689 \
  --local-dir data/synthetic/personas
```

### slim 재생성 절차
원본 다운로드 후 `synthetic-data-generator` Gradle subproject의 `SlimifyPersonas` 태스크 (또는 동등 Python 스크립트)로 14개 컬럼 추출 + zstd-9 압축.

---

---

## 2. HuggingFace 외부 신용·연체 데이터 (자동심사 학습 reference)

한국어 정형 신용 데이터가 사실상 부재해, 영어 클래식·합성 데이터를 모델 구조 검증·SHAP sanity check·분포 비교용으로 사용. 직접 학습 데이터 아님 — Layer 4 oracle 규칙 보정과 외부 reference 비교에 사용.

저장 위치: `data/external/credit/` (`.gitignore` 처리)

### 2.1 deburky/home-credit-credit-risk-model-stability
- 출처: <https://huggingface.co/datasets/deburky/home-credit-credit-risk-model-stability>
- 라이선스: Other (확인 필요, 학습/포트폴리오 한정 사용)
- 규모: 522,596 rows × 48 cols, target 이진 (default 3.3%)
- 핵심 컬럼: DPD(연체일수) 다종, credit amount, age, sex, education, marital state
- 용도: 단일 테이블 형식이라 즉시 학습 가능. 공정성 평가에 인구학 컬럼 포함.

### 2.2 mohameddhameem/home-credit-default-risk
- 출처: <https://huggingface.co/datasets/mohameddhameem/home-credit-default-risk>
- 라이선스: **CC-BY-4.0**
- 규모: application_train 307,511 rows × 122 cols + 보조 6 테이블 (bureau, credit_card_balance, installments_payments, pos_cash_balance, previous_application)
- 핵심: EXT_SOURCE_1/2/3 외부 신용점수, AMT_INCOME_TOTAL, AMT_CREDIT, DAYS_BIRTH 등
- 용도: 실제 다중 테이블 심사 시나리오. 다리 모델 학습 외부 reference.

### 2.3 marcilioduarte/german_credit_risk
- 출처: <https://huggingface.co/datasets/marcilioduarte/german_credit_risk>
- 라이선스: (Kaggle/PSU 출처, 공개)
- 규모: 1,000 rows × 21 cols, target `Creditability`
- 용도: 클래식 데이터셋. SHAP 결과가 교과서적으로 나오는지 sanity check.

### 2.4 hpestrellag/payments_delinquency
- 출처: <https://huggingface.co/datasets/hpestrellag/payments_delinquency>
- 라이선스: **CC-BY-NC-4.0** (비상업 OK)
- 규모: 500,000 rows × 9 cols (합성)
- 핵심: 결제 거절·연체 알람 행동
- 용도: 우리 합성 데이터 분포 비교 reference.

---

## 3. 한국 공공 API 데이터 (한국 macro 분포 시드)

자동심사 합성 파이프라인의 한국 분포 시드와 Oracle base rate 보정에 사용.

저장 위치: `data/external/korean/<source>/` (`.gitignore` 처리)
수집 코드: `synthetic-data-generator/src/loaders/`

### 3.1 한국은행 ECOS (3개 시계열)

API: <https://ecos.bok.or.kr/api/> · 키 환경변수 `KOREA_BANK_API_KEY`

| 통계코드 | 주기 | rows | 친근명 | 용도 |
|---------|-----|------|--------|------|
| `151Y005` | Q | 297 | household_credit | 가계신용 잔액 (Layer 4 macro feature) |
| `104Y014` | M | 891 | deposit_inst_household_loan | 예금취급기관 가계대출 |
| `722Y001` | M | 900 | policy_rate | 한국은행 기준금리·여수신금리 (금리 환경 feature) |

### 3.2 통계청 KOSIS — 가계금융복지조사 (23개 통계표, 178,890 rows)

API: <https://kosis.kr/openapi/> · 키 환경변수 `KOSIS_API_KEY` · 모두 orgId=`101`

**자산·부채·소득 결합 (4)**
| tblId | rows | 친근명 |
|-------|------|--------|
| `DT_1HDAAA10` | 15,240 | income_quintile_asset_debt |
| `DT_1HDAAA22` | 10,692 | income_decile_asset_debt |
| `DT_1HDAAA09` | 12,700 | employment_status_asset_debt |
| `DT_1HDAAA14` | 31,104 | income_x_asset_quintile_asset_debt |

**가구주 특성별 (8) — 피처 분포·공정성 시드**
| tblId | rows | 친근명 |
|-------|------|--------|
| `DT_1HDAAA05` | 7,620 | gender_asset_debt |
| `DT_1HDAAA06` | 20,316 | age_group_asset_debt |
| `DT_1HDAAA07` | 12,700 | marital_status_asset_debt |
| `DT_1HDAAA08` | 12,700 | education_asset_debt |
| `DT_1HDAAA03` | 12,670 | housing_tenure_asset_debt |
| `DT_1HDAAA02` | 12,696 | housing_type_asset_debt |
| `DT_1HDAAA04` | 15,236 | household_size_asset_debt |
| `DT_1HDAAB01` | 3,712 | income_source_decomposition |

**재무건전성 (5) — DSR/부채상환능력 지표**
| tblId | rows | 친근명 |
|-------|------|--------|
| `DT_1HDAAA17` | 720 | employment_status_financial_health |
| `DT_1HDAAA16` | 1,152 | age_group_financial_health |
| `DT_1HDAAA18` | 864 | income_quintile_financial_health |
| `DT_1HDAAA19` | 864 | asset_quintile_financial_health |
| `DT_1HDAAA20` | 864 | net_asset_quintile_financial_health |

**담보부채 비율 (3) — LTV proxy**
| tblId | rows | 친근명 |
|-------|------|--------|
| `DT_1HDAAC03` | 1,200 | income_quintile_collateral_debt_ratio |
| `DT_1HDAAC01` | 1,600 | age_group_collateral_debt_ratio |
| `DT_1HDAAC04` | 1,200 | asset_quintile_collateral_debt_ratio |

**신용부채 비율 (3) — 무담보 부담 proxy**
| tblId | rows | 친근명 |
|-------|------|--------|
| `DT_1HDAAC08` | 960 | income_quintile_credit_debt_ratio |
| `DT_1HDAAC06` | 1,280 | age_group_credit_debt_ratio |
| `DT_1HDAAC07` | 800 | employment_status_credit_debt_ratio |

### 3.3 공공데이터포털 / FISIS — 보류

| 소스 | 상태 |
|------|------|
| 공공데이터포털 (`PUBLIC_DATA_API_KEY`) | 데이터셋별 endpoint 활용신청 승인 후 운영 URL 발급. 코드 골격은 `loaders/data_go_kr.py` 에 placeholder |
| 금감원 FISIS (`FISIS_API_KEY`) | ECOS 와 통계 중복 다수. placeholder 만 유지 |

---

## 재현 절차

전체 한국 공공 데이터 수집:
```bash
cd synthetic-data-generator
pip install -r requirements.txt
python -m scripts.fetch_all --source ecos,kosis
```

KOSIS API 는 통계표별 분류 차원 수가 달라 `loaders/kosis.py` 의 `fetch_table` 이 자동 depth 1~6 fallback 으로 처리.

HuggingFace 데이터셋 재다운로드:
```bash
hf download <dataset_id> --repo-type dataset --local-dir data/external/credit/<short_name>
```
