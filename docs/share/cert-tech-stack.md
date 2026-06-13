# 인증서 구현 — 기술 스택

> **작성자**: 문수현
> **분류**: 작업공유 (고객·인증 도메인)
> **정본 기준**: 아래 항목은 `services/customer-service` 실제 코드와 대조 검증함(파일·라인 명시).

한 문장 요약: **Spring Boot + JPA + PostgreSQL/Flyway** 위에서 **BCrypt(PIN) · SHA-256(MessageDigest) · JWT + Redis(세션)**로 인증서 발급·검증·로그인을 구현했고, **공개키/전자서명 연산만 mock**이다.

---

## 도메인 / 영속성

- Java 17 / Spring Boot 3.x, JPA(Hibernate) `@Entity` 매핑 — `Certificate.java`, `BaseEntity` 상속(soft delete `deletedAt`)
- PostgreSQL 16, Flyway 마이그레이션으로 스키마 버전 관리 — V2(`auth_security` 스키마) → V4(AXful/QR) → V5(`cert_pin_hash`) → V7(OTP/보안카드)
- 상태값은 엔티티 상수 + DB CHECK 제약 (`ACTIVE` / `EXPIRED` / `REVOKED` / `SUSPENDED`)

## 인증서 PIN 처리

- Spring Security `PasswordEncoder`(BCrypt)로 PIN 해시 저장·검증 — `passwordEncoder.encode()` / `.matches()`
- 인증서 로그인 실패 누적 잠금: 엔티티 내부 카운터 — `certLoginFailureCount` / `maxCertLoginFailureCount` / `certLoginLockedAt` (`Certificate.java:77,80,86`)

## 로그인 / 세션

- 인증 성공 시 JWT 발급 — `JwtProvider`로 access/refresh 토큰 생성, claims에 `customerId` · `email` · `roles` · `branch` · `grade`
- Redis(`StringRedisTemplate`)에 refresh 토큰을 SHA-256 해시로 저장, TTL = `JwtProperties.refreshTokenValidity()`

## 서명 / 시리얼

- 시리얼번호: `java.util.UUID` 기반 — `{TYPE}-{CUSTOMER_ID}-{yyyyMMdd}-{UUID8}` (`CertIssueService.java:69,74`)
- `signedDataHash`: JDK `java.security.MessageDigest` SHA-256 (`CertLoginService.java:158`)
- ⚠️ `certificatePublicKey`는 `"MOCK_PUBLIC_KEY_" + serial` 문자열 — 실제 키쌍/CA 서명(BouncyCastle·X.509) **미구현** (`CertIssueService.java:79`, `QrCertService.java:135`)

## 트랜잭션 / 연동

- `@Transactional(noRollbackFor = BusinessException.class)` — 인증 실패해도 사용이력은 커밋 (`CertLoginService.java:49`)
- FDS 모듈(`FdsService.evaluate`)에 인증서 로그인 이벤트 전달 → 룰 평가(`CERT_FAIL_BLOCK_5`), 발동 시 `CUST_060` 차단 (`CertLoginService.java:89-90`)
- 로그인 시도 이력·토큰·세션·FDS 공통 후처리는 `AuthEventService`에 위임 (`CertLoginService.java:46`)

## API 계층

- Spring MVC `@RestController` — `CertIssueController` / `CertLoginController` / `CertManageController` / `QrLoginController`
- 프론트(Next.js)는 `app/api/auth/cert-login/route.ts` 등 route handler로 프록시

---

> 관련 문서: [고객·인증 도메인 아키텍처](../customer-auth-architecture.md) · [인증보안계 DDL](../auth_security_ddl_design.md)
