# GitHub Actions + Claude API 자동 코드 리뷰

> Last updated: 2026-05-22
> Scope: PR이 열리거나 커밋이 추가될 때 Claude API를 통해 자동으로 코드 리뷰를 수행하는 CI 파이프라인.

---

## 1. 개요

### 왜 도입하는가
- 사람 리뷰어의 부담을 줄이고, **반복적인 스타일·보안·패턴 지적은 자동화**에 위임
- 리뷰 사각지대(야간 커밋, 소규모 PR) 방지
- `AI_GUIDELINES.md` 준수 여부를 사람이 일일이 체크하지 않아도 되도록

### 무엇을 리뷰하는가
`AI_GUIDELINES.md`에 정의된 규칙을 기준으로 리뷰한다. 리뷰 관점은 아래 §4 참고.

서비스별 도메인 세부 기준은 각 담당자가 별도로 관리한다. 본 파이프라인은 **공통 기준만** 적용.

---

## 2. 동작 흐름

```
PR 오픈 / 커밋 추가
        ↓
GitHub Actions 트리거 (.github/workflows/code-review.yml)
        ↓
PR diff 수집 (변경된 파일 목록 + 내용)
        ↓
Claude API 호출 (시스템 프롬프트 + diff)
        ↓
리뷰 코멘트 생성
        ↓
PR에 코멘트 게시 (GitHub API)
```

### 트리거 조건
- `pull_request` 이벤트: `opened`, `synchronize`
- 대상 브랜치: `main`으로 향하는 PR
- 스킵 조건: PR 제목에 `[skip review]` 포함 시 실행 안 함

---

## 3. 설정 방법

### 3.1 필요한 GitHub Secrets

| Secret 이름 | 설명 | 발급처 |
|---|---|---|
| `ANTHROPIC_API_KEY` | Claude API 인증 키 | console.anthropic.com |
| `GITHUB_TOKEN` | PR 코멘트 게시용 | Actions 자동 제공 (별도 발급 불필요) |

Secrets 등록: GitHub 레포 → Settings → Secrets and variables → Actions → New repository secret

### 3.2 워크플로 파일 위치

```
.github/
└── workflows/
    └── code-review.yml     ← 워크플로 정의 (구현 시 생성)
```

### 3.3 권한 설정

`code-review.yml` 내에 다음 권한이 필요하다:

```yaml
permissions:
  contents: read
  pull-requests: write    # PR 코멘트 게시
```

---

## 4. 리뷰 범위

Claude에게 전달되는 리뷰 기준. `AI_GUIDELINES.md` 섹션 번호로 참조.

| 관점 | 기준 문서 | 주요 체크 |
|---|---|---|
| 코드 스타일 | §4 | Lombok 정책, DTO·엔티티 분리, 패키지 구조 |
| 모듈 의존 방향 | §12 | controller→service→repository 단방향, 서비스 간 직접 import 금지 |
| SQL / JPA | §13 | `SELECT *` 금지, `${}` 금지, N+1 금지, `@Transactional` 위치 |
| 보안 | §5 | API 키 하드코딩, `.env` 노출, SQL Injection 가능성 |
| 금융 도메인 | §19 | `BigDecimal` 사용, 소프트 삭제, 상태 이력, 민감 컬럼 암호화, UTC 저장 |
| 예외 처리 | §15 | 광범위 catch, 예외 삼키기, Controller 직접 try/catch |
| 로깅 | §16 | 레벨 기준, 민감정보 마스킹, MDC traceId |
| 테스트 | §14 | `@Disabled` 사유 없음, 테스트 비활성화로 통과 |
| 성능 | §21 | 트랜잭션 내 외부 API 호출, TTL 없는 캐시, 페이징 없는 `findAll` |

### 리뷰 대상 파일

- `.java` — 기본 리뷰 대상
- `.gradle`, `.yml`, `.yaml` — 보안·설정 관련 항목만
- `.md` — 리뷰 제외 (문서 변경은 사람이 검토)

---

## 5. 출력 예시

Claude가 PR 코멘트로 게시하는 리뷰 형식:

```
## 자동 코드 리뷰 결과

### 🔴 차단 (머지 전 반드시 수정)
- `LoanApplicationService.java:42` — `catch (Exception e)`로 모든 예외 삼킴.
  구체 타입 지정 필요. (AI_GUIDELINES §15)

### 🟡 권고 (수정 권장)
- `LoanController.java:87` — `findAll()` 호출에 페이징 없음.
  운영 데이터 증가 시 OOM 위험. (AI_GUIDELINES §21)

### 🟢 참고 (선택 사항)
- `LoanApplicationDto.java:15` — `@Setter` 사용. DTO라면 허용이나,
  불변 객체 설계 검토 권장. (AI_GUIDELINES §4)

---
리뷰 기준: AI_GUIDELINES.md | 모델: claude-sonnet-4-6
```

### 심각도 기준

| 레벨 | 의미 | 예시 |
|---|---|---|
| 🔴 차단 | 보안·금융 규칙 위반, 운영 사고 가능성 | SQL Injection, 예외 삼키기, `double`로 금액 계산 |
| 🟡 권고 | 성능·유지보수 이슈, 가이드라인 위반 | N+1, 페이징 누락, 잘못된 `@Transactional` 위치 |
| 🟢 참고 | 스타일 제안, 선택적 개선 | Lombok 최적화, 변수명 개선 |

---

## 6. 비용 추산

사용 모델: `claude-sonnet-4-6`

| 항목 | 추산치 |
|---|---|
| PR 1건당 입력 토큰 | 1,000 ~ 3,000 (diff 크기에 따라) |
| PR 1건당 출력 토큰 | 500 ~ 1,500 |
| 월 PR 수 (예상) | 30 ~ 50건 |
| **월 예상 비용** | **$2 ~ $5** |

> 300줄 초과 PR은 청크 분할 처리로 비용이 2~3배 증가할 수 있다.

---

## 7. 비활성화 / 스킵 방법

### PR 단위 스킵
PR 제목 앞에 `[skip review]` 추가:
```
[skip review] chore(infra): Grafana 대시보드 JSON 업데이트
```

### 워크플로 일시 중단
GitHub 레포 → Actions → `Code Review` 워크플로 → Disable workflow

### 특정 파일 제외
워크플로 구현 시 `REVIEW_EXCLUDE_PATHS` 환경변수로 제외 경로 지정:
```yaml
env:
  REVIEW_EXCLUDE_PATHS: "docs/,*.md,*.json"
```

---

## 8. 담당자별 확장 가이드

본 파이프라인은 `AI_GUIDELINES.md` 공통 기준만 적용한다.
서비스별 도메인 리뷰 기준(예: 여신 상태 전이 규칙, DSR 산출 로직 검증)이 필요한 경우 각 담당자가 직접 프롬프트 설정 파일에 추가한다.

추가 방법은 워크플로 구현 완료 후 본 섹션에 보강 예정.
