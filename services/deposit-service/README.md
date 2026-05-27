# Deposit Service — 수신계

인터넷뱅킹 플랫폼의 **수신(예·적금·청약) 도메인 백엔드 서비스**입니다.  
상품 등록·조회, 계약 체결, 계좌 관리, 거래 기록, 이자 계산, 수신 특약, 고객 맞춤 상품 추천까지 수신 업무 전 영역을 담당합니다.

---

## 기술 스택

| 항목 | 내용 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Build | Gradle (멀티 모듈) |
| DB (운영) | PostgreSQL 16 |
| DB (로컬) | H2 In-Memory |
| 마이그레이션 | Flyway |
| ORM | Spring Data JPA / Hibernate |
| 문서 | Springdoc OpenAPI (Swagger UI) |

---

## 주요 기능

### 1. 수신 상품 관리
- 정기예금(DEPOSIT) · 입출금자유(DEMAND) · 적금(SAVINGS) · 청약(SUBSCRIPTION) 4종 상품 CRUD
- 상품 상태 관리 (`SELLING` / `SUSPENDED` / `CLOSED`)
- 가입 채널 · 대상 그룹 · 금리 조건 연결
- 고객 대면 시드 데이터 21개 상품 기 등록
  - 정기예금 4종, 입출금자유통장 10종, 적금 5종, 청약 2종

### 2. 계약 관리
- 상품별 계약 체결 / 조회 / 해지
- 계약 상태 이력 자동 기록 (`StatusHistory`)
- 계약 가입금액 범위 검증, 기간(periodMonth) 최솟값 제약

### 3. 계좌 관리
- 계약과 1:1 연결된 수신 계좌 생성 및 상태 관리
- 계좌 상태: `ACTIVE` / `FROZEN` / `CLOSED`

### 4. 거래 기록
- 입금(IN) / 출금(OUT) 거래 기록
- 거래 상태(`SUCCESS` / `FAILED`) 및 실패 사유 코드 관리

### 5. 이자 계산 및 지급 이력
- 기본금리 · 우대금리 관리 (`BASE` / `PREFERENTIAL`)
- 이자 지급 이력 기록

### 6. 수신 특약 관리
- 특약 등록 및 상품·계약별 특약 동의 연결
- 공통 약관 참조 연동

### 7. 현금흐름 기반 상품 추천 에이전트
- 고객 거래 이력을 분석해 최적 수신 상품 자동 추천
- `GET /products/recommend-agent?customerId={id}&periodMonth={n}` 으로 호출
- 분석 기간(periodMonth) 동안의 순입금액 기준 → 금리 최고 상품 매칭

---

## API 엔드포인트 요약

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/products` | 상품 목록 조회 (type·status 필터) |
| `POST` | `/products` | 상품 등록 |
| `GET` | `/products/{id}` | 상품 단건 조회 |
| `PUT` | `/products/{id}` | 상품 정보 수정 |
| `PATCH` | `/products/{id}` | 상품 상태 변경 |
| `GET` | `/products/{id}/deposit` | 예금 상세 조회 |
| `POST` | `/products/{id}/deposit` | 예금 상세 등록 |
| `GET` | `/products/recommend-agent` | 고객 맞춤 상품 추천 |
| `POST` | `/contracts` | 계약 체결 |
| `GET` | `/contracts/{id}` | 계약 조회 |
| `POST` | `/accounts` | 계좌 생성 |
| `GET` | `/accounts/{id}` | 계좌 조회 |
| `POST` | `/transactions` | 거래 등록 |
| `GET` | `/transactions` | 거래 내역 조회 |
| `GET` | `/interests` | 이자 이력 조회 |
| `GET` | `/special-terms` | 특약 목록 조회 |
| `GET` | `/join-targets` | 가입 대상 그룹 조회 |
| `GET` | `/health` | 헬스 체크 |

> Swagger UI: `http://localhost:8082/swagger-ui.html`

---

## DB 마이그레이션 (Flyway)

| 버전 | 파일 | 내용 |
|------|------|------|
| V1 | `V1__initial_schema.sql` | 초기 스키마 |
| V2 | `V2__seed_postman_data.sql` | Postman 테스트용 시드 데이터 |
| V5 | `V5__full_erd_schema.sql` | 전체 ERD 반영 스키마 |
| V6 | `V6__term_application_management.sql` | 약관 신청 관리 테이블 |
| V7 | `V7__seed_regular_savings.sql` | 적금 상품 시드 데이터 |
| V8 | `V8__seed_customer_frontend_products.sql` | 고객 대면 상품 21종 시드 데이터 |

---

## 도메인 모델 (주요 엔티티)

```
Product (수신 상품)
├── DepositProduct      (예금 상세)
├── SavingsProduct      (적금 상세)
├── SubscriptionProduct (청약 상세)
├── ProductInterestRate (금리 조건)
├── ProductJoinChannel  (가입 채널)
├── ProductSpecialTerm  (상품-특약 연결)
└── ProductTargetGroup  (대상 그룹 연결)

Contract (계약)
├── Account             (계좌)
│   └── Transaction     (거래)
├── InterestHistory     (이자 이력)
├── ContractAppliedRate (적용 금리)
└── ContractSpecialTermAgreement (특약 동의)

Department (부서)
TargetGroup (대상 그룹)
SpecialTerm (수신 특약)
```

---

## 로컬 실행 방법

### 사전 조건
- Java 21+
- Gradle (래퍼 사용 가능)
- PostgreSQL 16 (운영 프로파일) 또는 별도 설정 없이 H2(로컬 프로파일)

### H2 인메모리로 빠른 실행 (local 프로파일)

```bash
# 프로젝트 루트에서
./gradlew :services:deposit-service:bootRun --args="--spring.profiles.active=local"
```

실행 시 `LocalDataSeeder`가 초기 상품 데이터를 자동 삽입합니다.

### PostgreSQL 연결 (default 프로파일)

1. `.env.sample`을 복사해 `.env` 작성
2. `docker compose up -d db` 로 DB 컨테이너 기동
3. Flyway 마이그레이션 자동 적용 후 서비스 시작

```bash
./gradlew :services:deposit-service:bootRun
```

---

## 테스트 실행

```bash
./gradlew :services:deposit-service:test
```

- `TransactionRepository` DataJPA 테스트
- `RecommendAgentService` 유닛 테스트 (추천 에이전트 누락 시나리오 포함)
- 총 360개 테스트 통과 기준

---

## 주요 설계 결정

| 결정 | 이유 |
|------|------|
| Flyway 마이그레이션 | 환경별 스키마 일관성 보장 |
| H2 로컬 프로파일 분리 | DB 없이 즉시 개발·검증 가능 |
| `StatusHistory` 자동 기록 | 계약·계좌 상태 변경 감사 추적 |
| 추천 에이전트 별도 컨트롤러 분리 | AI 기능 확장 시 독립적 교체 가능 |
| V8 고객 대면 시드 분리 | 프론트엔드 product 페이지와 ID 1:1 매핑 |
