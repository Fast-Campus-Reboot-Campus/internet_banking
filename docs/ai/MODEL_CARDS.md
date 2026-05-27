# Model Cards

> Last updated: 2026-05-26
> 모든 ML·LLM 모델 버전의 학습 데이터·평가·한계 명시 — banking-review-llm.md §9.

신규 모델 등록 또는 버전 업그레이드 시 본 문서에 항목 추가 필수.

---

## 목록

| 모델 ID | 유형 | 용도 | 상태 |
|---------|------|------|------|
| `hmda_v1` | ML (XGBoost/LightGBM) | Decision score P(APPROVE) | 운영 중 |
| `homecredit_kr_v1` | ML (LightGBM) | PD score P(default_12m) | 운영 중 |
| `gemini-2.5-flash` | LLM | 프롬프트 파이프라인 전체 | 목표 운영 provider |
| `stub-v1` | Stub | 로컬·CI 결정론 테스트 | 개발·테스트 전용 |

---

## ML 모델

### `hmda_v1` — Decision Score 모델

- **목적**: HMDA 데이터 기반 P(APPROVE) 산출. `RuleEngineService`가 트랙 분기에 사용
- **학습 데이터**: HMDA 2018 공개 데이터 + 합성 데이터 Layer 1~3 (한국 금융 도메인 보강)
- **피처 수**: 25개 (Layer 1~3, Layer 4 PD 전용 제외)
- **평가 지표**:
  - AUC-ROC: 0.87 (내부 검증셋)
  - 4/5ths Rule: PASS (protected attributes 기준)
- **한계**:
  - 미국 모기지 데이터 기반 — 한국 금융 관행과 일부 상이
  - 합성 데이터로 보완했으나 실제 대출 포트폴리오 대비 분포 차이 존재
  - 학습 목적·포트폴리오 용도, 합성 데이터 기반 비상업 프로젝트
- **재현성**: `synthetic-data-generator/` 시드 고정, ONNX export 예정
- **버전 이력**:
  - v1 (2026-05): 초기 학습, HMDA 데이터 한국화 보강

---

### `homecredit_kr_v1` — PD 모델

- **목적**: 12개월 내 디폴트 확률 P(default_12m) 산출. `AutoReviewService`가 decision 모델과 결합 분기에 사용
- **학습 데이터**: Home Credit 공개 데이터 (한국 도메인 적응) + 합성 Layer 4 PD 피처
- **피처 수**: 32개 (Layer 1~4, Bureau 정보 포함)
- **평가 지표**:
  - Gini: 0.64 (내부 검증셋)
  - KS Statistic: 0.41
- **한계**:
  - PD 모델 미배포 시 decision 모델 P(REJECT) 폴백 사용 (`AutoReviewService.java:54`)
  - Layer 4 PD 피처 미수신 시 NaN imputation으로 처리 — 정확도 저하 가능
  - 학습 목적·포트폴리오 용도, 합성 데이터 기반 비상업 프로젝트
- **버전 이력**:
  - v1 (2026-05): 초기 학습

---

## LLM 모델

### `gemini-2.5-flash` — 목표 운영 Provider

- **제공자**: Google (AI Studio 무료 API, OpenAI 호환 endpoint)
- **endpoint**: `https://generativelanguage.googleapis.com/v1beta/openai/`
- **용도**: 사전 심사 에이전트 전체 프롬프트 파이프라인
  - `purpose_analysis` — 신청 사유 분석
  - `review_report_track{1,2,3}` — 트랙별 심사 리포트
  - `agent_reasoning_summary` — 에이전트 위험도 요약 (Track 3)
  - `rejection_reason_draft` — 거절 통보문 초안 (Track 2)
- **비용 (AI Studio 무료)**:
  - RPD 한도: 1,500 요청/일
  - RPM 한도: 15 요청/분
  - 초과 시 `LlmRequestRateMeter` → `TemplateFallback` 우회
- **운영 설정**:
  ```yaml
  ai.llm.provider: gemini-openai-compat
  ai.llm.model: gemini-2.5-flash
  ai.llm.base-url: https://generativelanguage.googleapis.com/v1beta/openai/
  ```
- **출력 강제**: structured output (`BeanOutputConverter`) — 자유 텍스트 응답 거부
- **PII 보호**: `PiiAwareChatClient` — 입력 마스킹 + 출력 PII 검사
- **한계**:
  - 무료 한도 초과 시 서비스 불가 → `TemplateFallback`으로 품질 저하
  - 한국어 금융 도메인 특화 파인튜닝 미실시 — 일반 한국어 수준
  - 구조화 출력 파싱 실패 가능 (낮은 확률) → `LlmCallException` + fallback
  - 모델 출력은 심사원 보조 참고용 — 승인/거절 최종 결정은 RuleEngine 단독

---

### `stub-v1` — 개발·테스트 전용 Stub

- **클래스**: `com.bank.ai.llm.client.StubLlmClient`
- **용도**: 로컬 개발·CI 환경에서 외부 API 호출 없이 파이프라인 E2E 검증
- **활성 조건**: `ai.llm.provider=stub` (기본값)
- **동작**: promptId별 결정론적 JSON 응답 — 동일 입력 → 동일 출력
- **지원 promptId**: 6종 (PROMPT_REGISTRY.md 목록 전체)
- **한계**:
  - 실제 LLM 의미 분석 대체 불가 — 인터페이스 계약 검증 전용
  - 프롬프트 품질·한국어 자연스러움 검증 불가

---

## 공정성·감사 원칙

- **4/5ths Rule**: 보호 속성(성별·연령·지역) 기준 승인률 비율 정기 검사
- **Shadow 모드**: 운영 첫 2개월, 자동심사 결과 표시만 — 사람 심사 일치율 측정
- **Drift 감지**: feature PSI 매일 계산, 0.2 초과 시 알람
- **audit_log**: 모든 자동 결정의 입력/출력/모델버전/프롬프트버전 영구 보존
- **재현성**: 합성 데이터 시드 고정, 모델 버전 pin, PromptRegistry YAML 버전 관리
