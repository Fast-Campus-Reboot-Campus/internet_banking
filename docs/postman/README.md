# Deposit Service Postman Test

## 실행 상태

- API base URL: `http://localhost:8082`
- Postman collection: `docs/postman/deposit-service.postman_collection.json`
- Local PostgreSQL profile: `postgres-local`
- Local PostgreSQL DB: `deposit_postman`
- DB user/password: `deposit` / `deposit`

## 서버 실행

```powershell
.\gradlew.bat :services:deposit-service:bootJar
java -jar services\deposit-service\build\libs\deposit-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=postgres-local
```

Docker 없이 H2 메모리 DB로만 테스트할 때:

```powershell
java -jar services\deposit-service\build\libs\deposit-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

## 기본 테스트 데이터

- `productId`: `1` 예금, `2` 적금, `3` 청약
- `contractId`: `1` 예금 계약, `2` 적금 계약, `3` 계좌 생성 테스트용 계약
- `accountId`: `1` 예금 계좌, `2` 적금 계좌
- `customerId`: `CUST001`

## 검증 완료

- Flyway `V1`, `V2` migration 적용
- API 명세 18개 영역 스모크 테스트 통과
- `.\gradlew.bat :services:deposit-service:build` 통과
