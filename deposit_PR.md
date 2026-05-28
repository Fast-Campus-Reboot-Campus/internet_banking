## Summary

수신계(예·적금·청약) 도메인 백엔드 서비스(Spring Boot 3.3.5 / Java 21)를 신규 구현했습니다.
상품 CRUD, 계약·계좌·거래·이자 관리 전 영역과, 고객 거래 이력 기반 상품 추천 에이전트를 포함합니다.

## Changes

- **수신 도메인 ERD 설계 및 Flyway 마이그레이션 적용** (V1 초기 스키마 → V8 고객 대면 시드)
- **수신 상품 관리 API**: 정기예금·입출금자유·적금·청약 4종 CRUD, 상태 관리(`SELLING`/`SUSPENDED`/`CLOSED`)
- **계약 관리**: 계약 체결·조회·해지, 가입금액 범위 및 기간 최솟값 유효성 검증, 상태 이력 자동 기록(`StatusHistory`)
- **계좌·거래·이자 관리**: 1계약-1계좌 구조, 입출금 거래 기록, 기본·우대금리 이력 관리
- **수신 특약 관리**: 상품·계약별 특약 동의 연결
- **현금흐름 기반 상품 추천 에이전트**: `GET /products/recommend-agent` — 고객 거래 이력의 순입금액 분석 → 최적 금리 상품 매칭
- **시드 데이터**: 고객 대면 상품 21개(정기예금 4, 입출금자유 10, 적금 5, 청약 2) + 적금 REGULAR 타입 추가
- **상품 단건 조회 경로 보안 강화**: 숫자 ID만 허용(`/products/{id}` numeric 제약)
- **테스트 보강**: `TransactionRepository` DataJpaTest, 추천 에이전트 경계 시나리오 포함
- **README 작성**: API 엔드포인트 전체 목록, 실행 방법, Flyway 마이그레이션 순서 문서화

## Test Plan

- [x] `./gradlew build` 통과
- [x] `./gradlew test` 통과
- [x] 수동 검증:
  - Swagger UI(`/swagger-ui.html`)에서 상품 목록·단건 조회 확인
  - 계약 체결 → 계좌 생성 → 입금 거래 → 이자 이력 전체 흐름 확인
  - `GET /products/recommend-agent?customerId=CUST001&periodMonth=3` 추천 결과 확인
  - Flyway 마이그레이션 V1→V2→V5→V6→V7→V8 순서 정상 적용 확인

## Risks / Rollback

- **Flyway 버전 갭(V3·V4 누락)**: 현재 V1→V2→V5 순서로 적용됨. 다른 브랜치에서 V3·V4를 사용 중이라면 충돌 가능. 롤백은 `flyway repair` 후 해당 버전 스크립트 제거로 처리.
- **H2 ↔ PostgreSQL 방언 차이**: 운영 환경 첫 배포 시 PostgreSQL 방언 호환성 재확인 필요. 롤백은 이전 Flyway 버전으로 `flyway undo` 또는 스키마 재생성.
- **시드 데이터 중복**: `V2__seed_postman_data.sql` / `V8__seed_customer_frontend_products.sql` 재실행 시 PK 충돌 가능 — Flyway checksum으로 재실행 차단됨.

## Checklist

- [x] 커밋 메시지가 Conventional Commits 형식 (`docs/AI_GUIDELINES.md` §1)
- [x] `.env` 등 비밀 파일 커밋되지 않음
- [x] AI 모델명·세션 링크·자기 홍보 문구 없음
- [x] 1 PR = 1 의도 원칙 준수 (수신계 도메인 신규 구현)
