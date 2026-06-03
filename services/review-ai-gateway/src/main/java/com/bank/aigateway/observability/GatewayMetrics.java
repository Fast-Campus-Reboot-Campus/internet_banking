package com.bank.aigateway.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GatewayMetrics {

    private final MeterRegistry registry;

    public Timer.Sample startAnalysisTimer() {
        return Timer.start(registry);
    }

    public void recordAnalysisDuration(Timer.Sample sample, String analysisType) {
        sample.stop(Timer.builder("aigateway.analysis.duration")
                .tag("type", analysisType)
                .register(registry));
    }

    public void recordTokens(String analysisType, int inputTokens, int outputTokens) {
        Counter.builder("aigateway.tokens.input")
                .tag("type", analysisType)
                .register(registry)
                .increment(inputTokens);
        Counter.builder("aigateway.tokens.output")
                .tag("type", analysisType)
                .register(registry)
                .increment(outputTokens);
    }

    /** 분석 결과 건수 (conclusion 태그: BIAS_SUSPECTED, NO_BIAS_DETECTED, VIOLATION_SUSPECTED, COMPLIANT, INSUFFICIENT_DATA) */
    public void recordAnalysisResult(String analysisType, String conclusion) {
        Counter.builder("aigateway.analysis.result.total")
                .tag("type", analysisType)
                .tag("conclusion", conclusion)
                .register(registry)
                .increment();
    }

    /** 최대 턴 초과로 INSUFFICIENT_DATA 폴백된 건수 */
    public void recordLoopTimeout(String analysisType) {
        Counter.builder("aigateway.loop.timeout.total")
                .tag("type", analysisType)
                .register(registry)
                .increment();
    }
}
