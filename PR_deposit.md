## Summary

수신계 서비스에 고객 대면 상품 시드 데이터(21종)를 추가하고, LocalDataSeeder를 정리합니다.
프론트엔드 상품 목록 페이지와 ID가 1:1 매핑되도록 V8 Flyway 마이그레이션을 통해 일괄 등록합니다.

## Changes

- 고객 대면 상품 21종 시드 데이터 추가 (정기예금 4종, 입출금자유 10종, 적금 5종, 청약 2종)
- `V8__seed_customer_frontend_products.sql` Flyway 마이그레이션 추가
- `LocalDataSeeder` 중복 데이터 제거 및 정리
- `services/deposit-service/README.md` 최초 작성

## Test Plan

- [x] `./gradlew build` 통과
- [x] `./gradlew test` 통과
- [ ] 수동 검증:
  - `spring.profiles.active=local` 로 bootRun 후 `GET /products` 응답에 21개 상품 확인
  - `GET /products?productType=DEPOSIT` / `SAVINGS` / `SUBSCRIPTION` 필터 정상 동작 확인
  - V8 마이그레이션 적용 후 기존 V1~V7 데이터 충돌 없음 확인

## Risks / Rollback

- 상품 ID가 프론트엔드 하드코딩 값과 매핑되어 있어, ID 변경 시 화면 오류 가능
- 기존 시드 데이터와 ID 충돌 방지를 위해 `ON CONFLICT DO NOTHING` 처리
- 롤백 방법: V8 마이그레이션 revert 또는 해당 커밋 `git revert 93ef318`

## Checklist

- [x] 커밋 메시지가 Conventional Commits 형식 (`docs/AI_GUIDELINES.md` §1)
- [x] `.env` 등 비밀 파일 커밋되지 않음
- [x] AI 모델명·세션 링크·자기 홍보 문구 없음
- [x] 1 PR = 1 의도 원칙 준수
