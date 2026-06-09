# 면접 대비 학습 노트 — Internet Banking 프로젝트

> 구성: **① 개념(한 줄) → ② 내 코드에선(파일·방식) → ③ 답변 스크립트(말로 할 때) → ④ 꼬리질문 대비**
> 핵심 원칙: **"왜 이 선택을 했는가(트레이드오프)"** 를 한 문장 붙이면 면접관이 가장 좋아한다.

---

# PART 1. Java & Spring (보안 / 동시성 / 권한)

## 1. AES-256 (개인정보 대칭키 암호화)

**① 개념**
대칭키 암호화 알고리즘. 같은 키로 암호화/복호화. "256"은 키 길이(비트). 우리는 정확히는 **AES-256-GCM** 모드를 씀.
- GCM = 암호화 + **무결성 검증(인증 태그)** 을 동시에. 누가 암호문을 몰래 변조하면 복호화 시점에 탐지돼서 터진다(AEAD: 인증된 암호화).

**② 내 코드에선**
- `common/security/crypto/AesGcmCryptoService.java` — `AES/GCM/NoPadding`, 12바이트 nonce(IV) 매번 랜덤 생성, 128비트 인증 태그.
- 저장 형식: `[12바이트 nonce][암호문+태그]` → Base64 인코딩해서 컬럼에 저장.
- 고객 주민번호는 `customer-service/crypto/CryptoService.java`에서 별도로 암호화 → `PartyPerson.rrnEncrypted` 컬럼.

**③ 답변 스크립트**
> "개인정보는 AES-256-GCM으로 암호화해 저장했습니다. GCM을 택한 이유는 기밀성뿐 아니라 무결성까지 같이 보장되기 때문입니다 — 암호문이 변조되면 복호화 단계에서 인증 태그 검증에 실패해 바로 탐지됩니다. nonce(IV)는 레코드마다 랜덤하게 새로 생성해서, 같은 평문이라도 매번 다른 암호문이 나오도록 했습니다. nonce 재사용은 GCM에서 치명적이라 거기에 특히 주의했습니다."

**④ 꼬리질문**
- *"왜 CBC가 아니라 GCM?"* → CBC는 무결성 보장이 없어 패딩 오라클 공격에 취약. GCM은 AEAD라 변조 탐지가 됨.
- *"nonce를 재사용하면?"* → 같은 키+같은 nonce 조합이 재사용되면 키 스트림이 노출돼 평문 복원이 가능해짐. 그래서 매번 랜덤 생성.
- *"대칭키 vs 비대칭키?"* → 대칭키(AES)는 빠르고 대용량 적합, 키 공유가 숙제. 비대칭키(RSA 등)는 느리지만 키 교환에 유리. 그래서 보통 데이터는 AES, 키 전달은 비대칭으로 하이브리드.

---

## 2. KMS / Vault (키 관리)

**① 개념**
암호화 키를 코드/설정파일에 박지 않고 **외부 비밀 관리 시스템**에서 주입. KMS(AWS Key Management Service), HashiCorp Vault 등. 키 교체(rotation), 접근 감사, 권한 분리가 핵심 가치.

**② 내 코드에선 (정직하게)**
- 현재는 **환경변수 주입**까지 구현. `CryptoProperties`(`prefix=crypto`)가 `CRYPTO_KEY_BASE64` 환경변수에서 Base64 인코딩된 32바이트 키를 받음.
- **fail-fast**: 키가 안 들어오면 기본값 없이 바인딩 실패 → 부팅 자체가 중단. (키 없이 떠서 평문 저장되는 사고를 원천 차단)
- KMS/Vault 실연동은 **"운영 단계 고려사항"** 으로 설계만. 이력서에도 "연동 고려"라고 정직하게 적혀 있음 — 면접에서도 그대로 말할 것.

**③ 답변 스크립트**
> "키는 코드와 분리해 환경변수로 주입했고, 키가 주입되지 않으면 애플리케이션이 아예 부팅에 실패하도록 fail-fast로 막았습니다. 키 없이 떠서 평문이 저장되는 사고를 막기 위해서입니다. 다만 환경변수 방식은 키 교체와 접근 감사가 약하기 때문에, 운영에서는 KMS나 Vault로 옮겨서 키 로테이션과 접근 권한 감사를 붙이는 걸 다음 단계로 설계해뒀습니다."

**④ 꼬리질문**
- *"환경변수 방식의 한계는?"* → 키 로테이션이 수동, 프로세스 메모리/덤프에 노출 가능, 접근 이력이 안 남음. → KMS/Vault로 보완.
- *"KMS는 키를 어떻게 보호하나?"* → 보통 **봉투 암호화(envelope encryption)**: 데이터는 데이터키(DEK)로 암호화하고, DEK는 KMS의 마스터키(KEK)로 암호화해서 같이 저장. 마스터키는 KMS 밖으로 안 나옴.

---

## 3. 권한별 마스킹 (3단계 노출)

**① 개념**
같은 데이터라도 **보는 사람의 역할/관계**에 따라 노출 수준을 다르게. 최소권한 원칙.

**② 내 코드에선**
- `loan-service/security/PiiLevel.java` — **FULL / MASKED / REDACTED** 3단계.
- 결정 로직: `LoanActorContext.piiLevel(application, review)`
  - OPS/INTERNAL/ADMIN, 또는 본인이 심사자/소유자 → **FULL**(전체)
  - 승인자(심사자와 다른 사람), 지점장(같은 지점), 컴플라이언스 → **MASKED**(식별자만 가림, 소득·DSR 등 판단정보는 보임)
  - 고객/권한 없는 경로 → **REDACTED**(금액 구간만, PII 전부 제거)
- 마스킹 유틸 `common/security/mask/Masking.java`: 이름 `홍*동`, 휴대폰 `010-****-5678`, 주민 `901231-1******`.
- 금액 구간화 `PiiMaskingUtil`: `52,000,000 → "5천만원대"`.

**③ 답변 스크립트**
> "권한별로 3단계 노출을 뒀습니다. 핵심은 단순히 역할만 보는 게 아니라 '역할 + 그 건과의 관계'를 같이 봤다는 점입니다. 예를 들어 같은 승인자라도 그 건의 심사자 본인이면 전체를 보고, 심사를 승인만 하는 사람은 직접 식별정보는 가리고 소득·DSR 같은 판단 근거만 보게 했습니다. 고객 본인이나 권한 밖 경로로 들어오면 금액도 '5천만원대' 같은 구간으로만 노출했습니다. 판단에 필요한 최소 정보만 준다는 최소권한 원칙을 데이터 노출에 그대로 적용한 겁니다."

**④ 꼬리질문**
- *"마스킹을 어디서 하나 — DB? 응답?"* → 응답 DTO 변환 시점(`LoanReviewResponse.of(..., PiiLevel)`). 원본은 암호화 저장, 노출 시점에 권한 따라 가공.
- *"전체 조회 권한 남용은 어떻게 막나?"* → FULL/UNMASK 접근은 감사 로그(8번)에 남기고, 임시 권한은 자동 회수(6번).

---

## 4. 조건부 UPDATE (동시성 제어 / 낙관적 락)

**① 개념**
조회→검증→수정을 따로 하면 그 사이 다른 요청이 끼어든다(race condition). 대신 **WHERE에 조건을 넣은 단일 UPDATE**로 "원하는 상태일 때만" 갱신하고, **영향받은 행 수(affected rows)** 로 내가 이겼는지 판정.

**② 내 코드에선**
- `RepaymentScheduleRepository.claimStatusChange(rschId, newStatus, allowedStatuses)`
  ```sql
  UPDATE RepaymentSchedule s SET s.rschStatusCd = :newStatus
   WHERE s.rschId = :rschId
     AND s.rschStatusCd IN :allowedStatuses   -- DUE/OVERDUE 일 때만
     AND s.deletedAt IS NULL
  ```
- `RepaymentService.repayInstallment()`: `affected == 0`이면 "다른 요청이 선점했다"는 뜻 → `LOAN_091` 예외.
- payment-service는 **버전 컬럼** 방식: `WHERE ... AND version = #{version}`, 갱신 시 `version + 1` (`PaymentInstructionMapper.xml`).

**③ 답변 스크립트**
> "같은 회차에 상환 요청이 동시에 들어와도 한 번만 처리되도록 조건부 UPDATE를 썼습니다. 조회와 갱신을 분리하면 그 사이에 다른 트랜잭션이 끼어들 수 있어서, DB 단일 UPDATE 문의 WHERE 절에 'DUE나 OVERDUE 상태일 때만'이라는 조건을 넣었습니다. 그러면 DB가 행 잠금으로 직렬화해주기 때문에 먼저 도달한 요청만 갱신에 성공하고, 나머지는 affected rows가 0으로 돌아옵니다. 그 0을 '선점당했다'는 신호로 보고 예외 처리했습니다. 별도 버전 컬럼 없이 상태 값 자체를 조건으로 쓴 게 포인트입니다."

**④ 꼬리질문**
- *"낙관적 락 vs 비관적 락?"* → 낙관(version/조건부 UPDATE): 락을 안 잡고 커밋 시점에 충돌 감지, 충돌 적을 때 유리. 비관(SELECT ... FOR UPDATE): 미리 락, 충돌 잦을 때 유리하나 처리량↓·데드락 위험.
- *"왜 버전 컬럼 대신 상태?"* → 상태 전이 자체가 비즈니스 불변식(DUE→PAID는 1회)이라 상태가 곧 가드. 단순하고 의미가 명확.
- *"affected=0이면 사용자에겐?"* → "이미 처리된 회차" 같은 멱등적 결과로 변환(5번 멱등키와 연결).

---

## 5. 멱등키 (Idempotency Key)

**① 개념**
같은 요청을 여러 번 보내도 **결과가 한 번 보낸 것과 같게**. 네트워크 타임아웃 후 클라이언트 재시도 시 이중 결제/이중 처리 방지. 클라이언트가 요청마다 고유 키를 부여하고, 서버는 그 키로 중복을 판별.

**② 내 코드에선**
- payment-service `IdempotencyKey` 도메인: `idempotencyKey`, `requestHash`(요청내용 해시), `idempotencyStatus`(PROCESSING→COMPLETED/FAILED), `firstResponseSnap`(최초 성공 응답을 JSONB로 **박제**), `expiresAt`.
  - 키 형식: `{API코드}-{결제지시ID}-{시도번호}`
  - 재시도 시: 이미 COMPLETED면 박제된 첫 응답을 그대로 반환.
- deposit-service `IdempotentTransactionSaver.saveOrFetch()`: **UNIQUE 제약 + 예외 캐치** 패턴.
  ```java
  try { return repo.save(tx); }            // (idempotencyKey, accountId) UNIQUE
  catch (DataIntegrityViolationException e) {
      return repo.findByIdempotencyKeyAndAccountId(key, accountId).orElseThrow(() -> e);
  }
  ```
  `REQUIRES_NEW`로 바깥 트랜잭션 오염 방지.

**③ 답변 스크립트**
> "결제처럼 재시도가 위험한 작업에는 멱등키를 적용했습니다. 클라이언트가 'API코드-결제지시ID-시도번호'로 키를 만들어 보내면, 서버는 그 키의 상태를 추적합니다. 처음이면 PROCESSING으로 잡고 처리한 뒤 성공 응답 자체를 스냅샷으로 박제해둡니다. 같은 키로 다시 오면 재실행하지 않고 박제된 첫 응답을 그대로 돌려줘서, 타임아웃 후 재시도가 와도 이중 결제가 안 납니다. 입금 쪽은 더 단순하게 (멱등키, 계좌ID) UNIQUE 제약을 걸고, 제약 위반 예외가 나면 기존 건을 조회해 반환하는 방식으로 DB가 동시성을 보장하게 했습니다."

**④ 꼬리질문**
- *"멱등 vs 동시성 제어(4번) 차이?"* → 동시성 제어는 "동시에 온 서로 다른 요청"의 충돌, 멱등은 "같은 요청의 반복". 둘은 보완 관계.
- *"requestHash는 왜?"* → 같은 키인데 본문이 다르면 키 재사용/오용이므로 거부하기 위해.
- *"멱등키 만료(expiresAt) 이유?"* → 무한 저장 방지 + 일정 시간 후 같은 키 재사용 허용 정책.
- *"PROCESSING 중에 또 오면?"* → "처리 중" 응답(409 등)으로 동시 중복 실행 차단.

---

## 6. RBAC + 임시 권한 자동 회수 (Break-glass)

**① 개념**
- RBAC: 역할 기반 접근 제어. 사람마다가 아니라 **역할**에 권한을 묶음.
- Break-glass(유리 깨기): 평소엔 막혀 있지만 **긴급 상황에 사유를 남기고 임시로** 열어주는 접근. 단, **시간 제한 + 감사** 가 필수.

**② 내 코드에선**
- 역할: `common/security/BankRole.java` — CUSTOMER, TELLER, BRANCH_MANAGER, HQ_REVIEWER, COMPLIANCE, OPS, ADMIN 등. 기능별 그룹(`AUDIT_VIEW_ROLES`, `COMPLIANCE_DESK_ROLES` 등).
- 자동 회수: `BreakGlassService` — `GRANT_TTL = Duration.ofHours(1)`. 사유 10자 이상 강제, Redis 기반 `RedisBreakGlassGrantStore`에 1시간 TTL로 grant 저장 → **만료 시 자동 소멸**(별도 배치 불필요).
- 동시에 `AccessAuditLog`에 BREAK_GLASS 액션 + 사유 + 시각 기록.

**③ 답변 스크립트**
> "권한은 역할 기반으로 묶고, 민감한 작업엔 break-glass 패턴을 적용했습니다. 평소 막힌 데이터를 긴급히 봐야 할 때, 최소 10자 이상의 사유를 남겨야 임시 권한이 발급됩니다. 이 권한은 Redis에 1시간 TTL로 저장돼서 한 시간 뒤 자동으로 사라집니다 — 회수를 위한 별도 배치를 돌릴 필요 없이 TTL 만료로 자연 소멸하게 한 게 포인트입니다. 그리고 발급 사실과 사유를 감사 로그에 남겨서, '왜 열었는지'가 항상 추적되게 했습니다."

**④ 꼬리질문**
- *"왜 1시간? 왜 Redis?"* → 긴급 작업 처리에 충분하면서 노출 창을 최소화. Redis는 TTL 만료가 네이티브라 회수 로직이 공짜.
- *"TTL 만료 직전 진행 중 작업은?"* → 권한 체크는 요청 시점마다 하므로 만료 후 새 요청은 거부. 필요 시 재발급(또 사유·로그).
- *"RBAC vs ABAC?"* → RBAC는 역할 단위라 단순·관리 쉬움. ABAC는 속성(지점, 시간, 건과의 관계) 기반이라 더 세밀 — 우리 piiLevel 로직(역할+관계)은 사실상 ABAC 요소를 섞은 것.

---

## 7. 심사자/승인자 분리 (4-eyes / 직무 분리)

**① 개념**
중요한 결정을 **한 사람이 끝까지 못 하게** 둘로 나눔. "4개의 눈" 원칙. 내부 통제·부정 방지의 기본.

**② 내 코드에선**
- `LoanReviewApproverService.approverApprove()`:
  ```java
  if (approverId.equals(review.getReviewerId()))
      throw new BusinessException(LoanErrorCode.LOAN_196); // 본인 심사를 본인이 승인 불가
  ```
- 수정도 마찬가지: `LoanReviewReviseService.revise()` — 승인자가 자기 승인 건을 직접 수정 못 함(`LOAN_207`).
- 승인 옵션: `APPROVE_AS_IS` / `OVERRIDE_APPROVED` / `OVERRIDE_REJECTED`.
- 검증 테스트까지 존재: `LoanReview4EyeUnitTest`, `LoanReviewRevise4EyeUnitTest`.

**③ 답변 스크립트**
> "대출 승인 같은 중요한 결정은 심사자와 승인자를 코드 레벨에서 강제로 분리했습니다. 승인 시점에 승인자 ID가 그 건의 심사자 ID와 같으면 예외를 던져서, 자기가 심사한 건을 자기가 승인하는 걸 원천 차단했습니다. 수정 단계에도 같은 가드를 둬서 승인자가 자기 승인을 단독으로 뒤집지 못하게 했고요. 이걸 단위 테스트로도 고정해서, 누가 나중에 코드를 바꿔도 이 규칙이 깨지면 테스트가 실패하도록 했습니다."

**④ 꼬리질문**
- *"system 계정으로 우회하면?"* → actorId가 null이거나 SYSTEM이면 거부(`LOAN_196`).
- *"왜 DB 제약이 아니라 서비스 코드?"* → "두 ID가 달라야 한다"는 건 행 간 비교라 단순 컬럼 제약으론 표현이 어려움. 비즈니스 규칙이라 서비스 계층 + 테스트로 고정.

---

## 8. 감사 로그 (Audit Log)

**① 개념**
"누가/언제/무엇을/왜" 했는지 **추가 전용(append-only)** 으로 남김. 규제 대응·사후 추적·부정 탐지의 근거.

**② 내 코드에선**
- `AccessAuditLog` (loan-service): 민감정보 접근 이벤트 — actorId, targetType(LOAN_APPLICATION 등), actionCd(VIEW/UNMASK/BREAK_GLASS), branchId, breakGlassReason, loggedAt.
- `StatusHistory` (common): 상태 변경 감사 — before/after 상태, 변경 사유 코드(INSTALLMENT_PAID, APPROVER_APPROVED 등), 변경자, 시각.
- 발행: `StatusHistoryPublisher.publish()` → Spring `@EventListener` 비동기 저장(본 트랜잭션 부담↓).
- 조회: `AuditLogService.listBreakGlass(actorId)`, `listByTarget(type, id)`.

**③ 답변 스크립트**
> "감사 로그를 두 축으로 나눴습니다. 하나는 접근 감사 — 누가 어떤 건을 보거나 마스킹을 풀었는지, break-glass를 썼는지를 사유와 함께 남깁니다. 다른 하나는 상태 변경 이력 — 상환이 PAID로 바뀌거나 승인이 떨어질 때 이전/이후 상태와 사유 코드, 변경자를 기록합니다. 둘 다 추가 전용이라 수정·삭제가 안 되고, 상태 이력은 Spring 이벤트로 비동기 저장해서 본 거래 트랜잭션에 부담을 안 주게 했습니다."

**④ 꼬리질문**
- *"비동기인데 로그 유실은?"* → 트레이드오프. 강한 보장이 필요하면 같은 트랜잭션에 동기 기록 or outbox 패턴. 현재는 거래 성능 우선.
- *"로그 위변조 방지?"* → append-only + 권한 분리. 더 강하게는 해시 체이닝/WORM 스토리지.

---

# PART 2. AI & RAG

> RAG = Retrieval-Augmented Generation. LLM이 자기 기억에만 의존하지 않고, **외부 문서를 검색해서 그 근거로 답변**하게 하는 구조. 환각(hallucination)을 줄이고 출처를 댈 수 있는 게 핵심.

## 9. LLM 인터페이스 추상화 (교체 가능 설계)

**① 개념**
특정 LLM 벤더에 코드가 묶이지 않게 **인터페이스(포트)** 뒤로 숨김. 구현체만 갈아끼우면 모델/벤더 교체.

**② 내 코드에선**
- `llm/client/LlmClient.java` 인터페이스: `<T> T call(LlmRequest, Class<T> outputSchema)`.
- 구현체: `StubLlmClient`(테스트·로컬, 결정적 응답), `GeminiOpenAiCompatLlmClient`(Gemini), 게이트웨이엔 `ClaudeLlmClient`(Claude).
- 선택: `ai.llm.provider` 설정값(`stub`/`gemini-openai-compat`/`vertex`/`anthropic`) + `@ConditionalOnProperty`.

**③ 답변 스크립트**
> "LLM을 코드 수정 없이 교체할 수 있도록 LlmClient 인터페이스 하나로 추상화했습니다. 호출부는 '요청을 주면 이 스키마로 응답을 달라'만 알고, 실제로 Gemini를 쓸지 Claude를 쓸지는 설정값으로 결정됩니다. 덕분에 로컬·테스트에선 결정적 응답을 주는 Stub 구현으로 돌리고, 운영에선 실제 모델로 바꾸는 걸 설정 한 줄로 처리할 수 있었습니다. 의존성 역전 원칙을 LLM 벤더에 적용한 셈입니다."

**④ 꼬리질문**
- *"Stub 구현을 왜?"* → 외부 API 없이 테스트 가능, 비용·레이트리밋 무관, 결정적이라 CI에 적합.
- *"응답 스키마 강제는 어떻게?"* → `outputSchema` 타입으로 구조화 출력을 파싱/검증(아래 grounding과 연결).

---

## 10. Kill-Switch (배포 없이 AI 끄기)

**① 개념**
장애·사고 시 **재배포 없이** 환경변수+롤링 재시작만으로 기능을 즉시 끄는 스위치. 사고 시 영향 최소화(blast radius↓).

**② 내 코드에선**
- `application.yml`: `AI_LLM_ENABLED`, `AI_RAG_ENABLED`, `AI_SHADOW_ENABLED`, `AI_AGENT_ENABLED` 등 기능별 플래그.
- 체크: `ReviewReportService` — `if (!props.enabled()) return templateFallback.generate(input, "LLM 비활성화");`
- 끄면 LLM 단계를 우회하고 **템플릿 fallback**으로 대체(서비스 자체는 계속 동작).

**③ 답변 스크립트**
> "AI는 외부 의존성이고 언제든 죽을 수 있다고 보고, 기능별 kill-switch를 환경변수로 뒀습니다. AI가 오작동하면 코드 재배포 없이 환경변수만 바꾸고 롤링 재시작하면 LLM 단계를 통째로 우회합니다. 중요한 건 끈다고 서비스가 멈추는 게 아니라, 템플릿 기반 fallback으로 전환돼서 대출 심사 자체는 계속 굴러간다는 점입니다. AI를 '있으면 좋은 보조'로 두고 핵심 흐름은 AI 없이도 살아있게 설계했습니다."

**④ 꼬리질문**
- *"왜 재배포 대신 환경변수+재시작?"* → 재배포는 빌드·승인으로 분 단위가 걸림. 사고 대응은 초 단위가 필요. 플래그는 즉시.
- *"진짜 무중단이면 동적 토글이 낫지 않나?"* → 맞음. 다음 단계는 설정 서버/feature flag로 재시작 없이 토글. 현재는 롤링 재시작으로 타협.

---

## 11. Fallback (레이트리밋 초과 시 대체 응답)

**① 개념**
외부 LLM 호출량 한도를 넘거나 실패하면, 에러 대신 **미리 정한 안전한 응답**으로 대체해 서비스 연속성 유지.

**② 내 코드에선**
- `LlmRequestRateMeter.tryAcquire()`: RPD(일일 1500)/RPM(분당 15) 한도 체크 — Gemini 무료 티어 기준. 초과 시 `false` 반환.
- `false`면 `TemplateFallback.generate()`가 트랙별 템플릿 응답 생성(TRACK_1 승인형 / TRACK_2 거절형 / TRACK_3 사람 검토형).
- 설정: `GEMINI_RPD_CAP:1500`, `GEMINI_RPM_CAP:15`.

**③ 답변 스크립트**
> "무료 티어 LLM이라 분당·일일 호출 한도가 있었습니다. 한도를 넘으면 외부에 그냥 에러를 내는 게 아니라, 자체 레이트 미터로 먼저 막고 템플릿 기반 응답으로 대체했습니다. 대출 트랙에 따라 승인형·거절형·사람 검토 회부형 템플릿을 미리 만들어둬서, AI 한도가 차도 심사 결과는 일관되게 나가도록 했습니다. 외부 한도를 우리 쪽에서 선제적으로 관리한 게 포인트입니다."

**④ 꼬리질문**
- *"한도를 클라가 아니라 왜 우리가 세나?"* → 외부 429를 맞고 처리하면 이미 실패 비용 발생. 우리가 카운팅해 선제 차단하면 깔끔.
- *"fallback이 위험한 결정 내리면?"* → 그래서 애매하면 TRACK_3(사람 검토 회부)로 보냄. AI 불가 시 보수적으로.

---

## 12. (정직 필요) 읽기 전용 토큰 4개 로테이션

> **주의**: 이 항목은 **코드에서 명확한 구현을 못 찾았습니다.** 현재 코드는 벤더별 **단일 API 키** + HTTP 재시도(지수 백오프, 최대 3회, `ClaudeLlmClient`)까지만 확인됨.

**면접 전략 (둘 중 택1)**
1. **설계 의도로 정직하게**: "여러 무료 토큰을 순서대로 돌려 쓰며 한도가 찬 토큰은 건너뛰고 다음 토큰으로 넘어가, 한 토큰이 죽어도 트래픽이 살아남게 하는 걸 **설계/시도**했다. 현재 머지된 코드엔 단일 키 + 재시도까지 반영돼 있고, 토큰 풀 로테이션은 보완 중"이라고 말하기. → **거짓말 리스크 0.**
2. **개념만 확실히**: 만약 다른 브랜치/스크립트에 진짜 구현이 있으면 그 파일을 캡처해 두기. (지금 메인엔 없음)

**개념 설명 (질문 들어오면)**
> "무료 티어 키는 각각 RPM/RPD 한도가 있어서, 키 풀을 만들어 라운드로빈으로 분산하면 합산 한도를 늘릴 수 있습니다. 한 키가 429를 맞으면 쿨다운 표시하고 다음 키로 넘기는 페일오버를 붙이면, 한 키가 죽어도 전체 트래픽은 유지됩니다. 읽기 전용으로 권한을 최소화하면 키 유출 시 피해도 줄고요."

**④ 꼬리질문**
- *"로테이션 상태는 어디 저장?"* → 멀티 인스턴스면 Redis 등 공유 저장소에 키별 카운터/쿨다운(로컬 메모리면 인스턴스마다 따로 세서 한도 초과 위험).

⚠️ **이 항목은 면접 전에 "내가 실제로 어디까지 했는지" 스스로 확인하고 답을 정해두세요.** 코드에 없는 걸 있다고 하면 꼬리질문에서 무너집니다.

---

## 13. 근거/인용 (Grounding / Citation)

**① 개념**
LLM 답변에 **무슨 문서를 근거로 했는지** 출처를 붙임. 환각 방지 + 규제(설명 가능성) 대응. RAG의 존재 이유.

**② 내 코드에선**
- `ReviewReport.Citation` 레코드: `id`(정책 ID), `source`(예: internal_policy_2026q2), `text`(근거 원문).
- `GroundingValidator.validate()`: 인용 ID가 실제 존재하는지 검증(`citation id '...' 미존재`), **TRACK_2(거절)는 인용 최소 2건 강제**(법/정책 근거 없이 거절 금지).
- 인용 prefix 라우팅: `rag:` → RAG 검색 인덱스, `inline:` → 설정 내장 정책.

**③ 답변 스크립트**
> "모든 AI 심사 의견은 반드시 근거를 달도록 했습니다. 답변에 정책 ID와 원문을 citation으로 붙이고, GroundingValidator가 그 인용 ID가 실재하는 정책인지 검증합니다. 특히 거절 결정은 인용을 최소 2건 이상 요구해서, 근거 없이 사람을 떨어뜨리는 일이 없게 강제했습니다. AI가 그럴듯하게 지어내는 환각을 막고, 나중에 '왜 이렇게 판단했나'를 설명할 수 있게 하려는 목적입니다."

**④ 꼬리질문**
- *"인용 ID를 LLM이 지어내면?"* → 그래서 검증기가 실존 여부를 대조하고, 없으면 이슈로 잡아 거른다.
- *"왜 거절만 2건?"* → 거절이 고객에게 불이익이 큰 결정이라 입증 책임을 높게.

---

## 14. Shadow 실행 + 비교평가/리랭킹

**① 개념**
새 검색/모델을 바로 교체하지 않고, **운영 트래픽을 복제해 그림자로 같이 돌린 뒤** 기존과 결과를 비교. 사용자 영향 없이 신버전을 검증.

**② 내 코드에선**
- `ShadowModeService.runShadow()` — `@Async`로 운영과 별개로 그림자 파이프라인 실행, 샘플링 비율 제어.
- `ShadowComparisonEvaluator.evaluate()` — 운영 vs 그림자 4가지 비교:
  1. 리스크 레벨 불일치(RISK_LEVEL_MISMATCH)
  2. 결정 점수 차이 임계 초과(DECISION_SCORE_GAP)
  3. 이견 플래그 불일치(DISAGREEMENT_MISMATCH)
  4. (RAG 켜졌을 때) 정책 플래그 수 차이(POLICY_FLAG_DIFF)
- 카나리 게이트(`application.yml`): es-weight 5%→25%→100% 점진 확대, `min-shadow-runs:100`, 게이트 조건 예: agreementRate ≥ 0.95, citationMissRate ≤ 0.05, p99 < 500ms.

**③ 답변 스크립트**
> "새 RAG 검색을 바로 적용하면 위험하니까, shadow 모드로 기존 파이프라인과 신규 파이프라인을 동시에 돌리고 결과만 비교했습니다. 사용자에겐 기존 결과가 나가고, 신규는 백그라운드에서 같은 입력으로 돌려 리스크 레벨·결정 점수·정책 플래그가 얼마나 갈리는지를 자동으로 평가했습니다. 합성 데이터로 충분한 샘플을 쌓고, 합의율 95% 이상·인용 누락 5% 이하·p99 지연 500ms 미만 같은 게이트를 통과해야 트래픽을 5%, 25%, 100%로 점진 확대하는 카나리로 안전하게 전환하도록 설계했습니다."

**④ 꼬리질문**
- *"shadow가 운영 지연에 영향?"* → `@Async`로 분리 + 샘플링이라 운영 응답 경로엔 영향 없음.
- *"리랭킹은 뭐였나?"* → 두 파이프라인 결과를 점수·근거 기준으로 재평가/비교해 어느 쪽이 나은지 판정하는 로직(divergence 사유 태깅).
- *"왜 합성 데이터?"* → 실제 금융 PII로 실험하기 곤란 → 합성 데이터로 분포를 모사해 검증.

---

## 15. 하이브리드 검색 (pgvector + 키워드 + GIN, 정규화·가중결합/RRF)

**① 개념**
- **벡터 검색(시맨틱)**: 임베딩 코사인 유사도로 "의미가 비슷한" 문서. 동의어·문맥에 강하지만 정확한 키워드엔 약함.
- **키워드 검색(렉시컬)**: BM25/전문검색(FTS). 정확한 단어 매칭에 강함.
- **하이브리드**: 둘을 합쳐 약점 보완. 점수 스케일이 다르니 **정규화 후 가중합**, 또는 **RRF(순위 기반 융합)**.
- **GIN 인덱스**: PostgreSQL에서 전문검색/JSONB 같은 "한 행에 여러 토큰"을 빠르게 찾는 역색인.

**② 내 코드에선**
- pgvector 백엔드 `RagSearchService`: 한 SQL에서 `vec`(코사인, `1 - (embedding <=> query)`) CTE + `fts`(`ts_rank_cd` 전문검색) CTE를 LEFT JOIN, `alpha*vec + (1-alpha)*fts`로 **가중결합**. `alpha=0.7`(벡터 0.7 / 키워드 0.3), threshold 0.5.
- GIN 인덱스: `GIN(metadata)`, `GIN(fts_tokens)`.
- ES 백엔드 `EsHybridSearchService`: BM25 retriever + kNN(dense vector) retriever를 **RRF**로 융합(`rankConstant=60`, `rankWindowSize=50`).

**③ 답변 스크립트**
> "검색은 벡터 단독으론 정확한 용어 매칭이 약하고 키워드 단독으론 동의어·문맥에 약해서, 둘을 합친 하이브리드로 갔습니다. pgvector로는 코사인 유사도와 PostgreSQL 전문검색 점수를 한 쿼리에서 구해 0.7대 0.3 가중치로 결합했고, 전문검색과 JSONB 메타데이터엔 GIN 역색인을 걸어 속도를 확보했습니다. 두 점수는 스케일이 달라서, Elasticsearch 백엔드에서는 점수 자체를 더하는 대신 RRF로 순위 기반 융합을 써서 스케일 차이 문제를 아예 없앴습니다."

**④ 꼬리질문**
- *"가중합 vs RRF 차이?"* → 가중합은 점수 정규화가 필요(스케일 민감), RRF는 순위만 쓰므로 스케일 무관·튜닝 단순. 그래서 ES는 RRF.
- *"`<=>` 연산자?"* → pgvector의 거리 연산자(코사인 거리). `1 - 거리`로 유사도화.
- *"GIN vs B-tree?"* → B-tree는 단일 스칼라 정렬/범위에 강함. GIN은 한 컬럼이 여러 값(토큰 배열, tsvector, JSONB)을 가질 때의 역색인 — 전문검색·태그 검색에 적합.
- *"alpha는 어떻게 정했나?"* → shadow/오프라인 평가로 정확도 보고 튜닝(고정값 아님을 강조).

---

## 16. 인제스트 파이프라인 (파싱→PII 마스킹→슬라이딩 청킹→임베딩→SHA-256 멱등)

**① 개념**
원문을 검색 가능한 벡터로 만드는 적재 파이프라인. **청킹**(긴 문서를 검색 단위로 자름), **오버랩**(경계에서 문맥 손실 방지), **멱등 적재**(같은 내용 재적재해도 중복 안 생기게 해시로 판별).

**② 내 코드에선**
- `DocumentIngestionService` + `EmbeddingBatchService`:
  1. 파싱/등록(처리 중엔 `activeYn="N"`)
  2. **PII 마스킹** — `PiiMaskingFilter.assertNoSensitivePii()`로 민감정보 유입 차단
  3. **슬라이딩 윈도우 청킹** — `CHUNK_SIZE=800`, `CHUNK_OVERLAP=100`, `pos += size - overlap`로 100자 겹치며 이동, 위치(`char:pos`) 추적
  4. **임베딩** — `embeddingClient.embed(text)` → pgvector 컬럼에 저장
  5. **멱등** — Stub 임베딩은 텍스트 SHA-256 → 고정 벡터(같은 입력=같은 결과). DB `UNIQUE(corpus, source_id, chunk_seq, embedding_model)`로 중복 적재 차단.

**③ 답변 스크립트**
> "문서 적재는 파싱 → PII 마스킹 → 청킹 → 임베딩 → 멱등 저장 파이프라인으로 만들었습니다. 먼저 마스킹 필터로 민감정보가 인덱스에 새어 들어가는 걸 막고, 800자 단위로 자르되 100자씩 겹치게 슬라이딩 윈도우로 청킹해서 청크 경계에서 문맥이 끊기는 문제를 줄였습니다. 적재 멱등성은 콘텐츠 해시로 보장했는데, 같은 문서를 다시 넣어도 (코퍼스, 소스ID, 청크순번, 모델)에 UNIQUE 제약이 걸려 중복이 안 쌓입니다. 13청크 규모로 동작을 검증하고 100K~1M 대규모 적재를 목표로 설계했습니다."

**④ 꼬리질문**
- *"왜 800자/오버랩 100?"* → 임베딩 모델 토큰 한도 안에서 의미 단위 유지 + 경계 문맥 보존의 타협. 토큰 환산 시 보수적으로 잡음.
- *"청킹 전략 다른 거?"* → 고정 크기 vs 문장/문단 단위 vs 시맨틱 청킹. 고정+오버랩은 단순·견고해서 1차 선택.
- *"왜 SHA-256으로 멱등?"* → 내용이 같으면 해시가 같아 재적재를 식별. 재처리·중복 임베딩 비용 방지.
- *"PII 마스킹을 적재 단계에서 하는 이유?"* → 인덱스에 한번 들어가면 검색·LLM 입력으로 계속 노출됨. 입구에서 막는 게 최선.

---

# 마지막 체크리스트 (면접 직전)

- [ ] **트레이드오프 한 문장**: 각 키워드마다 "왜 이걸 택했고 대안 대비 뭘 포기/얻었나"를 한 줄로 준비 (면접관이 가장 좋아함).
- [ ] **12번(토큰 로테이션)**: 실제 구현 여부를 스스로 확정하고 답 정하기. 없으면 "설계/시도"로 정직하게.
- [ ] **2번(KMS/Vault)**: "고려/다음 단계"라고 정직하게 (현재는 환경변수까지).
- [ ] **숫자 외우기**: alpha 0.7/0.3, 청크 800/100, TTL 1시간, RPM 15·RPD 1500, RRF rankConstant 60, 거절 인용 ≥2.
- [ ] **합성 데이터**: "실제 PII로 실험 불가 → 합성 데이터로 검증"이라는 맥락을 RAG/Shadow 답변에 엮기.
- [ ] **한 문장 요약 연습**: 각 키워드를 30초 안에 설명 → 꼬리질문에서 깊이 들어가기.
