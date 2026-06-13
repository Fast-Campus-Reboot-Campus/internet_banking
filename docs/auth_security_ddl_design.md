# 인증보안계 DDL 설계 문서

> **DB**: PostgreSQL
> **최종 수정**: 2026-06-11
> **테이블 수**: 17개 (V2 15개 + 마이그레이션 신설 2개, 외부 참조 1개 별도)
> **정본 기준**: 본 문서는 ERDCloud 설계가 아니라 **실제 적용된 Flyway 마이그레이션(V2~V26)** 을 정본으로 한다.

---

## 목차

1. [테이블 목록](#1-테이블-목록)
2. [ERD 관계 구조](#2-erd-관계-구조)
3. [테이블 상세](#3-테이블-상세)
4. [인덱스](#4-인덱스)
5. [설계 원칙](#5-설계-원칙)
6. [ERD 오류 기록](#6-erd-오류-기록)
7. [마이그레이션 반영 변경점 (V4~V26)](#7-마이그레이션-반영-변경점-v4v26)
8. [도메인 간 의존성](#8-도메인-간-의존성)
- [부록 A: 코드 그룹 명세](#부록-a-코드-그룹-명세)
- [부록 B: DDL 적용 순서](#부록-b-ddl-적용-순서)

---

## 1. 테이블 목록

| # | 테이블명 | 한글명 | 설명 | 신설 |
|---|---|---|---|---|
| 외부 | `customer` ⬚ | 고객 | **고객계 외부 참조** — 인증보안계 전 테이블의 FK 기준점 | — |
| 1 | `fds_rule` | FDS탐지룰 | FDS 탐지 조건·위험가중치·조치유형 | V2 |
| 2 | `credential` | 계정자격증명 | 로그인ID·비밀번호·계정상태 | V2 |
| 3 | `registered_device` | 등록기기 | 고객 등록 기기(모바일/PC/태블릿) | V2 |
| 4 | `auth_method` | 인증수단 | SMS·PASS·인증서·PIN·생체 등 | V2 |
| 5 | `certificate` | 금융인증서 | 공개키 기반 금융인증서 | V2 |
| 6 | `mobile_auth` | 휴대폰인증요청 | SMS/PASS 인증코드 발송·검증 | V2 |
| 7 | `login_attempt` | 로그인시도이력 | 로그인 시도 전체 기록 | V2 |
| 8 | `login_session` | 로그인세션 | 활성 세션 상태 | V2 |
| 9 | `api_token` | API토큰 | ACCESS/REFRESH/OAUTH 토큰 | V2 |
| 10 | `pin` | 간편비밀번호 | 기기 연결 간편 PIN | V2 |
| 11 | `password_history` | 비밀번호이력 | 비밀번호 변경 관리 | V2 |
| 12 | `fds_detection` | FDS탐지결과 | FDS 룰 기반 이상거래 탐지 | V2 |
| 13 | `fds_incident` | FDS사고처리 | FDS 탐지 기반 사고처리 | V2 |
| 14 | `identity_verification` | 본인확인이력 | 본인확인기관 확인 관리 | V2 |
| 15 | `certificate_use` | 인증서사용이력 | 금융인증서 서명·검증 사용 | V2 |
| 16 | `qr_login_token` | QR로그인토큰 | QR코드 로그인(PC 생성→모바일 승인) | **V4** |
| 17 | `withdrawal_account` | 출금계좌 | 고객 출금계좌 등록·순위 | **V6** |

> ⬚ 외부 참조 테이블 — DDL 미포함, 고객계 소속
> ⚠️ **철회됨**: V7이 선반영한 `otp_device`·`security_card`·`security_card_code`·`auth_token`은 참조 코드 0건·설계 미반영으로 **V9에서 DROP**되어 최종 스키마에 없다. (§7 참조)

---

## 2. ERD 관계 구조

```
[고객계 외부 참조]
customer ⬚
├── credential              (1:1, customer_id FK)
│   └── password_history    (1:N, credential_id FK)
├── registered_device       (1:N, customer_id FK)
├── auth_method             (1:N, customer_id FK)
│   ├── certificate         (1:N, auth_method_id FK)
│   │   └── certificate_use (1:N, certificate_id FK)
│   └── pin                 (1:N, auth_method_id FK + device_id FK)
├── mobile_auth             (1:N, customer_id FK — 가입 전 NULL 허용)
│   └── identity_verification (1:N, mobile_auth_id FK)
├── login_attempt           (1:N, customer_id FK)
│   └── login_session       (1:N, login_attempt_id FK)
│       ├── api_token       (1:N, session_id FK)  ↔ 순환참조
│       └── qr_login_token  (1:N, session_id FK — 승인 후 연결)
├── fds_detection           (1:N, customer_id FK)
│   └── fds_incident        (1:N, fds_detection_id FK)
├── identity_verification   (1:N, customer_id FK)
└── withdrawal_account      (1:N, customer_id FK)

fds_rule
└── fds_detection           (1:N, fds_rule_id FK)
```

### FK 제약 목록

| 제약명 | 자식 테이블.컬럼 | 부모 테이블.컬럼 |
|---|---|---|
| `fk_credential_customer` | `credential.customer_id` | `customer.customer_id` |
| `fk_registered_device_customer` | `registered_device.customer_id` | `customer.customer_id` |
| `fk_auth_method_customer` | `auth_method.customer_id` | `customer.customer_id` |
| `fk_certificate_customer` | `certificate.customer_id` | `customer.customer_id` |
| `fk_certificate_auth_method` | `certificate.auth_method_id` | `auth_method.auth_method_id` |
| `fk_mobile_auth_customer` | `mobile_auth.customer_id` | `customer.customer_id` |
| `fk_login_attempt_customer` | `login_attempt.customer_id` | `customer.customer_id` |
| `fk_login_attempt_device` | `login_attempt.device_id` | `registered_device.device_id` |
| `fk_login_session_customer` | `login_session.customer_id` | `customer.customer_id` |
| `fk_login_session_login_attempt` | `login_session.login_attempt_id` | `login_attempt.login_attempt_id` |
| `fk_login_session_device` | `login_session.device_id` | `registered_device.device_id` |
| `fk_login_session_token` | `login_session.token_id` | `api_token.token_id` (DEFERRABLE) |
| `fk_api_token_customer` | `api_token.customer_id` | `customer.customer_id` |
| `fk_api_token_session` | `api_token.session_id` | `login_session.session_id` (DEFERRABLE) |
| `fk_pin_customer` | `pin.customer_id` | `customer.customer_id` |
| `fk_pin_auth_method` | `pin.auth_method_id` | `auth_method.auth_method_id` |
| `fk_pin_device` | `pin.device_id` | `registered_device.device_id` |
| `fk_password_history_credential` | `password_history.credential_id` | `credential.credential_id` |
| `fk_password_history_customer` | `password_history.customer_id` | `customer.customer_id` |
| `fk_fds_detection_customer` | `fds_detection.customer_id` | `customer.customer_id` |
| `fk_fds_detection_rule` | `fds_detection.fds_rule_id` | `fds_rule.fds_rule_id` |
| `fk_fds_incident_detection` | `fds_incident.fds_detection_id` | `fds_detection.fds_detection_id` |
| `fk_identity_verification_customer` | `identity_verification.customer_id` | `customer.customer_id` |
| `fk_identity_verification_mobile_auth` | `identity_verification.mobile_auth_id` | `mobile_auth.mobile_auth_id` |
| `fk_certificate_use_certificate` | `certificate_use.certificate_id` | `certificate.certificate_id` |
| `fk_certificate_use_customer` | `certificate_use.customer_id` | `customer.customer_id` |
| `fk_qr_login_token_customer` | `qr_login_token.customer_id` | `customer.customer_id` |
| `fk_qr_login_token_session` | `qr_login_token.session_id` | `login_session.session_id` |
| `fk_withdrawal_account_customer` | `withdrawal_account.customer_id` | `customer.customer_id` |

---

## 3. 테이블 상세

> **공통 컬럼**: 모든 테이블은 표준 감사 컬럼(`created_at`/`created_by`/`updated_at`/`updated_by`/`deleted_at`/`deleted_by`)과 낙관적 락 컬럼(`version INT NOT NULL DEFAULT 0`)을 가진다. (§5.8). 아래 표에서는 변별 컬럼만 명시하고 공통 컬럼은 생략한다(`api_token` 예외는 §3.9 주석 참조).

### 3.1 fds_rule (FDS탐지룰)

| 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|:---:|---|---|
| `fds_rule_id` | `BIGINT` | ✅ | IDENTITY | PK |
| `fds_rule_code` | `VARCHAR(30)` | ✅ | | |
| `fds_rule_name` | `VARCHAR(100)` | ✅ | | |
| `fds_rule_category_code` | `VARCHAR(30)` | ✅ | | |
| `fds_rule_target_event_code` | `VARCHAR(50)` | ✅ | | |
| `fds_rule_condition_json` | `JSON` | ✅ | | FDS 룰 조건식 |
| `fds_rule_risk_weight` | `INT` | ✅ | `50` | 0~100 |
| `fds_rule_action_type_code` | `VARCHAR(20)` | ✅ | | BLOCK/CHALLENGE/MONITOR |
| `fds_rule_active_yn` | `CHAR(1)` | ✅ | `'F'` | T/F |
| `fds_rule_effective_date` | `CHAR(8)` | ✅ | | YYYYMMDD |
| `fds_rule_expiry_date` | `CHAR(8)` | | | |

**CHECK**: `action_type IN ('BLOCK','CHALLENGE','MONITOR')`, `active_yn IN ('T','F')`, `risk_weight BETWEEN 0 AND 100`
> V8에서 기본 FDS 룰 시드 적재.

---

### 3.2 credential (계정자격증명)

| 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|:---:|---|---|
| `credential_id` | `BIGINT` | ✅ | IDENTITY | PK |
| `customer_id` | `BIGINT` | ✅ | | FK → customer |
| `login_id` | `VARCHAR(50)` | ✅ | | `uq_credential_active_login_id` |
| `password_hash` | `VARCHAR(255)` | ✅ | | bcrypt/argon2 |
| `password_changed_at` | `TIMESTAMPTZ(3)` | ✅ | | |
| `password_expiry_at` | `TIMESTAMPTZ(3)` | | | |
| `account_status_code` | `VARCHAR(20)` | ✅ | `'ACTIVE'` | ACTIVE/LOCKED/DORMANT/CLOSED |
| `password_login_failure_count` | `INT` | ✅ | `0` | |
| `max_password_login_failure_count` | `INT` | ✅ | `5` | 잠금 임계치 |
| `password_login_locked_at` | `TIMESTAMPTZ(3)` | | | |
| `password_login_unlocked_at` | `TIMESTAMPTZ(3)` | | | |
| `password_last_login_at` | `TIMESTAMPTZ(3)` | | | |

**CHECK**: `account_status_code IN ('ACTIVE','LOCKED','DORMANT','CLOSED')`

---

### 3.3 registered_device (등록기기)

| 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|:---:|---|---|
| `device_id` | `BIGINT` | ✅ | IDENTITY | PK |
| `customer_id` | `BIGINT` | ✅ | | FK → customer |
| `device_name` | `VARCHAR(100)` | | | |
| `device_type_code` | `VARCHAR(20)` | ✅ | | MOBILE/PC/TABLET |
| `device_os_name` | `VARCHAR(50)` | | | |
| `device_os_version` | `VARCHAR(50)` | | | |
| `device_fingerprint_hash` | `VARCHAR(255)` | ✅ | | |
| `trusted_device_yn` | `CHAR(1)` | ✅ | `'F'` | |
| `designated_pc_yn` | `CHAR(1)` | ✅ | `'F'` | |
| `device_registered_ip` | `VARCHAR(45)` | ✅ | | IPv4/IPv6 |
| `device_last_used_at` | `TIMESTAMPTZ(3)` | | | |
| `device_status_code` | `VARCHAR(20)` | ✅ | `'ACTIVE'` | ACTIVE/SUSPENDED/REVOKED |

**CHECK**: device_type / device_status / trusted_yn / designated_pc_yn 각 IN 제약

---

### 3.4 auth_method (인증수단)

| 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|:---:|---|---|
| `auth_method_id` | `BIGINT` | ✅ | IDENTITY | PK |
| `customer_id` | `BIGINT` | ✅ | | FK → customer |
| `auth_method_type_code` | `VARCHAR(20)` | ✅ | | 타입 집합은 아래 CHECK |
| `auth_method_alias_name` | `VARCHAR(50)` | | | |
| `auth_method_status_code` | `VARCHAR(20)` | ✅ | | |
| `primary_auth_method_yn` | `CHAR(1)` | ✅ | `'F'` | |
| `auth_method_registered_date` | `CHAR(8)` | ✅ | | YYYYMMDD |
| `auth_method_expiry_date` | `CHAR(8)` | | | |
| `auth_method_last_used_at` | `TIMESTAMPTZ(3)` | | | |

**CHECK** (V4에서 `CERT_AXFUL` 추가, V9에서 최종 복구)
```sql
CONSTRAINT chk_auth_method_type CHECK (auth_method_type_code IN (
    'SMS','PASS','CERT_FIN','CERT_COMMON','CERT_AXFUL','PIN','BIO_FACE','BIO_FINGER'
)),
CONSTRAINT chk_auth_method_primary CHECK (primary_auth_method_yn IN ('T','F'))
```
> V5가 일시적으로 `OTP`/`SECURITY_CARD`를 추가하고 `PIN`·생체를 누락시켰으나, V9가 위 정본 집합으로 복구했다. (§7)

---

### 3.5 certificate (금융인증서)

| 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|:---:|---|---|
| `certificate_id` | `BIGINT` | ✅ | IDENTITY | PK |
| `customer_id` | `BIGINT` | ✅ | | FK → customer |
| `auth_method_id` | `BIGINT` | ✅ | | FK → auth_method |
| `certificate_type_code` | `VARCHAR(20)` | ✅ | | |
| `certificate_serial_number` | `VARCHAR(100)` | ✅ | | `uq_certificate_serial_number` UNIQUE |
| `certificate_issuer_name` | `VARCHAR(50)` | ✅ | | yessign/KFTC 등 |
| `certificate_subject_dn` | `TEXT` | ✅ | | |
| `certificate_issuer_dn` | `TEXT` | ✅ | | |
| `certificate_public_key` | `TEXT` | ✅ | | PEM 형식 |
| `certificate_purpose_code` | `VARCHAR(50)` | ✅ | | LOGIN/TXN_SIGN/CONTRACT_SIGN |
| `certificate_issued_date` | `CHAR(8)` | ✅ | | YYYYMMDD |
| `certificate_expiry_date` | `CHAR(8)` | ✅ | | YYYYMMDD |
| `certificate_renewal_scheduled_date` | `CHAR(8)` | | | |
| `certificate_status_code` | `VARCHAR(20)` | ✅ | | ACTIVE/EXPIRED/REVOKED/SUSPENDED |
| `certificate_revoke_reason_code` | `VARCHAR(200)` | | | |
| `certificate_revoked_at` | `TIMESTAMPTZ(3)` | | | |
| `cert_pin_hash` | `VARCHAR(255)` | | | **V5** 추가. 인증서 암호(PIN) 해시 — 로그인 비밀번호와 분리 |
| `cert_login_failure_count` | `INT` | | | nullable(ERD 확정) |
| `max_cert_login_failure_count` | `INT` | | | nullable |
| `last_cert_login_failure_at` | `TIMESTAMPTZ(3)` | | | |
| `cert_login_locked_at` | `TIMESTAMPTZ(3)` | | | |
| `cert_login_unlocked_at` | `TIMESTAMPTZ(3)` | | | |

**CHECK**: `certificate_status_code IN ('ACTIVE','EXPIRED','REVOKED','SUSPENDED')`

---

### 3.6 mobile_auth (휴대폰인증요청)

> customer_id nullable — 가입 전 본인확인 허용.

| 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|:---:|---|---|
| `mobile_auth_id` | `BIGINT` | ✅ | IDENTITY | PK |
| `customer_id` | `BIGINT` | | | FK → customer (가입 전 NULL) |
| `mobile_auth_method_type_code` | `VARCHAR(20)` | ✅ | | |
| `mobile_auth_telecom_carrier_code` | `VARCHAR(20)` | ✅ | | |
| `mobile_auth_recipient_phone_number` | `VARCHAR(20)` | ✅ | | |
| `mobile_auth_code_hash` | `VARCHAR(255)` | ✅ | | |
| `mobile_auth_purpose_code` | `VARCHAR(30)` | ✅ | | |
| `mobile_auth_request_ip` | `VARCHAR(45)` | ✅ | | |
| `mobile_auth_request_channel_code` | `VARCHAR(20)` | ✅ | | |
| `mobile_auth_sent_at` | `TIMESTAMPTZ(3)` | ✅ | | |
| `mobile_auth_expiry_at` | `TIMESTAMPTZ(3)` | ✅ | | |
| `mobile_auth_verified_at` | `TIMESTAMPTZ(3)` | | | |
| `mobile_auth_verified_yn` | `CHAR(1)` | ✅ | `'F'` | |
| `mobile_auth_attempt_count` | `INT` | ✅ | `0` | |
| `mobile_auth_failure_reason_code` | `VARCHAR(200)` | | | |

**CHECK**: `mobile_auth_verified_yn IN ('T','F')`

---

### 3.7 login_attempt (로그인시도이력)

> customer_id·device_id nullable — 미존재 ID/미등록 기기 시도 허용.

| 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|:---:|---|---|
| `login_attempt_id` | `BIGINT` | ✅ | IDENTITY | PK |
| `customer_id` | `BIGINT` | | | FK → customer |
| `device_id` | `BIGINT` | | | FK → registered_device |
| `attempted_login_id` | `VARCHAR(50)` | ✅ | | |
| `login_attempt_channel_code` | `VARCHAR(20)` | ✅ | | |
| `login_attempt_ip` | `VARCHAR(45)` | ✅ | | |
| `login_attempt_ip_country_code` | `CHAR(3)` | | | ISO 3166 alpha-3 |
| `login_attempt_user_agent` | `TEXT` | | | |
| `login_attempt_device_fingerprint_hash` | `VARCHAR(255)` | | | |
| `login_attempt_success_yn` | `CHAR(1)` | ✅ | `'F'` | |
| `login_attempt_failure_reason_code` | `VARCHAR(20)` | | | |
| `login_attempted_at` | `TIMESTAMPTZ(3)` | ✅ | | |

**CHECK**: `login_attempt_success_yn IN ('T','F')`

---

### 3.8 login_session (로그인세션)

> PK 예외: `VARCHAR(64)` (UUID/랜덤 토큰).

| 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|:---:|---|---|
| `session_id` | `VARCHAR(64)` | ✅ | | PK |
| `customer_id` | `BIGINT` | ✅ | | FK → customer |
| `login_attempt_id` | `BIGINT` | ✅ | | FK → login_attempt |
| `device_id` | `BIGINT` | | | FK → registered_device |
| `token_id` | `BIGINT` | ✅ | | FK → api_token (순환참조, DEFERRABLE) |
| `session_issued_ip` | `VARCHAR(45)` | ✅ | | |
| `session_channel_code` | `VARCHAR(20)` | ✅ | | |
| `session_status_code` | `VARCHAR(20)` | ✅ | | ACTIVE/EXPIRED/LOGGED_OUT/FORCED_OUT |
| `session_mfa_completed_yn` | `CHAR(1)` | ✅ | `'F'` | 2FA 완료 시 고위험 거래 가능 |
| `session_expiry_at` | `TIMESTAMPTZ(3)` | ✅ | | |
| `session_ended_at` | `TIMESTAMPTZ(3)` | | | |
| `session_end_reason_code` | `VARCHAR(20)` | | | |

**CHECK**: `session_status_code IN (...)`, `session_mfa_completed_yn IN ('T','F')`

---

### 3.9 api_token (API토큰)

> **미결정 사항 유지**: `created_at`·`updated_at`·`updated_by`는 ERD `isAllowNull: true` 기준 nullable 유지(다른 엔티티 테이블은 created_at/updated_at NOT NULL). 거래량 높은 테이블이라 NULL 허용 시 모니터링 쿼리 복잡도 증가 — NOT NULL 통일 여부 팀 결정 대기. `token_revoked_at`으로 폐기 상태 별도 관리.

| 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|:---:|---|---|
| `token_id` | `BIGINT` | ✅ | IDENTITY | PK |
| `customer_id` | `BIGINT` | ✅ | | FK → customer |
| `session_id` | `VARCHAR(64)` | ✅ | | FK → login_session (DEFERRABLE) |
| `token_type_code` | `VARCHAR(20)` | ✅ | | ACCESS/REFRESH/OAUTH |
| `token_hash` | `VARCHAR(255)` | ✅ | | |
| `token_issued_channel_code` | `VARCHAR(20)` | ✅ | | |
| `token_scope` | `VARCHAR(500)` | | | |
| `token_client_id` | `VARCHAR(50)` | | | |
| `token_issued_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| `token_expiry_at` | `TIMESTAMPTZ(3)` | ✅ | | |
| `token_revoked_at` | `TIMESTAMPTZ(3)` | | | |
| `token_revoke_reason_code` | `VARCHAR(20)` | | | |
| `created_at` | `TIMESTAMPTZ(3)` | | `CURRENT_TIMESTAMP(3)` | **nullable(예외)** |
| `updated_at` / `updated_by` | | | | **nullable(예외)** |

**CHECK**: `token_type_code IN ('ACCESS','REFRESH','OAUTH')`

---

### 3.10 pin (간편비밀번호)

| 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|:---:|---|---|
| `pin_id` | `BIGINT` | ✅ | IDENTITY | PK |
| `customer_id` | `BIGINT` | ✅ | | FK → customer |
| `auth_method_id` | `BIGINT` | ✅ | | FK → auth_method |
| `device_id` | `BIGINT` | ✅ | | FK → registered_device |
| `pin_hash` | `VARCHAR(255)` | ✅ | | |
| `pin_length` | `INT` | ✅ | | |
| `pin_login_failure_count` | `INT` | ✅ | `0` | |
| `max_pin_login_failure_count` | `INT` | ✅ | `5` | 잠금 임계치 |
| `pin_login_locked_at` | `TIMESTAMPTZ(3)` | | | |
| `pin_login_unlocked_at` | `TIMESTAMPTZ(3)` | | | |
| `pin_last_login_at` | `TIMESTAMPTZ(3)` | | | |
| `pin_status_code` | `VARCHAR(20)` | ✅ | | |

---

### 3.11 password_history (비밀번호이력)

| 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|:---:|---|---|
| `password_history_id` | `BIGINT` | ✅ | IDENTITY | PK |
| `credential_id` | `BIGINT` | ✅ | | FK → credential |
| `customer_id` | `BIGINT` | ✅ | | FK → customer |
| `password_hash` | `VARCHAR(255)` | ✅ | | |
| `password_change_channel_code` | `VARCHAR(20)` | ✅ | | |
| `password_change_reason_code` | `VARCHAR(200)` | | | |
| `password_change_ip` | `VARCHAR(45)` | | | |

---

### 3.12 fds_detection (FDS탐지결과)

| 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|:---:|---|---|
| `fds_detection_id` | `BIGINT` | ✅ | IDENTITY | PK |
| `customer_id` | `BIGINT` | ✅ | | FK → customer (ERD DEFAULT 오류 무시) |
| `fds_rule_id` | `BIGINT` | ✅ | | FK → fds_rule |
| `fds_detection_event_type_code` | `VARCHAR(30)` | ✅ | | |
| `fds_detection_event_reference_id` | `BIGINT` | ✅ | | 거래계 이벤트 soft reference (FK 미설정) |
| `fds_detected_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| `fds_detection_status_code` | `VARCHAR(20)` | ✅ | `'PENDING'` | PENDING/CONFIRMED/FALSE_POSITIVE |

**CHECK**: `fds_detection_status_code IN ('PENDING','CONFIRMED','FALSE_POSITIVE')`

---

### 3.13 fds_incident (FDS사고처리)

| 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|:---:|---|---|
| `fds_incident_id` | `BIGINT` | ✅ | IDENTITY | PK |
| `fds_detection_id` | `BIGINT` | ✅ | | FK → fds_detection |
| `fds_incident_handler_employee_id` | `BIGINT` | | | soft ref |
| `fds_incident_type_code` | `VARCHAR(20)` | ✅ | | |
| `fds_incident_process_status_code` | `VARCHAR(20)` | ✅ | | |
| `fds_incident_fss_reported_yn` | `CHAR(1)` | ✅ | `'F'` | 금감원 신고 여부 |
| `fds_incident_reported_at` | `TIMESTAMPTZ(3)` | | | |
| `fds_incident_closed_at` | `TIMESTAMPTZ(3)` | | | |

**CHECK**: `fds_incident_fss_reported_yn IN ('T','F')`

---

### 3.14 identity_verification (본인확인이력)

> customer_id nullable — SIGNUP 목적 시 customer 미생성 허용. V18에서 주민번호 암호문·소비 컬럼 추가.

| 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|:---:|---|---|
| `identity_verification_id` | `BIGINT` | ✅ | IDENTITY | PK |
| `customer_id` | `BIGINT` | | | FK → customer (SIGNUP 시 NULL) |
| `mobile_auth_id` | `BIGINT` | ✅ | | FK → mobile_auth |
| `identity_verification_agency_code` | `VARCHAR(30)` | ✅ | | NICE/KCB/SCI/PASS |
| `identity_verification_purpose_code` | `VARCHAR(30)` | ✅ | | |
| `identity_verification_ci_value` | `VARCHAR(88)` | ✅ | | 연계정보 |
| `identity_verification_name` | `VARCHAR(50)` | ✅ | | |
| `identity_verification_birth_date` | `CHAR(8)` | ✅ | | |
| `identity_verification_gender_code` | `CHAR(1)` | ✅ | | |
| `identity_verification_nationality_type_code` | `VARCHAR(20)` | ✅ | | |
| `identity_verification_telecom_carrier_code` | `VARCHAR(20)` | | | |
| `identity_verification_phone_number` | `VARCHAR(20)` | | | |
| `identity_verified_at` | `TIMESTAMPTZ(3)` | ✅ | | |
| `rrn_encrypted` | `VARCHAR(255)` | | | **V18** 주민번호 AES-256 암호문(평문 미보관). 가입 시 `party_person.rrn_encrypted`로 복사 |
| `consumed_yn` | `CHAR(1)` | ✅ | `'F'` | **V18** 가입 소비 여부 (검증 1건 = 가입 1건) |
| `consumed_customer_id` | `BIGINT` | | | **V18** 소비 시 연결된 고객 |
| `consumed_at` | `TIMESTAMPTZ(3)` | | | **V18** 소비 일시 |

**CHECK**: `agency_code IN ('NICE','KCB','SCI','PASS')`, `consumed_yn IN ('T','F')` (V18)

---

### 3.15 certificate_use (인증서사용이력)

> `certificate_use_target_transaction_id`: 거래계 미구축 → `VARCHAR(50)` 임시(soft reference). 거래계 구축 후 PK 타입 통일 필요(§8).

| 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|:---:|---|---|
| `certificate_use_id` | `BIGINT` | ✅ | IDENTITY | PK |
| `certificate_id` | `BIGINT` | ✅ | | FK → certificate |
| `customer_id` | `BIGINT` | ✅ | | FK → customer |
| `purpose_code` | `VARCHAR(30)` | ✅ | | |
| `certificate_use_target_transaction_id` | `VARCHAR(50)` | | | 거래계 soft ref |
| `certificate_use_target_system_code` | `VARCHAR(20)` | | | |
| `certificate_use_signed_data_hash` | `VARCHAR(255)` | ✅ | | |
| `certificate_use_signature_value` | `TEXT` | ✅ | | |
| `certificate_use_verification_result_code` | `VARCHAR(20)` | ✅ | | |
| `certificate_use_failure_reason_code` | `VARCHAR(200)` | | | |
| `certificate_use_request_ip` | `VARCHAR(45)` | ✅ | | |
| `certificate_use_request_channel_code` | `VARCHAR(20)` | ✅ | | |
| `certificate_used_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |

---

### 3.16 qr_login_token (QR로그인토큰) — V4 신설

> QR코드 로그인 플로우: PC에서 QR 생성(PENDING) → 모바일 앱 스캔(SCANNED) → 모바일 승인(APPROVED) → PC 세션 발급. customer_id는 스캔 후 확정(스캔 전 NULL), session_id는 승인 완료 후 연결.

| 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|:---:|---|---|
| `qr_token_id` | `BIGINT` | ✅ | IDENTITY | PK |
| `qr_token_hash` | `VARCHAR(255)` | ✅ | | `uq_qr_login_token_hash` UNIQUE |
| `customer_id` | `BIGINT` | | | FK → customer (스캔 전 NULL) |
| `session_id` | `VARCHAR(64)` | | | FK → login_session (승인 후 연결) |
| `qr_status_code` | `VARCHAR(20)` | ✅ | `'PENDING'` | PENDING/SCANNED/APPROVED/EXPIRED/CANCELLED |
| `request_ip` | `VARCHAR(45)` | ✅ | | |
| `request_channel_code` | `VARCHAR(20)` | ✅ | `'WEB'` | |
| `issued_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| `expiry_at` | `TIMESTAMPTZ(3)` | ✅ | | |
| `scanned_at` | `TIMESTAMPTZ(3)` | | | |
| `approved_at` | `TIMESTAMPTZ(3)` | | | |

**CHECK**: `qr_status_code IN ('PENDING','SCANNED','APPROVED','EXPIRED','CANCELLED')`

---

### 3.17 withdrawal_account (출금계좌) — V6 신설

> 고객의 출금계좌 등록/삭제/순위변경. 동일 고객의 활성(미삭제) 계좌는 계좌번호 중복 금지.

| 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|:---:|---|---|
| `withdrawal_account_id` | `BIGINT` | ✅ | IDENTITY | PK |
| `customer_id` | `BIGINT` | ✅ | | FK → customer |
| `account_number` | `VARCHAR(50)` | ✅ | | `uq_withdrawal_account_active` |
| `bank_code` | `VARCHAR(10)` | ✅ | | |
| `bank_name` | `VARCHAR(50)` | ✅ | | |
| `account_holder_name` | `VARCHAR(100)` | | | |
| `account_alias` | `VARCHAR(100)` | | | |
| `registration_type` | `VARCHAR(20)` | ✅ | `'ONLINE'` | |
| `priority_order` | `SMALLINT` | ✅ | `0` | 순위 |
| `registered_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |

---

## 4. 인덱스

| 인덱스명 | 테이블 | 컬럼 | 종류 | 조건 | 신설 |
|---|---|---|---|---|---|
| `uq_credential_active_login_id` | `credential` | `(login_id)` | UNIQUE | `deleted_at IS NULL` | V2 |
| `idx_registered_device_customer` | `registered_device` | `(customer_id)` | INDEX | `deleted_at IS NULL` | V2 |
| `idx_login_attempt_customer_at` | `login_attempt` | `(customer_id, login_attempted_at DESC)` | INDEX | | V2 |
| `idx_login_session_customer_expiry` | `login_session` | `(customer_id, session_expiry_at)` | INDEX | `deleted_at IS NULL` | V2 |
| `idx_password_history_credential` | `password_history` | `(credential_id, created_at DESC)` | INDEX | | V2 |
| `idx_fds_detection_customer_at` | `fds_detection` | `(customer_id, fds_detected_at DESC)` | INDEX | | V2 |
| `idx_certificate_use_cert_at` | `certificate_use` | `(certificate_id, certificate_used_at DESC)` | INDEX | | V2 |
| `uq_certificate_serial_number` | `certificate` | `(certificate_serial_number)` | UNIQUE | | V2 |
| `idx_qr_login_token_hash_status` | `qr_login_token` | `(qr_token_hash, qr_status_code)` | INDEX | `deleted_at IS NULL` | **V4** |
| `idx_qr_login_token_expiry` | `qr_login_token` | `(expiry_at)` | INDEX | `qr_status_code = 'PENDING' AND deleted_at IS NULL` | **V4** |
| `uq_qr_login_token_hash` | `qr_login_token` | `(qr_token_hash)` | UNIQUE | | **V4** |
| `uq_withdrawal_account_active` | `withdrawal_account` | `(customer_id, account_number)` | UNIQUE | `deleted_at IS NULL` | **V6** |

---

## 5. 설계 원칙

### 5.1 타임스탬프
- 타임스탬프는 `TIMESTAMPTZ(3)`, 날짜는 `CHAR(8)`(YYYYMMDD). `created_at` 기본값 `CURRENT_TIMESTAMP(3)`.

### 5.2 불리언 컬럼
- `CHAR(1)` + `DEFAULT 'F'` (`'T'`/`'F'`).

### 5.3 PK 규칙
- 기본 `BIGINT GENERATED ALWAYS AS IDENTITY`. 예외: `login_session.session_id VARCHAR(64)`(외부 생성 UUID/랜덤 토큰).

### 5.4 Soft Delete
- 인증보안계 전 테이블에 `deleted_at`/`deleted_by` 적용(full audit). 이력 테이블(mobile_auth, login_attempt, password_history, fds_*, identity_verification, certificate_use, api_token)도 적용.

### 5.5 NULL 허용 FK

| 컬럼 | 이유 |
|---|---|
| `mobile_auth.customer_id` | 가입 전 본인확인 허용 |
| `login_attempt.customer_id` | 미존재 ID 로그인 시도 허용 |
| `login_attempt.device_id` | 미등록 기기 시도 허용 |
| `login_session.device_id` | 미등록 기기 세션 허용 |
| `identity_verification.customer_id` | SIGNUP 목적 시 customer 미생성 허용 |
| `qr_login_token.customer_id` | 스캔 전 고객 미확정 (V4) |
| `qr_login_token.session_id` | 승인 전 세션 미발급 (V4) |

### 5.6 코드 참조 방식
- 코드 컬럼은 `cust_code_master` 소프트 참조(FK 미설정). 유효성은 애플리케이션 도메인 상수에서 검증.

### 5.7 외부 참조 테이블
- `customer`는 고객계 소속 — 인증보안계 DDL 미포함. FK 제약은 정상 설정(`REFERENCES customer(customer_id)`).

### 5.8 낙관적 락 (version)
- 모든 테이블에 `version INT NOT NULL DEFAULT 0` (JPA `@Version`). 설계 문서(2026-05-26)에는 없던 컬럼으로, V2부터 실제 스키마에 포함돼 있다.

### 5.9 암호화/해시 컬럼
- `_encrypted` 접미사 = AES-256 암호문(`identity_verification.rrn_encrypted`). `_hash` 접미사 = 단방향 해시(`password_hash`, `token_hash`, `pin_hash`, `cert_pin_hash`, `mobile_auth_code_hash`, `qr_token_hash` 등).

### 5.10 순환 참조 처리
- `login_session.token_id` ↔ `api_token.session_id`는 상호 참조. 두 FK 모두 `DEFERRABLE INITIALLY DEFERRED`. 동일 트랜잭션 내 **세션 INSERT → 토큰 INSERT → 세션 token_id UPDATE → COMMIT** 순서로 처리.

---

## 6. ERD 오류 기록

> ERDCloud → V2 DDL 변환 시점의 보정 내역. (V4 이후 변경은 §7 참조)

| 테이블 | 컬럼 | 오류 내용 | 처리 |
|---|---|---|---|
| `fds_detection` | `customer_id` | ERD 도구 오류로 `DEFAULT 'PENDING'` 표기 | 무시 — `BIGINT NOT NULL` |
| `login_session` | `token_id` | 한글 컬럼명(`토큰ID`) | `token_id BIGINT NOT NULL` |
| `login_attempt` | 실패사유코드 | pName 오타(연결 누락) | `login_attempt_failure_reason_code` |
| `api_token` | `session_id` | `isAllowNull: true` vs COMMENT `[NOT NULL]` | 논리상 NOT NULL |
| `auth_method` | `customer_id` | COMMENT "가입 전 NULL 가능" | ERD 기준 NOT NULL(가입 전은 `mobile_auth.customer_id`) |
| `identity_verification` | `mobile_auth_id` | `isAllowNull: true` vs COMMENT `[NOT NULL]` | NOT NULL |
| `credential` | `max_password_login_failure_count` | COMMENT `[NOT NULL, DEFAULT 5]` | NOT NULL, DEFAULT 5 |
| `pin` | `max_pin_login_failure_count` | COMMENT `[NOT NULL, DEFAULT 5]` | NOT NULL, DEFAULT 5 |
| `certificate` | `cert_login_failure_count`, `max_*` | 이전 문서 NOT NULL 오기재 | nullable로 수정, DEFAULT 제거 |
| `registered_device` | `device_registered_ip` | `isAllowNull: true` vs COMMENT `[NOT NULL]` | NOT NULL |
| `api_token` | `created_at`/`updated_at`/`updated_by` | ERD nullable | nullable 유지. **NOT NULL 통일 여부 팀 결정 대기** (§3.9) |

---

## 7. 마이그레이션 반영 변경점 (V4~V26)

> V2 초기 스키마 이후 적용된 인증보안계 변경. (V1·고객계 변경은 `customer_ddl_design.md` §7 참조)

| 버전 | 구분 | 변경 내용 |
|---|---|---|
| **V4** | 스키마 | `auth_method` 타입에 `CERT_AXFUL` 추가 + `qr_login_token` 테이블 신설 + customer 1 인증서/인증수단 테스트 시드 |
| **V5** | 스키마 | `certificate.cert_pin_hash` 추가 (인증서 PIN을 로그인 비밀번호와 분리) |
| **V6** | 스키마 | `withdrawal_account` 테이블 신설 |
| V7 | 스키마(철회됨) | `otp_device`/`security_card`/`security_card_code`/`auth_token` 선반영 + `auth_method` 타입에 OTP/SECURITY_CARD 추가(PIN·생체 누락) — **V9에서 전량 철회** |
| V8 | 시드 | 기본 FDS 룰 시드 |
| **V9** | 스키마 | V7 신설 4테이블 DROP + `auth_method` 타입 CHECK를 정본 집합(SMS/PASS/CERT_FIN/CERT_COMMON/CERT_AXFUL/PIN/BIO_FACE/BIO_FINGER)으로 복구 |
| V10 | 시드 | 인증서/PIN 시드 리셋 |
| **V18** | 스키마 | `identity_verification`에 `rrn_encrypted`/`consumed_yn`/`consumed_customer_id`/`consumed_at` 추가 + `chk_identity_verification_consumed` |

> V11~V17·V19~V26 중 인증보안계 구조 변경은 없다(해당 버전은 고객계·시드·직원 디렉토리 변경 — 고객계 문서 참조). `api_token` `cert_login_*` 등은 V2 정의 유지.

---

## 8. 도메인 간 의존성

### 외부 참조 관계
- **인증보안계 → 고객계** (`customer.customer_id`) — `credential`, `registered_device`, `auth_method`, `certificate`, `mobile_auth`, `login_attempt`, `login_session`, `api_token`, `pin`, `password_history`, `fds_detection`, `identity_verification`, `certificate_use`, `qr_login_token`, `withdrawal_account`가 참조.
- `customer`는 고객계 소속, 인증보안계 DDL 미포함.

### 향후 연결 예정

| 컬럼 | 현재 타입 | 연결 대상 (예정) |
|---|---|---|
| `certificate_use.certificate_use_target_transaction_id` | `VARCHAR(50)` | 여신계·수신계 거래 테이블 |
| `fds_detection.fds_detection_event_reference_id` | `BIGINT` | 거래계 이벤트 테이블 |

> **타입 통일 메모**: 거래계 구축 시 위 두 컬럼 타입을 거래계 PK 타입에 맞춰 동시 통일(현재 `VARCHAR(50)` vs `BIGINT` 불일치).

### 행위자 컬럼 (soft reference)
- `fds_incident.fds_incident_handler_employee_id`는 `employee.employee_id`를 논리적으로 가리키나 FK 미설정.

---

## 부록 A: 코드 그룹 명세

> 인증보안계 코드 컬럼은 `cust_code_master` 소프트 참조. **정본은 도메인 상수**, 본 표는 표시/시드 기준.

| code_group_id | 사용 위치 | 예시 code_value |
|---|---|---|
| `FDS_RULE_CATEGORY` | `fds_rule.fds_rule_category_code` | — |
| `FDS_EVENT_TYPE` | `fds_rule.target_event`, `fds_detection.event_type` | `LOGIN`/`TXN`/`PW_CHANGE`/`CERT_USE`/`DEVICE_REG`/`TRANSFER` |
| `FDS_ACTION_TYPE` | `fds_rule.action_type` | `BLOCK`/`CHALLENGE`/`MONITOR` |
| `FDS_DETECTION_STATUS` | `fds_detection.status` | `PENDING`/`CONFIRMED`/`FALSE_POSITIVE` |
| `FDS_INCIDENT_TYPE` / `FDS_PROCESS_STATUS` | `fds_incident.*` | — |
| `ACCOUNT_STATUS` | `credential.account_status_code` | `ACTIVE`/`LOCKED`/`DORMANT`/`CLOSED` |
| `DEVICE_TYPE` / `DEVICE_STATUS` | `registered_device.*` | `MOBILE`/`PC`/`TABLET` · `ACTIVE`/`SUSPENDED`/`REVOKED` |
| `AUTH_METHOD_TYPE` | `auth_method.type`, `mobile_auth.method_type` | `SMS`/`PASS`/`CERT_FIN`/`CERT_COMMON`/**`CERT_AXFUL`**/`PIN`/`BIO_FACE`/`BIO_FINGER` |
| `AUTH_METHOD_STATUS` | `auth_method.status` | — |
| `CERT_TYPE` / `CERT_PURPOSE` / `CERT_STATUS` / `CERT_REVOKE_REASON` | `certificate.*` | `LOGIN`/`TXN_SIGN`/`CONTRACT_SIGN` · `ACTIVE`/`EXPIRED`/`REVOKED`/`SUSPENDED` |
| `TELECOM_CARRIER` | `mobile_auth.*`, `identity_verification.*` | — |
| `MOBILE_AUTH_PURPOSE` | `mobile_auth.purpose` | — |
| `CHANNEL` | 다수(`mobile_auth`/`login_attempt`/`login_session`/`api_token`/`certificate_use`/`qr_login_token`) | `APP`/`WEB`/`BRANCH` |
| `LOGIN_FAILURE_REASON` | `login_attempt.failure_reason` | — |
| `SESSION_STATUS` / `SESSION_END_REASON` | `login_session.*` | `ACTIVE`/`EXPIRED`/`LOGGED_OUT`/`FORCED_OUT` |
| `TOKEN_TYPE` / `TOKEN_REVOKE_REASON` | `api_token.*` | `ACCESS`/`REFRESH`/`OAUTH` |
| `PIN_STATUS` | `pin.status` | — |
| `PASSWORD_CHANGE_REASON` / `PASSWORD_CHANGE_CHANNEL` | `password_history.*` | — |
| `ID_VERIFY_AGENCY` / `ID_VERIFY_PURPOSE` | `identity_verification.*` | `NICE`/`KCB`/`SCI`/`PASS` |
| `GENDER` / `NATIONALITY_TYPE` | `identity_verification.*` | `M`/`F`/`U` · `DOMESTIC`/`FOREIGN` |
| `CERT_USE_PURPOSE` / `CERT_VERIFY_RESULT` / `TARGET_SYSTEM` | `certificate_use.*` | — |
| `QR_STATUS` | `qr_login_token.qr_status_code` | **(V4)** `PENDING`/`SCANNED`/`APPROVED`/`EXPIRED`/`CANCELLED` |
| `WITHDRAWAL_REG_TYPE` | `withdrawal_account.registration_type` | **(V6)** `ONLINE` 등 |

---

## 부록 B: DDL 적용 순서

> 고객계 DDL 전체 적용 완료 후 아래 순서로 적용.

| 순서 | 테이블 | 의존 대상 | 신설 |
|:---:|---|---|---|
| — | *(고객계 전체 선행 적용)* | `customer_ddl_design.md` 부록 B | — |
| 1 | `fds_rule` | 없음 | V2 |
| 2 | `credential` | `customer` | V2 |
| 3 | `registered_device` | `customer` | V2 |
| 4 | `auth_method` | `customer` | V2 |
| 5 | `certificate` | `customer`, `auth_method` | V2 |
| 6 | `mobile_auth` | `customer` | V2 |
| 7 | `login_attempt` | `customer`, `registered_device` | V2 |
| 8 | `login_session` | `customer`, `login_attempt`, `registered_device` | V2 |
| 9 | `api_token` | `customer`, `login_session` (순환참조 DEFERRABLE) | V2 |
| 10 | `pin` | `customer`, `auth_method`, `registered_device` | V2 |
| 11 | `password_history` | `credential`, `customer` | V2 |
| 12 | `fds_detection` | `customer`, `fds_rule` | V2 |
| 13 | `fds_incident` | `fds_detection` | V2 |
| 14 | `identity_verification` | `customer`, `mobile_auth` | V2 |
| 15 | `certificate_use` | `certificate`, `customer` | V2 |
| 16 | `qr_login_token` | `customer`, `login_session` | V4 |
| 17 | `withdrawal_account` | `customer` | V6 |
