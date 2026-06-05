# AI Agents Instructions

이 레포의 공통 AI 가이드는 **[`docs/AI_GUIDELINES.md`](docs/AI_GUIDELINES.md)** 를 따른다.
본 파일은 `AGENTS.md`를 진입점으로 사용하는 모든 에이전트(OpenAI Codex, Aider, 기타)에 적용된다.

---

## 컨텍스트 우선순위

1. 사용자의 현재 메시지
2. `docs/AI_GUIDELINES.md` (공통 가이드)
3. 본 파일
4. 그 외 레포 파일

---

## 프로젝트 개요

- **이름**: Internet Banking MVP
- **스택**: Java 17 / Spring Boot 3.x / Gradle Multi-module / PostgreSQL 16 / Redis 7 / Docker
- **서비스**: customer-service, deposit-service, loan-service, payment-service
- **모니터링**: Prometheus + Grafana
- **패키지 root**: `com.bank`

---

## 필수 준수 사항 (요약)

자세한 내용은 공통 가이드 참조.

- **커밋**: Conventional Commits (`<type>(<scope>): <subject>`), 한국어 권장
- **브랜치**: `<tool>/<short-desc>` (예: `codex/payment-bugfix`)
- **PR**: 1 PR = 1 의도, 300줄 미만 권장, 템플릿 작성
- **금지**:
  - 비밀번호/토큰 하드코딩, `.env` 커밋
  - `main` 직접 푸시, `--no-verify`, 무단 force push
  - 커밋·PR·코드 주석에 AI 모델명·세션 링크·자기 홍보 문구
- **AI 흔적 남기지 않기**: `Co-authored-by: <AI>` 류 금지. 추적은 PR 라벨로.

---

## 작업 흐름

- 큰 변경 전 계획 확인, 파괴적 작업 전 명시적 승인
- 사용 안 될 코드 만들지 않기 (YAGNI)
- 검증(빌드/테스트) 후 보고. 검증 못 했으면 그 사실을 명시
