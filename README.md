# Internet Banking MVP

작성자: 정혜영

## 프로젝트 개요

Internet Banking MVP는 고객, 예금, 상담, 결제 등 인터넷뱅킹의 핵심 기능을 서비스 단위로 분리한 Spring Boot 기반 멀티 모듈 프로젝트이다.

이번 작업에서는 `services/deposit-service`에 고객 현금흐름 기반 예금상품 추천 기능인 `recommend-agent`를 구현하고 검증했다.

## 주요 서비스

- `services/deposit-service`: 예금상품, 예금계약, 계좌, 거래내역, 상품 추천 기능
- `services/consultation-service`: 챗봇 및 상담 관련 기능
- `common`: 서비스 공통 모듈
- `infra`: 로컬 인프라 및 모니터링 설정

## recommend-agent 기능 요약

`recommend-agent`는 고객의 최근 거래내역을 기반으로 현금흐름을 계산하고, 예상 저축 가능 금액에 맞는 예금상품을 추천한다.

### API

```http
GET /api/products/recommend-agent?customerId=CUST001&periodMonth=3
```

### 요청 파라미터

- `customerId`: 추천 대상 고객 ID
- `periodMonth`: 현금흐름을 분석할 최근 개월 수

### 응답 핵심 항목

- `cashFlow.totalInflow`: 조회 기간 내 총 입금액
- `cashFlow.totalOutflow`: 조회 기간 내 총 출금액
- `cashFlow.netCashFlow`: 총 입금액 - 총 출금액
- `cashFlow.estimatedSavingsAmount`: 월 예상 저축 가능 금액
- `recommendations`: 추천 상품 목록
- `recommendations[].reason`: 추천 사유

## 구현 범위

- `RecommendAgentController` 추가
- `RecommendAgentService` 추가
- 현금흐름 응답 DTO 추가
  - `CashFlowSummary`
  - `ProductRecommendResponse`
  - `RecommendedProduct`
- `TransactionRepository`에 기간별 성공 거래 조회 기능 추가
- local 프로필용 `CUST001` 거래 시드 데이터 추가
- recommend-agent Controller 테스트 보강
- recommend-agent Service 테스트 보강

## local 시드 데이터

발표 및 로컬 검증을 위해 `LocalDataSeeder`에 `CUST001` 기준 최근 3개월 내 성공 거래 데이터를 추가했다.

- 입금 거래: 3,000,000원
- 출금 거래: 2,000,000원
- 순현금흐름: 1,000,000원
- 예상 저축 가능 금액: 약 333,333원

이 데이터는 local 프로필 테스트 및 발표용 검증 데이터를 목적으로 한다.

## 검증 결과

### 백엔드

```powershell
.\gradlew.bat :services:deposit-service:test
```

결과:

- 전체 테스트 통과
- `BUILD SUCCESSFUL`
- recommend-agent 성공 케이스 검증
- 추천 결과 생성 검증
- 추천 사유 `reason` 생성 검증
- `estimatedSavingsAmount > 0` 검증
- 실제 추천 상품명 검증

### 실제 API 검증

정상 고객:

- 고객 ID: `CUST001`
- 조회 기간: `3개월`
- 추천 결과: 3건 반환 확인
- cashFlow 계산 정상
- reason 생성 정상

계좌 없는 고객:

- 고객 ID: `CUST_NO_ACCOUNT`
- 추천 결과: 빈 배열 반환 확인
- 예외 없이 정상 응답 확인

### 프론트엔드

```powershell
npm run build
```

결과:

- 프론트 빌드 성공
- recommend-agent API 호출 성공
- 추천 결과 화면 렌더링 확인
- 상품명, 상품 유형, 추천 사유, cashFlow 표시 확인

## 발표 시연 순서

1. deposit-service 실행

```powershell
.\gradlew.bat :services:deposit-service:bootRun
```

2. recommend-agent API 호출

```http
GET http://localhost:8082/api/products/recommend-agent?customerId=CUST001&periodMonth=3
```

3. 응답에서 확인할 항목

- `cashFlow.totalInflow`
- `cashFlow.totalOutflow`
- `cashFlow.netCashFlow`
- `cashFlow.estimatedSavingsAmount`
- `recommendations`
- `recommendations[].reason`

4. 프론트 화면에서 상품 추천 결과 확인

- customerId 입력
- periodMonth 입력
- 추천 실행
- 추천 결과 목록 확인

## 커밋 정보

- 브랜치: `deposit`
- 커밋 메시지: `feat(deposit): implement cash-flow based recommendation agent`
- 작성자: 정혜영

## 커밋 제외 대상

아래 파일과 디렉터리는 recommend-agent 실제 구현과 무관하므로 커밋 대상에서 제외한다.

- `services/deposit-api/*`
- `services/consultation-service/tests/test_basic.py`

## 발표 전 체크리스트

- deposit-service 테스트 통과 여부 확인
- 프론트 빌드 성공 여부 확인
- `CUST001` 기준 추천 3건 반환 확인
- 추천 사유 `reason` 표시 확인
- 계좌 없는 고객 응답 확인
- Mock 프로젝트와 실제 deposit-service 혼동 방지
- push 전 `git status`에서 제외 대상이 staged에 포함되지 않았는지 확인
