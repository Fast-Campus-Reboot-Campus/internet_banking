# 고객계 DDL 설계 문서

> **DB**: PostgreSQL
> **최종 수정**: 2026-06-11
> **테이블 수**: 17개 (V1 13개 + 마이그레이션 신설 4개)
> **정본 기준**: 본 문서는 ERDCloud 설계가 아니라 **실제 적용된 Flyway 마이그레이션(V1~V26)** 을 정본으로 한다.

---

## 목차

1. [테이블 목록](#1-테이블-목록)
2. [ERD 관계 구조](#2-erd-관계-구조)
3. [테이블 상세](#3-테이블-상세)
4. [인덱스](#4-인덱스)
5. [설계 원칙](#5-설계-원칙)
6. [ERD 오류 기록](#6-erd-오류-기록)
7. [마이그레이션 반영 변경점 (V3~V26)](#7-마이그레이션-반영-변경점-v3v26)
8. [도메인 간 의존성](#8-도메인-간-의존성)
- [부록 A: 코드 그룹 명세](#부록-a-코드-그룹-명세)
- [부록 B: DDL 적용 순서](#부록-b-ddl-적용-순서)

---

## 1. 테이블 목록

| # | 테이블명 | 한글명 | 설명 | 신설 |
|---|---|---|---|---|
| 1 | `cust_code_master` | 고객코드마스터 | 전 도메인 공통 코드 관리. FK 참조 대상 | V1 |
| 2 | `party` | 관계자 | 개인·법인·비법인 공통 최상위 엔티티 | V1 |
| 3 | `customer` | 고객 | party 중 실제 거래 고객. party와 1:N 비식별 | V1 |
| 4 | `party_person` | 개인관계자 | party 중 자연인 상세정보. party와 1:1 식별 | V1 |
| 5 | `party_organization` | 기업관계자 | party 중 법인·비법인 상세정보. party와 1:1 식별 | V1 |
| 6 | `foreigner_info` | 외국인정보 | party_person 중 외국인 추가정보. party_person과 1:1 식별 | V1 |
| 7 | `tax_residency_info` | 납세거주정보 | party별 납세 거주지 정보. party와 1:N | V1 |
| 8 | `compliance_info` | 컴플라이언스정보 | AML·KYC·FATCA·CRS 등 규제 정보. party와 1:1 식별 | V1 |
| 9 | `party_role` | 관계자역할 | party가 수행하는 역할 이력. party와 1:N | V1 |
| 10 | `party_relation` | 관계자관계 | party 간 대표·UBO·가족 등 관계. party와 N:M | V1 |
| 11 | `business_info` | 사업자정보 | party의 사업자등록 정보. party와 1:N | V1 |
| 12 | `customer_status_history` | 고객상태이력 | customer 상태 변경 이력. customer와 1:N | V1 |
| 13 | `customer_grade_history` | 고객등급이력 | customer 등급 변경 이력. customer와 1:N | V1 |
| 14 | `employee` | 직원 | party 중 직원 상세정보(지점·직급). party와 1:1 식별 | **V11** |
| 15 | `sanction_screening_hit` | 제재스크리닝Hit | 제재 스크리닝 탐지 건별 검토 큐. party와 1:N | **V15** |
| 16 | `duplicate_review_case` | 중복고객검토케이스 | 중복 후보 party 쌍의 검토 케이스. party와 N:M | **V16** |
| 17 | `customer_access_log` | 고객조회감사로그 | 직원의 고객 조회 접근 감사로그(append-only) | **V19** |

---

## 2. ERD 관계 구조

```
cust_code_master  (코드 조회, FK 미설정 — soft reference)

party
├── party_person          (1:1 식별, party_id PK+FK)
│   └── foreigner_info    (1:1 식별, party_id PK+FK)
├── party_organization    (1:1 식별, party_id PK+FK)
├── employee              (1:1 식별, employee_id 독립 PK + uq_employee_party)
├── compliance_info       (1:1 식별, party_id PK+FK)
├── tax_residency_info    (1:N, tax_residency_id 독립 PK)
├── party_role            (1:N, role_id 독립 PK)
├── party_relation        (N:M self, from_party_id / to_party_id)
├── business_info         (1:N, business_info_id 독립 PK)
├── sanction_screening_hit (1:N, sanction_screening_hit_id 독립 PK)
├── duplicate_review_case (N:M self, new_party_id / existing_party_id)
└── customer              (1:N 비식별, customer_id 독립 PK)
    ├── customer_status_history  (1:N + self-ref)
    └── customer_grade_history   (1:N + self-ref)

customer_access_log  (감사로그 — accessor/target 모두 soft reference, FK 미설정)
```

### FK 제약 목록

| 제약명 | 자식 테이블.컬럼 | 부모 테이블.컬럼 |
|---|---|---|
| `fk_customer_party` | `customer.party_id` | `party.party_id` |
| `fk_party_person_party` | `party_person.party_id` | `party.party_id` |
| `fk_party_organization_party` | `party_organization.party_id` | `party.party_id` |
| `fk_foreigner_info_party_person` | `foreigner_info.party_id` | `party_person.party_id` |
| `fk_employee_party` | `employee.party_id` | `party.party_id` |
| `fk_tax_residency_info_party` | `tax_residency_info.party_id` | `party.party_id` |
| `fk_compliance_info_party` | `compliance_info.party_id` | `party.party_id` |
| `fk_party_role_party` | `party_role.party_id` | `party.party_id` |
| `fk_party_relation_from` | `party_relation.from_party_id` | `party.party_id` |
| `fk_party_relation_to` | `party_relation.to_party_id` | `party.party_id` |
| `fk_business_info_party` | `business_info.party_id` | `party.party_id` |
| `fk_sanction_screening_hit_party` | `sanction_screening_hit.party_id` | `party.party_id` |
| `fk_duplicate_review_case_new` | `duplicate_review_case.new_party_id` | `party.party_id` |
| `fk_duplicate_review_case_existing` | `duplicate_review_case.existing_party_id` | `party.party_id` |
| `fk_customer_status_history_customer` | `customer_status_history.customer_id` | `customer.customer_id` |
| `fk_customer_status_history_self` | `customer_status_history.previous_customer_status_history_id` | `customer_status_history.customer_status_history_id` |
| `fk_customer_grade_history_customer` | `customer_grade_history.customer_id` | `customer.customer_id` |
| `fk_customer_grade_history_self` | `customer_grade_history.previous_customer_grade_history_id` | `customer_grade_history.customer_grade_history_id` |

> **FK 미설정(soft reference) 컬럼**
> - `customer_status_history.changed_by_employee_id`, `customer_grade_history.changed_by_employee_id` (V19): 직원 행위자. 감사 적재가 FK로 막히면 안 되므로 FK 미설정.
> - `compliance_info.aml_last_assessed_by_employee_id`, `compliance_info.kyc_completed_by_employee_id` (V22): 위와 동일.
> - `sanction_screening_hit.reviewer_employee_id`, `duplicate_review_case.reviewer_employee_id`: 검토자 직원. FK 미설정.
> - `customer_access_log` 전 컬럼: append-only 감사로그. accessor(직원)·target(고객) 모두 FK 미설정.

---

## 3. 테이블 상세

> **공통 컬럼**: 모든 엔티티 테이블은 표준 감사 컬럼(`created_at`/`created_by`/`updated_at`/`updated_by`/`deleted_at`/`deleted_by`)과 낙관적 락 컬럼(`version INT NOT NULL DEFAULT 0`)을 가진다. (§5.8 참조)
> **예외(로그 테이블)**: `customer_status_history`·`customer_grade_history`는 `created_at`/`created_by`만, `customer_access_log`는 `accessed_at`만 보유 — `version`·soft delete 미적용.

### 3.1 cust_code_master (고객코드마스터)

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 코드그룹ID | `code_group_id` | `VARCHAR(30)` | ✅ | | PK(복합) |
| 코드값 | `code_value` | `VARCHAR(20)` | ✅ | | PK(복합) |
| 코드명 | `code_name` | `VARCHAR(100)` | ✅ | | |
| 코드설명 | `description` | `VARCHAR(500)` | | | |
| 정렬순서 | `sort_order` | `INT` | | | |
| 유효시작일자 | `effective_start_date` | `CHAR(8)` | ✅ | | YYYYMMDD |
| 유효종료일자 | `effective_end_date` | `CHAR(8)` | | | YYYYMMDD |
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| 최초등록자ID | `created_by` | `BIGINT` | | | |
| 최종수정일시 | `updated_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| 최종수정자ID | `updated_by` | `BIGINT` | | | |
| 삭제일시 | `deleted_at` | `TIMESTAMPTZ(3)` | | | soft delete |
| 삭제자ID | `deleted_by` | `BIGINT` | | | |
| 버전 | `version` | `INT` | ✅ | `0` | 낙관적 락 |

**PK**: `(code_group_id, code_value)`

---

### 3.2 party (관계자)

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 관계자ID | `party_id` | `BIGINT` | ✅ | IDENTITY | PK |
| 관계자유형코드 | `party_type_code` | `VARCHAR(20)` | ✅ | | PERSONAL/ORGANIZATION (정본은 Party.java 상수) |
| 관계자명 | `party_name` | `VARCHAR(100)` | ✅ | | |
| 관계자영문명 | `party_english_name` | `VARCHAR(200)` | | | |
| 관계자상태코드 | `party_status_code` | `VARCHAR(20)` | ✅ | | ACTIVE/SUSPENDED/CLOSED |
| (공통 감사 6컬럼) | | | | | created_at/by, updated_at/by, deleted_at/by |
| 버전 | `version` | `INT` | ✅ | `0` | 낙관적 락 |

> ⚠️ V11 직원 시드(9003~9009)가 `party_type_code='PERSON'`을 사용한다. 정본값은 `PERSONAL`(개인)이며, 해당 7건은 시드 불일치다. §6 ERD/시드 오류 기록 참조.

---

### 3.3 customer (고객)

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 고객ID | `customer_id` | `BIGINT` | ✅ | IDENTITY | PK |
| 관계자ID | `party_id` | `BIGINT` | ✅ | | FK → party |
| 고객등급코드 | `customer_grade_code` | `VARCHAR(10)` | | | NORMAL/VIP/PB |
| 고객상태코드 | `customer_status_code` | `VARCHAR(20)` | ✅ | | ACTIVE/DORMANT/**SUSPENDED**/CLOSED |
| 주거래여부 | `main_customer_yn` | `CHAR(1)` | ✅ | `'F'` | |
| 신용등급코드 | `credit_rating_code` | `VARCHAR(10)` | | | AAA~D |
| 신용평가일자 | `credit_evaluation_date` | `CHAR(8)` | | | |
| 신용평가기관코드 | `credit_agency_code` | `VARCHAR(10)` | | | KCB/NICE/INTERNAL |
| 선호언어코드 | `preferred_language_code` | `CHAR(2)` | | | ISO 639-1 |
| SMS수신여부 | `sms_receive_yn` | `CHAR(1)` | ✅ | `'F'` | |
| 이메일수신여부 | `email_receive_yn` | `CHAR(1)` | ✅ | `'F'` | |
| 우편물수신여부 | `postal_receive_yn` | `CHAR(1)` | ✅ | `'F'` | |
| 알림수단코드 | `notification_method_code` | `VARCHAR(10)` | | | SMS/EMAIL/KAKAO/APP |
| 이메일 | `email` | `VARCHAR(255)` | | | |
| 전화번호 | `phone` | `VARCHAR(20)` | | | |
| 우편번호 | `zip_code` | `VARCHAR(10)` | | | |
| 주소 | `address` | `VARCHAR(255)` | | | |
| 상세주소 | `address_detail` | `VARCHAR(255)` | | | |
| 가입채널코드 | `join_channel_code` | `VARCHAR(20)` | | | BRANCH/ONLINE/MOBILE/CALL/AGENT |
| 최초가입일자 | `first_join_date` | `CHAR(8)` | | | 재가입 시 불변 |
| 가입일시 | `joined_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | 활성 고객 관계 시작 |
| 최종거래일시 | `last_transaction_at` | `TIMESTAMPTZ(3)` | | | 휴면 판단 기준 |
| 휴면전환일시 | `dormant_at` | `TIMESTAMPTZ(3)` | | | DORMANT 시 NOT NULL |
| 정지전환일시 | `suspended_at` | `TIMESTAMPTZ(3)` | | | **V13** 추가. SUSPENDED 시 NOT NULL |
| 해지일시 | `closed_at` | `TIMESTAMPTZ(3)` | | | CLOSED 시 NOT NULL |
| 해지사유코드 | `close_reason_code` | `VARCHAR(20)` | | | CLOSED 시 NOT NULL |
| 개인정보보유기간만료일자 | `privacy_expiry_date` | `CHAR(8)` | | | closed_at + 5년 |
| (공통 감사 6컬럼) | | | | | |
| 버전 | `version` | `INT` | ✅ | `0` | 낙관적 락 |

**CHECK 제약** (V13에서 SUSPENDED 추가)
```sql
CONSTRAINT chk_customer_lifecycle CHECK (
    (customer_status_code = 'CLOSED'    AND closed_at IS NOT NULL AND close_reason_code IS NOT NULL) OR
    (customer_status_code = 'DORMANT'   AND dormant_at IS NOT NULL) OR
    (customer_status_code = 'SUSPENDED' AND suspended_at IS NOT NULL) OR
    (customer_status_code = 'ACTIVE')
)
```

> `uq_customer_active_per_party`(WHERE `customer_status_code <> 'CLOSED'`)는 SUSPENDED를 비-CLOSED로 간주 → 정지 고객도 활성 슬롯 점유(정지 중 재가입 불가, 의도된 동작).

---

### 3.4 party_person (개인관계자)

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 관계자ID | `party_id` | `BIGINT` | ✅ | | PK + FK → party |
| 주민등록번호 | `rrn_encrypted` | `VARCHAR(255)` | | | AES-256 암호화 |
| CI값 | `ci_value` | `VARCHAR(88)` | | | 본인확인기관 연계정보. `uq_party_person_ci` 부분 유니크(V17) |
| 내외국인구분코드 | `nationality_type_code` | `VARCHAR(20)` | | | DOMESTIC/FOREIGN |
| 국적코드 | `nationality_code` | `CHAR(3)` | | | ISO 3166 |
| 생년월일 | `birth_date` | `CHAR(8)` | | | YYYYMMDD |
| 성별코드 | `gender_code` | `CHAR(1)` | | | M/F/U |
| 혼인상태코드 | `marital_status_code` | `VARCHAR(10)` | | | SINGLE/MARRIED/DIVORCED/WIDOWED |
| 부양가족수 | `dependent_count` | `INT` | | | |
| 직업코드 | `occupation_code` | `VARCHAR(10)` | | | 통계청 KSCO |
| 직업명 | `occupation_name` | `VARCHAR(100)` | | | KSCO 코드 보완 자유 텍스트 |
| 직장명 | `workplace_name` | `VARCHAR(200)` | | | |
| 연소득금액 | `annual_income_amount` | `BIGINT` | | | 원화 단위 |
| 소득증빙코드 | `income_proof_code` | `VARCHAR(10)` | | | SALARY/BUSINESS/RENTAL/PENSION/OTHER |
| 제한능력자유형코드 | `capacity_limit_type_code` | `VARCHAR(20)` | | | NORMAL/MINOR/LIMITED_GUARDIAN/ADULT_GUARDIAN |
| PEP해당여부 | `is_pep_yn` | `CHAR(1)` | ✅ | `'F'` | 정치적 주요인물 여부 |
| PEP유형코드 | `pep_type_code` | `VARCHAR(10)` | | | SELF/FAMILY/CLOSE_ASSOC. is_pep_yn=T 시 필수 |
| PEP해당국가코드 | `pep_country_code` | `CHAR(3)` | | | ISO 3166 |
| PEP직위 | `pep_position` | `VARCHAR(200)` | | | 자유 텍스트 |
| 사망일자 | `death_date` | `CHAR(8)` | | | 사망 시 party_role 자동 종료 트리거 |
| (공통 감사 6컬럼) | | | | | |
| 버전 | `version` | `INT` | ✅ | `0` | 낙관적 락 |

**CHECK 제약**
```sql
CONSTRAINT chk_party_person_pep CHECK (
    (is_pep_yn = 'T' AND pep_type_code IS NOT NULL) OR
    (is_pep_yn = 'F' AND pep_type_code IS NULL AND pep_country_code IS NULL)
)
```

> `pep_type_code`는 고객과 PEP의 **관계 축**(SELF/FAMILY/CLOSE_ASSOC)이고, 국내/해외 구분은 `pep_country_code`(ISO 국가코드)가 담당한다. V26은 잘못 적재된 `'DOMESTIC'`을 `SELF`로 정정하고 국내 사실을 `pep_country_code='KOR'`로 옮겼다.

---

### 3.5 party_organization (기업관계자)

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 관계자ID | `party_id` | `BIGINT` | ✅ | | PK + FK → party |
| 조직세부유형코드 | `org_subtype_code` | `VARCHAR(20)` | ✅ | | CORPORATION/NON_CORPORATION |
| 법인등록번호 | `corp_reg_no` | `CHAR(14)` | | | CORPORATION 시 NOT NULL |
| 법인정식명 | `corp_formal_name` | `VARCHAR(200)` | | | CORPORATION 시 NOT NULL |
| 법인영문정식명 | `corp_formal_english_name` | `VARCHAR(400)` | | | SWIFT·국제 송금용 |
| 본점국가코드 | `hq_country_code` | `CHAR(3)` | | | ISO 3166. 국내=KOR |
| 외국법인등록번호 | `foreign_corp_reg_no_encrypted` | `VARCHAR(255)` | | | AES-256. 외국 법인만 NOT NULL |
| 법인유형코드 | `corp_type_code` | `VARCHAR(20)` | | | STOCK/LIMITED/LLC 등 |
| 비법인유형코드 | `non_corp_type_code` | `VARCHAR(10)` | | | RELIGION/VOLUNTARY 등 |
| 소유형태코드 | `ownership_type_code` | `VARCHAR(10)` | | | COLLECTIVE/COMMON/INDIVIDUAL |
| 대표체제구분코드 | `representative_type_code` | `VARCHAR(10)` | | | SINGLE/JOINT/COMMITTEE |
| 설립일자 | `establishment_date` | `CHAR(8)` | | | |
| 해산일자 | `dissolution_date` | `CHAR(8)` | | | 해산 시 party_role 자동 종료 |
| 자본금액 | `capital_amount` | `BIGINT` | | | 원화 단위. 비법인=NULL |
| 결산월 | `fiscal_month` | `SMALLINT` | | | 1-12. 법인만 |
| 설립목적 | `establishment_purpose` | `VARCHAR(500)` | | | 자유 텍스트 |
| 구성원수 | `member_count` | `INT` | | | 비법인단체만. 법인=NULL |
| 정관규약URL | `charter_url` | `VARCHAR(500)` | | | 파일 스토리지 URL |
| (공통 감사 6컬럼) | | | | | |
| 버전 | `version` | `INT` | ✅ | `0` | 낙관적 락 |

**CHECK 제약**
```sql
CONSTRAINT chk_party_org_subtype CHECK (
    (org_subtype_code = 'CORPORATION'     AND corp_reg_no IS NOT NULL AND corp_type_code IS NOT NULL) OR
    (org_subtype_code = 'NON_CORPORATION' AND non_corp_type_code IS NOT NULL)
),
CONSTRAINT chk_party_org_foreign_corp CHECK (
    (hq_country_code = 'KOR' AND foreign_corp_reg_no_encrypted IS NULL) OR
    (hq_country_code <> 'KOR' AND foreign_corp_reg_no_encrypted IS NOT NULL) OR
    hq_country_code IS NULL
)
```

---

### 3.6 foreigner_info (외국인정보)

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 관계자ID | `party_id` | `BIGINT` | ✅ | | PK + FK → party_person |
| 외국인등록번호 | `foreigner_no_encrypted` | `VARCHAR(255)` | | | AES-256 암호화 |
| 여권번호 | `passport_no` | `VARCHAR(20)` | | | 평문 저장, 화면 마스킹 |
| 여권발급국가코드 | `passport_country_code` | `CHAR(3)` | | | ISO 3166 |
| 여권만료일자 | `passport_expiry_date` | `CHAR(8)` | | | |
| 체류자격코드 | `stay_qualification_code` | `VARCHAR(10)` | | | F2/F4/F5/E7/H2 |
| 체류만료일자 | `stay_expiry_date` | `CHAR(8)` | | | |
| 최근입국일자 | `recent_entry_date` | `CHAR(8)` | | | |
| 체류지주소 | `stay_address` | `VARCHAR(500)` | | | 한국 내 거주지 |
| (공통 감사 6컬럼) | | | | | |
| 버전 | `version` | `INT` | ✅ | `0` | 낙관적 락 |

---

### 3.7 tax_residency_info (납세거주정보)

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 납세거주정보ID | `tax_residency_id` | `BIGINT` | ✅ | IDENTITY | PK |
| 관계자ID | `party_id` | `BIGINT` | ✅ | | FK → party |
| 거주자유형코드 | `resident_type_code` | `VARCHAR(20)` | ✅ | | RESIDENT/NON_RESIDENT |
| 납세거주국코드 | `tax_country_code` | `CHAR(3)` | | | ISO 3166 |
| 외국납세자식별번호 | `foreign_tin` | `VARCHAR(50)` | | | NON_RESIDENT 또는 tax_country≠KOR 시 NOT NULL |
| 원천징수율 | `withholding_rate_bps` | `INT` | | | bps 단위 (14%=1400) |
| 납세거주확인일자 | `tax_residency_confirm_date` | `CHAR(8)` | ✅ | | 납세거주상태확인일 |
| (공통 감사 6컬럼) | | | | | |
| 버전 | `version` | `INT` | ✅ | `0` | 낙관적 락 |

---

### 3.8 compliance_info (컴플라이언스정보)

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 관계자ID | `party_id` | `BIGINT` | ✅ | | PK + FK → party |
| AML위험등급코드 | `aml_risk_level_code` | `VARCHAR(20)` | ✅ | | LOW/MED/HIGH |
| AML최종평가일시 | `aml_last_assessed_at` | `TIMESTAMPTZ(3)` | | | |
| AML최종평가직원ID | `aml_last_assessed_by_employee_id` | `BIGINT` | | | **V22** 추가. 행위자(soft ref). 시스템 자동=NULL |
| AML차기평가예정일 | `aml_next_review_date` | `CHAR(8)` | | | |
| OFAC제재대상여부 | `is_ofac_sanctioned_yn` | `CHAR(1)` | ✅ | `'F'` | |
| UN제재대상여부 | `is_un_sanctioned_yn` | `CHAR(1)` | ✅ | `'F'` | |
| EU제재대상여부 | `is_eu_sanctioned_yn` | `CHAR(1)` | ✅ | `'F'` | |
| 한국제재대상여부 | `is_kr_sanctioned_yn` | `CHAR(1)` | ✅ | `'F'` | 외환거래법 등 |
| 제재대상여부 | `is_sanctioned_yn` | `CHAR(1)` | ✅ | GENERATED | OFAC·UN·EU·KR OR 합산. `GENERATED ALWAYS AS (...) STORED` |
| 제재최종스크리닝일시 | `sanction_last_screened_at` | `TIMESTAMPTZ(3)` | | | |
| 제재차기스크리닝예정일 | `sanction_next_screen_date` | `CHAR(8)` | | | |
| KYC상태코드 | `kyc_status_code` | `VARCHAR(20)` | ✅ | | PENDING/COMPLETED/EXPIRED/FAILED |
| KYC완료일시 | `kyc_completed_at` | `TIMESTAMPTZ(3)` | | | |
| KYC완료직원ID | `kyc_completed_by_employee_id` | `BIGINT` | | | **V22** 추가. 행위자(soft ref). 시스템 자동=NULL |
| KYC만료일 | `kyc_expiry_date` | `CHAR(8)` | | | 고위험 1년·중위험 3년·저위험 5년 |
| KYC차기재인증예정일 | `kyc_next_review_date` | `CHAR(8)` | | | |
| 본인확인수단코드 | `identity_verification_method_code` | `VARCHAR(10)` | | | NICE/KCB/BANK/CERT/ARS |
| CDD수준코드 | `cdd_level_code` | `VARCHAR(20)` | ✅ | | SIMPLE/STANDARD/ENHANCED |
| CDD최종검토일시 | `cdd_last_reviewed_at` | `TIMESTAMPTZ(3)` | | | |
| CDD차기검토예정일 | `cdd_next_review_date` | `CHAR(8)` | | | |
| EDD필요여부 | `edd_required_yn` | `CHAR(1)` | ✅ | `'F'` | |
| EDD최종검토일시 | `edd_last_reviewed_at` | `TIMESTAMPTZ(3)` | | | EDD 대상자만 NOT NULL |
| EDD차기검토예정일 | `edd_next_review_date` | `CHAR(8)` | | | |
| FATCA신고상태코드 | `fatca_status_code` | `VARCHAR(20)` | ✅ | | US_PERSON/NON_US/EXEMPT 등 |
| FATCA최종검토일시 | `fatca_last_reviewed_at` | `TIMESTAMPTZ(3)` | | | |
| FATCA차기검토예정일 | `fatca_next_review_date` | `CHAR(8)` | | | |
| FATCA보고대상여부 | `fatca_reportable_yn` | `CHAR(1)` | ✅ | `'F'` | IRS 보고 대상 |
| CRS신고상태코드 | `crs_status_code` | `VARCHAR(20)` | ✅ | | REPORTABLE/NON_REPORTABLE/EXEMPT 등 |
| CRS최종검토일시 | `crs_last_reviewed_at` | `TIMESTAMPTZ(3)` | | | |
| CRS차기검토예정일 | `crs_next_review_date` | `CHAR(8)` | | | |
| CRS보고대상여부 | `crs_reportable_yn` | `CHAR(1)` | ✅ | `'F'` | 자동정보교환 보고 대상 |
| (공통 감사 6컬럼) | | | | | |
| 버전 | `version` | `INT` | ✅ | `0` | 낙관적 락 |

> `cdd_level_code`·`fatca_status_code`·`crs_status_code`는 표시 전용 컬럼이며, 목록 필터는 불리언 플래그(`edd_required_yn`/`*_reportable_yn`/`is_*_sanctioned_yn`)·`kyc_status_code='COMPLETED'`로만 동작한다. V25가 데모 시드의 비정본 값(`'CDD'`/`'EDD'`/`'NONE'`)을 정본(`STANDARD`/`ENHANCED`/`EXEMPT`)으로 교정했다.

**is_sanctioned_yn GENERATED 정의**
```sql
is_sanctioned_yn CHAR(1) GENERATED ALWAYS AS (
    CASE WHEN is_ofac_sanctioned_yn = 'T' OR is_un_sanctioned_yn = 'T'
           OR is_eu_sanctioned_yn = 'T' OR is_kr_sanctioned_yn = 'T'
         THEN 'T' ELSE 'F' END
) STORED
```

---

### 3.9 party_role (관계자역할)

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 역할ID | `role_id` | `BIGINT` | ✅ | IDENTITY | PK |
| 관계자ID | `party_id` | `BIGINT` | ✅ | | FK → party |
| 역할유형코드 | `role_type_code` | `VARCHAR(20)` | ✅ | | CUST/GRT/UBO/LGAR/EMPLOYEE/BEN/FAM/PROSP/AGT |
| 역할상태코드 | `role_status_code` | `VARCHAR(20)` | ✅ | | ACTIVE/SUSPENDED/CLOSED |
| 역할시작일자 | `role_start_date` | `CHAR(8)` | ✅ | | |
| 역할종료일자 | `role_end_date` | `CHAR(8)` | | | CLOSED 시 NOT NULL |
| 역할종료사유코드 | `role_end_reason_code` | `VARCHAR(20)` | | | CLOSED 시 NOT NULL |
| (공통 감사 6컬럼) | | | | | |
| 버전 | `version` | `INT` | ✅ | `0` | 낙관적 락 |

**CHECK 제약**
```sql
CONSTRAINT chk_party_role_end CHECK (
    (role_status_code = 'CLOSED' AND role_end_date IS NOT NULL AND role_end_reason_code IS NOT NULL) OR
    (role_status_code <> 'CLOSED')
)
```

> 직원 디렉토리(V11)는 `role_type_code='EMPLOYEE'` party_role을 "이 party는 직원"의 정식 게이트로 사용한다. (설계 부록 A의 `ROLE_TYPE`에 `EMP`로 약기됐으나 실제 시드/코드는 `EMPLOYEE` 사용 — §6 참조)

---

### 3.10 party_relation (관계자관계)

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 관계ID | `relation_id` | `BIGINT` | ✅ | IDENTITY | PK |
| 시작_관계자ID | `from_party_id` | `BIGINT` | ✅ | | FK → party. 능동적 측 |
| 대상_관계자ID | `to_party_id` | `BIGINT` | ✅ | | FK → party. 수동적 측 |
| 관계유형코드 | `relation_type_code` | `VARCHAR(10)` | ✅ | | REP/UBO/LGAR/FAM/AGT |
| 관계세부코드 | `relation_detail_code` | `VARCHAR(10)` | | | CEO/CHAIRMAN/SPOUSE/PARENT |
| 지분율 | `equity_ratio_bps` | `INT` | | | bps 단위. UBO 기준 25%=2500 |
| 대리권범위 | `representation_scope` | `VARCHAR(200)` | | | 자유 텍스트 |
| 근거서류URL | `proof_url` | `VARCHAR(500)` | | | 파일 스토리지 |
| 관계검토상태코드 | `relation_review_status_code` | `VARCHAR(20)` | | | **V14** 추가. PENDING/APPROVED/REJECTED. 기존 시드는 NULL(검토 큐 제외) |
| 관계시작일자 | `relation_start_date` | `CHAR(8)` | ✅ | | |
| 관계종료일자 | `relation_end_date` | `CHAR(8)` | | | |
| 관계종료사유코드 | `relation_end_reason_code` | `VARCHAR(20)` | | | |
| (공통 감사 6컬럼) | | | | | |
| 버전 | `version` | `INT` | ✅ | `0` | 낙관적 락 |

**CHECK 제약**
```sql
CONSTRAINT chk_party_relation_no_self CHECK (from_party_id <> to_party_id)
```

---

### 3.11 business_info (사업자정보)

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 사업자정보ID | `business_info_id` | `BIGINT` | ✅ | IDENTITY | PK |
| 관계자ID | `party_id` | `BIGINT` | ✅ | | FK → party |
| 사업자등록번호 | `biz_reg_no` | `CHAR(12)` | ✅ | | NNN-NN-NNNNN. `uq_business_info_biz_reg_no` UNIQUE |
| 사업자상태코드 | `biz_status_code` | `VARCHAR(20)` | ✅ | | CONTINUE/SUSPEND/CLOSE |
| 상호명 | `trade_name` | `VARCHAR(200)` | ✅ | | 사업자등록상 정식 한글 상호명 |
| 영문상호명 | `english_trade_name` | `VARCHAR(400)` | | | SWIFT용 |
| 개업일자 | `opening_date` | `CHAR(8)` | ✅ | | 사업자등록증상 개업일 |
| 폐업일자 | `closing_date` | `CHAR(8)` | | | CLOSE 시 NOT NULL |
| 국세청업종코드 | `nts_industry_code` | `CHAR(6)` | ✅ | | 국세청 업종분류 코드 |
| 한국표준산업분류코드 | `ksic_code` | `CHAR(5)` | ✅ | | 통계청 5자리 |
| 업태코드 | `biz_type_code` | `VARCHAR(10)` | | | 도소매·서비스·제조 등 |
| 종목코드 | `biz_item_code` | `VARCHAR(10)` | ✅ | | 구체적 사업 종목 |
| 과세유형코드 | `tax_type_code` | `VARCHAR(10)` | ✅ | | GENERAL/SIMPLIFIED/EXEMPT |
| (공통 감사 6컬럼) | | | | | |
| 버전 | `version` | `INT` | ✅ | `0` | 낙관적 락 |

---

### 3.12 customer_status_history (고객상태이력)

> 로그 테이블 — soft delete·version 미적용, `created_at`/`created_by`만 보유

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 고객상태이력ID | `customer_status_history_id` | `BIGINT` | ✅ | IDENTITY | PK |
| 직전고객상태이력ID | `previous_customer_status_history_id` | `BIGINT` | | | self-ref FK. 최초 등록은 NULL |
| 고객ID | `customer_id` | `BIGINT` | ✅ | | FK → customer |
| 고객상태코드 | `customer_status_code` | `VARCHAR(20)` | ✅ | | 변경 후 상태 |
| 직전고객상태코드 | `previous_customer_status_code` | `VARCHAR(20)` | | | 변경 전 상태. 최초 등록은 NULL |
| 고객상태변경사유코드 | `customer_status_change_reason_code` | `VARCHAR(20)` | ✅ | | JOIN/INACTIVITY/CUST_REQ 등 |
| 고객상태변경상세사유 | `customer_status_change_reason_detail` | `VARCHAR(500)` | | | 자유 텍스트 |
| 고객상태발효일시 | `customer_status_effective_start_at` | `TIMESTAMPTZ(3)` | ✅ | | 이 상태가 발효된 시점 |
| 고객상태종료일시 | `customer_status_effective_end_at` | `TIMESTAMPTZ(3)` | | | 활성 이력은 NULL |
| 시스템자동전환여부 | `system_auto_triggered_yn` | `CHAR(1)` | ✅ | `'F'` | 휴면·사망 자동 종료 등 |
| 변경직원ID | `changed_by_employee_id` | `BIGINT` | | | **V19** 추가. 행위자(soft ref). 시스템 자동=NULL |
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| 최초등록자ID | `created_by` | `BIGINT` | | | |

---

### 3.13 customer_grade_history (고객등급이력)

> 로그 테이블 — soft delete·version 미적용, `created_at`/`created_by`만 보유

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 고객등급이력ID | `customer_grade_history_id` | `BIGINT` | ✅ | IDENTITY | PK |
| 직전고객등급이력ID | `previous_customer_grade_history_id` | `BIGINT` | | | self-ref FK. 최초 등록은 NULL |
| 고객ID | `customer_id` | `BIGINT` | ✅ | | FK → customer |
| 고객등급코드 | `customer_grade_code` | `VARCHAR(10)` | ✅ | | 변경 후 등급 (NORMAL/VIP/PB) |
| 직전고객등급코드 | `previous_customer_grade_code` | `VARCHAR(10)` | | | 변경 전 등급. 최초 등록은 NULL |
| 고객등급변경사유코드 | `customer_grade_change_reason_code` | `VARCHAR(20)` | ✅ | | INITIAL/PROMOTION/DEMOTION/MANUAL/PERIODIC |
| 고객등급변경상세사유 | `customer_grade_change_reason_detail` | `VARCHAR(500)` | | | 자유 텍스트 |
| 고객등급발효일자 | `customer_grade_effective_start_date` | `CHAR(8)` | ✅ | | 이 등급이 적용되기 시작한 날짜 |
| 고객등급종료일자 | `customer_grade_effective_end_date` | `CHAR(8)` | | | 활성 이력은 NULL |
| 고객등급평가일시 | `customer_grade_evaluated_at` | `TIMESTAMPTZ(3)` | ✅ | | 등급 평가가 수행된 시점 |
| 시스템자동전환여부 | `system_auto_triggered_yn` | `CHAR(1)` | ✅ | `'F'` | 정기 등급 평가·자동 승급/강등 등 |
| 변경직원ID | `changed_by_employee_id` | `BIGINT` | | | **V19** 추가. 행위자(soft ref). 시스템 자동=NULL |
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| 최초등록자ID | `created_by` | `BIGINT` | | | |

---

### 3.14 employee (직원) — V11 신설

> party 1:1 서브타입(party_person과 같은 위치). 직원 판정·직급·지점을 application.yml에서 DB로 이관. `grade_code`는 common `BankRole` enum 이름(예: HQ_RISK, BRANCH_MANAGER)을 그대로 사용하며, JWT roles는 grade_code에서 파생된다.

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 직원ID | `employee_id` | `BIGINT` | ✅ | IDENTITY | PK |
| 관계자ID | `party_id` | `BIGINT` | ✅ | | FK → party. `uq_employee_party` UNIQUE |
| 지점코드 | `branch_code` | `VARCHAR(10)` | ✅ | | 본부=0000, 지점=0001.. |
| 직급코드 | `grade_code` | `VARCHAR(30)` | ✅ | | BankRole enum 이름 |
| 상태코드 | `status_code` | `VARCHAR(20)` | ✅ | `'ACTIVE'` | ACTIVE/CLOSED |
| (공통 감사 6컬럼) | | | | | |
| 버전 | `version` | `INT` | ✅ | `0` | 낙관적 락 |

**PK**: `employee_id`  **UNIQUE**: `party_id`

**CHECK 제약**
```sql
CONSTRAINT chk_employee_status CHECK (status_code IN ('ACTIVE','CLOSED'))
```

---

### 3.15 sanction_screening_hit (제재스크리닝Hit) — V15 신설

> `compliance_info`의 제재 플래그는 "현재 제재대상 여부" 상태만 담는다. 스크리닝 탐지 건별(일치율·Hit유형·검토상태·검토자)을 담는 검토 큐. 제재 Hit 검토 화면(/admin/screening)용.

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 제재Hit ID | `sanction_screening_hit_id` | `BIGINT` | ✅ | IDENTITY | PK |
| 관계자ID | `party_id` | `BIGINT` | ✅ | | FK → party |
| Hit유형코드 | `hit_type_code` | `VARCHAR(30)` | ✅ | | OFAC_SDN / KR_PEP / UN / EU |
| 일치율 | `match_rate` | `INT` | ✅ | | 0~100 유사도(%) |
| 스크리닝상태코드 | `screening_status_code` | `VARCHAR(20)` | ✅ | | PENDING / CLEARED(동명이인) / CONFIRMED(제재확정) |
| 검토직원ID | `reviewer_employee_id` | `BIGINT` | | | soft ref |
| 검토의견 | `review_comment` | `VARCHAR(500)` | | | |
| 탐지일시 | `detected_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| 검토일시 | `reviewed_at` | `TIMESTAMPTZ(3)` | | | |
| (공통 감사 6컬럼) | | | | | |
| 버전 | `version` | `INT` | ✅ | `0` | 낙관적 락 |

**CHECK 제약**
```sql
CONSTRAINT chk_sanction_screening_hit_rate CHECK (match_rate BETWEEN 0 AND 100)
```

---

### 3.16 duplicate_review_case (중복고객검토케이스) — V16 신설

> CI/이름+생년월일로 탐지한 중복 후보 party 쌍의 검토 결과(복본/별개)를 담는 케이스. 중복고객 검토 화면(/admin/duplicates)용.

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 중복검토케이스ID | `duplicate_review_case_id` | `BIGINT` | ✅ | IDENTITY | PK |
| 신규관계자ID | `new_party_id` | `BIGINT` | ✅ | | FK → party. 신규(또는 후보) |
| 기존관계자ID | `existing_party_id` | `BIGINT` | ✅ | | FK → party. 기존 |
| 일치유형코드 | `match_type_code` | `VARCHAR(20)` | ✅ | | CI / NAME_BIRTH |
| 검토상태코드 | `review_status_code` | `VARCHAR(20)` | ✅ | | PENDING / DUPLICATE(복본) / DISTINCT(별개) |
| 검토직원ID | `reviewer_employee_id` | `BIGINT` | | | soft ref |
| 검토의견 | `review_comment` | `VARCHAR(500)` | | | |
| 탐지일시 | `detected_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| 검토일시 | `reviewed_at` | `TIMESTAMPTZ(3)` | | | |
| (공통 감사 6컬럼) | | | | | |
| 버전 | `version` | `INT` | ✅ | `0` | 낙관적 락 |

**CHECK 제약**
```sql
CONSTRAINT chk_duplicate_review_case_distinct_parties CHECK (new_party_id <> existing_party_id)
```

---

### 3.17 customer_access_log (고객조회감사로그) — V19 신설

> 직원의 고객 조회 접근 감사로그. append-only — `version`·soft delete·updated_* 없음, `accessed_at`만 보유. 직원명·역할·지점·고객명은 **조회 시점 스냅샷**으로 적재(조인 없는 단순 SELECT + 전보/개명 후에도 사실 보존). FK는 두지 않는다(감사 적재가 FK로 막히면 안 됨).

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 조회감사로그ID | `customer_access_log_id` | `BIGINT` | ✅ | IDENTITY | PK |
| 조회직원ID | `accessor_employee_id` | `BIGINT` | ✅ | | X-Employee-Id (soft ref) |
| 조회직원명 | `accessor_name` | `VARCHAR(100)` | | | 스냅샷 |
| 조회직원역할 | `accessor_role` | `VARCHAR(40)` | | | BankRole 스냅샷 |
| 조회직원지점코드 | `accessor_branch_code` | `VARCHAR(10)` | | | 스냅샷(지점 범위 필터용) |
| 대상고객ID | `target_customer_id` | `BIGINT` | ✅ | | 조회 대상 customer_id (soft ref) |
| 대상고객명 | `target_customer_name` | `VARCHAR(100)` | | | 스냅샷 |
| 접근행위코드 | `access_action_code` | `VARCHAR(40)` | ✅ | | CUSTOMER_DETAIL / CONTACT_VIEW 등 |
| 조회사유 | `access_reason` | `VARCHAR(500)` | | | 필요 역할만 입력 |
| 조회일시 | `accessed_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |

---

## 4. 인덱스

| 인덱스명 | 테이블 | 컬럼 | 종류 | 조건 | 목적 | 신설 |
|---|---|---|---|---|---|---|
| `uq_party_relation_active` | `party_relation` | `(from_party_id, to_party_id, relation_type_code)` | UNIQUE | `relation_end_date IS NULL AND deleted_at IS NULL` | 유효 관계 중복 방지 | V1 |
| `uq_customer_active_per_party` | `customer` | `(party_id)` | UNIQUE | `customer_status_code <> 'CLOSED' AND deleted_at IS NULL` | party당 활성 고객 1건 | V1 |
| `idx_party_relation_from` | `party_relation` | `(from_party_id, relation_type_code)` | INDEX | | 관계 시작 주체 조회 | V1 |
| `idx_party_relation_to` | `party_relation` | `(to_party_id, relation_type_code)` | INDEX | | 관계 대상 조회 | V1 |
| `idx_party_role_active` | `party_role` | `(party_id, role_status_code)` | INDEX | `role_status_code = 'ACTIVE' AND deleted_at IS NULL` | 활성 역할 조회 | V1 |
| `idx_business_info_party` | `business_info` | `(party_id)` | INDEX | `deleted_at IS NULL` | party 기준 사업자 조회 | V1 |
| `uq_business_info_biz_reg_no` | `business_info` | `(biz_reg_no)` | UNIQUE | | 사업자등록번호 유일성 | V1 |
| `uq_party_person_ci` | `party_person` | `(ci_value)` | UNIQUE | `ci_value IS NOT NULL AND deleted_at IS NULL` | "한 사람(CI)=한 party" 보장 | **V17** |
| `uq_employee_party` | `employee` | `(party_id)` | UNIQUE | | party당 직원 1건 | **V11** |
| `idx_employee_party` | `employee` | `(party_id)` | INDEX | | party 기준 직원 조회 | **V11** |
| `idx_sanction_screening_hit_status` | `sanction_screening_hit` | `(screening_status_code)` | INDEX | `deleted_at IS NULL` | 검토 대기 큐 | **V15** |
| `idx_duplicate_review_case_status` | `duplicate_review_case` | `(review_status_code)` | INDEX | `deleted_at IS NULL` | 검토 대기 큐 | **V16** |
| `idx_customer_access_log_target` | `customer_access_log` | `(target_customer_id)` | INDEX | | 대상 고객별 접근 이력 | **V19** |
| `idx_customer_access_log_accessor` | `customer_access_log` | `(accessor_employee_id)` | INDEX | | 직원별 접근 이력 | **V19** |
| `idx_customer_access_log_at` | `customer_access_log` | `(accessed_at DESC)` | INDEX | | 최근순(감사 기본) | **V19** |

---

## 5. 설계 원칙

### 5.1 타임스탬프

- 모든 타임스탬프 컬럼은 `TIMESTAMPTZ(3)` 사용 (밀리초 정밀도, 타임존 포함)
- 날짜만 필요한 컬럼은 `CHAR(8)` (YYYYMMDD 형식)
- `created_at` 기본값은 `CURRENT_TIMESTAMP(3)` (정밀도 지정자 포함) — `CURRENT_TIMESTAMP`와 `now()` 혼용 금지

### 5.2 불리언 컬럼

- PostgreSQL에서 `CHAR(1)` 불리언 컬럼의 boolean DEFAULT 불가
- `DEFAULT 'F'` 사용 (`'T'` = true, `'F'` = false)

### 5.3 암호화 컬럼 명명 규칙

- AES-256 암호화 컬럼은 `_encrypted` 접미사 사용, 길이 `VARCHAR(255)` 통일

| 컬럼 | 테이블 | 비고 |
|---|---|---|
| `rrn_encrypted` | `party_person` | 주민등록번호 |
| `foreigner_no_encrypted` | `foreigner_info` | 외국인등록번호 |
| `foreign_corp_reg_no_encrypted` | `party_organization` | 외국법인등록번호 |

### 5.4 Soft Delete

- 엔티티 테이블에만 `deleted_at TIMESTAMPTZ(3)`, `deleted_by BIGINT` 적용
- 물리 삭제 대신 `deleted_at IS NOT NULL`로 삭제 판단
- 이력(로그) 테이블은 Soft Delete 미적용

| 구분 | 해당 테이블 |
|---|---|
| Soft Delete 적용 | `cust_code_master`, `party`, `customer`, `party_person`, `party_organization`, `foreigner_info`, `tax_residency_info`, `compliance_info`, `party_role`, `party_relation`, `business_info`, `employee`, `sanction_screening_hit`, `duplicate_review_case` |
| Soft Delete 미적용 (로그) | `customer_status_history`, `customer_grade_history`, `customer_access_log` |

### 5.5 코드 참조 방식

- `cust_code_master`에 대한 FK 제약은 **설정하지 않음** (soft reference)
- 코드 유효성은 애플리케이션 계층(도메인 상수)에서 검증. `code_master`/`cust_code_master`는 표시·시드 용도이며 `@ValidCode` 등 검증에는 쓰이지 않는다(정본=도메인 상수).

### 5.6 isAllowNull vs COMMENT 불일치 처리 원칙

ERDCloud JSON의 `isAllowNull`과 `comment`의 `[NOT NULL]` 표기가 불일치하는 경우 **comment 표기를 우선** 적용(설계 의도로 판단).

### 5.7 국가코드 컬럼 타입

- ISO 3166-1 alpha-3는 항상 3자리 고정이므로 `CHAR(3)` 사용 (`VARCHAR(3)` 혼용 금지)

| 컬럼 | 테이블 |
|---|---|
| `nationality_code`, `pep_country_code` | `party_person` |
| `hq_country_code` | `party_organization` |
| `passport_country_code` | `foreigner_info` |
| `tax_country_code` | `tax_residency_info` |

### 5.8 낙관적 락 (version)

- 모든 엔티티 테이블은 `version INT NOT NULL DEFAULT 0`을 가진다 (JPA `@Version` 낙관적 락).
- **예외**: 이력/감사 로그 테이블(`customer_status_history`, `customer_grade_history`, `customer_access_log`)은 append-only이므로 `version` 미적용.
- 설계 문서(2026-05-26)에는 없던 컬럼으로, V1부터 실제 스키마에 포함돼 있다.

### 5.9 행위자(직원) 기록 — soft reference

- 직원 행위자 컬럼(`*_by_employee_id`)·검토자 컬럼(`reviewer_employee_id`)·감사로그(`customer_access_log`)는 **FK를 두지 않는다**: 감사·이력 적재가 참조 무결성으로 막히면 안 되기 때문. 게이트웨이가 JWT에서 주입하는 `X-Employee-Id`(검증된 employee_id)를 기록한다. 시스템 자동 처리는 NULL.

---

## 6. ERD 오류 기록

> ERDCloud → V1 DDL 변환 시점의 보정 내역. (V3 이후 변경은 §7 참조)

| 테이블 | 컬럼 | 오류 내용 | 처리 |
|---|---|---|---|
| `compliance_info` | `fatca_*` (4개 컬럼) | `FACTA` → **`FATCA`** 오타 | 수정 완료 |
| `compliance_info` | `fatca_status_code` COMMENT | `FACTA고객상태코드` → **`FATCA신고상태코드`** | 수정 완료 |
| `compliance_info` | `is_eu_sanctioned_yn` COMMENT | `[NOT NULL ,,` 이중 쉼표 | 수정 완료 |
| `foreigner_info` | `foreigner_no` | 암호화 컬럼이나 접미사·길이 미적용 | `foreigner_no_encrypted VARCHAR(255)`로 수정 |
| `party_relation` | `updated_at` | ERD 한글명 **"치종수정일시"** 오타 | COMMENT 명시, 영문명 정상 |
| `party_role` | `role_type_code` | `isAllowNull: true`이나 COMMENT `[NOT NULL]` | NOT NULL 적용 |
| `party_organization` | `org_subtype_code` | `isAllowNull: true`이나 COMMENT `[NOT NULL]` | NOT NULL 적용 |
| `customer_status_history` | `system_auto_triggered_yn` | `isAllowNull: true`이나 COMMENT `[NOT NULL]` | NOT NULL 적용 |
| `customer_grade_history` | `customer_grade_evaluated_at` | JSON/COMMENT 불일치 → 이미지에서 `[NOT NULL]` 확인 | NOT NULL 확정 |
| `business_info` | `party_id` | ERD에 FK 표기 누락 | `FK → party.party_id` 추가 |
| `customer` | `party_id` | COMMENT `[NOT NULL]` 미표기 | 관계 정의 기준 NOT NULL + FK 추가 |
| `compliance_info` | 상태코드 5개 | `isAllowNull: true`이나 COMMENT `[NOT NULL]` | NOT NULL 적용 (§5.6) |
| `compliance_info` | `is_sanctioned_yn` | GENERATED 컬럼 | `GENERATED ALWAYS AS ... STORED` 구현 |

### 시드/코드값 불일치 (운영 중 발견)

| 위치 | 불일치 | 정본 | 처리 |
|---|---|---|---|
| `party.party_type_code` | V11 직원 시드(9003~9009)가 `'PERSON'` 사용 | `PERSONAL` (Party.java 상수, RegisterService write) | **미정정** — 직원 7건 한정, 고객 검색은 `partyTypeCode='PERSONAL'` 필터라 직원이 자연히 제외돼 기능 영향 없음. 차기 시드 정정 권장 |
| `party_role.role_type_code` | 부록 A의 약기 `EMP` vs 실제 시드/코드 `EMPLOYEE` | `EMPLOYEE` | 본문/부록을 `EMPLOYEE`로 정정 |
| `compliance_info` cdd/fatca/crs | V24 데모 시드 `'CDD'`/`'EDD'`/`'NONE'` | `STANDARD`/`ENHANCED`/`EXEMPT` | **V25**에서 교정 |
| `party_person.pep_type_code` | V24 시드 `'DOMESTIC'`(국내/해외 축) | `SELF` (관계 축) + `pep_country_code='KOR'` | **V26**에서 교정 |

---

## 7. 마이그레이션 반영 변경점 (V3~V26)

> V1/V2 초기 스키마 이후 적용된 변경. (V2는 인증보안계 — `auth_security_ddl_design.md` 참조)

| 버전 | 구분 | 변경 내용 |
|---|---|---|
| V3 | 시드 | 직원 계정(9001/9002) 시드 |
| V10 | 시드 | 인증서/PIN 시드 리셋 |
| **V11** | 스키마 | `employee` 테이블 신설 + 직원 7계정(9003~9009) party/customer/credential 시드 + EMPLOYEE party_role 부여 |
| V12 | 시드 | 심사·운영 직원 계정 시드 |
| **V13** | 스키마 | `customer.suspended_at` 추가 + `chk_customer_lifecycle`에 SUSPENDED 추가 |
| **V14** | 스키마 | `party_relation.relation_review_status_code` 추가 (대리인 위임 검토 큐) |
| **V15** | 스키마 | `sanction_screening_hit` 테이블 신설 |
| **V16** | 스키마 | `duplicate_review_case` 테이블 신설 |
| **V17** | 인덱스 | `uq_party_person_ci` 부분 유니크 인덱스 |
| V18 | 스키마(인증보안계) | `identity_verification`에 rrn/소비 컬럼 추가 — 인증보안계 문서 참조 |
| **V19** | 스키마 | `customer_status_history`/`customer_grade_history`에 `changed_by_employee_id` 추가 + `customer_access_log` 테이블 신설 |
| V20/V21 | 시드 | 직원 비밀번호·역할·테스트명 정정 |
| **V22** | 스키마 | `compliance_info`에 `aml_last_assessed_by_employee_id`, `kyc_completed_by_employee_id` 추가 |
| V23/V24 | 시드 | 데모 고객/계좌 시드 |
| **V25** | 시드 정정 | `compliance_info` cdd/fatca/crs 비정본 코드값 교정 (§6) |
| **V26** | 시드 정정 | `party_person.pep_type_code` `'DOMESTIC'`→`SELF` 교정 (§6) |

> **주의**: V4(`qr_login_token`)·V5(`cert_pin_hash`)·V6(`withdrawal_account`)·V7/V9는 인증보안계 변경이므로 `auth_security_ddl_design.md`에 기록한다.

---

## 8. 도메인 간 의존성

### 외부 참조 관계

- **고객계 → 없음** (최상위 도메인, 타 도메인에서 참조됨)
- **인증보안계 → 고객계** (`customer.customer_id`) — 인증보안계 테이블이 `customer_id` 참조

### 행위자/검토자 컬럼 (soft reference)

- `*_by_employee_id`·`reviewer_employee_id`·`customer_access_log.accessor_employee_id`는 `employee.employee_id`를 논리적으로 가리키지만 FK 미설정(§5.9).

### 향후 연결 예정

| 참조 컬럼 | 현재 | 연결 대상 (예정) |
|---|---|---|
| — | — | 여신계·수신계·거래계 → `customer.customer_id` |

---

## 부록 A: 코드 그룹 명세

> `cust_code_master`에 적재할 코드 그룹·코드값. `(code_group_id, code_value)` 복합 PK. **정본은 도메인 상수**이며, 본 표는 표시/시드 기준.

| code_group_id | code_value | 한글명 |
|---|---|---|
| `PARTY_TYPE` | `PERSONAL` | 개인 |
| `PARTY_TYPE` | `ORGANIZATION` | 조직 |
| `PARTY_STATUS` | `ACTIVE` / `SUSPENDED` / `CLOSED` | 활성 / 정지 / 해지 |
| `ORG_SUBTYPE` | `CORPORATION` / `NON_CORPORATION` | 법인 / 비법인단체 |
| `CORP_TYPE` | `STOCK` `LIMITED` `LLC` `GENERAL` `LIMITED_PT` `COOPERATIVE` `NPO` | 주식/유한/유한책임/합명/합자/협동조합/비영리 |
| `NON_CORP_TYPE` | `RELIGION` `VOLUNTARY` `AUTONOMY` `CLUB` | 종교/자원봉사/자치/동호회 |
| `OWNERSHIP_TYPE` | `COLLECTIVE` `JOINT` `COMMON` `INDIVIDUAL` | 총유/합유/공유/단독 |
| `REP_TYPE` | `SINGLE` `JOINT` `COMMITTEE` | 단독/공동/위원회 대표 |
| `CUSTOMER_GRADE` | `NORMAL` `VIP` `PB` | 일반/VIP/PB |
| `CUSTOMER_STATUS` | `ACTIVE` `DORMANT` `SUSPENDED` `CLOSED` | 활성/휴면/**정지(V13)**/해지 |
| `CREDIT_RATING` | `AAA`~`D` | 신용등급 |
| `CREDIT_AGENCY` | `KCB` `NICE` `INTERNAL` | 신용평가기관 |
| `NOTIFICATION_METHOD` | `SMS` `EMAIL` `KAKAO` `APP` | 알림수단 |
| `JOIN_CHANNEL` | `BRANCH` `ONLINE` `MOBILE` `CALL` `AGENT` | 가입채널 |
| `END_REASON` | `CUST_REQ` `EXPIRY` `DEATH` `BIZ_CLOSE` `NATURAL_END` `TRANSFER` `REVOKED` `OTHER` | 종료사유 |
| `ROLE_TYPE` | `CUST` `GRT` `UBO` `LGAR` `EMPLOYEE` `BEN` `FAM` `PROSP` `AGT` | 고객/보증인/실소유자/법정대리인/**직원**/수익자/가족/잠재고객/대리인 |
| `ROLE_STATUS` | `ACTIVE` `SUSPENDED` `CLOSED` | 역할상태 |
| `RELATION_TYPE` | `REP` `UBO` `LGAR` `FAM` `AGT` | 관계유형 |
| `RELATION_DETAIL` | `CEO` `CHAIRMAN` `SPOUSE` `PARENT` | 관계세부 |
| `RELATION_REVIEW_STATUS` | `PENDING` `APPROVED` `REJECTED` | **(V14)** 관계 검토상태 |
| `AML_RISK` | `LOW` `MED` `HIGH` | AML 위험등급 |
| `KYC_STATUS` | `PENDING` `COMPLETED` `EXPIRED` `FAILED` | KYC 상태 |
| `ID_VERIFICATION_METHOD` | `NICE` `KCB` `BANK` `CERT` `ARS` | 본인확인수단 |
| `CDD_LEVEL` | `SIMPLE` `STANDARD` `ENHANCED` | CDD 수준 |
| `FATCA_STATUS` | `US_PERSON` `NON_US` `ACTIVE_NFFE` `PASSIVE_NFFE` `EXEMPT` `RECALCITRANT` | FATCA 상태 |
| `CRS_STATUS` | `REPORTABLE` `NON_REPORTABLE` `ACTIVE_NFE` `PASSIVE_NFE` `EXEMPT` | CRS 상태 |
| `BIZ_STATUS` | `CONTINUE` `SUSPEND` `CLOSE` | 사업자상태 |
| `TAX_TYPE` | `GENERAL` `SIMPLIFIED` `EXEMPT` | 과세유형 |
| `NATIONALITY_TYPE` | `DOMESTIC` `FOREIGN` | 내/외국인 |
| `GENDER` | `M` `F` `U` | 성별 |
| `MARITAL_STATUS` | `SINGLE` `MARRIED` `DIVORCED` `WIDOWED` | 혼인상태 |
| `INCOME_PROOF` | `SALARY` `BUSINESS` `RENTAL` `PENSION` `OTHER` | 소득증빙 |
| `CAPACITY_LIMIT` | `NORMAL` `MINOR` `LIMITED_GUARDIAN` `ADULT_GUARDIAN` | 제한능력자유형 |
| `PEP_TYPE` | `SELF` `FAMILY` `CLOSE_ASSOC` | PEP 관계축 (본인/가족/측근) |
| `STAY_QUALIFICATION` | `F2` `F4` `F5` `E7` `H2` | 체류자격 |
| `RESIDENT_TYPE` | `RESIDENT` `NON_RESIDENT` | 거주자유형 |
| `STATUS_CHANGE_REASON` | `JOIN` `INACTIVITY` `CUST_REQ` `REACTIVATE` `DEATH` `BIZ_CLOSE` `REGULATORY` `OTHER` | 상태변경사유 |
| `GRADE_CHANGE_REASON` | `INITIAL` `PROMOTION` `DEMOTION` `MANUAL` `PERIODIC` | 등급변경사유 |
| `EMPLOYEE_GRADE` | BankRole enum (`BRANCH_MANAGER` `DEPUTY_MANAGER` `TELLER` `HQ_REVIEWER` `HQ_RISK` `HQ_MARKETING` `COMPLIANCE` 등) | **(V11)** 직원 직급 = BankRole |
| `EMPLOYEE_STATUS` | `ACTIVE` `CLOSED` | **(V11)** 직원 상태 |
| `SCREENING_HIT_TYPE` | `OFAC_SDN` `KR_PEP` `UN` `EU` | **(V15)** 제재 Hit 유형 |
| `SCREENING_STATUS` | `PENDING` `CLEARED` `CONFIRMED` | **(V15)** 스크리닝 검토상태 |
| `DUP_MATCH_TYPE` | `CI` `NAME_BIRTH` | **(V16)** 중복 일치유형 |
| `DUP_REVIEW_STATUS` | `PENDING` `DUPLICATE` `DISTINCT` | **(V16)** 중복 검토상태 |
| `ACCESS_ACTION` | `CUSTOMER_DETAIL` `CONTACT_VIEW` 등 | **(V19)** 조회 접근행위 |
| `NTS_INDUSTRY` | *(6자리)* | 국세청 업종코드 — 별도 마스터 |
| `KSIC` | *(5자리)* | 한국표준산업분류 — 별도 마스터 |

---

## 부록 B: DDL 적용 순서

> FK 의존성 기준. 상위 테이블이 먼저 생성되어야 함.

| 순서 | 테이블 | 의존 대상 | 신설 |
|:---:|---|---|---|
| 1 | `cust_code_master` | 없음 | V1 |
| 2 | `party` | 없음 | V1 |
| 3 | `party_person` | `party` | V1 |
| 4 | `party_organization` | `party` | V1 |
| 5 | `foreigner_info` | `party_person` | V1 |
| 6 | `compliance_info` | `party` | V1 |
| 7 | `tax_residency_info` | `party` | V1 |
| 8 | `party_role` | `party` | V1 |
| 9 | `party_relation` | `party` | V1 |
| 10 | `business_info` | `party` | V1 |
| 11 | `customer` | `party` | V1 |
| 12 | `customer_status_history` | `customer` (+ self-ref) | V1 |
| 13 | `customer_grade_history` | `customer` (+ self-ref) | V1 |
| 14 | `employee` | `party` | V11 |
| 15 | `sanction_screening_hit` | `party` | V15 |
| 16 | `duplicate_review_case` | `party` | V16 |
| 17 | `customer_access_log` | 없음 (FK 미설정) | V19 |
