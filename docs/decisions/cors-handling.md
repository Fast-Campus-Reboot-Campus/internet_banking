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

게이트웨이에는 다음 필터를 둔다(증상 정정 — PR #115):

```yaml
# spring.cloud.gateway
default-filters:
  - DedupeResponseHeader=Access-Control-Allow-Origin Access-Control-Allow-Credentials Access-Control-Allow-Methods Access-Control-Allow-Headers Access-Control-Max-Age, RETAIN_UNIQUE
```

`Access-Control-Allow-Methods`·`Access-Control-Allow-Headers`·`Access-Control-Max-Age` 도 OPTIONS preflight 응답에서 동일하게 중복되므로 함께 병합한다(실제 장애는 아니나 일관성 차원).

`RETAIN_UNIQUE`는 중복 값이 **동일할 때만** 안전하게 1개로 합친다. Spring CORS는 `allowedOrigins`(구체값)든 `allowedOriginPatterns`(패턴)든 허용된 요청의 `Origin`을 **그대로 echo**해 헤더에 싣는다. 따라서 게이트웨이와 다운스트림의 허용 Origin 집합이 일치하면(현재 모두 `localhost:3000`/`localhost:3001`) 항상 동일한 값을 내보내 병합이 안전하다. deposit만 `allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")`로 범위가 넓지만, 위 두 Origin에 대해선 동일한 구체값을 반영하므로 병합에 문제가 없다.

## 적용 가이드 (신규 서비스 추가 시)

1. 이 서비스를 프론트가 **게이트웨이로만** 호출하는가? → 서비스에 CORS 설정을 두지 말 것.
2. **직접 호출**(게이트웨이 미경유)을 받는가? → 서비스 자체 CORS를 두되 `allowedOrigins`를 게이트웨이와 동일하게 맞출 것.
3. **둘 다**라면 → 2번처럼 두고, 게이트웨이의 `DedupeResponseHeader`가 중복을 흡수하도록 둘 것.

서비스의 `allowedOrigins`가 게이트웨이와 어긋나면(예: 게이트웨이가 허용하지 않는 포트를 서비스만 허용) 두 레이어 값이 달라져 `RETAIN_UNIQUE` 병합이 깨질 수 있으므로, 직접 호출 서비스의 허용 Origin은 게이트웨이와 일치시킨다.

## 대안 / 향후

- **근본 해법**(전 서비스 CORS 제거 + 게이트웨이 단독 처리)은 모든 브라우저 트래픽을 게이트웨이로 태운다는 전제에서만 성립한다. 현재 어드민/일부 프론트의 게이트웨이 우회는 의도된 설계이므로, 그 전제가 바뀌기 전까지는 위 혼합 원칙을 유지한다.
- 향후 어드민 프론트를 게이트웨이로 일원화한다면(라우트 추가 + 프론트 base URL 변경 + 인증 헤더 전파 + 재검증), 해당 서비스의 자체 CORS를 그때 제거한다. CORS 하나 때문에 선행할 작업은 아니다.
- **deposit `allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")` → 명시적 list(`localhost:3000`/`localhost:3001`)로 통일**은 별도 **후속 기술부채 PR**에서 진행한다(리뷰어 권고, 본 PR 범위 밖). 게이트웨이가 허용하지 않는 포트를 deposit만 허용하는 범위 불일치를 좁히는 것이 목적.

## 관련

- PR #115 — 게이트웨이 `DedupeResponseHeader`로 중복 CORS 헤더 정정(증상). 본 ADR이 포함된 PR.
- 코드 리뷰(jaho96): 근본 원인은 이중부착 구조이며 게이트웨이 일원화가 정석 — 본 문서로 원칙화하여 후속 부채로 트래킹.
- 여신계(양혜민): loan-service(8083)·auto-loan-review(8089)는 어드민이 게이트웨이 미경유 직접 호출 → 자체 CORS 유지(예외) 확정.
