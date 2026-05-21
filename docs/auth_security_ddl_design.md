# 인증보안계 DDL 설계 문서

> **DB**: PostgreSQL  
> **최종 수정**: 2026-05-21  
> **테이블 수**: 15개 (외부 참조 1개)

---

## 목차

1. [테이블 목록](#1-테이블-목록)
2. [ERD 관계 구조](#2-erd-관계-구조)
3. [테이블 상세](#3-테이블-상세)
4. [인덱스](#4-인덱스)
5. [설계 원칙](#5-설계-원칙)
6. [ERD 오류 기록](#6-erd-오류-기록)
7. [도메인 간 의존성](#7-도메인-간-의존성)
- [부록 A: 코드 그룹 명세](#부록-a-코드-그룹-명세)
- [부록 B: DDL 적용 순서](#부록-b-ddl-적용-순서)

---

## 1. 테이블 목록

| # | 테이블명 | 한글명 | 설명 |
|---|---|---|---|
| 외부 | `customer` ⬚ | 고객 | **고객계 외부 참조** — 인증보안계 전 테이블의 FK 기준점 |
| 1 | `fds_rule` | FDS탐지룰 | FDS 탐지 조건·위험가중치·조치유형 관리 |
| 2 | `credential` | 계정자격증명 | 로그인ID·비밀번호·계정상태 관리 |
| 3 | `registered_device` | 등록기기 | 고객 등록 기기(모바일/PC/태블릿) |
| 4 | `auth_method` | 인증수단 | SMS·PASS·인증서·PIN·생체 등 인증수단 등록 |
| 5 | `certificate` | 금융인증서 | 공개키 기반 금융인증서 발급·관리 |
| 6 | `mobile_auth` | 휴대폰인증요청 | SMS/PASS 인증코드 발송·검증 이력 |
| 7 | `login_attempt` | 로그인시도이력 | 로그인 시도 전체 이력 (성공/실패) |
| 8 | `login_session` | 로그인세션 | 활성 세션 상태 관리 |
| 9 | `api_token` | API토큰 | ACCESS/REFRESH/OAUTH 토큰 발급·폐기 |
| 10 | `pin` | 간편비밀번호 | 기기 연결 간편 PIN 등록·상태 관리 |
| 11 | `password_history` | 비밀번호이력 | 비밀번호 변경 이력 (재사용 방지용) |
| 12 | `fds_detection` | FDS탐지결과 | FDS 룰 기반 이상거래 탐지 결과 |
| 13 | `fds_incident` | FDS사고처리 | FDS 탐지 결과 기반 사고처리 진행 |
| 14 | `identity_verification` | 본인확인이력 | 본인확인기관(NICE/KCB/SCI/PASS) 확인 이력 |
| 15 | `certificate_use` | 인증서사용이력 | 금융인증서 서명·검증 사용 이력 |

> ⬚ 외부 참조 테이블 — DDL 미포함, 고객계 소속

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
│       └── api_token       (1:N, session_id FK)
├── fds_detection           (1:N, customer_id FK)
│   └── fds_incident        (1:N, fds_detection_id FK)
└── identity_verification   (1:N, customer_id FK)

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
| `fk_api_token_customer` | `api_token.customer_id` | `customer.customer_id` |
| `fk_api_token_session` | `api_token.session_id` | `login_session.session_id` |
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

---

## 3. 테이블 상세

### 3.1 fds_rule (FDS탐지룰)

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 룰ID | `fds_rule_id` | `BIGINT` | ✅ | | PK |
| 룰코드 | `fds_rule_code` | `VARCHAR(30)` | ✅ | | |
| 룰명 | `fds_rule_name` | `VARCHAR(100)` | ✅ | | |
| 룰분류코드 | `fds_rule_category_code` | `VARCHAR(30)` | ✅ | | cust_code_master 참조 |
| 대상이벤트코드 | `fds_rule_target_event_code` | `VARCHAR(50)` | ✅ | | cust_code_master 참조 |
| 조건식JSON | `fds_rule_condition_json` | `JSON` | ✅ | | |
| 위험가중치 | `fds_rule_risk_weight` | `INT` | ✅ | `50` | 0~100 |
| 조치유형코드 | `fds_rule_action_type_code` | `VARCHAR(20)` | ✅ | | cust_code_master 참조 |
| 활성여부 | `fds_rule_active_yn` | `CHAR(1)` | ✅ | `'F'` | T/F |
| 시행일 | `fds_rule_effective_date` | `CHAR(8)` | ✅ | | YYYYMMDD |
| 만료일 | `fds_rule_expiry_date` | `CHAR(8)` | | | YYYYMMDD |
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `now()` | |
| 최초등록자ID | `created_by` | `BIGINT` | ✅ | | |
| 최종수정일시 | `updated_at` | `TIMESTAMPTZ(3)` | ✅ | `now()` | |
| 최종수정자ID | `updated_by` | `BIGINT` | ✅ | | |
| 삭제일시 | `deleted_at` | `TIMESTAMPTZ(3)` | | | soft delete |
| 삭제자ID | `deleted_by` | `BIGINT` | | | |

**PK**: `fds_rule_id`

---

### 3.2 credential (계정자격증명)

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 자격증명ID | `credential_id` | `BIGINT` | ✅ | | PK |
| 고객ID | `customer_id` | `BIGINT` | ✅ | | FK → customer |
| 로그인ID | `login_id` | `VARCHAR(50)` | ✅ | | |
| 비밀번호해시 | `password_hash` | `VARCHAR(255)` | ✅ | | bcrypt/argon2 |
| 비밀번호변경일시 | `password_changed_at` | `TIMESTAMPTZ(3)` | ✅ | | |
| 비밀번호만료일시 | `password_expiry_at` | `TIMESTAMPTZ(3)` | | | |
| 계정상태코드 | `account_status_code` | `VARCHAR(20)` | ✅ | `'ACTIVE'` | cust_code_master 참조 |
| 비밀번호로그인실패횟수 | `password_login_failure_count` | `INT` | ✅ | `0` | |
| 최대비밀번호로그인실패횟수 | `max_password_login_failure_count` | `INT` | | | 잠금 임계치. NULL이면 무제한 |
| 비밀번호로그인잠금일시 | `password_login_locked_at` | `TIMESTAMPTZ(3)` | | | |
| 비밀번호로그인잠금해제일시 | `password_login_unlocked_at` | `TIMESTAMPTZ(3)` | | | |
| 비밀번호최종로그인일시 | `password_last_login_at` | `TIMESTAMPTZ(3)` | | | |
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `now()` | |
| 최초등록자ID | `created_by` | `BIGINT` | ✅ | | |
| 최종수정일시 | `updated_at` | `TIMESTAMPTZ(3)` | ✅ | `now()` | |
| 최종수정자ID | `updated_by` | `BIGINT` | ✅ | | |
| 삭제일시 | `deleted_at` | `TIMESTAMPTZ(3)` | | | soft delete |
| 삭제자ID | `deleted_by` | `BIGINT` | | | |

**PK**: `credential_id`

---

### 3.3 registered_device (등록기기)

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 기기ID | `device_id` | `BIGINT` | ✅ | | PK |
| 고객ID | `customer_id` | `BIGINT` | ✅ | | FK → customer |
| 기기명 | `device_name` | `VARCHAR(100)` | ✅ | | |
| 기기유형코드 | `device_type_code` | `VARCHAR(20)` | ✅ | | cust_code_master 참조 |
| OS명 | `device_os_name` | `VARCHAR(50)` | | | |
| OS버전 | `device_os_version` | `VARCHAR(50)` | | | |
| 기기핑거프린트해시 | `device_fingerprint_hash` | `VARCHAR(255)` | ✅ | | |
| 신뢰기기여부 | `trusted_device_yn` | `CHAR(1)` | ✅ | `'F'` | T/F |
| 지정PC여부 | `designated_pc_yn` | `CHAR(1)` | ✅ | `'F'` | T/F |
| 등록IP | `device_registered_ip` | `VARCHAR(45)` | | | IPv4/IPv6 |
| 최종사용일시 | `device_last_used_at` | `TIMESTAMPTZ(3)` | | | |
| 기기상태코드 | `device_status_code` | `VARCHAR(20)` | ✅ | `'ACTIVE'` | cust_code_master 참조 |
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `now()` | |
| 최초등록자ID | `created_by` | `BIGINT` | ✅ | | |
| 최종수정일시 | `updated_at` | `TIMESTAMPTZ(3)` | ✅ | `now()` | |
| 최종수정자ID | `updated_by` | `BIGINT` | ✅ | | |
| 삭제일시 | `deleted_at` | `TIMESTAMPTZ(3)` | | | soft delete |
| 삭제자ID | `deleted_by` | `BIGINT` | | | |

**PK**: `device_id`

---

### 3.4 auth_method (인증수단)

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 인증수단ID | `auth_method_id` | `BIGINT` | ✅ | | PK |
| 고객ID | `customer_id` | `BIGINT` | ✅ | | FK → customer |
| 인증수단유형코드 | `auth_method_type_code` | `VARCHAR(20)` | ✅ | | cust_code_master 참조 |
| 별칭명 | `auth_method_alias_name` | `VARCHAR(50)` | | | |
| 인증수단상태코드 | `auth_method_status_code` | `VARCHAR(20)` | ✅ | | cust_code_master 참조 |
| 주요인증수단여부 | `primary_auth_method_yn` | `CHAR(1)` | ✅ | `'F'` | T/F |
| 등록일자 | `auth_method_registered_date` | `CHAR(8)` | ✅ | | YYYYMMDD |
| 만료일자 | `auth_method_expiry_date` | `CHAR(8)` | | | YYYYMMDD |
| 최종사용일시 | `auth_method_last_used_at` | `TIMESTAMPTZ(3)` | | | |
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `now()` | |
| 최초등록자ID | `created_by` | `BIGINT` | ✅ | | |
| 최종수정일시 | `updated_at` | `TIMESTAMPTZ(3)` | ✅ | `now()` | |
| 최종수정자ID | `updated_by` | `BIGINT` | ✅ | | |
| 삭제일시 | `deleted_at` | `TIMESTAMPTZ(3)` | | | soft delete |
| 삭제자ID | `deleted_by` | `BIGINT` | | | |

**PK**: `auth_method_id`

---

### 3.5 certificate (금융인증서)

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 인증서ID | `certificate_id` | `BIGINT` | ✅ | | PK |
| 고객ID | `customer_id` | `BIGINT` | ✅ | | FK → customer |
| 인증수단ID | `auth_method_id` | `BIGINT` | ✅ | | FK → auth_method |
| 인증서유형코드 | `certificate_type_code` | `VARCHAR(20)` | ✅ | | cust_code_master 참조 |
| 인증서일련번호 | `certificate_serial_number` | `VARCHAR(100)` | ✅ | | UNIQUE |
| 발급기관 | `certificate_issuer_name` | `VARCHAR(50)` | ✅ | | yessign/KFTC 등 |
| 주체DN | `certificate_subject_dn` | `TEXT` | ✅ | | |
| 발급자DN | `certificate_issuer_dn` | `TEXT` | ✅ | | |
| 공개키 | `certificate_public_key` | `TEXT` | ✅ | | PEM 형식 |
| 용도코드 | `certificate_purpose_code` | `VARCHAR(50)` | ✅ | | cust_code_master 참조 |
| 발급일자 | `certificate_issued_date` | `CHAR(8)` | ✅ | | YYYYMMDD |
| 만료일자 | `certificate_expiry_date` | `CHAR(8)` | ✅ | | YYYYMMDD |
| 갱신예정일자 | `certificate_renewal_scheduled_date` | `CHAR(8)` | | | YYYYMMDD |
| 인증서상태코드 | `certificate_status_code` | `VARCHAR(20)` | ✅ | | cust_code_master 참조 |
| 폐기사유코드 | `certificate_revoke_reason_code` | `VARCHAR(200)` | | | cust_code_master 참조 |
| 폐기일시 | `certificate_revoked_at` | `TIMESTAMPTZ(3)` | | | |
| 인증서로그인실패횟수 | `cert_login_failure_count` | `INT` | ✅ | `0` | 누적 검증 실패 횟수, 성공 시 0으로 리셋 |
| 최대인증서로그인실패횟수 | `max_cert_login_failure_count` | `INT` | ✅ | `10` | 잠금 임계치, 인증서 종류별 정책 적용 가능 |
| 최종인증서로그인실패일시 | `last_cert_login_failure_at` | `TIMESTAMPTZ(3)` | | | 마지막 FAIL 발생 시각, 성공 시 NULL |
| 인증서로그인잠금일시 | `cert_login_locked_at` | `TIMESTAMPTZ(3)` | | | 실패횟수가 임계치 도달하여 잠긴 시각 |
| 인증서로그인잠금해제일시 | `cert_login_unlocked_at` | `TIMESTAMPTZ(3)` | | | 본인확인/관리자 처리로 잠금이 해제된 시각 |
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `now()` | |
| 최초등록자ID | `created_by` | `BIGINT` | ✅ | | |
| 최종수정일시 | `updated_at` | `TIMESTAMPTZ(3)` | ✅ | `now()` | |
| 최종수정자ID | `updated_by` | `BIGINT` | ✅ | | |
| 삭제일시 | `deleted_at` | `TIMESTAMPTZ(3)` | | | soft delete |
| 삭제자ID | `deleted_by` | `BIGINT` | | | |

**PK**: `certificate_id`  
**UNIQUE**: `certificate_serial_number`

---

### 3.6 mobile_auth (휴대폰인증요청)

> 로그 테이블 — soft delete 미적용, `created_at`/`created_by`만 보유

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 휴대폰인증요청ID | `mobile_auth_id` | `BIGINT` | ✅ | | PK |
| 고객ID | `customer_id` | `BIGINT` | | | FK → customer (가입 전 NULL 허용) |
| 인증수단유형코드 | `mobile_auth_type_code` | `VARCHAR(20)` | ✅ | | cust_code_master 참조 |
| 통신사코드 | `mobile_auth_telecom_carrier_code` | `VARCHAR(20)` | ✅ | | cust_code_master 참조 |
| 휴대폰번호 | `mobile_auth_phone_number` | `VARCHAR(20)` | ✅ | | |
| 인증코드해시 | `mobile_auth_code_hash` | `VARCHAR(255)` | ✅ | | |
| 용도코드 | `mobile_auth_purpose_code` | `VARCHAR(30)` | ✅ | | cust_code_master 참조 |
| 요청IP | `mobile_auth_request_ip` | `VARCHAR(45)` | ✅ | | |
| 요청채널코드 | `mobile_auth_request_channel_code` | `VARCHAR(20)` | ✅ | | cust_code_master 참조 |
| 발송일시 | `mobile_auth_sent_at` | `TIMESTAMPTZ(3)` | ✅ | | |
| 만료일시 | `mobile_auth_expiry_at` | `TIMESTAMPTZ(3)` | ✅ | | |
| 인증일시 | `mobile_auth_verified_at` | `TIMESTAMPTZ(3)` | | | |
| 인증여부 | `mobile_auth_verified_yn` | `CHAR(1)` | ✅ | `'F'` | T/F |
| 시도횟수 | `mobile_auth_attempt_count` | `INT` | ✅ | `0` | |
| 실패사유 | `mobile_auth_failure_reason` | `VARCHAR(200)` | | | |
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `now()` | |
| 최초등록자ID | `created_by` | `BIGINT` | ✅ | | |

**PK**: `mobile_auth_id`

---

### 3.7 login_attempt (로그인시도이력)

> 로그 테이블 — soft delete 미적용, `created_at`/`created_by`만 보유

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 로그인시도ID | `login_attempt_id` | `BIGINT` | ✅ | | PK |
| 고객ID | `customer_id` | `BIGINT` | | | FK → customer |
| 기기ID | `device_id` | `BIGINT` | | | FK → registered_device |
| 시도로그인ID | `login_attempt_login_id` | `VARCHAR(50)` | ✅ | | |
| 채널코드 | `login_attempt_channel_code` | `VARCHAR(20)` | ✅ | | cust_code_master 참조 |
| 시도IP | `login_attempt_ip` | `VARCHAR(45)` | ✅ | | |
| IP국가코드 | `login_attempt_ip_country_code` | `VARCHAR(3)` | | | ISO 3166-1 alpha-3 |
| 사용자에이전트 | `login_attempt_user_agent` | `TEXT` | | | |
| 기기핑거프린트 | `login_attempt_device_fingerprint` | `VARCHAR(255)` | | | |
| 성공여부 | `login_attempt_success_yn` | `CHAR(1)` | ✅ | `'F'` | T/F |
| 실패사유코드 | `login_attempt_failure_reason_code` | `VARCHAR(20)` | | | cust_code_master 참조 |
| 시도일시 | `login_attempted_at` | `TIMESTAMPTZ(3)` | ✅ | | |
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `now()` | |
| 최초등록자ID | `created_by` | `BIGINT` | ✅ | | |

**PK**: `login_attempt_id`

---

### 3.8 login_session (로그인세션)

> PK 예외: `VARCHAR(64)` (UUID/랜덤 토큰 형식)

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 세션ID | `session_id` | `VARCHAR(64)` | ✅ | | PK |
| 고객ID | `customer_id` | `BIGINT` | ✅ | | FK → customer |
| 로그인시도ID | `login_attempt_id` | `BIGINT` | ✅ | | FK → login_attempt |
| 기기ID | `device_id` | `BIGINT` | | | FK → registered_device |
| 토큰ID | `token_id` | `BIGINT` | ✅ | | FK 미설정 — api_token과의 참조 방향 확인 후 FK 추가 필요 |
| 발급IP | `session_issued_ip` | `VARCHAR(45)` | ✅ | | |
| 채널코드 | `session_channel_code` | `VARCHAR(20)` | ✅ | | cust_code_master 참조 |
| 세션상태코드 | `session_status_code` | `VARCHAR(20)` | ✅ | | cust_code_master 참조 |
| MFA완료여부 | `session_mfa_completed_yn` | `CHAR(1)` | ✅ | `'F'` | T/F — 2FA 완료 시 고위험 거래 가능 |
| 세션만료일시 | `session_expiry_at` | `TIMESTAMPTZ(3)` | ✅ | | |
| 종료일시 | `session_ended_at` | `TIMESTAMPTZ(3)` | | | |
| 종료사유코드 | `session_end_reason_code` | `VARCHAR(200)` | | | cust_code_master 참조 |
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `now()` | |
| 최초등록자ID | `created_by` | `BIGINT` | ✅ | | |
| 최종수정일시 | `updated_at` | `TIMESTAMPTZ(3)` | ✅ | `now()` | |
| 최종수정자ID | `updated_by` | `BIGINT` | ✅ | | |
| 삭제일시 | `deleted_at` | `TIMESTAMPTZ(3)` | | | soft delete |
| 삭제자ID | `deleted_by` | `BIGINT` | | | |

**PK**: `session_id`

---

### 3.9 api_token (API토큰)

> 로그 테이블 — soft delete 미적용, `token_revoked_at`으로 폐기 상태 관리

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 토큰ID | `token_id` | `BIGINT` | ✅ | | PK |
| 고객ID | `customer_id` | `BIGINT` | ✅ | | FK → customer |
| 세션ID | `session_id` | `VARCHAR(64)` | ✅ | | FK → login_session |
| 토큰유형코드 | `token_type_code` | `VARCHAR(20)` | ✅ | | cust_code_master 참조 |
| 토큰해시 | `token_hash` | `VARCHAR(255)` | ✅ | | |
| 발급채널코드 | `token_issued_channel_code` | `VARCHAR(20)` | ✅ | | cust_code_master 참조 |
| 스코프 | `token_scope` | `VARCHAR(500)` | | | |
| 클라이언트ID | `token_client_id` | `VARCHAR(50)` | | | |
| 발급일시 | `token_issued_at` | `TIMESTAMPTZ(3)` | ✅ | `now()` | |
| 만료일시 | `token_expiry_at` | `TIMESTAMPTZ(3)` | ✅ | | |
| 폐기일시 | `token_revoked_at` | `TIMESTAMPTZ(3)` | | | |
| 폐기사유코드 | `token_revoke_reason_code` | `VARCHAR(200)` | | | cust_code_master 참조 |
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `now()` | |
| 최초등록자ID | `created_by` | `BIGINT` | ✅ | | |

**PK**: `token_id`

---

### 3.10 pin (간편비밀번호)

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 간편비밀번호ID | `pin_id` | `BIGINT` | ✅ | | PK |
| 고객ID | `customer_id` | `BIGINT` | ✅ | | FK → customer |
| 인증수단ID | `auth_method_id` | `BIGINT` | ✅ | | FK → auth_method |
| 기기ID | `device_id` | `BIGINT` | ✅ | | FK → registered_device |
| 간편비밀번호해시 | `pin_hash` | `VARCHAR(255)` | ✅ | | |
| 비밀번호자릿수 | `pin_length` | `INT` | ✅ | | |
| PIN로그인실패횟수 | `pin_login_failure_count` | `INT` | ✅ | `0` | |
| 최대PIN로그인실패횟수 | `max_pin_login_failure_count` | `INT` | | | 잠금 임계치. NULL이면 무제한 |
| PIN로그인잠금일시 | `pin_login_locked_at` | `TIMESTAMPTZ(3)` | | | |
| PIN로그인잠금해제일시 | `pin_login_unlocked_at` | `TIMESTAMPTZ(3)` | | | |
| PIN최종로그인일시 | `pin_last_login_at` | `TIMESTAMPTZ(3)` | | | |
| 간편비밀번호상태코드 | `pin_status_code` | `VARCHAR(20)` | ✅ | | cust_code_master 참조 |
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `now()` | |
| 최초등록자ID | `created_by` | `BIGINT` | ✅ | | |
| 최종수정일시 | `updated_at` | `TIMESTAMPTZ(3)` | ✅ | `now()` | |
| 최종수정자ID | `updated_by` | `BIGINT` | ✅ | | |
| 삭제일시 | `deleted_at` | `TIMESTAMPTZ(3)` | | | soft delete |
| 삭제자ID | `deleted_by` | `BIGINT` | | | |

**PK**: `pin_id`

---

### 3.11 password_history (비밀번호이력)

> 로그 테이블 — soft delete 미적용

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 비밀번호이력ID | `password_history_id` | `BIGINT` | ✅ | | PK |
| 자격증명ID | `credential_id` | `BIGINT` | ✅ | | FK → credential |
| 고객ID | `customer_id` | `BIGINT` | ✅ | | FK → customer |
| 비밀번호해시 | `password_hash` | `VARCHAR(255)` | ✅ | | |
| 변경채널코드 | `password_change_channel_code` | `VARCHAR(20)` | ✅ | | cust_code_master 참조 |
| 변경사유코드 | `password_change_reason_code` | `VARCHAR(200)` | | | cust_code_master 참조 |
| 변경IP | `password_change_ip` | `VARCHAR(45)` | ✅ | | |
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `now()` | |
| 최초등록자ID | `created_by` | `BIGINT` | ✅ | | |

**PK**: `password_history_id`

---

### 3.12 fds_detection (FDS탐지결과)

> 로그 테이블 — soft delete 미적용

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 탐지ID | `fds_detection_id` | `BIGINT` | ✅ | | PK |
| 고객ID | `customer_id` | `BIGINT` | ✅ | | FK → customer |
| 룰ID | `fds_rule_id` | `BIGINT` | ✅ | | FK → fds_rule |
| 이벤트유형코드 | `fds_detection_event_type_code` | `VARCHAR(30)` | ✅ | | cust_code_master 참조 |
| 이벤트참조ID | `fds_detection_event_reference_id` | `BIGINT` | ✅ | | FK 미설정 — 거래계 이벤트 테이블 soft reference |
| 탐지일시 | `fds_detected_at` | `TIMESTAMPTZ(3)` | ✅ | `now()` | |
| 탐지상태코드 | `fds_detection_status_code` | `VARCHAR(20)` | ✅ | `'PENDING'` | cust_code_master 참조 |
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `now()` | |
| 최초등록자ID | `created_by` | `BIGINT` | ✅ | | |

**PK**: `fds_detection_id`

---

### 3.13 fds_incident (FDS사고처리)

> 로그 테이블 — soft delete 미적용

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 사고처리ID | `fds_incident_id` | `BIGINT` | ✅ | | PK |
| 탐지ID | `fds_detection_id` | `BIGINT` | ✅ | | FK → fds_detection |
| 처리담당직원ID | `fds_incident_handler_employee_id` | `BIGINT` | ✅ | | |
| 사고유형코드 | `fds_incident_type_code` | `VARCHAR(20)` | ✅ | | cust_code_master 참조 |
| 처리상태코드 | `fds_incident_process_status_code` | `VARCHAR(20)` | ✅ | | cust_code_master 참조 |
| 금감원신고여부 | `fds_incident_fss_reported_yn` | `CHAR(1)` | ✅ | `'F'` | T/F |
| 신고일시 | `fds_incident_reported_at` | `TIMESTAMPTZ(3)` | | | |
| 종결일시 | `fds_incident_closed_at` | `TIMESTAMPTZ(3)` | | | |
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `now()` | |
| 최초등록자ID | `created_by` | `BIGINT` | ✅ | | |

**PK**: `fds_incident_id`

---

### 3.14 identity_verification (본인확인이력)

> 로그 테이블 — soft delete 미적용

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 본인확인이력ID | `identity_verification_id` | `BIGINT` | ✅ | | PK |
| 고객ID | `customer_id` | `BIGINT` | ✅ | | FK → customer |
| 휴대폰인증요청ID | `mobile_auth_id` | `BIGINT` | | | FK → mobile_auth |
| 본인확인기관코드 | `identity_verification_agency_code` | `VARCHAR(30)` | ✅ | | cust_code_master 참조 |
| 용도코드 | `identity_verification_purpose_code` | `VARCHAR(30)` | ✅ | | cust_code_master 참조 |
| CI값 | `identity_verification_ci_value` | `VARCHAR(88)` | ✅ | | 연계정보 |
| 성명 | `identity_verification_name` | `VARCHAR(50)` | ✅ | | |
| 생년월일 | `identity_verification_birth_date` | `CHAR(8)` | ✅ | | YYYYMMDD |
| 성별코드 | `identity_verification_gender_code` | `CHAR(1)` | ✅ | | cust_code_master 참조 |
| 국적유형코드 | `identity_verification_nationality_type_code` | `VARCHAR(20)` | ✅ | | cust_code_master 참조 |
| 통신사코드 | `identity_verification_telecom_carrier_code` | `VARCHAR(20)` | | | cust_code_master 참조 |
| 휴대폰번호 | `identity_verification_phone_number` | `VARCHAR(20)` | | | |
| 확인일시 | `identity_verified_at` | `TIMESTAMPTZ(3)` | ✅ | | |
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `now()` | |
| 최초등록자ID | `created_by` | `BIGINT` | ✅ | | |

**PK**: `identity_verification_id`

---

### 3.15 certificate_use (인증서사용이력)

> 로그 테이블 — soft delete 미적용

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 인증서사용이력ID | `certificate_use_id` | `BIGINT` | ✅ | | PK |
| 인증서ID | `certificate_id` | `BIGINT` | ✅ | | FK → certificate |
| 고객ID | `customer_id` | `BIGINT` | ✅ | | FK → customer |
| 용도코드 | `certificate_use_purpose_code` | `VARCHAR(30)` | ✅ | | cust_code_master 참조 |
| 대상거래ID | `certificate_use_target_transaction_id` | `VARCHAR(50)` | | | FK 미설정 — 거래계 미구축 상태로 의도적 soft reference. 거래계 구축 후 FK 추가 필요 |
| 대상시스템코드 | `certificate_use_target_system_code` | `VARCHAR(20)` | | | cust_code_master 참조 |
| 서명데이터해시 | `certificate_use_signed_data_hash` | `VARCHAR(255)` | ✅ | | |
| 서명값 | `certificate_use_signature_value` | `TEXT` | ✅ | | |
| 검증결과코드 | `certificate_use_verification_result_code` | `VARCHAR(20)` | ✅ | | cust_code_master 참조 |
| 실패사유 | `certificate_use_failure_reason` | `VARCHAR(200)` | | | |
| 요청IP | `certificate_use_request_ip` | `VARCHAR(45)` | ✅ | | |
| 요청채널코드 | `certificate_use_request_channel_code` | `VARCHAR(20)` | ✅ | | cust_code_master 참조 |
| 사용일시 | `certificate_used_at` | `TIMESTAMPTZ(3)` | ✅ | `now()` | |
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `now()` | |
| 최초등록자ID | `created_by` | `BIGINT` | ✅ | | |

**PK**: `certificate_use_id`

---

## 4. 인덱스

| 인덱스명 | 테이블 | 컬럼 | 종류 | 조건 | 목적 |
|---|---|---|---|---|---|
| `uq_credential_active_login_id` | `credential` | `(login_id)` | UNIQUE | `deleted_at IS NULL` | 활성 계정 내 로그인ID 중복 방지 |
| `idx_registered_device_customer` | `registered_device` | `(customer_id)` | INDEX | `deleted_at IS NULL` | 고객 기준 활성 기기 조회 |
| `idx_login_attempt_customer_at` | `login_attempt` | `(customer_id, login_attempted_at DESC)` | INDEX | | 고객별 로그인 시도 이력 시간순 조회 |
| `idx_login_session_customer_expiry` | `login_session` | `(customer_id, session_expiry_at)` | INDEX | `deleted_at IS NULL` | 고객 활성 세션 만료 일시 조회 |
| `idx_password_history_credential` | `password_history` | `(credential_id, created_at DESC)` | INDEX | | 자격증명별 비밀번호 이력 시간순 조회 |
| `idx_fds_detection_customer_at` | `fds_detection` | `(customer_id, fds_detected_at DESC)` | INDEX | | 고객별 FDS 탐지 이력 시간순 조회 |
| `idx_certificate_use_cert_at` | `certificate_use` | `(certificate_id, certificate_used_at DESC)` | INDEX | | 인증서별 사용 이력 시간순 조회 |

---

## 5. 설계 원칙

### 5.1 타임스탬프

- 모든 타임스탬프 컬럼은 `TIMESTAMPTZ(3)` 사용 (밀리초 정밀도, 타임존 포함)
- 날짜만 필요한 컬럼은 `CHAR(8)` (YYYYMMDD 형식)

### 5.2 불리언 컬럼

- PostgreSQL에서 `CHAR(1)` 불리언 컬럼의 boolean DEFAULT 불가
- `DEFAULT 'F'` 사용 (`'T'` = true, `'F'` = false)

### 5.3 PK 규칙

- 기본: `BIGINT GENERATED ALWAYS AS IDENTITY`
- 예외: `login_session.session_id VARCHAR(64)` — UUID/랜덤 토큰 형식으로 외부 생성

### 5.4 Soft Delete

- 엔티티 테이블에만 `deleted_at TIMESTAMPTZ(3)`, `deleted_by BIGINT` 적용
- 로그·이력 테이블은 Soft Delete 미적용 — `created_at`/`created_by`만 보유

| 구분 | 해당 테이블 |
|---|---|
| Soft Delete 적용 | `fds_rule`, `credential`, `registered_device`, `auth_method`, `certificate`, `login_session`, `pin` |
| Soft Delete 미적용 (로그) | `mobile_auth`, `login_attempt`, `api_token`, `password_history`, `fds_detection`, `fds_incident`, `identity_verification`, `certificate_use` |

### 5.5 NULL 허용 FK

- `mobile_auth.customer_id` — 가입 전 본인확인 절차 허용

### 5.6 코드 참조 방식

- 코드 컬럼은 `cust_code_master` 소프트 참조 (FK 제약 미설정)
- 코드 유효성은 애플리케이션 계층에서 검증

### 5.7 외부 참조 테이블

- `customer` 테이블은 고객계 소속 — 인증보안계 DDL에 미포함
- FK 제약은 정상 설정 (`REFERENCES customer(customer_id)`)

---

## 6. ERD 오류 기록

| 테이블 | 컬럼 | 오류 내용 | 처리 |
|---|---|---|---|
| `fds_detection` | `customer_id` | ERD 도구 오류로 `DEFAULT 'PENDING'` 표기 | 무시 — `BIGINT NOT NULL`, default 없음으로 처리 |
| `login_session` | `token_id` | ERD에서 **`토큰ID BIGINT NOT NULL`** (한글 컬럼명) | `token_id BIGINT NOT NULL`로 수정 |
| `login_attempt` | 실패사유코드 | **`failure_reasonlogin_attempt_failure_reason_code`** 연결 오타 | `login_attempt_failure_reason_code`로 수정 |

---

## 7. 도메인 간 의존성

### 외부 참조 관계

- **인증보안계 → 고객계** (`customer.customer_id`) — 인증보안계 13개 테이블이 `customer_id` 참조
- `customer` 테이블은 고객계 소속, 인증보안계 DDL 미포함

| 인증보안계 테이블 | 참조 컬럼 | 고객계 테이블.컬럼 |
|---|---|---|
| `credential` | `customer_id` | `customer.customer_id` |
| `registered_device` | `customer_id` | `customer.customer_id` |
| `auth_method` | `customer_id` | `customer.customer_id` |
| `certificate` | `customer_id` | `customer.customer_id` |
| `mobile_auth` | `customer_id` | `customer.customer_id` |
| `login_attempt` | `customer_id` | `customer.customer_id` |
| `login_session` | `customer_id` | `customer.customer_id` |
| `api_token` | `customer_id` | `customer.customer_id` |
| `pin` | `customer_id` | `customer.customer_id` |
| `password_history` | `customer_id` | `customer.customer_id` |
| `fds_detection` | `customer_id` | `customer.customer_id` |
| `identity_verification` | `customer_id` | `customer.customer_id` |
| `certificate_use` | `customer_id` | `customer.customer_id` |

### 향후 연결 예정

| 컬럼 | 현재 | 연결 대상 (예정) |
|---|---|---|
| `certificate_use.certificate_use_target_transaction_id` | FK 미설정 (soft reference) | 여신계·수신계 거래 테이블 |
| `fds_detection.fds_detection_event_reference_id` | FK 미설정 (soft reference) | 거래계 이벤트 테이블 |
| `login_session.token_id` | FK 미설정 (참조 방향 미확정) | `api_token.token_id` (양방향 참조 여부 확인 후 결정) |

---

## 부록 A: 코드 그룹 명세

> 인증보안계 코드 컬럼은 `cust_code_master` 소프트 참조. `(code_group_id, code_value)` 복합 PK.

| code_group_id | 사용 테이블.컬럼 | 예시 code_value |
|---|---|---|
| `FDS_RULE_CATEGORY` | `fds_rule.fds_rule_category_code` | — |
| `FDS_EVENT_TYPE` | `fds_rule.fds_rule_target_event_code`, `fds_detection.fds_detection_event_type_code` | `LOGIN` / `TXN` / `PW_CHANGE` / `CERT_USE` / `DEVICE_REG` / `TRANSFER` |
| `FDS_ACTION_TYPE` | `fds_rule.fds_rule_action_type_code` | `BLOCK` / `CHALLENGE` / `MONITOR` |
| `FDS_DETECTION_STATUS` | `fds_detection.fds_detection_status_code` | `PENDING` / `CONFIRMED` / `FALSE_POSITIVE` |
| `FDS_INCIDENT_TYPE` | `fds_incident.fds_incident_type_code` | — |
| `FDS_PROCESS_STATUS` | `fds_incident.fds_incident_process_status_code` | — |
| `ACCOUNT_STATUS` | `credential.account_status_code` | `ACTIVE` / `LOCKED` / `DORMANT` / `CLOSED` |
| `DEVICE_TYPE` | `registered_device.device_type_code` | `MOBILE` / `PC` / `TABLET` |
| `DEVICE_STATUS` | `registered_device.device_status_code` | `ACTIVE` / `SUSPENDED` / `REVOKED` |
| `AUTH_METHOD_TYPE` | `auth_method.auth_method_type_code`, `mobile_auth.mobile_auth_type_code` | `SMS` / `PASS` / `CERT_FIN` / `CERT_COMMON` / `PIN` / `BIO_FACE` / `BIO_FINGER` |
| `AUTH_METHOD_STATUS` | `auth_method.auth_method_status_code` | — |
| `CERT_TYPE` | `certificate.certificate_type_code` | — |
| `CERT_PURPOSE` | `certificate.certificate_purpose_code` | `LOGIN` / `TXN_SIGN` / `CONTRACT_SIGN` |
| `CERT_STATUS` | `certificate.certificate_status_code` | `ACTIVE` / `EXPIRED` / `REVOKED` / `SUSPENDED` |
| `CERT_REVOKE_REASON` | `certificate.certificate_revoke_reason_code` | — |
| `TELECOM_CARRIER` | `mobile_auth.mobile_auth_telecom_carrier_code`, `identity_verification.identity_verification_telecom_carrier_code` | — |
| `MOBILE_AUTH_PURPOSE` | `mobile_auth.mobile_auth_purpose_code` | — |
| `CHANNEL` | `mobile_auth.mobile_auth_request_channel_code`, `login_attempt.login_attempt_channel_code`, `login_session.session_channel_code`, `api_token.token_issued_channel_code`, `certificate_use.certificate_use_request_channel_code` | `APP` / `WEB` / `BRANCH` |
| `LOGIN_FAILURE_REASON` | `login_attempt.login_attempt_failure_reason_code` | — |
| `SESSION_STATUS` | `login_session.session_status_code` | `ACTIVE` / `EXPIRED` / `LOGGED_OUT` / `FORCED_OUT` |
| `SESSION_END_REASON` | `login_session.session_end_reason_code` | — |
| `TOKEN_TYPE` | `api_token.token_type_code` | `ACCESS` / `REFRESH` / `OAUTH` |
| `TOKEN_REVOKE_REASON` | `api_token.token_revoke_reason_code` | — |
| `PIN_STATUS` | `pin.pin_status_code` | — |
| `PASSWORD_CHANGE_REASON` | `password_history.password_change_reason_code` | — |
| `PASSWORD_CHANGE_CHANNEL` | `password_history.password_change_channel_code` | — |
| `ID_VERIFY_AGENCY` | `identity_verification.identity_verification_agency_code` | `NICE` / `KCB` / `SCI` / `PASS` |
| `ID_VERIFY_PURPOSE` | `identity_verification.identity_verification_purpose_code` | — |
| `GENDER` | `identity_verification.identity_verification_gender_code` | `M` / `F` / `U` |
| `NATIONALITY_TYPE` | `identity_verification.identity_verification_nationality_type_code` | `DOMESTIC` / `FOREIGN` |
| `CERT_USE_PURPOSE` | `certificate_use.certificate_use_purpose_code` | — |
| `CERT_VERIFY_RESULT` | `certificate_use.certificate_use_verification_result_code` | — |
| `TARGET_SYSTEM` | `certificate_use.certificate_use_target_system_code` | — |

---

## 부록 B: DDL 적용 순서

> 고객계 DDL 전체 적용 완료 후 아래 순서로 적용.

| 순서 | 테이블 | 의존 대상 |
|:---:|---|---|
| — | *(고객계 전체 선행 적용)* | `customer_ddl_design.md` 부록 B 참조 |
| 1 | `fds_rule` | 없음 |
| 2 | `credential` | `customer` |
| 3 | `registered_device` | `customer` |
| 4 | `auth_method` | `customer` |
| 5 | `certificate` | `customer`, `auth_method` |
| 6 | `mobile_auth` | `customer` |
| 7 | `login_attempt` | `customer`, `registered_device` |
| 8 | `login_session` | `customer`, `login_attempt`, `registered_device` |
| 9 | `api_token` | `customer`, `login_session` |
| 10 | `pin` | `customer`, `auth_method`, `registered_device` |
| 11 | `password_history` | `credential`, `customer` |
| 12 | `fds_detection` | `customer`, `fds_rule` |
| 13 | `fds_incident` | `fds_detection` |
| 14 | `identity_verification` | `customer`, `mobile_auth` |
| 15 | `certificate_use` | `certificate`, `customer` |
