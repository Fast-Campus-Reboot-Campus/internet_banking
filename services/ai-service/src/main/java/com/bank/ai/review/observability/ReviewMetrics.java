package com.bank.ai.review.observability;

import com.bank.ai.review.dto.AutoReviewRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 자동심사(ML) 관측 메트릭.
 *
 * Prometheus 노출명 (Micrometer dot→underscore 변환):
 *   review_duration_seconds         — 추론 응답시간 p50/p95 (tag: modelVersion)
 *   review_decision_total           — 결정 건수 (tag: decision, modelVersion)
 *   review_score                    — 예측 신뢰도 분포 (tag: decision)
 *   review_inference_error_total    — 추론 실패 건수
 *   review_input_dsr                — DSR 입력 분포 (데이터 드리프트 감지)
 *   review_input_credit_score       — 신용점수 입력 분포 (데이터 드리프트 감지)
 *   review_input_missing_total      — 핵심 필드 결측치 건수 (tag: field)
 *   review_input_outlier_total      — 핵심 필드 이상치 건수 (tag: field)
 */
@Component
@RequiredArgsConstructor
public class ReviewMetrics {

    private final MeterRegistry registry;

    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    public void stopTimer(Timer.Sample sample, String modelVersion) {
        sample.stop(Timer.builder("review.duration")
                .description("자동심사 추론 응답시간")
                .tag("modelVersion", safeTag(modelVersion))
                .publishPercentileHistogram(true)
                .register(registry));
    }

    public void recordDecision(String decision, double score, String modelVersion) {
        Counter.builder("review.decision.total")
                .description("자동심사 결정 건수")
                .tag("decision", safeTag(decision))
                .tag("modelVersion", safeTag(modelVersion))
                .register(registry)
                .increment();

        DistributionSummary.builder("review.score")
                .description("모델 예측 신뢰도 분포")
                .tag("decision", safeTag(decision))
                .publishPercentileHistogram(true)
                .minimumExpectedValue(0.01)
                .maximumExpectedValue(1.0)
                .register(registry)
                .record(score);
    }

    public void recordInferenceError() {
        Counter.builder("review.inference.error.total")
                .description("추론 실패 건수")
                .register(registry)
                .increment();
    }

    /** DSR·신용점수 분포를 기록해 학습 데이터 대비 입력 분포 변화(드리프트)를 감지한다. */
    public void recordInputFeatures(Double dsr, Integer creditScoreProxy) {
        if (dsr != null) {
            DistributionSummary.builder("review.input.dsr")
                    .description("입력 DSR 분포 (데이터 드리프트 감지)")
                    .publishPercentileHistogram(true)
                    .minimumExpectedValue(0.01)
                    .maximumExpectedValue(1.0)
                    .register(registry)
                    .record(Math.max(0.0, Math.min(1.0, dsr)));
        }
        if (creditScoreProxy != null) {
            DistributionSummary.builder("review.input.credit.score")
                    .description("입력 신용점수 분포 (데이터 드리프트 감지)")
                    .publishPercentileHistogram(true)
                    .minimumExpectedValue(1.0)
                    .maximumExpectedValue(1000.0)
                    .register(registry)
                    .record(Math.max(0.0, (double) creditScoreProxy));
        }
    }

    /**
     * 핵심 금융 필드의 결측치(null)·이상치(허용 범위 초과)를 기록한다.
     * 결측치가 쌓이면 upstream 데이터 파이프라인 이상, 이상치가 쌓이면 입력 분포 변화 신호.
     */
    public void recordInputQuality(AutoReviewRequest req) {
        checkMissing("dsr",                  req.dsr());
        checkMissing("ltv",                  req.ltv());
        checkMissing("credit_score_proxy",   req.creditScoreProxy());
        checkMissing("annual_income_kw",     req.annualIncomeKw());
        checkMissing("requested_amount_kw",  req.requestedAmountKw());
        checkMissing("total_debt_kw",        req.totalDebtKw());

        if (req.dsr()               != null && (req.dsr() < 0.0 || req.dsr() > 1.0))           incOutlier("dsr");
        if (req.ltv()               != null && (req.ltv() < 0.0 || req.ltv() > 1.5))            incOutlier("ltv");
        if (req.creditScoreProxy()  != null && (req.creditScoreProxy() < 0 || req.creditScoreProxy() > 1000)) incOutlier("credit_score_proxy");
        if (req.age()               != null && (req.age() < 18 || req.age() > 100))              incOutlier("age");
        if (req.annualIncomeKw()    != null && req.annualIncomeKw() < 0)                         incOutlier("annual_income_kw");
        if (req.requestedAmountKw() != null && req.requestedAmountKw() <= 0)                     incOutlier("requested_amount_kw");
        if (req.delinquencyHistory24m() != null && (req.delinquencyHistory24m() < 0 || req.delinquencyHistory24m() > 24)) incOutlier("delinquency_history_24m");
    }

    private void checkMissing(String field, Object value) {
        if (value == null) {
            Counter.builder("review.input.missing.total")
                    .description("핵심 필드 결측치 건수")
                    .tag("field", field)
                    .register(registry)
                    .increment();
        }
    }

    private void incOutlier(String field) {
        Counter.builder("review.input.outlier.total")
                .description("핵심 필드 이상치 건수")
                .tag("field", field)
                .register(registry)
                .increment();
    }

    private static String safeTag(String v) {
        return v == null || v.isBlank() ? "unknown" : v;
    }
}
