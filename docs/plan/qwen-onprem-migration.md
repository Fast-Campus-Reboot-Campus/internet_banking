# 사내 온프레미스 LLM 단일화 — Qwen2.5-72B 전환 계획

> Last updated: 2026-06-05
> Status: 설계 (착수 전). 코드 변경 없음.
> 선행 문서: `docs/plan/llm-pipeline.md`, `docs/plan/banking-review-llm.md`, `docs/plan/agent-loop-limits-metrics-plan.md`
> 관련 서비스: `auto-loan-review`, `review-ai-gateway`

본 문서는 은행 내부망(폐쇄망) 운영을 위해 외부 LLM API 의존을 제거하고,
오픈웨이트 모델 **Qwen2.5-72B-Instruct** 를 사내 GPU 서버에 셀프호스팅해
loan 도메인의 모든 LLM 호출을 단일 모델로 통합하는 계획이다.

---

## 0. 동기·전제

1. **내부망 강제**. 행원 업무 환경은 폐쇄망이라 `api.anthropic.com` ·
   Google AI Studio 등 외부 엔드포인트 호출 불가 → 자체 호스팅이 강제 전제.
2. **단일 모델 정책**. 운영·검증·비용 단순화를 위해 두 서비스를 한 모델
   (Qwen2.5-72B-Instruct) + 단일 vLLM 엔드포인트로 통합.
3. **결정권 없음 원칙 유지** (`banking-review-llm` §5). LLM 출력은 feature 보강·
   정보 제공만. 승인/거절/트랙 분기는 RuleEngine 유지.

---

## 1. 비용·라이선스

| 항목 | 내용 |
|------|------|
| 모델 가중치 | 무료 (HuggingFace / ModelScope 다운로드) |
| 라이선스 | **Qwen License** (Apache 2.0 아님). 제약은 "월 활성사용자 1억 명 초과 시 별도 라이선스" 한 조항 → 사내 행원용은 무료 사용 가능 |
| API 과금 | 0원 (셀프호스팅) |
| 실비용 | **GPU 인프라**. FP16 ≈ 144GB VRAM (A100 80GB ×2). 4bit 양자화(AWQ/GPTQ) ≈ 40~48GB → A100 80GB ×1 또는 L40S ×2 수준 |

→ 결론: 가중치·라이선스는 본 용도에서 무료, 비용은 사내 GPU 서버로 한정.

---

## 2. 서빙 레이어 (전제 인프라)

**vLLM** 으로 Qwen2.5-72B-Instruct 서빙 → OpenAI 호환
`/v1/chat/completions` 엔드포인트 노출.

```
사내 GPU 서버: vLLM serve Qwen2.5-72B-Instruct-AWQ
  → http://qwen-vllm.internal:8000/v1
```

핵심: vLLM 이 OpenAI 호환이므로 **두 서비스가 동일 엔드포인트 하나를 공유**.
양자화·텐서병렬·max-model-len 등 서빙 파라미터는 Step 0 PoC 에서 확정.

---

## 3. 서비스별 영향도

### 3.1 `auto-loan-review` — 🟢 거의 설정만

이미 Spring AI `OpenAiChatModel` 추상화 완료
(`GeminiOpenAiCompatLlmClient`, `GeminiOpenAiCompatConfig`).
vLLM 이 OpenAI 호환이라 **클라이언트 코드 변경 사실상 불필요**.

작업:
- provider 값 일반화(`openai-compat`) 또는 `qwen-vllm` 분기 추가
- `application.yml`: `ai.llm.base-url` → vLLM 주소, `model` → `Qwen2.5-72B-Instruct`
- 프롬프트 yml 8종(`review_report_track{1,2,3}_v{1,2}`, `purpose_analysis_v1`,
  `rejection_reason_draft_v2`)의 `model.default/fallback` 교체
- 단일 모델화 시 `fallback` 의미 재정의 필요(§5 참조)
- 구조화 출력은 `BeanOutputConverter` 가 JSON Schema 를 system prompt 에
  append 하는 방식 → Qwen instruction-following 으로 충분히 커버 예상

리스크: 낮음.

### 3.2 `review-ai-gateway` — 🔴 신규 클라이언트 필요

`ClaudeLlmClient` 가 Anthropic 전용:
- `/v1/messages` 엔드포인트, `x-api-key` · `anthropic-version` 헤더
- content-block 구조(`content[].type`, `tool_use`, `input_schema`)
- agentic tool-calling 이 Anthropic 포맷에 결합
  (`ClaudeAgenticResponse`, `ToolCall`, `AgenticLoop`)

작업: **`QwenLlmClient` 신규 작성** — OpenAI `/v1/chat/completions` 포맷 번역
- `LlmClient.complete()` — 단순 (system+user → `choices[0].message.content`)
- `ToolAwareLlmClient.completeWithTools()` — **핵심 작업**.
  Anthropic `tools`/`tool_use` ↔ OpenAI `tools[].function`/`tool_calls`
  포맷 양방향 변환, `AgenticLoop` 가 소비하는 `ClaudeAgenticResponse` 로 매핑

리스크: 높음 (포맷 변환 + tool-calling 추론 품질 회귀).

---

## 4. 핵심 의사결정 포인트 (착수 전 확정)

1. **agentic tool-calling 충실도** — Qwen2.5-72B function calling 지원하나
   Opus 4.7 수준 멀티턴 tool 추론은 검증 필요. 편향심사 품질 회귀 테스트 필수.
2. **`ClaudeAgenticResponse` 추상화** — Anthropic 색채 제거한 벤더 중립 DTO 로
   리네이밍할지, Qwen 응답을 기존 DTO 에 매핑할지.
3. **fallback 전략** — 단일화 시 모델 fallback 소멸. Qwen 장애 시 template
   fallback 만 남는 구조가 맞는지(§3.1, banking-review-llm §5).
4. **하드웨어·양자화** — FP16 vs AWQ/GPTQ 4bit, GPU 대수, max-model-len.

---

## 5. 단계 (한 단계씩 진행 후 커밋·보고)

| Step | 내용 | 산출물 |
|------|------|--------|
| 0 | vLLM 서빙 PoC + 양자화/하드웨어 확정 | 인프라 스펙 문서 |
| 1 | `auto-loan-review` provider 전환 | 코드+설정 커밋 / 테스트 커밋(분리) |
| 2 | `review-ai-gateway` `QwenLlmClient` (non-tool) | 코드+테스트 |
| 3 | tool-calling 포맷 변환 + agentic loop 연동 | 코드+테스트 |
| 4 | 품질 회귀 검증 (편향심사 골든셋) | 검증 리포트 |

> feat / test 커밋은 항상 분리. 각 Step 종료 시 커밋 + 보고 후 정지.

---

## 6. 미해결·후속

- Qwen 한국어 금융 도메인 프롬프트 튜닝 필요 여부 (기존 Gemini/Claude 프롬프트 재사용 가능성)
- 비용 메터(`LlmCostMeter`) 의미 변경 — 셀프호스팅은 토큰 단가 0, GPU 가동률 기반 지표로 전환 검토
- `docs/ai/PROMPT_REGISTRY.md` 모델 메타 갱신
