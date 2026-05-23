# Internet Banking MVP

작성자: 정혜영

---

## 프로젝트 개요

Internet Banking MVP는 고객, 예금, 상담, 결제 등 인터넷뱅킹의 핵심 기능을 서비스 단위로 분리한 Spring Boot 기반 멀티 모듈 프로젝트다.

---

## 서비스 구성

| 서비스 | 언어 / 프레임워크 | 역할 |
|---|---|---|
| `services/deposit-service` | Java 17 / Spring Boot 3.x | 예금상품, 계약, 계좌, 거래, 상품 추천 |
| `services/deposit-api` | Python / FastAPI | 수신 시스템 REST API (프론트 연동) |
| `services/consultation-service` | Python / FastAPI | 챗봇 및 상담 기능 |
| `common` | Java | 서비스 공통 모듈 |
| `infra` | Docker Compose | PostgreSQL, Redis, Prometheus, Grafana |

---

## 기술 스택

- **백엔드**: Java 17, Spring Boot 3.x, Gradle Multi-module
- **DB**: PostgreSQL 16, H2 (테스트), SQLite (deposit-api)
- **캐시**: Redis 7
- **모니터링**: Prometheus, Grafana
- **패키지 루트**: `com.bank`
- **테스트**: JUnit 5, Mockito, pytest

---

## 모듈 구조

```
internet_banking/
├── common/                         # 공통 모듈
├── services/
│   ├── deposit-service/            # Java Spring Boot — 수신계 핵심
│   ├── deposit-api/                # Python FastAPI  — 프론트 연동 API
│   └── consultation-service/      # Python FastAPI  — 챗봇
├── infra/                          # Docker Compose 인프라
└── docs/                           # 문서
```

---

## recommend-agent 기능

고객의 최근 거래내역을 분석해 현금흐름을 계산하고, 월 예상 저축 가능 금액에 맞는 예금상품을 추천한다.

### 추천 로직

```
1. 고객의 활성 계좌 조회
2. 지정 기간(periodMonth) 내 성공 거래내역 조회
3. 입금 합계 / 출금 합계 / 순현금흐름 계산
4. estimatedSavingsAmount = netCashFlow / periodMonth (내림)
5. estimatedSavingsAmount > 0 이고 상품 가입 최소금액 이상인 상품 필터
6. 최고금리 내림차순 정렬 후 최대 5개 추천 반환
```

### API

```
GET /api/products/recommend-agent?customerId={customerId}&periodMonth={periodMonth}
```

| 파라미터 | 필수 | 기본값 | 설명 |
|---|---|---|---|
| `customerId` | ✅ | - | 추천 대상 고객 ID |
| `periodMonth` | ❌ | 3 | 현금흐름 분석 기간 (개월) |

### 응답 예시

```json
{
  "customerId": "CUST001",
  "analysisPeriodMonth": 3,
  "cashFlow": {
    "totalInflow": 4500000,
    "totalOutflow": 3000000,
    "netCashFlow": 1500000,
    "estimatedSavingsAmount": 500000
  },
  "recommendations": [
    {
      "productId": 1,
      "productName": "자유적금",
      "productType": "SAVINGS",
      "baseRate": 3.20,
      "bestRate": 3.50,
      "minJoinAmount": 10000,
      "maxJoinAmount": 1000000,
      "minPeriodMonth": 6,
      "maxPeriodMonth": 36,
      "reason": "월 평균 저축 가능 금액(500,000원) 기반 추천. 연 3.50% 금리 적용."
    }
  ]
}
```

### 엣지 케이스 처리

| 상황 | 처리 결과 |
|---|---|
| 계좌 없음 | cashFlow 전부 0, recommendations 빈 배열 |
| 거래내역 없음 | cashFlow 전부 0, recommendations 빈 배열 |
| 출금 > 입금 | cashFlow 수치 정상 채움, recommendations 빈 배열 |
| 판매 상품 없음 | recommendations 빈 배열 |
| estimatedSavings < minJoinAmount | 해당 상품 추천 제외 |

---

## 테스트 현황

### deposit-service

```powershell
.\gradlew :services:deposit-service:test
```

| 항목 | 수치 |
|---|---|
| 전체 테스트 수 | **226개** |
| 통과 | 226개 |
| 실패 | 0개 |
| 에러 | 0개 |
| 스킵 | 0개 |

#### 서비스별 테스트 파일

| 서비스 | Service 테스트 | Controller 테스트 |
|---|---|---|
| AccountService | AccountServiceTest | AccountControllerTest |
| ContractService | ContractServiceTest | ContractControllerTest |
| DepartmentService | DepartmentServiceTest | DepartmentControllerTest |
| InterestService | InterestServiceTest | InterestControllerTest |
| ProductService | ProductServiceTest | ProductControllerTest |
| RecommendAgentService | RecommendAgentServiceTest | RecommendAgentControllerTest |
| SpecialTermService | SpecialTermServiceTest | SpecialTermControllerTest |
| TargetGroupService | TargetGroupServiceTest | TargetGroupControllerTest |
| TransactionService | TransactionServiceTest | TransactionControllerTest |
| TermApplicationManagementService | TermApplicationManagementServiceTest | TermApplicationManagementControllerTest |
| SubscriptionPaymentRecognitionHistoryService | SubscriptionPaymentRecognitionHistoryServiceTest | SubscriptionPaymentRecognitionHistoryControllerTest |

#### RecommendAgentServiceTest 시나리오 (10개)

| 시나리오 | 검증 내용 |
|---|---|
| 정상 추천 | 입금 > 출금 → 추천 목록 반환, bestRate 내림차순 정렬 |
| 계좌 없음 | cashFlow 전부 0, recommendations=[] |
| 거래내역 없음 | cashFlow 전부 0, recommendations=[] |
| 판매 상품 없음 | recommendations=[] |
| minJoinAmount 경계 | estimatedSavings < minJoin → 해당 상품 제외 |
| periodMonth=1 | net / 1 = estimated 전액 |
| periodMonth=6 | net / 6 = estimated |
| periodMonth=12 | net / 12 = estimated |
| 출금 > 입금 | net 음수 → cashFlow 채움 + recommendations=[] |
| 다중 계좌 | 2개 계좌 거래내역 합산 → cashFlow 정확히 계산 |

---

## 로컬 실행

### 사전 조건

- Java 17
- Docker Desktop (PostgreSQL, Redis)
- Python 3.11 (deposit-api, consultation-service)

### 인프라 실행

```powershell
docker compose -f infra/docker-compose.yml up -d
```

### deposit-service 실행

```powershell
.\gradlew :services:deposit-service:bootRun
```

기본 포트: `8082`

### deposit-api 실행

```powershell
cd services/deposit-api
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

기본 포트: `8000`

### consultation-service 실행

```powershell
cd services/consultation-service
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8001
```

기본 포트: `8001`

---

## 발표 시연 순서

### 1. 서비스 실행 확인

```powershell
# deposit-service 헬스 체크
curl http://localhost:8082/actuator/health

# deposit-api 헬스 체크
curl http://localhost:8000/health
```

### 2. 상품 조회

```
GET http://localhost:8000/products
GET http://localhost:8000/products?product_type=DEPOSIT&status=SELLING
GET http://localhost:8000/products/{product_id}
```

### 3. 계약 생성

```
POST http://localhost:8000/contracts
{
  "customer_id": "CUST001",
  "banking_product_id": 1,
  "join_amount": 3000000,
  "period_months": 12
}
```

### 4. 계좌 / 거래내역 조회

```
GET http://localhost:8000/accounts?customer_id=CUST001
GET http://localhost:8000/accounts/{account_id}/transactions
GET http://localhost:8000/accounts/{account_id}/cash-flow
```

### 5. recommend-agent 호출

```
GET http://localhost:8082/api/products/recommend-agent?customerId=CUST001&periodMonth=3
```

확인 항목:
- `cashFlow.totalInflow` / `totalOutflow` / `netCashFlow` / `estimatedSavingsAmount`
- `recommendations` 목록 (상품명, bestRate, reason)

### 6. 챗봇 기능

```
GET  http://localhost:8001/chatbot/features
POST http://localhost:8001/chatbot/features/MY_ACCOUNTS/execute
     {"customer_no": "CUST001"}
```

---

## 발표 전 체크리스트

```
[ ] docker compose up -d — PostgreSQL, Redis 정상 기동
[ ] deposit-service bootRun — 포트 8082 정상 기동
[ ] deposit-api uvicorn — 포트 8000 정상 기동
[ ] consultation-service uvicorn — 포트 8001 정상 기동
[ ] CUST001 계좌 조회 — 계좌 1건 이상 반환 확인
[ ] recommend-agent 호출 — recommendations 1건 이상 반환 확인
[ ] cashFlow 수치 정상 확인 (estimatedSavingsAmount > 0)
[ ] reason 문자열 표시 확인
[ ] 프론트 CORS 오류 없음 확인
[ ] 브라우저 콘솔 에러 없음 확인
[ ] deposit-api DB(deposit.db) 시드 데이터 존재 확인
```

---

## 브랜치 / 커밋 정보

- **브랜치**: `deposit`
- **작성자**: 정혜영
- **주요 커밋**:
  - `feat(deposit): implement cash-flow based recommendation agent`
  - `test(deposit): 수신계 기능 테스트 보강`
  - `test(deposit): recommend-agent 누락 시나리오 테스트 추가`

---

## 주의사항

- `main` 브랜치 직접 커밋/푸시 금지
- `.env` 파일 커밋 금지
- 운영/스테이징 DB 접속 명령 금지
- 커밋 메시지에 AI 모델명·서명 삽입 금지 (공통 가이드 §5)
