# CORS 적용 위치 원칙 (게이트웨이 vs 서비스)

## 배경 / 문제

브라우저에서 로그인 시 "로그인 서버에 연결할 수 없습니다"(`ERR_NETWORK`)가 발생했다. 원인은 서버 다운이 아니라 **CORS 응답 헤더 중복**이었다.

`브라우저 → 게이트웨이(8080) → customer-service(8081)` 경로에서 게이트웨이(`globalcors`)와 customer-service(`SecurityConfig`)가 **각각** `Access-Control-Allow-Origin`을 부착해, 응답에 같은 값이 2개(`http://localhost:3001, http://localhost:3001`) 실렸다. 브라우저는 다중값 CORS를 거부하지만 curl/Postman은 중복 헤더를 무시해 그동안 드러나지 않았다.

조사 결과 여러 서비스(customer / loan / payment / doc-agent / auto-loan-review = `SecurityConfig`, deposit = `CorsConfig`)가 각자 CORS를 설정하고 있었다. 단순히 "CORS는 게이트웨이 한 곳에서만 처리해야 하므로 서비스 CORS를 전부 제거" 하는 것이 정석이지만, 이 시스템은 **어드민/일부 프론트가 게이트웨이를 의도적으로 우회해 서비스를 직접 호출**한다(예: 어드민이 loan 8083·auto-loan-review 8089·deposit 8082·doc-agent 8087를 직접 호출). 직접 호출 서비스는 자체 CORS가 **반드시 필요**하다.

즉 "전 서비스 CORS 제거"는 이 혼합 아키텍처와 맞지 않는다.

## 결정

CORS 적용 **위치를 호출 경로에 따라 결정**한다. 일괄 제거가 아니라 다음 원칙을 따른다.

- **게이트웨이 경유만** 하는 서비스 → CORS는 게이트웨이에서 처리(서비스 자체 설정 불필요).
- **직접 호출만** 받는 서비스(loan · auto-loan-review · doc-agent 등) → 서비스 자체 CORS 유지.
- **둘 다**인 서비스(deposit · loan 등) → 서비스 CORS 유지 + 게이트웨이 `DedupeResponseHeader`로 중복 헤더 흡수.
- **한 코드베이스가 두 역할을 겸함**(payment) → 단일 `SecurityConfig`가 게이트웨이-뒤(A)와 직접 호출(B)을 모두 담당하므로, CORS를 통째로 끄면 직접 호출이 깨진다. CORS를 **환경 플래그 `app.cors.enabled`로 게이팅**(기본 `false`)해, 직접 호출 인스턴스(B)에서만 `APP_CORS_ENABLED=true`로 켠다(#116) → 아래 "payment 사례" 참조.

게이트웨이에는 다음 필터를 둔다(증상 정정 — PR #115):

```yaml
# spring.cloud.gateway
default-filters:
  - DedupeResponseHeader=Access-Control-Allow-Origin Access-Control-Allow-Credentials Access-Control-Allow-Methods Access-Control-Allow-Headers Access-Control-Max-Age, RETAIN_UNIQUE
```

`Access-Control-Allow-Methods`·`Access-Control-Allow-Headers`·`Access-Control-Max-Age` 도 OPTIONS preflight 응답에서 동일하게 중복되므로 함께 병합한다(실제 장애는 아니나 일관성 차원).

`RETAIN_UNIQUE`는 중복 값이 **동일할 때만** 안전하게 1개로 합친다. Spring CORS는 `allowedOrigins`(구체값)든 `allowedOriginPatterns`(패턴)든 허용된 요청의 `Origin`을 **그대로 echo**해 헤더에 싣는다. 따라서 게이트웨이와 다운스트림의 허용 Origin 집합이 일치하면(현재 모두 `localhost:3000`/`localhost:3001`) 항상 동일한 값을 내보내 병합이 안전하다. deposit은 명시 list(`http://localhost:3000`/`:3001`, `http://127.0.0.1:3000`/`:3001`)를 쓰며(과거 `allowedOriginPatterns("localhost:*")` 와일드카드에서 좁혀짐), 게이트웨이와 공통인 `localhost:3000`/`:3001`에 대해 동일 구체값을 반영하므로 병합에 문제가 없다(`127.0.0.1`은 게이트웨이 미허용 — 로컬 직접호출 전용).

### payment 사례 — 단일 코드베이스, 이중 역할 (`app.cors.enabled` 플래그 게이팅)

payment-service는 같은 이미지·같은 `SecurityConfig`로 두 인스턴스를 띄운다.

- **payment-service-A (8084, 게이트웨이 뒤)**: 개인 이체 프론트는 게이트웨이(8084)도 직접도 아닌 **자기 오리진(3000)** 상대경로(`/api/payment`)로 호출한다. 따라서 A의 자체 CORS를 제거해도 개인 이체에는 영향이 없다 → **A 경로에서는 CORS off**.
- **payment-service-B (8180, `mock` 프로파일 · 타행 수신은행 "다온" 시연)**: 다온 화면이 브라우저에서 **8180을 직접 호출**한다(baseURL 8180 하드코딩, 게이트웨이에 `8180`/inbound 라우트 없음). 이 직접 호출은 **B의 자체 CORS가 있어야** 동작한다.
- A·B는 **동일 `SecurityConfig`**라, CORS를 통째로 disable하면 B(다온)도 함께 깨진다. → CORS를 **환경 플래그 `app.cors.enabled`로 게이팅**(`@Value("${app.cors.enabled:false}")`)해, B 인스턴스만 `APP_CORS_ENABLED=true`(env)로 켜고 게이트웨이 뒤(A)는 기본 `false`로 끈다(#116, 커밋 ffd7e155). `@Profile`이 아니라 플래그라, CORS on/off가 `mock` 프로파일 의미에 종속되지 않는다(B가 mock으로 도는 건 별개 사실).
- 따라서 payment는 **일괄 제거 대상에서 제외**하고, payment 담당(임형진)이 별도 PR로 처리한다.
- (참고) payment 프론트 호출은 POST/GET뿐이라 게이트웨이 `globalcors`의 PATCH 추가와 무관하다.

## 적용 가이드 (신규 서비스 추가 시)

1. 이 서비스를 프론트가 **게이트웨이로만** 호출하는가? → 서비스에 CORS 설정을 두지 말 것.
2. **직접 호출**(게이트웨이 미경유)을 받는가? → 서비스 자체 CORS를 두되 `allowedOrigins`를 게이트웨이와 동일하게 맞출 것.
3. **둘 다**라면 → 2번처럼 두고, 게이트웨이의 `DedupeResponseHeader`가 중복을 흡수하도록 둘 것.
4. 한 코드베이스(같은 `SecurityConfig`)가 **게이트웨이-뒤 + 직접 호출** 두 역할을 겸한다면 → CORS를 **환경 플래그(예: `app.cors.enabled`)로 게이팅**해 직접 호출 인스턴스에서만 켤 것(`@Profile` 대신 플래그 — payment 사례).

서비스의 `allowedOrigins`가 게이트웨이와 어긋나면(예: 게이트웨이가 허용하지 않는 포트를 서비스만 허용) 두 레이어 값이 달라져 `RETAIN_UNIQUE` 병합이 깨질 수 있으므로, 직접 호출 서비스의 허용 Origin은 게이트웨이와 일치시킨다.

## 대안 / 향후

- **근본 해법**(전 서비스 CORS 제거 + 게이트웨이 단독 처리)은 모든 브라우저 트래픽을 게이트웨이로 태운다는 전제에서만 성립한다. 현재 어드민/일부 프론트의 게이트웨이 우회는 의도된 설계이므로, 그 전제가 바뀌기 전까지는 위 혼합 원칙을 유지한다.
- 향후 어드민 프론트를 게이트웨이로 일원화한다면(라우트 추가 + 프론트 base URL 변경 + 인증 헤더 전파 + 재검증), 해당 서비스의 자체 CORS를 그때 제거한다. CORS 하나 때문에 선행할 작업은 아니다.
- **deposit 와일드카드 → 명시 list 통일: 완료.** deposit은 현재 명시 list(`localhost:3000`/`:3001`, `127.0.0.1:3000`/`:3001`)를 사용해 `allowedOriginPatterns("localhost:*")` 와일드카드 범위 문제는 해소됨. (잔여 — `127.0.0.1:*`는 게이트웨이 미허용이나 로컬 직접호출 전용이라 무방. 더 좁히려면 명시 list에서 `127.0.0.1` 제거.)

## 관련

- PR #115 — 게이트웨이 `DedupeResponseHeader`로 중복 CORS 헤더 정정(증상). 본 ADR이 포함된 PR.
- 코드 리뷰(jaho96): 근본 원인은 이중부착 구조이며 게이트웨이 일원화가 정석 — 본 문서로 원칙화하여 후속 부채로 트래킹.
- 여신계(양혜민): loan-service(8083)·auto-loan-review(8089)는 어드민이 게이트웨이 미경유 직접 호출 → 자체 CORS 유지(예외) 확정.
- 지급계(임형진): payment는 단일 코드베이스가 A(게이트웨이 뒤)·B(8180 직접·`mock`) 두 역할을 겸함 → CORS `app.cors.enabled` 플래그 게이팅으로 분리(#116, ffd7e155). 일괄 제거 대상에서 제외.
- 게이트웨이 `globalcors.allowedMethods`에 **PATCH 추가**(상품 해지 등 PATCH 요청 지원). deposit은 이미 PATCH 보유 → 게이트웨이·deposit 값이 일치해 `RETAIN_UNIQUE` 병합이 안전.

## 코드 일치 검증

- 본 문서는 **코드/커밋 기준**으로 작성·갱신한다(슬랙 논의의 *용어*가 아니라 *구현*을 정본으로). 실제 불일치 사례: 채팅 "프로파일 게이팅" ↔ 구현 "`app.cors.enabled` 플래그"(#116). **커밋 메시지가 정본.**
- 최종 코드 대조: **2026-06-16** — payment `SecurityConfig`(#116 ffd7e155), deposit `CorsConfig`(명시 list), 게이트웨이 `application.yml`(globalcors + PATCH + DedupeResponseHeader) 기준.
- 코드가 바뀌면 **같은 PR에서 본 ADR도 갱신**한다(이번 #116이 ADR을 안 건드린 게 드리프트의 직접 원인).
