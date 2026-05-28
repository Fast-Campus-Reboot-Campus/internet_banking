package com.bank.ai.review.observability;

import com.bank.ai.review.dto.AutoReviewRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ReviewMetrics 단위 테스트 — SimpleMeterRegistry 로 메트릭 동작 검증.
 */
class ReviewMetricsTest {

    private MeterRegistry registry;
    private ReviewMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics  = new ReviewMetrics(registry);
    }

    @Test
    void recordDecision_은_decision_modelVersion_태그로_Counter_생성() {
        metrics.recordDecision("APPROVE", 0.87, "v2");
        metrics.recordDecision("APPROVE", 0.91, "v2");
        metrics.recordDecision("REJECT",  0.73, "v2");

        Counter approve = registry.find("review.decision.total")
                .tag("decision", "APPROVE").tag("modelVersion", "v2").counter();
        Counter reject  = registry.find("review.decision.total")
                .tag("decision", "REJECT").tag("modelVersion", "v2").counter();

        assertThat(approve).isNotNull();
        assertThat(approve.count()).isEqualTo(2.0);
        assertThat(reject).isNotNull();
        assertThat(reject.count()).isEqualTo(1.0);
    }

    @Test
    void recordDecision_은_null_modelVersion이면_unknown_태그() {
        metrics.recordDecision("REJECT", 0.65, null);

        Counter c = registry.find("review.decision.total")
                .tag("decision", "REJECT").tag("modelVersion", "unknown").counter();
        assertThat(c).isNotNull();
        assertThat(c.count()).isEqualTo(1.0);
    }

    @Test
    void stopTimer_는_modelVersion_태그로_Timer_기록() {
        Timer.Sample sample = metrics.startTimer();
        metrics.stopTimer(sample, "v3");

        Timer t = registry.find("review.duration").tag("modelVersion", "v3").timer();
        assertThat(t).isNotNull();
        assertThat(t.count()).isEqualTo(1);
    }

    @Test
    void recordInferenceError_는_에러_카운터_증가() {
        metrics.recordInferenceError();
        metrics.recordInferenceError();

        Counter c = registry.find("review.inference.error.total").counter();
        assertThat(c).isNotNull();
        assertThat(c.count()).isEqualTo(2.0);
    }

    @Test
    void recordInputQuality_는_null_필드에_결측치_카운터_증가() {
        AutoReviewRequest req = requestWithNullFinancials();
        metrics.recordInputQuality(req);

        Counter dsrMissing = registry.find("review.input.missing.total")
                .tag("field", "dsr").counter();
        Counter ltvMissing = registry.find("review.input.missing.total")
                .tag("field", "ltv").counter();

        assertThat(dsrMissing).isNotNull();
        assertThat(dsrMissing.count()).isEqualTo(1.0);
        assertThat(ltvMissing).isNotNull();
        assertThat(ltvMissing.count()).isEqualTo(1.0);
    }

    @Test
    void recordInputQuality_는_범위_초과_값에_이상치_카운터_증가() {
        AutoReviewRequest req = requestWithOutliers();
        metrics.recordInputQuality(req);

        Counter dsrOutlier     = registry.find("review.input.outlier.total").tag("field", "dsr").counter();
        Counter creditOutlier  = registry.find("review.input.outlier.total").tag("field", "credit_score_proxy").counter();

        assertThat(dsrOutlier).isNotNull();
        assertThat(dsrOutlier.count()).isEqualTo(1.0);
        assertThat(creditOutlier).isNotNull();
        assertThat(creditOutlier.count()).isEqualTo(1.0);
    }

    @Test
    void recordInputQuality_는_정상_입력에_이상치_카운터_미증가() {
        AutoReviewRequest req = normalRequest();
        metrics.recordInputQuality(req);

        Counter c = registry.find("review.input.outlier.total").counter();
        assertThat(c).isNull();
    }

    // ---- 테스트 픽스처 ----

    private static AutoReviewRequest requestWithNullFinancials() {
        return new AutoReviewRequest(
                "M", 35, null, null, null, null, null, null,
                null, null, null, null,
                3, 50000L, 100000L, 20000L, 0L, 20000L,
                null,   // dsr — null
                null,   // ltv — null
                3000L, 500L, 0, 700,
                "P001", 30000L, 12, "LIVING", false
        );
    }

    private static AutoReviewRequest requestWithOutliers() {
        return new AutoReviewRequest(
                "F", 40, null, null, null, null, null, null,
                null, null, null, null,
                2, 60000L, 200000L, 30000L, 10000L, 20000L,
                1.5,    // dsr — 이상치 (> 1.0)
                0.6,
                4000L, 600L, 1, -50,  // creditScoreProxy — 이상치 (< 0)
                "P002", 50000L, 24, "BUSINESS", false
        );
    }

    private static AutoReviewRequest normalRequest() {
        return new AutoReviewRequest(
                "M", 30, null, null, null, null, null, null,
                null, null, null, null,
                4, 80000L, 300000L, 10000L, 0L, 10000L,
                0.3, 0.5,
                5000L, 700L, 0, 750,
                "P001", 20000L, 12, "LIVING", false
        );
    }
}
