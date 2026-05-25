# 고객계 DDL 설계 문서

> **DB**: PostgreSQL  
> **최종 수정**: 2026-05-26  
> **테이블 수**: 13개

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
| 1 | `cust_code_master` | 고객코드마스터 | 전 도메인 공통 코드 관리. FK 참조 대상 |
| 2 | `party` | 관계자 | 개인·법인·비법인 공통 최상위 엔티티 |
| 3 | `customer` | 고객 | party 중 실제 거래 고객. party와 1:N 비식별 |
| 4 | `party_person` | 개인관계자 | party 중 자연인 상세정보. party와 1:1 식별 |
| 5 | `party_organization` | 기업관계자 | party 중 법인·비법인 상세정보. party와 1:1 식별 |
| 6 | `foreigner_info` | 외국인정보 | party_person 중 외국인 추가정보. party_person과 1:1 식별 |
| 7 | `tax_residency_info` | 납세거주정보 | party별 납세 거주지 정보. party와 1:N |
| 8 | `compliance_info` | 컴플라이언스정보 | AML·KYC·FATCA·CRS 등 규제 정보. party와 1:1 식별 |
| 9 | `party_role` | 관계자역할 | party가 수행하는 역할 이력. party와 1:N |
| 10 | `party_relation` | 관계자관계 | party 간 대표·UBO·가족 등 관계. party와 N:M |
| 11 | `business_info` | 사업자정보 | party의 사업자등록 정보. party와 1:N |
| 12 | `customer_status_history` | 고객상태이력 | customer 상태 변경 이력. customer와 1:N |
| 13 | `customer_grade_history` | 고객등급이력 | customer 등급 변경 이력. customer와 1:N |

---

## 2. ERD 관계 구조

```
cust_code_master  (코드 조회, FK 미설정 — soft reference)

party
├── party_person          (1:1 식별, party_id PK+FK)
│   └── foreigner_info    (1:1 식별, party_id PK+FK)
├── party_organization    (1:1 식별, party_id PK+FK)
├── compliance_info       (1:1 식별, party_id PK+FK)
├── tax_residency_info    (1:N, tax_residency_id 독립 PK)
├── party_role            (1:N, role_id 독립 PK)
├── party_relation        (N:M self, from_party_id / to_party_id)
├── business_info         (1:N, business_info_id 독립 PK)
└── customer              (1:N 비식별, customer_id 독립 PK)
    ├── customer_status_history  (1:N + self-ref)
    └── customer_grade_history   (1:N + self-ref)
```

### FK 제약 목록

| 제약명 | 자식 테이블.컬럼 | 부모 테이블.컬럼 |
|---|---|---|
| `fk_customer_party` | `customer.party_id` | `party.party_id` |
| `fk_party_person_party` | `party_person.party_id` | `party.party_id` |
| `fk_party_organization_party` | `party_organization.party_id` | `party.party_id` |
| `fk_foreigner_info_party_person` | `foreigner_info.party_id` | `party_person.party_id` |
| `fk_tax_residency_info_party` | `tax_residency_info.party_id` | `party.party_id` |
| `fk_compliance_info_party` | `compliance_info.party_id` | `party.party_id` |
| `fk_party_role_party` | `party_role.party_id` | `party.party_id` |
| `fk_party_relation_from` | `party_relation.from_party_id` | `party.party_id` |
| `fk_party_relation_to` | `party_relation.to_party_id` | `party.party_id` |
| `fk_business_info_party` | `business_info.party_id` | `party.party_id` |
| `fk_customer_status_history_customer` | `customer_status_history.customer_id` | `customer.customer_id` |
| `fk_customer_status_history_self` | `customer_status_history.previous_customer_status_history_id` | `customer_status_history.customer_status_history_id` |
| `fk_customer_grade_history_customer` | `customer_grade_history.customer_id` | `customer.customer_id` |
| `fk_customer_grade_history_self` | `customer_grade_history.previous_customer_grade_history_id` | `customer_grade_history.customer_grade_history_id` |

---

## 3. 테이블 상세

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

**PK**: `(code_group_id, code_value)`

---

### 3.2 party (관계자)

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 관계자ID | `party_id` | `BIGINT` | ✅ | | PK |
| 관계자유형코드 | `party_type_code` | `VARCHAR(20)` | ✅ | | PERSONAL/ORGANIZATION |
| 관계자명 | `party_name` | `VARCHAR(100)` | ✅ | | |
| 관계자영문명 | `party_english_name` | `VARCHAR(200)` | | | |
| 관계자상태코드 | `party_status_code` | `VARCHAR(20)` | ✅ | | ACTIVE/SUSPENDED/CLOSED |
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| 최초등록자ID | `created_by` | `BIGINT` | | | |
| 최종수정일시 | `updated_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| 최종수정자ID | `updated_by` | `BIGINT` | | | |
| 삭제일시 | `deleted_at` | `TIMESTAMPTZ(3)` | | | soft delete |
| 삭제자ID | `deleted_by` | `BIGINT` | | | |

---

### 3.3 customer (고객)

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 고객ID | `customer_id` | `BIGINT` | ✅ | | PK |
| 관계자ID | `party_id` | `BIGINT` | ✅ | | FK → party |
| 고객등급코드 | `customer_grade_code` | `VARCHAR(10)` | | | NORMAL/VIP/PB |
| 고객상태코드 | `customer_status_code` | `VARCHAR(20)` | ✅ | | ACTIVE/DORMANT/CLOSED |
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
| 해지일시 | `closed_at` | `TIMESTAMPTZ(3)` | | | CLOSED 시 NOT NULL |
| 해지사유코드 | `close_reason_code` | `VARCHAR(20)` | | | CLOSED 시 NOT NULL |
| 개인정보보유기간만료일자 | `privacy_expiry_date` | `CHAR(8)` | | | closed_at + 5년 |
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| 최초등록자ID | `created_by` | `BIGINT` | | | |
| 최종수정일시 | `updated_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| 최종수정자ID | `updated_by` | `BIGINT` | | | |
| 삭제일시 | `deleted_at` | `TIMESTAMPTZ(3)` | | | soft delete |
| 삭제자ID | `deleted_by` | `BIGINT` | | | |

**CHECK 제약**
```sql
CONSTRAINT chk_customer_lifecycle CHECK (
    (customer_status_code = 'CLOSED'  AND closed_at IS NOT NULL AND close_reason_code IS NOT NULL) OR
    (customer_status_code = 'DORMANT' AND dormant_at IS NOT NULL) OR
    (customer_status_code = 'ACTIVE')
)
```

---

### 3.4 party_person (개인관계자)

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 관계자ID | `party_id` | `BIGINT` | ✅ | | PK + FK → party |
| 주민등록번호 | `rrn_encrypted` | `VARCHAR(255)` | | | AES-256 암호화 |
| CI값 | `ci_value` | `VARCHAR(88)` | | | 본인확인기관 연계정보 |
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
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| 최초등록자ID | `created_by` | `BIGINT` | | | |
| 최종수정일시 | `updated_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| 최종수정자ID | `updated_by` | `BIGINT` | | | |
| 삭제일시 | `deleted_at` | `TIMESTAMPTZ(3)` | | | soft delete |
| 삭제자ID | `deleted_by` | `BIGINT` | | | |

**CHECK 제약**
```sql
CONSTRAINT chk_party_person_pep CHECK (
    (is_pep_yn = 'T' AND pep_type_code IS NOT NULL) OR
    (is_pep_yn = 'F' AND pep_type_code IS NULL AND pep_country_code IS NULL)
)
```

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
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| 최초등록자ID | `created_by` | `BIGINT` | | | |
| 최종수정일시 | `updated_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| 최종수정자ID | `updated_by` | `BIGINT` | | | |
| 삭제일시 | `deleted_at` | `TIMESTAMPTZ(3)` | | | soft delete |
| 삭제자ID | `deleted_by` | `BIGINT` | | | |

**CHECK 제약**
```sql
CONSTRAINT chk_party_org_subtype CHECK (
    (org_subtype_code = 'CORPORATION'     AND corp_reg_no IS NOT NULL AND corp_type_code IS NOT NULL) OR
    (org_subtype_code = 'NON_CORPORATION' AND non_corp_type_code IS NOT NULL)
),
CONSTRAINT chk_party_org_foreign_corp CHECK (
    (hq_country_code = 'KOR' AND foreign_corp_reg_no_encrypted IS NULL) OR
    (hq_country_code != 'KOR' AND foreign_corp_reg_no_encrypted IS NOT NULL) OR
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
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| 최초등록자ID | `created_by` | `BIGINT` | | | |
| 최종수정일시 | `updated_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| 최종수정자ID | `updated_by` | `BIGINT` | | | |
| 삭제일시 | `deleted_at` | `TIMESTAMPTZ(3)` | | | soft delete |
| 삭제자ID | `deleted_by` | `BIGINT` | | | |

---

### 3.7 tax_residency_info (납세거주정보)

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 납세거주정보ID | `tax_residency_id` | `BIGINT` | ✅ | | PK |
| 관계자ID | `party_id` | `BIGINT` | ✅ | | FK → party |
| 거주자유형코드 | `resident_type_code` | `VARCHAR(20)` | ✅ | | RESIDENT/NON_RESIDENT |
| 납세거주국코드 | `tax_country_code` | `CHAR(3)` | | | ISO 3166 |
| 외국납세자식별번호 | `foreign_tin` | `VARCHAR(50)` | | | NON_RESIDENT 또는 tax_country≠KOR 시 NOT NULL |
| 원천징수율 | `withholding_rate_bps` | `INT` | | | bps 단위 (14%=1400) |
| 납세거주확인일자 | `tax_residency_confirm_date` | `CHAR(8)` | ✅ | | 납세거주상태확인일 |
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| 최초등록자ID | `created_by` | `BIGINT` | | | |
| 최종수정일시 | `updated_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| 최종수정자ID | `updated_by` | `BIGINT` | | | |
| 삭제일시 | `deleted_at` | `TIMESTAMPTZ(3)` | | | soft delete |
| 삭제자ID | `deleted_by` | `BIGINT` | | | |

---

### 3.8 compliance_info (컴플라이언스정보)

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 관계자ID | `party_id` | `BIGINT` | ✅ | | PK + FK → party |
| AML위험등급코드 | `aml_risk_level_code` | `VARCHAR(20)` | ✅ | | LOW/MED/HIGH |
| AML최종평가일시 | `aml_last_assessed_at` | `TIMESTAMPTZ(3)` | | | |
| AML차기평가예정일 | `aml_next_review_date` | `CHAR(8)` | | | |
| 제재대상여부 | `is_sanctioned_yn` | `CHAR(1)` | ✅ | | OFAC·UN·EU·KR 합산. GENERATED ALWAYS AS STORED (4개 개별 제재여부 OR 합산) |
| OFAC제재대상여부 | `is_ofac_sanctioned_yn` | `CHAR(1)` | ✅ | `'F'` | |
| UN제재대상여부 | `is_un_sanctioned_yn` | `CHAR(1)` | ✅ | `'F'` | |
| EU제재대상여부 | `is_eu_sanctioned_yn` | `CHAR(1)` | ✅ | `'F'` | |
| 한국제재대상여부 | `is_kr_sanctioned_yn` | `CHAR(1)` | ✅ | `'F'` | 외환거래법 등 |
| 제재최종스크리닝일시 | `sanction_last_screened_at` | `TIMESTAMPTZ(3)` | | | |
| 제재차기스크리닝예정일 | `sanction_next_screen_date` | `CHAR(8)` | | | |
| KYC상태코드 | `kyc_status_code` | `VARCHAR(20)` | ✅ | | PENDING/COMPLETED/EXPIRED/FAILED |
| KYC완료일시 | `kyc_completed_at` | `TIMESTAMPTZ(3)` | | | |
| KYC만료일 | `kyc_expiry_date` | `CHAR(8)` | | | 고위험 1년·중위험 3년·저위험 5년 |
| KYC차기재인증예정일 | `kyc_next_review_date` | `CHAR(8)` | | | |
| 본인확인수단코드 | `identity_verification_method_code` | `VARCHAR(10)` | | | NICE/KCB/BANK/CERT/ARS |
| CDD수준코드 | `cdd_level_code` | `VARCHAR(20)` | ✅ | | SIMPLE/STANDARD/ENHANCED |
| CDD최종검토일시 | `cdd_last_reviewed_at` | `TIMESTAMPTZ(3)` | | | |
| CDD차기검토예정일 | `cdd_next_review_date` | `CHAR(8)` | | | |
| EDD필요여부 | `edd_required_yn` | `CHAR(1)` | ✅ | `'F'` | |
| EDD최종검토일시 | `edd_last_reviewed_at` | `TIMESTAMPTZ(3)` | | | EDD 대상자만 NOT NULL |
| EDD차기검토예정일 | `edd_next_review_date` | `CHAR(8)` | | | |
| FATCA신고상태코드 | `fatca_status_code` | `VARCHAR(20)` | ✅ | | US_PERSON/NON_US/ACTIVE_NFFE 등 |
| FATCA최종검토일시 | `fatca_last_reviewed_at` | `TIMESTAMPTZ(3)` | | | |
| FATCA차기검토예정일 | `fatca_next_review_date` | `CHAR(8)` | | | |
| FATCA보고대상여부 | `fatca_reportable_yn` | `CHAR(1)` | ✅ | `'F'` | IRS 보고 대상 |
| CRS신고상태코드 | `crs_status_code` | `VARCHAR(20)` | ✅ | | REPORTABLE/NON_REPORTABLE 등 |
| CRS최종검토일시 | `crs_last_reviewed_at` | `TIMESTAMPTZ(3)` | | | |
| CRS차기검토예정일 | `crs_next_review_date` | `CHAR(8)` | | | |
| CRS보고대상여부 | `crs_reportable_yn` | `CHAR(1)` | ✅ | `'F'` | 자동정보교환 보고 대상 |
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| 최초등록자ID | `created_by` | `BIGINT` | | | |
| 최종수정일시 | `updated_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| 최종수정자ID | `updated_by` | `BIGINT` | | | |
| 삭제일시 | `deleted_at` | `TIMESTAMPTZ(3)` | | | soft delete |
| 삭제자ID | `deleted_by` | `BIGINT` | | | |

---

### 3.9 party_role (관계자역할)

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 역할ID | `role_id` | `BIGINT` | ✅ | | PK |
| 관계자ID | `party_id` | `BIGINT` | ✅ | | FK → party |
| 역할유형코드 | `role_type_code` | `VARCHAR(20)` | ✅ | | CUST/GRT/UBO/LGAR/EMP/BEN/FAM/PROSP/AGT |
| 역할상태코드 | `role_status_code` | `VARCHAR(20)` | ✅ | | ACTIVE/SUSPENDED/CLOSED |
| 역할시작일자 | `role_start_date` | `CHAR(8)` | ✅ | | |
| 역할종료일자 | `role_end_date` | `CHAR(8)` | | | CLOSED 시 NOT NULL |
| 역할종료사유코드 | `role_end_reason_code` | `VARCHAR(20)` | | | CLOSED 시 NOT NULL |
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| 최초등록자ID | `created_by` | `BIGINT` | | | |
| 최종수정일시 | `updated_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| 최종수정자ID | `updated_by` | `BIGINT` | | | |
| 삭제일시 | `deleted_at` | `TIMESTAMPTZ(3)` | | | soft delete |
| 삭제자ID | `deleted_by` | `BIGINT` | | | |

**CHECK 제약**
```sql
CONSTRAINT chk_party_role_end CHECK (
    (role_status_code = 'CLOSED' AND role_end_date IS NOT NULL AND role_end_reason_code IS NOT NULL) OR
    (role_status_code != 'CLOSED')
)
```

---

### 3.10 party_relation (관계자관계)

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 관계ID | `relation_id` | `BIGINT` | ✅ | | PK |
| 시작_관계자ID | `from_party_id` | `BIGINT` | ✅ | | FK → party. 능동적 측 |
| 대상_관계자ID | `to_party_id` | `BIGINT` | ✅ | | FK → party. 수동적 측 |
| 관계유형코드 | `relation_type_code` | `VARCHAR(10)` | ✅ | | REP/UBO/LGAR/FAM/AGT |
| 관계세부코드 | `relation_detail_code` | `VARCHAR(10)` | | | CEO/CHAIRMAN/SPOUSE/PARENT |
| 지분율 | `equity_ratio_bps` | `INT` | | | bps 단위. UBO 기준 25%=2500 |
| 대리권범위 | `representation_scope` | `VARCHAR(200)` | | | 자유 텍스트 |
| 근거서류URL | `proof_url` | `VARCHAR(500)` | | | 파일 스토리지 |
| 관계시작일자 | `relation_start_date` | `CHAR(8)` | ✅ | | |
| 관계종료일자 | `relation_end_date` | `CHAR(8)` | | | |
| 관계종료사유코드 | `relation_end_reason_code` | `VARCHAR(20)` | | | |
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| 최초등록자ID | `created_by` | `BIGINT` | | | |
| 최종수정일시 | `updated_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | ERD 한글명 오타("치종수정일시") |
| 최종수정자ID | `updated_by` | `BIGINT` | | | |
| 삭제일시 | `deleted_at` | `TIMESTAMPTZ(3)` | | | soft delete |
| 삭제자ID | `deleted_by` | `BIGINT` | | | |

**CHECK 제약**
```sql
CONSTRAINT chk_party_relation_no_self CHECK (from_party_id != to_party_id)
```

---

### 3.11 business_info (사업자정보)

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 사업자정보ID | `business_info_id` | `BIGINT` | ✅ | | PK |
| 관계자ID | `party_id` | `BIGINT` | ✅ | | FK → party |
| 사업자등록번호 | `biz_reg_no` | `CHAR(12)` | ✅ | | NNN-NN-NNNNN |
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
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| 최초등록자ID | `created_by` | `BIGINT` | | | |
| 최종수정일시 | `updated_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| 최종수정자ID | `updated_by` | `BIGINT` | | | |
| 삭제일시 | `deleted_at` | `TIMESTAMPTZ(3)` | | | soft delete |
| 삭제자ID | `deleted_by` | `BIGINT` | | | |

---

### 3.12 customer_status_history (고객상태이력)

> 로그 테이블 — soft delete 미적용, `created_at`/`created_by`만 보유

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 고객상태이력ID | `customer_status_history_id` | `BIGINT` | ✅ | | PK |
| 직전고객상태이력ID | `previous_customer_status_history_id` | `BIGINT` | | | self-ref FK. 최초 등록은 NULL |
| 고객ID | `customer_id` | `BIGINT` | ✅ | | FK → customer |
| 고객상태코드 | `customer_status_code` | `VARCHAR(20)` | ✅ | | 변경 후 상태 |
| 직전고객상태코드 | `previous_customer_status_code` | `VARCHAR(20)` | | | 변경 전 상태. 최초 등록은 NULL |
| 고객상태변경사유코드 | `customer_status_change_reason_code` | `VARCHAR(20)` | ✅ | | JOIN/INACTIVITY/CUST_REQ 등 |
| 고객상태변경상세사유 | `customer_status_change_reason_detail` | `VARCHAR(500)` | | | 자유 텍스트 |
| 고객상태발효일시 | `customer_status_effective_start_at` | `TIMESTAMPTZ(3)` | ✅ | | 이 상태가 발효된 시점 |
| 고객상태종료일시 | `customer_status_effective_end_at` | `TIMESTAMPTZ(3)` | | | 활성 이력은 NULL |
| 시스템자동전환여부 | `system_auto_triggered_yn` | `CHAR(1)` | ✅ | `'F'` | 휴면·사망 자동 종료 등 |
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| 최초등록자ID | `created_by` | `BIGINT` | | | |

---

### 3.13 customer_grade_history (고객등급이력)

> 로그 테이블 — soft delete 미적용, `created_at`/`created_by`만 보유

| 한글명 | 영문명 | 타입 | NOT NULL | 기본값 | 설명 |
|---|---|---|:---:|---|---|
| 고객등급이력ID | `customer_grade_history_id` | `BIGINT` | ✅ | | PK |
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
| 최초등록일시 | `created_at` | `TIMESTAMPTZ(3)` | ✅ | `CURRENT_TIMESTAMP(3)` | |
| 최초등록자ID | `created_by` | `BIGINT` | | | |

---

## 4. 인덱스

| 인덱스명 | 테이블 | 컬럼 | 종류 | 조건 | 목적 |
|---|---|---|---|---|---|
| `uq_party_relation_active` | `party_relation` | `(from_party_id, to_party_id, relation_type_code)` | UNIQUE | `relation_end_date IS NULL AND deleted_at IS NULL` | 동일 주체·대상·유형의 유효 관계 중복 방지 |
| `uq_customer_active_per_party` | `customer` | `(party_id)` | UNIQUE | `customer_status_code != 'CLOSED' AND deleted_at IS NULL` | party당 활성 고객 1건 제한 |
| `idx_party_relation_from` | `party_relation` | `(from_party_id, relation_type_code)` | INDEX | | 관계 시작 주체 기준 조회 |
| `idx_party_relation_to` | `party_relation` | `(to_party_id, relation_type_code)` | INDEX | | 관계 대상 기준 조회 |
| `idx_party_role_active` | `party_role` | `(party_id, role_status_code)` | INDEX | `role_status_code = 'ACTIVE' AND deleted_at IS NULL` | 활성 역할 조회 |
| `idx_business_info_party` | `business_info` | `(party_id)` | INDEX | `deleted_at IS NULL` | party 기준 사업자 조회 |
| `uq_business_info_biz_reg_no` | `business_info` | `(biz_reg_no)` | UNIQUE | | 사업자등록번호 유일성 |

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

- AES-256 암호화 컬럼은 `_encrypted` 접미사 사용
- 길이: `VARCHAR(255)` 통일

| 컬럼 | 테이블 | 비고 |
|---|---|---|
| `rrn_encrypted` | `party_person` | 주민등록번호 |
| `foreigner_no_encrypted` | `foreigner_info` | 외국인등록번호 |
| `foreign_corp_reg_no_encrypted` | `party_organization` | 외국법인등록번호 |

### 5.4 Soft Delete

- 엔티티 테이블에만 `deleted_at TIMESTAMPTZ(3)`, `deleted_by BIGINT` 적용
- 물리 삭제 대신 `deleted_at IS NOT NULL`로 삭제 판단
- 이력(로그) 테이블은 Soft Delete 미적용 — `created_at`/`created_by`만 보유

| 구분 | 해당 테이블 |
|---|---|
| Soft Delete 적용 | `cust_code_master`, `party`, `customer`, `party_person`, `party_organization`, `foreigner_info`, `tax_residency_info`, `compliance_info`, `party_role`, `party_relation`, `business_info` |
| Soft Delete 미적용 (로그) | `customer_status_history`, `customer_grade_history` |

### 5.5 코드 참조 방식

- `cust_code_master`에 대한 FK 제약은 **설정하지 않음** (soft reference)
- 코드 유효성은 애플리케이션 계층에서 검증

### 5.6 isAllowNull vs COMMENT 불일치 처리 원칙

ERDCloud JSON의 `isAllowNull` 필드와 `comment` 필드의 `[NOT NULL]` 표기가 불일치하는 경우, **comment의 `[NOT NULL]` 표기를 우선** 적용 (설계 의도로 판단).

예외: `customer_grade_history.customer_grade_evaluated_at` — 이미지에서 `[NOT NULL]` 확인 후 NOT NULL로 최종 확정.

### 5.7 국가코드 컬럼 타입

- ISO 3166-1 alpha-3는 항상 정확히 3자리 고정이므로 `CHAR(3)` 사용
- `VARCHAR(3)` 혼용 금지

| 컬럼 | 테이블 |
|---|---|
| `nationality_code` | `party_person` |
| `pep_country_code` | `party_person` |
| `hq_country_code` | `party_organization` |
| `passport_country_code` | `foreigner_info` |
| `tax_country_code` | `tax_residency_info` |

---

## 6. ERD 오류 기록

| 테이블 | 컬럼 | 오류 내용 | 처리 |
|---|---|---|---|
| `compliance_info` | `fatca_*` (4개 컬럼) | `FACTA` → **`FATCA`** 오타 (Foreign Account Tax Compliance Act) | 수정 완료 |
| `compliance_info` | `fatca_status_code` COMMENT | `FACTA고객상태코드` → **`FATCA신고상태코드`** | 수정 완료 |
| `compliance_info` | `is_eu_sanctioned_yn` COMMENT | `[NOT NULL ,,` 이중 쉼표 | 수정 완료 |
| `foreigner_info` | `foreigner_no` | 암호화 컬럼이나 `_encrypted` 접미사 및 `VARCHAR(100)` 미적용 | `foreigner_no_encrypted VARCHAR(255)` 로 수정 |
| `party_relation` | `updated_at` | ERD 한글명 **"치종수정일시"** 오타 (→ 최종수정일시) | COMMENT에 오타 명시, 영문명은 정상 |
| `party_role` | `role_type_code` | `isAllowNull: true`이나 COMMENT `[NOT NULL]` | NOT NULL 적용 |
| `party_organization` | `org_subtype_code` | `isAllowNull: true`이나 COMMENT `[NOT NULL]` | NOT NULL 적용 |
| `customer_status_history` | `system_auto_triggered_yn` | `isAllowNull: true`이나 COMMENT `[NOT NULL]` | NOT NULL 적용 |
| `customer_grade_history` | `customer_grade_evaluated_at` | JSON `isAllowNull: true`, COMMENT `[NOT NULL]` 불일치 → 이미지에서 `[NOT NULL]` 확인 | NOT NULL 확정 |
| `business_info` | `party_id` | ERD에 FK 표기 누락 | `FK → party.party_id` 추가 |
| `customer` | `party_id` | ERD COMMENT에 `[NOT NULL]` 미표기 | 관계 정의(1:N 비식별) 기준 NOT NULL + FK 추가 |
| `compliance_info` | `aml_risk_level_code`, `kyc_status_code`, `cdd_level_code`, `fatca_status_code`, `crs_status_code` (5개) | `isAllowNull: true`이나 COMMENT `[NOT NULL]` 표기 | NOT NULL 적용 (§5.6 규칙) |
| `compliance_info` | `is_sanctioned_yn` | ERD COMMENT에 GENERATED 컬럼 표기 (4개 개별 제재여부 OR 합산) | DEFAULT 제거, GENERATED 설명 추가. DDL 구현 시 `GENERATED ALWAYS AS ... STORED` 사용 |

---

## 7. 도메인 간 의존성

### 외부 참조 관계

- **고객계 → 없음** (최상위 도메인, 타 도메인에서 참조됨)
- **인증보안계 → 고객계** (`customer.customer_id`) — 인증보안계 12개 테이블이 `customer_id` 참조

### 향후 연결 예정

| 참조 컬럼 | 현재 | 연결 대상 (예정) |
|---|---|---|
| `customer.party_id` | FK → party (고객계 내부) | — |
| — | — | 여신계·수신계 → `customer.customer_id` |
| — | — | 거래계 → `customer.customer_id` |

---

## 부록 A: 코드 그룹 명세

> `cust_code_master`에 적재할 코드 그룹·코드값 목록. `(code_group_id, code_value)` 복합 PK.

| code_group_id | code_value | 한글명 |
|---|---|---|
| `PARTY_TYPE` | `PERSONAL` | 개인 |
| `PARTY_TYPE` | `ORGANIZATION` | 조직 |
| `PARTY_STATUS` | `ACTIVE` | 활성 |
| `PARTY_STATUS` | `SUSPENDED` | 정지 |
| `PARTY_STATUS` | `CLOSED` | 해지 |
| `ORG_SUBTYPE` | `CORPORATION` | 법인 |
| `ORG_SUBTYPE` | `NON_CORPORATION` | 비법인단체 |
| `CORP_TYPE` | `STOCK` | 주식회사 |
| `CORP_TYPE` | `LIMITED` | 유한회사 |
| `CORP_TYPE` | `LLC` | 유한책임회사 |
| `CORP_TYPE` | `GENERAL` | 합명회사 |
| `CORP_TYPE` | `LIMITED_PT` | 합자회사 |
| `CORP_TYPE` | `COOPERATIVE` | 협동조합 |
| `CORP_TYPE` | `NPO` | 비영리법인 |
| `NON_CORP_TYPE` | `RELIGION` | 종교단체 |
| `NON_CORP_TYPE` | `VOLUNTARY` | 자원봉사단체 |
| `NON_CORP_TYPE` | `AUTONOMY` | 자치단체 |
| `NON_CORP_TYPE` | `CLUB` | 동호회 |
| `OWNERSHIP_TYPE` | `COLLECTIVE` | 총유 |
| `OWNERSHIP_TYPE` | `JOINT` | 합유 |
| `OWNERSHIP_TYPE` | `COMMON` | 공유 |
| `OWNERSHIP_TYPE` | `INDIVIDUAL` | 단독 |
| `REP_TYPE` | `SINGLE` | 단독대표 |
| `REP_TYPE` | `JOINT` | 공동대표 |
| `REP_TYPE` | `COMMITTEE` | 위원회 |
| `CUSTOMER_GRADE` | `NORMAL` | 일반 |
| `CUSTOMER_GRADE` | `VIP` | VIP |
| `CUSTOMER_GRADE` | `PB` | PB |
| `CUSTOMER_STATUS` | `ACTIVE` | 활성 |
| `CUSTOMER_STATUS` | `DORMANT` | 휴면 |
| `CUSTOMER_STATUS` | `CLOSED` | 해지 |
| `CREDIT_RATING` | `AAA` | AAA |
| `CREDIT_RATING` | `AA` | AA |
| `CREDIT_RATING` | `A` | A |
| `CREDIT_RATING` | `BBB` | BBB |
| `CREDIT_RATING` | `BB` | BB |
| `CREDIT_RATING` | `B` | B |
| `CREDIT_RATING` | `CCC` | CCC |
| `CREDIT_RATING` | `CC` | CC |
| `CREDIT_RATING` | `C` | C |
| `CREDIT_RATING` | `D` | D |
| `CREDIT_AGENCY` | `KCB` | KCB |
| `CREDIT_AGENCY` | `NICE` | NICE평가정보 |
| `CREDIT_AGENCY` | `INTERNAL` | 내부평가 |
| `NOTIFICATION_METHOD` | `SMS` | SMS |
| `NOTIFICATION_METHOD` | `EMAIL` | 이메일 |
| `NOTIFICATION_METHOD` | `KAKAO` | 카카오알림 |
| `NOTIFICATION_METHOD` | `APP` | 앱푸시 |
| `JOIN_CHANNEL` | `BRANCH` | 영업점 |
| `JOIN_CHANNEL` | `ONLINE` | 온라인 |
| `JOIN_CHANNEL` | `MOBILE` | 모바일 |
| `JOIN_CHANNEL` | `CALL` | 콜센터 |
| `JOIN_CHANNEL` | `AGENT` | 대리인 |
| `END_REASON` | `CUST_REQ` | 고객요청 |
| `END_REASON` | `EXPIRY` | 만료 |
| `END_REASON` | `DEATH` | 사망 |
| `END_REASON` | `BIZ_CLOSE` | 폐업 |
| `END_REASON` | `NATURAL_END` | 자연종료 |
| `END_REASON` | `TRANSFER` | 이관 |
| `END_REASON` | `REVOKED` | 취소 |
| `END_REASON` | `OTHER` | 기타 |
| `ROLE_TYPE` | `CUST` | 고객 |
| `ROLE_TYPE` | `GRT` | 보증인 |
| `ROLE_TYPE` | `UBO` | 실소유자 |
| `ROLE_TYPE` | `LGAR` | 법정대리인 |
| `ROLE_TYPE` | `EMP` | 임직원 |
| `ROLE_TYPE` | `BEN` | 수익자 |
| `ROLE_TYPE` | `FAM` | 가족 |
| `ROLE_TYPE` | `PROSP` | 잠재고객 |
| `ROLE_TYPE` | `AGT` | 대리인 |
| `ROLE_STATUS` | `ACTIVE` | 활성 |
| `ROLE_STATUS` | `SUSPENDED` | 정지 |
| `ROLE_STATUS` | `CLOSED` | 종료 |
| `RELATION_TYPE` | `REP` | 대표자 |
| `RELATION_TYPE` | `UBO` | 실소유자 |
| `RELATION_TYPE` | `LGAR` | 법정대리인 |
| `RELATION_TYPE` | `FAM` | 가족 |
| `RELATION_TYPE` | `AGT` | 대리인 |
| `RELATION_DETAIL` | `CEO` | 대표이사 |
| `RELATION_DETAIL` | `CHAIRMAN` | 회장 |
| `RELATION_DETAIL` | `SPOUSE` | 배우자 |
| `RELATION_DETAIL` | `PARENT` | 부모 |
| `AML_RISK` | `LOW` | 저위험 |
| `AML_RISK` | `MED` | 중위험 |
| `AML_RISK` | `HIGH` | 고위험 |
| `KYC_STATUS` | `PENDING` | 대기 |
| `KYC_STATUS` | `COMPLETED` | 완료 |
| `KYC_STATUS` | `EXPIRED` | 만료 |
| `KYC_STATUS` | `FAILED` | 실패 |
| `ID_VERIFICATION_METHOD` | `NICE` | NICE |
| `ID_VERIFICATION_METHOD` | `KCB` | KCB |
| `ID_VERIFICATION_METHOD` | `BANK` | 계좌인증 |
| `ID_VERIFICATION_METHOD` | `CERT` | 인증서 |
| `ID_VERIFICATION_METHOD` | `ARS` | ARS |
| `CDD_LEVEL` | `SIMPLE` | 간소화 |
| `CDD_LEVEL` | `STANDARD` | 일반 |
| `CDD_LEVEL` | `ENHANCED` | 강화 |
| `FATCA_STATUS` | `US_PERSON` | 미국인 |
| `FATCA_STATUS` | `NON_US` | 비미국인 |
| `FATCA_STATUS` | `ACTIVE_NFFE` | 능동적 비금융외국법인 |
| `FATCA_STATUS` | `PASSIVE_NFFE` | 수동적 비금융외국법인 |
| `FATCA_STATUS` | `EXEMPT` | 면제 |
| `FATCA_STATUS` | `RECALCITRANT` | 비협조자 |
| `CRS_STATUS` | `REPORTABLE` | 보고대상 |
| `CRS_STATUS` | `NON_REPORTABLE` | 비보고대상 |
| `CRS_STATUS` | `ACTIVE_NFE` | 능동적 비금융단체 |
| `CRS_STATUS` | `PASSIVE_NFE` | 수동적 비금융단체 |
| `CRS_STATUS` | `EXEMPT` | 면제 |
| `BIZ_STATUS` | `CONTINUE` | 계속 |
| `BIZ_STATUS` | `SUSPEND` | 휴업 |
| `BIZ_STATUS` | `CLOSE` | 폐업 |
| `TAX_TYPE` | `GENERAL` | 일반과세 |
| `TAX_TYPE` | `SIMPLIFIED` | 간이과세 |
| `TAX_TYPE` | `EXEMPT` | 면세 |
| `NATIONALITY_TYPE` | `DOMESTIC` | 내국인 |
| `NATIONALITY_TYPE` | `FOREIGN` | 외국인 |
| `GENDER` | `M` | 남성 |
| `GENDER` | `F` | 여성 |
| `GENDER` | `U` | 미확인 |
| `MARITAL_STATUS` | `SINGLE` | 미혼 |
| `MARITAL_STATUS` | `MARRIED` | 기혼 |
| `MARITAL_STATUS` | `DIVORCED` | 이혼 |
| `MARITAL_STATUS` | `WIDOWED` | 사별 |
| `INCOME_PROOF` | `SALARY` | 근로소득 |
| `INCOME_PROOF` | `BUSINESS` | 사업소득 |
| `INCOME_PROOF` | `RENTAL` | 임대소득 |
| `INCOME_PROOF` | `PENSION` | 연금소득 |
| `INCOME_PROOF` | `OTHER` | 기타 |
| `CAPACITY_LIMIT` | `NORMAL` | 완전행위능력자 |
| `CAPACITY_LIMIT` | `MINOR` | 미성년자 |
| `CAPACITY_LIMIT` | `LIMITED_GUARDIAN` | 한정후견인 |
| `CAPACITY_LIMIT` | `ADULT_GUARDIAN` | 성년후견인 |
| `PEP_TYPE` | `SELF` | 본인 |
| `PEP_TYPE` | `FAMILY` | 가족 |
| `PEP_TYPE` | `CLOSE_ASSOC` | 측근 |
| `STAY_QUALIFICATION` | `F2` | 거주(F-2) |
| `STAY_QUALIFICATION` | `F4` | 재외동포(F-4) |
| `STAY_QUALIFICATION` | `F5` | 영주(F-5) |
| `STAY_QUALIFICATION` | `E7` | 특정활동(E-7) |
| `STAY_QUALIFICATION` | `H2` | 방문취업(H-2) |
| `RESIDENT_TYPE` | `RESIDENT` | 거주자 |
| `RESIDENT_TYPE` | `NON_RESIDENT` | 비거주자 |
| `STATUS_CHANGE_REASON` | `JOIN` | 신규가입 |
| `STATUS_CHANGE_REASON` | `INACTIVITY` | 비활동 |
| `STATUS_CHANGE_REASON` | `CUST_REQ` | 고객요청 |
| `STATUS_CHANGE_REASON` | `REACTIVATE` | 재활성화 |
| `STATUS_CHANGE_REASON` | `DEATH` | 사망 |
| `STATUS_CHANGE_REASON` | `BIZ_CLOSE` | 폐업 |
| `STATUS_CHANGE_REASON` | `REGULATORY` | 규제조치 |
| `STATUS_CHANGE_REASON` | `OTHER` | 기타 |
| `GRADE_CHANGE_REASON` | `INITIAL` | 최초등록 |
| `GRADE_CHANGE_REASON` | `PROMOTION` | 승급 |
| `GRADE_CHANGE_REASON` | `DEMOTION` | 강등 |
| `GRADE_CHANGE_REASON` | `MANUAL` | 수동조정 |
| `GRADE_CHANGE_REASON` | `PERIODIC` | 정기평가 |
| `NTS_INDUSTRY` | *(6자리)* | 국세청 업종코드 — 별도 마스터 데이터 로드 |
| `KSIC` | *(5자리)* | 한국표준산업분류 — 별도 마스터 데이터 로드 |

---

## 부록 B: DDL 적용 순서

> FK 의존성 기준. 상위 테이블이 먼저 생성되어야 함.

| 순서 | 테이블 | 의존 대상 |
|:---:|---|---|
| 1 | `cust_code_master` | 없음 |
| 2 | `party` | 없음 |
| 3 | `party_person` | `party` |
| 4 | `party_organization` | `party` |
| 5 | `foreigner_info` | `party_person` |
| 6 | `compliance_info` | `party` |
| 7 | `tax_residency_info` | `party` |
| 8 | `party_role` | `party` |
| 9 | `party_relation` | `party` |
| 10 | `business_info` | `party` |
| 11 | `customer` | `party` |
| 12 | `customer_status_history` | `customer` (+ self-ref) |
| 13 | `customer_grade_history` | `customer` (+ self-ref) |
