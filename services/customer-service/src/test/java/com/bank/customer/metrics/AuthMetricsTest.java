package com.bank.customer.metrics;

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AuthMetrics 가 노출하는 Prometheus 시계열 이름이 alerts.yml 규칙
 * (customer_login_failure_total{reason="bad_credentials"}, customer_jwt_invalid_total)과
 * 정확히 일치하는지 검증한다. 이름이 어긋나면 알림이 영영 발화하지 않으므로 회귀 방지가 핵심.
 */
class AuthMetricsTest {

    private PrometheusMeterRegistry newRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }

    @Test
    void 기동시_알림_대상_시계열이_0으로_사전노출된다() {
        PrometheusMeterRegistry registry = newRegistry();
        new AuthMetrics(registry);

        String scrape = registry.scrape();
        // 첫 이벤트 전에도 규칙이 평가 가능하도록 시계열이 존재해야 한다.
        assertThat(scrape).contains("customer_jwt_invalid_total");
        assertThat(scrape).contains("customer_login_failure_total");
        assertThat(scrape).contains("reason=\"bad_credentials\"");
    }

    @Test
    void 로그인_실패는_reason_라벨과_함께_집계된다() {
        PrometheusMeterRegistry registry = newRegistry();
        AuthMetrics metrics = new AuthMetrics(registry);

        metrics.loginFailure("bad_credentials");
        metrics.loginFailure("bad_credentials");
        metrics.loginFailure("locked");

        assertThat(registry.get("customer.login.failure").tag("reason", "bad_credentials").counter().count())
                .isEqualTo(2.0);
        assertThat(registry.get("customer.login.failure").tag("reason", "locked").counter().count())
                .isEqualTo(1.0);

        // Prometheus 노출 이름(_total 접미사)이 alerts.yml 과 일치하는지 확인
        assertThat(registry.scrape())
                .contains("customer_login_failure_total{reason=\"bad_credentials\"");
    }

    @Test
    void JWT_검증실패가_집계된다() {
        PrometheusMeterRegistry registry = newRegistry();
        AuthMetrics metrics = new AuthMetrics(registry);

        metrics.jwtInvalid();
        metrics.jwtInvalid();
        metrics.jwtInvalid();

        assertThat(registry.get("customer.jwt.invalid").counter().count()).isEqualTo(3.0);
        assertThat(registry.scrape()).contains("customer_jwt_invalid_total");
    }
}
