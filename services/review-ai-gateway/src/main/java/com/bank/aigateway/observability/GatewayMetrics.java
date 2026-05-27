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
}
