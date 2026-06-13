package com.bank.customer.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 인증 보안 메트릭. Prometheus {@code infra/prometheus/alerts.yml} 의
 * {@code BruteForceLoginDetected} / {@code HighJwtInvalidRate} 규칙이 참조하는 시계열을 생성한다.
 *
 * <ul>
 *   <li>{@code customer_login_failure_total{reason="..."}} — 로그인 실패(비밀번호/PIN/인증서/QR 공통)</li>
 *   <li>{@code customer_jwt_invalid_total} — JWT 검증 실패(리프레시 토큰 등 customer-service 직접 검증분)</li>
 * </ul>
 *
 * <p>Micrometer Counter 는 Prometheus 노출 시 {@code _total} 접미사가 붙어,
 * 코드의 {@code customer.login.failure} 가 규칙의 {@code customer_login_failure_total} 과 매칭된다.
 *
 * <p>reason 은 알림이 집계하는 <em>저카디널리티</em> 라벨만 사용한다(에러코드 → 고정 라벨 매핑).
 * loginId/IP 등 고카디널리티 값을 태그로 쓰면 시계열이 폭증하므로 금지.
 */
@Component
public class AuthMetrics {

    private static final String LOGIN_FAILURE_METRIC = "customer.login.failure";
    private static final String JWT_INVALID_METRIC   = "customer.jwt.invalid";

    private final MeterRegistry registry;
    private final Counter jwtInvalid;
    private final Map<String, Counter> loginFailureByReason = new ConcurrentHashMap<>();

    public AuthMetrics(MeterRegistry registry) {
        this.registry = registry;
        // 첫 이벤트가 발생하기 전에도 알림 규칙이 평가 가능하도록, 대상 시계열을 기동 시 0 으로 사전 노출한다.
        this.jwtInvalid = Counter.builder(JWT_INVALID_METRIC)
                .description("JWT 검증 실패 횟수 (리프레시 토큰 등 customer-service 직접 검증분)")
                .register(registry);
        loginFailureCounter("bad_credentials"); // 브루트포스 알림 대상 시계열 사전 등록
    }

    /** 로그인 실패 1건 기록. */
    public void loginFailure(String reason) {
        loginFailureCounter(reason).increment();
    }

    /** JWT 검증 실패 1건 기록. */
    public void jwtInvalid() {
        jwtInvalid.increment();
    }

    private Counter loginFailureCounter(String reason) {
        return loginFailureByReason.computeIfAbsent(reason, r ->
                Counter.builder(LOGIN_FAILURE_METRIC)
                        .tag("reason", r)
                        .description("로그인 실패 횟수 (비밀번호/PIN/인증서/QR 공통)")
                        .register(registry));
    }
}
