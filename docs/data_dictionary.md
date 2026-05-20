# 📒 데이터 표준 단어 사전 (Data Dictionary)

여신계(LON) 도메인의 표준 단어 정의. 모든 논리명은 본 사전의 조합으로 생성합니다.

> **표기 규칙**: `[수식어] + [핵심어] + [도메인]`
> 예: `상품 + 판매 + 시작 + 일자` → 「상품판매시작일자」

---

## 1. 도메인 접두어 (테이블별 한정어)

| 테이블 | 한국어 접두어 | 영문 접두어 |
|---|---|---|
| CODE_MASTER | 코드 | code |
| STATUS_HISTORY | 상태이력 | sthist |
| LOAN_PRODUCT | 상품 | prod |
| BUSINESS_CALENDAR | 영업일 | cal |
| LOAN_APPLICATION | 대출신청 | appl |
| LOAN_PRESCREENING | 가심사 | presc |
| CREDIT_CONSENT | 신용정보동의 | csnt |
| LOAN_IDENTITY_VERIFICATION | 본인확인 | idv |
| LOAN_DOCUMENT | 서류 | doc |
| LOAN_DOCUMENT_OCR | OCR | ocr |
| GUARANTOR_MASTER | 보증인 | gmst |
| GUARANTOR_AGREEMENT | 보증계약 | gagr |
| CREDIT_EVALUATION | 신용평가 | ceval |
| DSR_CALCULATION | DSR | dsr |
| LOAN_REVIEW | 심사 | rev |
| REVIEW_CHECK_LOG | 심사점검 | rchk |
| LTV_CALCULATION | LTV | ltv |
| COLLATERAL | 담보 | col |
| COLLATERAL_EVALUATION | 담보평가 | ceval |
| LOAN_CONTRACT | 계약 | cntr |
| REPAYMENT_ACCOUNT | 상환계좌 | racct |
| LOAN_EXECUTION | 대출실행 | exec |
| GUARANTEE_INSURANCE | 보증보험 | gins |
| REPAYMENT_SCHEDULE | 상환스케줄 | rsch |
| INTEREST_ACCRUAL | 이자발생 | iacc |
| REPAYMENT_TRANSACTION | 상환거래 | rtx |
| RATE_CHANGE_HISTORY | 금리변경 | rchg |
| MATURITY | 만기 | mat |
| DELINQUENCY | 연체 | dlq |
| DELINQUENCY_DAILY_SNAPSHOT | 연체일별 | dlqs |
| CREDIT_INFO_REPORT | 신용정보신고 | crpt |
| LOAN_CLOSURE | 약정종료 | clos |
| LOAN_CERTIFICATE | 증명서 | cert |

---

## 2. 핵심어 (Core Term)

| 한국어 | 영문 | 비고 |
|---|---|---|
| 식별번호 / ID | id | PK 또는 FK |
| 코드 | cd / code | |
| 명칭 / 이름 | name / nm | |
| 종류 / 유형 | type | |
| 상태 | status | 상태는 모두 공통 상태 코드 테이블에 저장한다.|
| 사유 | reason | |
| 설명 | desc / description | |
| 비고 | remark | |
| 결과 | result | |
| 결정 | decision | |
| 등급 | grade | |
| 점수 | score | |
| 비율 | ratio | bps 단위 |
| 한도 | limit | |
| 금액 | amount / amt | BIGINT 원 단위 |
| 잔액 | balance / bal | BIGINT |
| 원금 | principal | |
| 이자 | interest | |
| 합계 | total | |
| 수수료 | fee | |
| 금리 | rate | bps 단위 INT |
| 기간 | period | 개월 단위 INT |
| 회차 | installment | INT |
| 횟수 | count | |
| 버전 | version | |
| 채널 | channel | |
| 출처 | source | |
| 대상 | target | |
| 권한 | scope | |
| 경로 | path | |
| 유형 | type | |

## 3. 시간/일자 단어

| 한국어 | 영문 | 타입 | 설명 |
|---|---|---|---|
| 월 | mo | VARCHAR(6)) | 월(month), YYYYMM |
| 일자 | date | VARCHAR(8) | 날(day), YYYYMMDD |
| 일시 | _at | TIMESTAMPTZ(3) | 시점(moment) |

## 3-2. 보안·식별·디바이스 단어

| 한국어 | 영문 | 타입 | 용도 |
|---|---|---|---|
| 노출번호 / 외부번호 | no | VARCHAR | 고객 노출용 ID (PK 시퀀스 보호), UNIQUE |
| IP | client_ip | VARCHAR(45) | IPv4/IPv6 (FDS·중복방지) |
| 멱등성키 | idempotency_key | VARCHAR(100) | 이중 처리 방지 |
| 디바이스 | device | VARCHAR | 동의 시점 디바이스 |
| 콘텐츠해시 | content_hash | VARCHAR(64) | SHA256 |
| 멱등성토큰 | consent_token | VARCHAR | 동의 토큰 |

## 4. 수식어 (Modifier)

| 한국어 | 영문 |
|---|---|
| 최초 | first / initial |
| 최종 | last / final |
| 신규 | new |
| 기존 | existing |
| 현재 | current |
| 이전 | previous / old |
| 예상 | estimated |
| 적용 | applied |
| 승인 | approved |
| 거절 | rejected |
| 추정 | estimated |
| 희망 | requested |
| 실제 | actual |
| 활성 | active |
| 영업 | business |
| 약정 | contracted |
| 실행 | executed |

## 5. 공통 시스템 컬럼 (도메인 prefix 미사용)

전사 표준이므로 prefix 없이 사용:

| 물리명 | 논리명 | 타입 |
|---|---|---|
| created_at | 최초등록일시 | TIMESTAMPTZ(3) |
| created_by | 최초등록자ID | BIGINT |
| updated_at | 최종수정일시 | TIMESTAMPTZ(3) |
| updated_by | 최종수정자ID | BIGINT |
| deleted_at | 삭제일시 | TIMESTAMPTZ(3) |
| deleted_by | 삭제자ID | BIGINT |
| version | 버전 | INT |

---

## 6. 사용 예시

```
LOAN_PRODUCT 테이블의 sale_start_date
→ [상품(테이블)] + [판매] + [시작] + [일자]
→ 논리명: 상품판매시작일자

LOAN_CONTRACT 테이블의 total_rate_bps
→ [약정] + [적용] + [금리]
→ 논리명: 약정적용금리
```

---

## 7. 운영 규칙

1. **새 컬럼 추가 시** 반드시 본 사전에 등록 후 사용
2. **새 단어가 필요한 경우** PR로 사전 갱신 → 리뷰 후 머지
3. **시스템 컬럼은 prefix 없음** (모든 테이블 공통)
4. **FK 컬럼**은 참조 테이블의 PK 명칭 유지 (예: `customer_id`)
5. **bps 단위** 금리 컬럼은 논리명 끝에 「(bps)」 표기 권장

---

## 8. Soft Delete 정책

본 프로젝트는 **물리 삭제(DELETE) 금지, Soft Delete만 허용**합니다.

### 삭제 컬럼 (모든 테이블 공통)
| 컬럼 | 타입 | 의미 |
|---|---|---|
| `deleted_at` | TIMESTAMPTZ | NULL = 활성, 값 있음 = 삭제됨 |
| `deleted_by` | BIGINT | 삭제 수행자 ID |

### 판단 기준
- **활성 여부**: `deleted_at IS NULL` 로 관리한다.
- 별도 `deleted_yn` Y/N 플래그 컬럼은 사용하지 않는다.
- 삭제 시점·삭제자 정보가 함께 보존되므로 감사 추적성을 만족한다.

### 표준 쿼리 패턴
```sql
-- 활성 행만 조회
SELECT * FROM loan_contract WHERE deleted_at IS NULL;

-- Soft Delete 수행 (UPDATE)
UPDATE loan_contract
SET deleted_at = now(),
    deleted_by = :user_id
WHERE contract_id = :id
  AND deleted_at IS NULL;

-- 삭제 이력 포함 전체 조회 (감사용)
SELECT * FROM loan_contract;
```

### 인덱스 권장
자주 조회되는 컬럼은 `deleted_at IS NULL` 부분 인덱스로 효율 ↑.
```sql
CREATE INDEX idx_contract_active_customer
  ON loan_contract (customer_id)
  WHERE deleted_at IS NULL;
```

### JPA(Hibernate) 적용 예시
```java
@Entity
@SQLDelete(sql = "UPDATE loan_contract SET deleted_at = now(), deleted_by = ? WHERE contract_id = ?")
@Where(clause = "deleted_at IS NULL")
public class LoanContract { ... }
```

### 예외
- **감사 로그/이력 테이블**(`STATUS_HISTORY`, `DELINQUENCY_DAILY_SNAPSHOT`, `RATE_CHANGE_HISTORY` 등)은 본래 이력 보존이 목적이므로 Soft Delete 대상이 아님 — 행을 절대 변경/삭제하지 않는다(append-only).
- **외부 규제로 강제 파기 요건이 있는 데이터**(보존기간 경과 등)는 별도 파기 배치로 물리 삭제 가능. 단, **파기 사실은 별도 감사 로그에 기록.**
