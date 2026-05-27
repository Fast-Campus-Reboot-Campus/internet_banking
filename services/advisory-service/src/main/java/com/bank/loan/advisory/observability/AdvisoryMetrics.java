package com.bank.loan.advisory.observability;

import com.bank.loan.advisory.domain.ReviewAdvisoryReport;
import com.bank.loan.advisory.repository.ReviewAdvisoryReportRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 어드바이저리 운영 지표. Prometheus `/actuator/prometheus` 로 노출.
 *
 *   advisory_report_published_total{rule_cd, severity}   counter   리포트 발행 누적
 *   advisory_ack_response_total{response_cd}             counter   ack 응답 누적
 *   advisory_critical_gate_blocked_total                 counter   약정 게이트 차단 누적
 *   advisory_open_reports{severity}                      gauge     미해결(OPEN/VIEWED) 리포트 수
 *   advisory_evaluate_duration_seconds{mode}             timer     룰 평가 지연시간 분포
 */
@Component
@RequiredArgsConstructor
public class AdvisoryMetrics {

    public static final String M_PUBLISHED = "advisory_report_published_total";
    public static final String M_ACK_RESPONSE = "advisory_ack_response_total";
    public static final String M_GATE_BLOCKED = "advisory_critical_gate_blocked_total";
    public static final String M_OPEN_REPORTS = "advisory_open_reports";
    public static final String M_EVALUATE_DURATION = "advisory_evaluate_duration_seconds";

    private final MeterRegistry meterRegistry;
    private final ReviewAdvisoryReportRepository reportRepo;

    @PostConstruct
    void registerGauges() {
        for (String severity : List.of(
                ReviewAdvisoryReport.SEVERITY_INFO,
                ReviewAdvisoryReport.SEVERITY_WARN,
                ReviewAdvisoryReport.SEVERITY_CRITICAL)) {
            Gauge.builder(M_OPEN_REPORTS, reportRepo, r -> r.countOpenBySeverity(severity))
                    .tag("severity", severity)
                    .description("미해결(OPEN/VIEWED) 어드바이저리 리포트 수")
                    .register(meterRegistry);
        }
    }

    public void incrementPublished(String ruleCd, String severityCd) {
        meterRegistry.counter(M_PUBLISHED, "rule_cd", ruleCd, "severity", severityCd).increment();
    }

    public void incrementAckResponse(String responseCd) {
        meterRegistry.counter(M_ACK_RESPONSE, "response_cd", responseCd).increment();
    }

    public void incrementGateBlocked() {
        meterRegistry.counter(M_GATE_BLOCKED).increment();
    }

    public Timer.Sample startEvaluateTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordEvaluateDuration(Timer.Sample sample, String mode) {
        sample.stop(Timer.builder(M_EVALUATE_DURATION)
                .tag("mode", mode)
                .description("어드바이저리 룰 평가 지연시간")
                .register(meterRegistry));
    }
}
