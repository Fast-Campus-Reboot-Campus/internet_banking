## Summary

챗봇 서비스에 키워드 기반 Intent 분류 및 LLM 폴백 응답을 추가하고, Kafka 이벤트 발행을 연동합니다.
상품 문의(금리·가입조건·상품 추천 등)를 Intent로 분류해 DB 조회 응답을 제공하며, 미분류 질문은 OpenAI LLM이 처리합니다.

## Changes

- `IntentClassifier` 키워드 기반 Intent 분류 추가 (PRODUCT_GUIDE, RATE_GUIDE, JOIN_CONDITION, PRODUCT_COMPARE, TERMS_RAG, FAQ)
- `LlmAdapter` (OpenAI gpt-4o-mini) 폴백 응답 구현
- `FeatureAnswerFormatter` DB 조회 결과 자연어 포맷팅 추가
- Kafka 이벤트 발행 연동 (ChatbotStarted, AgentHandoffRequested, ChatMessageSent 등)
- 종료된 상담에 메시지 전송 차단 처리
- `services/consultation-service/README.md` 최초 작성

## Test Plan

- [x] `pytest` 통과 (360 passed, 1 skipped)
- [ ] 수동 검증:
  - `POST /chatbot/consultations/start` → 챗봇 상담 시작 정상 응답 확인
  - "금리 알려줘" 입력 → `RATE_GUIDE` Intent 분류 후 금리 안내 응답 확인
  - "상품 추천해줘" 입력 → `PRODUCT_GUIDE` Intent 분류 후 상품 목록 응답 확인
  - 미분류 자유 텍스트 입력 → LLM 폴백 응답 확인 (OPENAI_API_KEY 설정 시)
  - 종료된 상담에 메시지 전송 시 400 오류 반환 확인
  - `KAFKA_ENABLED=false` 설정 시 Kafka 없이 정상 동작 확인

## Risks / Rollback

- LLM 폴백 응답은 OpenAI API 상태에 의존하며, 장애 시 상담사 연결 안내로 자동 전환됨
- Kafka 미설정 환경에서는 이벤트 발행 없이 정상 동작 (KAFKA_ENABLED=false)
- 롤백 방법: OPENAI_API_KEY 환경 변수 제거 시 기존 상담사 이관 폴백으로 즉시 복귀

## Checklist

- [x] 커밋 메시지가 Conventional Commits 형식 (`docs/AI_GUIDELINES.md` §1)
- [x] `.env` 등 비밀 파일 커밋되지 않음
- [x] AI 모델명·세션 링크·자기 홍보 문구 없음
- [x] 1 PR = 1 의도 원칙 준수
