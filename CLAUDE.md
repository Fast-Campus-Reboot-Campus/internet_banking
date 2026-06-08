# Claude Code Instructions

이 레포의 공통 AI 가이드는 **[`docs/AI_GUIDELINES.md`](docs/AI_GUIDELINES.md)** 를 따른다.
본 파일은 Claude Code 환경에서만 적용되는 추가 사항이다.

---

## 컨텍스트 우선순위

1. 사용자의 현재 메시지
2. `docs/AI_GUIDELINES.md` (공통 가이드)
3. 본 파일 (Claude Code 특이사항)
4. 그 외 레포 파일

상충 시 위쪽이 우선.

---

## 프로젝트 개요

- **이름**: Internet Banking MVP
- **스택**: Java 17 / Spring Boot 3.x / Gradle Multi-module / PostgreSQL 16 / Redis 7 / Docker
- **서비스**: customer-service, deposit-service, loan-service, payment-service
- **모니터링**: Prometheus + Grafana
- **패키지 root**: `com.bank`

상세 구조는 `README.md` 참조 (작성 예정).

---

## Claude Code 특이사항

### 자동 허용 권장 명령
다음은 부작용이 작거나 검증용이라 매번 묻지 않아도 무방:
- `./gradlew build`, `./gradlew test`, `./gradlew :services:*:bootRun`
- `docker compose ps`, `docker compose logs`
- `git status`, `git diff`, `git log`
- `curl http://localhost:*`

(실제 허용 설정은 `.claude/settings.json` 또는 사용자 권한 모드로 관리)

### 금지
- 운영/스테이징 DB 접속 명령
- `main` 브랜치 직접 커밋·푸시
- `.env` 파일 커밋
- 커밋 메시지·PR 본문에 AI 모델명·세션 링크·자기 홍보 문구 삽입 (공통 가이드 §5)

### 작업 흐름
- 큰 변경(파일 5개 이상 또는 설계 변경) 전엔 계획을 보여주고 승인 받기
- 파괴적 작업(`rm`, `reset --hard`, `force push`) 전 명시적 승인 받기
- 브랜치 네이밍: `claude/<short-desc>`
- 커밋 컨벤션: 공통 가이드 §1 (Conventional Commits, 한국어 권장)
