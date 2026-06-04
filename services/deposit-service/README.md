# Deposit Service

작성자: 정혜영  
수정일: 2026-06-04

Deposit Service는 예금, 적금, 입출금, 청약 상품과 예금 계좌, 계약, 거래 내역을 담당하는 백엔드 서비스입니다. 프론트엔드의 예금 상품 조회, 상품 상세, 계좌이체, 이체 결과 조회, 거래내역 조회 화면과 연동됩니다.

## 수정 요약

이번 수정은 deposit 담당 범위만 포함합니다.

| 구분 | 변경 내용 |
| --- | --- |
| 상품 조회 | 상품 목록과 상품 상세 응답에 `bestRate`를 추가했습니다. |
| 최고금리 계산 | 활성화된 금리 row 중 기본금리 또는 기간기본금리의 최댓값에 우대금리를 합산해 최고금리를 계산합니다. |
| 프론트 상품 표시 | 상품 카드와 상품 상세 화면에서 `최고 연 n%`, `기본 연 n%`를 구분해 표시합니다. |
| 계좌이체 단일화 | 이체 실행 지점을 result 페이지 하나로 단일화했습니다. confirm 페이지의 중복 호출을 제거하고, result 페이지에서 `/transactions/transfer` API만 호출합니다. |
| 이체 계좌 조회 fallback | deposit API 실패 시 localStorage의 `joinedAccounts`로 fallback해 출금계좌 목록을 표시합니다. |
| 이체 가능 계좌 | `rawAccountType === 'DEPOSIT'`이고 출금 가능하며 해지되지 않은 계좌만 출금계좌로 선택합니다. |
| 이체 결과 | 이체 처리 중 로딩 표시를 추가했습니다. 이체 성공 후 계좌 잔액을 다시 조회하고, 실패 시 오류 메시지를 화면에 표시합니다. |
| 이체 조회 | 계좌별 거래내역을 조회해 즉시이체 결과조회에 반영합니다. |
| 거래내역 | 기본 계좌 자동 선택, 이체 메모 문구 정리, 거래 후 잔액 표시를 추가했습니다. |
| 거래 채널 | `TransactionChannel`에 `CHATBOT` 값을 추가했습니다. |
| 테스트 | 상품 목록/상세 응답의 `bestRate` 계산과 컨트롤러 응답 검증을 추가했습니다. |
| 테스트 보정 | 최신 계약/거래 서비스 시그니처와 계좌 조회 방식에 맞춰 기존 테스트 fixture를 보정했습니다. |
| 이체 시나리오 테스트 | INTERNAL 토AccountId null, 존재하지 않는 계좌, CLOSED 계좌, 계좌번호 불일치, 타행이체 잔액 차감 검증 추가. |

## 백엔드 변경 상세

### 상품 응답 최고금리

`ProductResponse`에 `bestRate` 필드를 추가했습니다.

- 기존 `baseInterestRate`는 상품에 등록된 기본 표시 금리입니다.
- 신규 `bestRate`는 실제 활성 금리 조건을 기준으로 계산한 최고 금리입니다.
- 활성 금리 row가 없거나 계산 가능한 금리가 없으면 `bestRate`는 `null`입니다.

계산 기준:

1. `RateType.BASE` 또는 `RateType.PERIOD_BASE` 중 가장 높은 금리를 기준금리로 사용합니다.
2. `RateType.PREFERENTIAL` 금리는 모두 합산합니다.
3. 기준금리가 없고 우대금리만 있으면 상품의 `baseInterestRate`를 기준금리로 대체합니다.
4. 기준금리와 우대금리를 더해 `bestRate`로 반환합니다.

관련 파일:

| 파일 | 내용 |
| --- | --- |
| `src/main/java/com/bank/deposit/dto/response/ProductResponse.java` | `bestRate` 필드와 변환 메서드 추가 |
| `src/main/java/com/bank/deposit/service/ProductService.java` | 상품 목록/상세 응답 생성 및 최고금리 계산 로직 추가 |
| `src/main/java/com/bank/deposit/controller/ProductController.java` | 컨트롤러가 `ProductResponse` 응답 메서드를 사용하도록 변경 |
| `src/main/java/com/bank/deposit/domain/enums/TransactionChannel.java` | `CHATBOT` 채널 추가 |

### 상품 API

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/products` | 상품 목록 조회. `bestRate` 포함 |
| `GET` | `/products/{productId}` | 상품 단건 조회. `bestRate` 포함 |

응답 예시:

```json
{
  "productId": 1,
  "productType": "DEPOSIT",
  "productName": "AXful 정기예금",
  "baseInterestRate": 3.5,
  "bestRate": 4.0,
  "productStatus": "SELLING"
}
```

## 프론트엔드 연동 변경

### 상품 화면

상품 목록, 메인 상품 쇼케이스, 예금 상품 상세 화면에서 `bestRate`가 있으면 최고금리를 우선 표시합니다.

- `bestRate`가 있으면 `최고 연 n%`
- `bestRate`가 없고 `baseInterestRate`가 있으면 `기본 연 n%`
- 둘 다 없으면 기존 fallback 값 사용

관련 파일:

| 파일 | 내용 |
| --- | --- |
| `web/lib/deposit-api.ts` | `DepositProduct.bestRate` 타입 추가 |
| `web/components/home/ProductShowcase.tsx` | 상품 카드 금리 표시 변경 |
| `web/app/(personal)/products/deposit/[id]/page.tsx` | 상품 상세 금리 표시 변경 |

### 계좌이체

이체 결과 페이지에서 실제 deposit-service 이체 API를 호출하도록 변경했습니다.

처리 흐름:

1. 이체 입력 페이지에서 출금계좌, 입금계좌, 금액, 이체 유형을 `sessionStorage.pendingTransfer`에 저장합니다.
2. 이체 결과 페이지 진입 시 `/transactions/transfer` API를 호출합니다.
3. 성공하면 계좌 목록을 다시 조회해 잔액을 갱신합니다.
4. 실패하면 실패 메시지를 결과 영역에 표시합니다.
5. 처리 후 `pendingTransfer`를 제거해 중복 실행을 막습니다.

관련 파일:

| 파일 | 내용 |
| --- | --- |
| `web/lib/deposit-api.ts` | `executeDepositTransfer` API 함수와 거래 응답 잔액 필드 추가 |
| `web/app/(personal)/transfer/account/page.tsx` | 출금 가능 계좌 필터링, API 실패 시 localStorage fallback, 내부이체 대상 계좌 ID 저장 |
| `web/app/(personal)/transfer/result/page.tsx` | 실제 이체 API 호출, 성공/실패 표시, 잔액 갱신 |
| `web/app/(personal)/transfer/inquiry/page.tsx` | 계좌별 거래내역 기반 이체 결과 조회 |

### 거래내역

거래내역 화면에서 사용자가 계좌를 직접 고르지 않아도 기본 계좌가 선택되도록 보정했습니다. 또한 이체 메모 문구와 거래 후 잔액 표시를 개선했습니다.

관련 파일:

| 파일 | 내용 |
| --- | --- |
| `web/app/(personal)/inquiry/transactions/page.tsx` | 기본 계좌 선택, 이체 메모 표시 정리, 거래 후 잔액 표시 |
| `web/lib/deposit-api.ts` | `DepositTransaction.balanceAfter`, `availableBalanceAfter` 타입 추가 |

## 테스트

추가/수정된 테스트:

| 파일 | 검증 내용 |
| --- | --- |
| `src/test/java/com/bank/deposit/controller/ProductControllerTest.java` | 상품 목록/상세 응답에 `bestRate`가 포함되는지 검증 |
| `src/test/java/com/bank/deposit/service/ProductServiceTest.java` | 활성 금리 row를 기준으로 `bestRate`가 계산되는지 검증 |
| `src/test/java/com/bank/deposit/controller/ContractControllerTest.java` | 계약 해지 API mock 인자를 현재 컨트롤러 호출 방식에 맞게 보정 |
| `src/test/java/com/bank/deposit/service/ContractServiceTest.java` | 계약 생성 테스트를 현재 서비스 시그니처와 고정 Clock 기준에 맞게 보정 |
| `src/test/java/com/bank/deposit/service/TransactionServiceTest.java` | 거래 서비스 테스트 보정 + 이체 추가 시나리오 5개: INTERNAL toAccountId null, 존재하지 않는 계좌, CLOSED 계좌, 계좌번호 불일치, 타행이체 잔액 차감 검증 |

테스트 실행:

```bash
./gradlew :services:deposit-service:test
```

## 변경 파일 목록

백엔드:

- `services/deposit-service/src/main/java/com/bank/deposit/controller/ProductController.java`
- `services/deposit-service/src/main/java/com/bank/deposit/domain/enums/TransactionChannel.java`
- `services/deposit-service/src/main/java/com/bank/deposit/dto/response/ProductResponse.java`
- `services/deposit-service/src/main/java/com/bank/deposit/service/ProductService.java`
- `services/deposit-service/src/test/java/com/bank/deposit/controller/ProductControllerTest.java`
- `services/deposit-service/src/test/java/com/bank/deposit/controller/ContractControllerTest.java`
- `services/deposit-service/src/test/java/com/bank/deposit/service/ContractServiceTest.java`
- `services/deposit-service/src/test/java/com/bank/deposit/service/ProductServiceTest.java`
- `services/deposit-service/src/test/java/com/bank/deposit/service/TransactionServiceTest.java`

프론트엔드 deposit 연동:

- `web/lib/deposit-api.ts`
- `web/components/home/ProductShowcase.tsx`
- `web/app/(personal)/products/deposit/[id]/page.tsx`
- `web/app/(personal)/transfer/account/page.tsx`
- `web/app/(personal)/transfer/result/page.tsx`
- `web/app/(personal)/transfer/inquiry/page.tsx`
- `web/app/(personal)/inquiry/transactions/page.tsx`

## 담당 범위 확인

이번 커밋에는 customer-service 변경을 포함하지 않습니다. 인증, 고객, 상담 서비스 담당 영역의 파일은 제외하고 deposit-service와 deposit 프론트 연동 파일만 포함합니다.
