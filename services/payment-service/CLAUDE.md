# Payment Service — Claude Code 도메인 가이드

> `services/payment-service` 전용 가이드. 루트 전사 가이드와 충돌 시 아래 **컨텍스트 우선순위** 기준으로 판단.

---

## 1. 컨텍스트 우선순위

상충 시 위쪽이 우선.

1. 사용자의 현재 메시지
2. `/docs/AI_GUIDELINES.md` — 전사 공통 AI 가이드
3. `/CLAUDE.md` — 팀 루트 Claude Code 가이드
4. **본 파일** — 결제계 도메인 특화 규약
5. `/services/payment-service/docs/` 안 설계 산출물 — **진실의 원천**
6. 그 외 레포 파일

---

## 2. 도메인 개요

- 결제계 = 자행·타행 이체 처리 + KFTC/BOK 외부망 통신 + Saga 보상 트랜잭션 관리
- A/B 두 은행이 동일 코드베이스 운영. `BANK_CODE` 환경변수(`A` / `B`)로 분리
- 의존 서비스: **deposit-service** (잔액·계좌마스터 통합, 필수) / **loan-service** (선택) / **customer-service** (드물게)

---

## 3. 설계 산출물 (★ 진실의 원천)

`services/payment-service/docs/` 에 아래 10개 xlsx가 박제된다.
코드·DB 스키마·Kafka 토픽 작성 전 **반드시** 해당 산출물을 먼저 확인한다.

| 파일 | 핵심 내용 |
|---|---|
| `결제계_정책문서_v7.2.xlsx` | 30개 정책 (P-001 ~ P-030) |
| `결제계_enum_상태전이도_v9.xlsx` | 9개 진행상태 + 전이매트릭스 9×9 |
| `결제계_API명세서_v2.3.xlsx` | 22개 API |
| `결제계_Kafka토픽정의서_v3.1.xlsx` | 18개 토픽 + DLQ |
| `결제계_deposit_API합의서_v1.0.xlsx` | deposit 도메인 9개 API (🟡 가정 7건 포함) |
| `결제계_시나리오_v4.1.xlsx` | 14개 시나리오 |
| `결제계_컬럼명세서_v12.2.xlsx` | 203 컬럼 (컬럼명 기준) |
| `결제계_테이블정의서_v2.xlsx` | 테이블 목록·관계 |
| `결제계_기술스택정의서_v2.2.xlsx` | Java 17, Spring Boot 3.x, PostgreSQL 16, Kafka |
| `결제계_용어집.xlsx` | 도메인 용어 정의 |

---

## 4. 핵심 설계 결정

| 정책 | 내용 |
|---|---|
| **P-028** | OUT 트랜잭션 분해: TX-1 DRAFT INSERT → 외부검증(TX 밖) → 외부 출금호출(TX 밖) → TX-2 분개+전이+Outbox INSERT → Outbox 워커 발행 |
| **P-029** | Self-Listening 방지: Consumer에서 `sender/receiver_bank_code` 필터링 |
| **P-030** | 외부 시스템 트리거 강제 취소는 반드시 `CANCEL_REQUESTED` 경유 |
| **P-002 보강** | `PROCESSING` 종료 분기 = 외부 자금 변동 여부 기준으로 판단 |
| **P-014 보강** | ACK 이후 `REVERSING` 진입 = 외부/운영자 트리거만 허용 |
| **청산상태 ACK** | Kafka Producer `acks=all` 응답 수신 시점으로 재정의 (enum v9 기준) |
| **운영자 강제 취소** | 모든 외부/운영자 트리거 강제 취소는 `CANCEL_REQUESTED` 경유 (일관성 G) |

---

## 5. 코드 작성 규칙 (★ Claude Code 행동 규약)

- **외부 API 호출 격리**: `@Transactional` 범위 안에 외부 HTTP 호출 절대 금지 (P-028 핵심)
- **Kafka 발행**: 모두 Outbox 워커 경유. `KafkaTemplate.send()` 직접 호출 금지
- **Kafka Consumer ack**: DB COMMIT 완료 후 `ack.acknowledge()` 호출 (at-least-once 보장)
- **외부 응답 박제**: deposit·KFTC 등 모든 외부 응답은 스냅샷 컬럼에 동시 저장
- **멱등키**: 결제계 책임으로 발급. 형식 `{API}-{거래ID}-{시도번호}`
- **보상 호출 전 검증**: 결제지시 진행상태 자체 검증 선행 필수 (이중 보상 방지)
- **DB 접근**: MyBatis XML 매퍼 사용 (JPA 미사용). 매퍼 위치 `src/main/resources/mappers/`. 복잡 SQL(분개 4건 동시 INSERT 등) 정확 제어 목적 (기술스택 정의서 v2.2)
- **환경 분리**: `application.yml` = 컨테이너용 default (port 8080) / `application-local.yml` = 로컬 IDE 전용 (`SPRING_PROFILES_ACTIVE=local`, port 8084, .gitignore 제외)
- **패키지 분리**:
  - 도메인 모델 → `com.bank.payment.domain`
  - 외부 발신 (Feign, Kafka Producer) → `com.bank.payment.outbound`
  - 외부 수신 (Kafka Consumer) → `com.bank.payment.inbound`

---

## 6. 모르는 것 처리 (★ 중요)

- 정책·enum·스키마에 명시 안 된 부분은 **추측 금지** — 사용자에게 질문
- 산출물 간 모순 발견 시 **자동 결정 금지** — 사용자에게 보고 후 지시 대기
- xlsx 미확인 상태로 컬럼명·enum값·토픽명을 코드에 직접 작성 금지
  - 컬럼 기준: `결제계_컬럼명세서_v12.2.xlsx`
  - enum 기준: `결제계_enum_상태전이도_v9.xlsx`
- `결제계_deposit_API합의서_v1.0.xlsx` 의 🟡 가정(7건) 영역 작업 시 반드시 사용자 확인
- ★deposit-service 인스턴스 분리 가정: 현재 "은행별 분리(A은행 deposit ↔ A은행 결제계)"로 가정.
  합의서 🔴 확인 3 미해소. Stage 1 인프라 결정 시 사용자 재확인 필요.

---

## 7. Stage 진행 상태

| Stage | 내용 | 상태 |
|---|---|---|
| **0** | CLAUDE.md + 디렉토리 구조 + 산출물 박제 | ✅ 완료 (2026-05-19) |
| **1** | `services/payment-service/docker-compose-kafka.yml` (Kafka 3 클러스터 + UI) + 토픽 18개 + payment-db A/B | ✅ 완료 (2026-05-19) |
| **2** | build.gradle 확장 + Spring Boot 골격 + 멀티 Kafka Config + MyBatis 전환 (Redis/JPA 제거) | ✅ 완료 (2026-05-19) |
| **3** | Flyway V1~V5 마이그레이션 (결제지시/멱등키/외부호출/Outbox/상태이력 5개 테이블) | 🔜 다음 |
| **4** | 결제지시 Aggregate Root + 도메인 모델 + 단위테스트 | 대기 |
| **5** | API 컨트롤러 + 자행 이체(S1) end-to-end + Testcontainers 통합테스트 | 대기 |
| **6** | 외부 도메인 Mock(deposit/loan/customer) + Feign 클라이언트 + 박제 검증 | 대기 |
| **7+** | 외부망 통신 (S2-A/S2-B/IN-01) + 보상 흐름 (F2/F5) + 운영자 강제 취소 (F6/F7) | 대기 |

---

## 8. Git 작업 규약

루트 `docs/AI_GUIDELINES.md §5` (AI 흔적 금지)를 그대로 따른다. 아래는 결제계 강조 사항.

- `Co-authored-by: AI명` 류 footer **절대 금지**
- Claude Code는 `git commit / push / add / rebase / merge` **직접 실행 금지**
- `git status / diff / log` 등 읽기 전용 명령만 실행 가능
- 사용자가 명시적으로 요청해도 쓰기 git 명령 거부 → 커밋 메시지 후보만 제안

### 매 작업 종료 시 출력 형식

```
[작업 요약]
- 생성/수정된 파일: (리스트)
- 핵심 변경 사항: (3~5줄)

[제안 커밋 메시지]
<type>(payment): <subject>

- 변경 디테일 (3~5줄)

관련 정책/산출물: (P-XXX, 컬럼명세서 v12.2 등)

[다음 단계]
사용자가 IDE/터미널에서 직접 git add + commit + push 실행
```

---

## 9. 도메인 용어

> 자세한 정의는 `docs/결제계_용어집.xlsx` 참조. 아래는 자주 헷갈리는 것만.

| 용어 | 1줄 정의 |
|---|---|
| 박제(Snapshot) | 외부 응답을 결제계 DB에 영구 저장. 이후 변경 없음 |
| 멱등키 | 재시도 시 deposit이 직전 응답을 반환하게 하는 키 |
| 거래분개(ledger) | 결제계 내부 회계 차/대변 분개. 한 결제에 2~4건 발생 |
| deposit common_transaction | 통장 거래내역 row. 결제계 ledger와 별개 개념 |
| 청산대기(CLEARING_PENDING) | KFTC 외부망 송신 후 정산 전 임시 계정 상태 |
| Outbox 워커 | 결제계 DB Outbox 테이블 → Kafka 비동기 발행 워커 |
| Saga / 보상 트랜잭션 | 외부 자금 변동 후 실패 시 역 호출로 원상복구하는 패턴 (P-014) |
| CANCEL_REQUESTED | 운영자/외부 트리거로 강제 취소 요청된 상태. REVERSING 직전 단계 (enum v9) |
| 청산상태 ACK | Kafka Producer acks=all 응답 수신 시점 = KFTC 접수 신호로 간주 (enum v9 재정의) |
