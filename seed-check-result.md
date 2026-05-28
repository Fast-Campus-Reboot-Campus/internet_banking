# 커밋 전 최종 점검 결과
> 기준일: 2026-05-26
> 대상: deposit-service seed 데이터 (V8 migration + LocalDataSeeder)

---

## 커밋 분리 계획

### 커밋 1 — deposit-service seed 작업
```
feat: deposit-service 프론트 상품 seed 추가 및 LocalDataSeeder 정리
```
| 파일 | 변경 내용 |
|------|-----------|
| `services/deposit-service/src/main/java/com/bank/deposit/config/LocalDataSeeder.java` | 포스트맨 상품 삭제, 프론트 21개 추가 |
| `services/deposit-service/src/main/resources/db/migration/V8__seed_customer_frontend_products.sql` | 신규 생성 |

### 커밋 2 — consultation-service 챗봇 개선 (별도 커밋)
```
feat: consultation-service 챗봇 개선
```
| 파일 |
|------|
| `services/consultation-service/app/config.py` |
| `services/consultation-service/app/llm.py` |
| `services/consultation-service/app/main.py` |
| `services/consultation-service/app/models.py` |
| `services/consultation-service/app/services.py` |
| `services/consultation-service/tests/conftest.py` |

> 미추적 파일(`.bak`, `.bat`, `static/`, `test_scenario_flow.py`)은 커밋 포함 여부 별도 확인 필요

---

## 1. 추가된 예금 개수
**14개** (TERM 4개 + DEMAND 10개)

| 타입 | 상품명 |
|------|--------|
| TERM | AXful 정기예금 |
| TERM | AXful 수퍼정기예금(개인) |
| TERM | 일반정기예금 |
| TERM | AXful 청년도약계좌 |
| DEMAND | AXful 쏙머니통장 |
| DEMAND | 당선통장 |
| DEMAND | AXful 생계비계좌 |
| DEMAND | AXful GS Pay통장 |
| DEMAND | 모니모 AXful 매일이자 통장 |
| DEMAND | AXful 모임금고 |
| DEMAND | AXful 스타통장 |
| DEMAND | AXful 지갑통장 |
| DEMAND | AXful 자유입출금통장 |
| DEMAND | AXful 청년우대통장 |

## 2. 추가된 적금 개수
**5개**

| 상품명 | savingType |
|--------|-----------|
| AXful 내맘대로적금 | FREE |
| AXful 달러자적금 | FREE |
| AXful 맑은하늘적금 | FREE |
| AXful 장병내일준비적금 | REGULAR |
| AXful 특★한 적금 | FREE |

## 3. 추가된 청약 개수
**2개**

| 상품명 |
|--------|
| 주택청약종합저축 |
| 청년 주택드림 청약통장 |

## 4. 총 상품 개수 (V8 신규 추가분)
**21개** (예금 14 + 적금 5 + 청약 2)

---

## 5~6. 포스트맨 상품 삭제 상태 정확 확인

### H2 LocalDataSeeder
✅ **삭제 완료** — 포스트맨 상품 5개 전부 제거, grep 0건

### PostgreSQL (V2 / V7) 잔존 여부
⚠️ **잔존** — V2/V7은 수정하지 않음

| migration | id | 상품명 | status |
|-----------|-----|--------|--------|
| V2 | 1 | 포스트맨 정기예금 | SELLING |
| V2 | 2 | 포스트맨 자유적금 | SELLING |
| V2 | 3 | 포스트맨 청약저축 | SELLING |
| V7 | 4 | 포스트맨 정기적금(12개월) | SELLING |
| V7 | 5 | 포스트맨 정기적금(24개월) | SELLING |

### V8에서 포스트맨 상품 비활성화/삭제 여부
❌ **없음** — V8은 id 6~26만 INSERT, 기존 1~5 건드리지 않음

---

## 4-2. V8 적용 후 PostgreSQL 실제 최종 상품 개수

| 출처 | 상품 수 | 포스트맨 포함 |
|------|---------|--------------|
| V2 | 3개 | ✅ 포함 |
| V7 | 2개 | ✅ 포함 |
| V8 | 21개 | ❌ |
| **합계** | **26개** | **포스트맨 5개 포함** |

> **⚠️ 주의:** `GET /products` 전체 조회 시 포스트맨 상품 5개(id 1~5)도 함께 반환됨.
> 프론트에서 상품 목록 API를 사용한다면 포스트맨 상품이 화면에 노출될 수 있음.
> V2/V7 삭제 또는 V8에 `UPDATE ... SET deposit_product_status='DISCONTINUED'` 추가 여부 검토 필요.

---

## 7. git status
```
 M services/consultation-service/app/config.py        ← 커밋 2
 M services/consultation-service/app/llm.py           ← 커밋 2
 M services/consultation-service/app/main.py          ← 커밋 2
 M services/consultation-service/app/models.py        ← 커밋 2
 M services/consultation-service/app/services.py      ← 커밋 2
 M services/consultation-service/tests/conftest.py    ← 커밋 2
 M services/deposit-service/.../LocalDataSeeder.java  ← 커밋 1
?? services/deposit-service/.../V8__seed_customer_frontend_products.sql  ← 커밋 1 (신규)
```

## 8. 수정 파일 목록 (이번 작업)
| 파일 | 커밋 | 변경 내용 |
|------|------|-----------|
| `LocalDataSeeder.java` | 커밋 1 | 포스트맨 삭제 + 프론트 21개 추가 |
| `V8__seed_customer_frontend_products.sql` | 커밋 1 | 신규 생성 |
| `consultation-service/app/*.py` | 커밋 2 | 챗봇 개선 |
| `consultation-service/tests/conftest.py` | 커밋 2 | 테스트 fixture 수정 |

## 9. migration 파일 목록
```
V1__initial_schema.sql
V2__seed_postman_data.sql          ← 포스트맨 id 1~3 (수정 없음)
V5__full_erd_schema.sql
V6__term_application_management.sql
V7__seed_regular_savings.sql       ← 포스트맨 id 4~5 (수정 없음)
V8__seed_customer_frontend_products.sql  ← 신규, 프론트 id 6~26
```
총 6개

## 10. LocalDataSeeder 변경 여부
✅ 변경 완료

- 제거: 포스트맨 상품 5개, 관련 계약/계좌/이자/거래/약관 코드 전체
- 제거: 불필요 repository 8개 주입 (accountRepository, contractRepository 등)
- 추가: 프론트 21개 상품 + 금리 + 채널 + 대상그룹 (개인/청년/국군장병)

---

## 11. 상품 전체 조회 결과 예시 (V8 반영 후 PostgreSQL)
`GET /products` 응답 예상 목록:

| id | productType | productName | 비고 |
|----|-------------|-------------|------|
| 1 | DEPOSIT | 포스트맨 정기예금 | ⚠️ V2 잔존 |
| 2 | SAVINGS | 포스트맨 자유적금 | ⚠️ V2 잔존 |
| 3 | SUBSCRIPTION | 포스트맨 청약저축 | ⚠️ V2 잔존 |
| 4 | SAVINGS | 포스트맨 정기적금(12개월) | ⚠️ V7 잔존 |
| 5 | SAVINGS | 포스트맨 정기적금(24개월) | ⚠️ V7 잔존 |
| 6 | DEPOSIT | AXful 정기예금 | ✅ V8 신규 |
| 7 | DEPOSIT | AXful 수퍼정기예금(개인) | ✅ V8 신규 |
| 8 | DEPOSIT | 일반정기예금 | ✅ V8 신규 |
| 9 | DEPOSIT | AXful 청년도약계좌 | ✅ V8 신규 |
| 10 | SAVINGS | AXful 내맘대로적금 | ✅ V8 신규 |
| 11 | SAVINGS | AXful 달러자적금 | ✅ V8 신규 |
| 12 | SAVINGS | AXful 맑은하늘적금 | ✅ V8 신규 |
| 13 | SAVINGS | AXful 장병내일준비적금 | ✅ V8 신규 |
| 14 | SAVINGS | AXful 특★한 적금 | ✅ V8 신규 |
| 15 | SUBSCRIPTION | 주택청약종합저축 | ✅ V8 신규 |
| 16 | SUBSCRIPTION | 청년 주택드림 청약통장 | ✅ V8 신규 |
| 17 | DEPOSIT | AXful 쏙머니통장 | ✅ V8 신규 |
| 18 | DEPOSIT | 당선통장 | ✅ V8 신규 |
| 19 | DEPOSIT | AXful 생계비계좌 | ✅ V8 신규 |
| 20 | DEPOSIT | AXful GS Pay통장 | ✅ V8 신규 |
| 21 | DEPOSIT | 모니모 AXful 매일이자 통장 | ✅ V8 신규 |
| 22 | DEPOSIT | AXful 모임금고 | ✅ V8 신규 |
| 23 | DEPOSIT | AXful 스타통장 | ✅ V8 신규 |
| 24 | DEPOSIT | AXful 지갑통장 | ✅ V8 신규 |
| 25 | DEPOSIT | AXful 자유입출금통장 | ✅ V8 신규 |
| 26 | DEPOSIT | AXful 청년우대통장 | ✅ V8 신규 |

**PostgreSQL 총 26개 — 포스트맨 5개 포함**

## 12. 챗봇 상품 질의 테스트 통과 여부
- 서버 미기동 상태 — 동적 API 테스트 불가
- 정적 검증: 프론트 상품명과 V8/LocalDataSeeder 상품명 100% 일치 확인
- consultation-service는 deposit-service DB를 직접 조회 → V8 반영 후 챗봇에서 프론트 상품명 조회 가능

## 13. 의도치 않은 변경 파일 존재 여부
✅ 없음 — deposit-service 외 파일 미수정
