package com.bank.ai.review.service;

import com.bank.ai.review.client.InferenceClient;
import com.bank.ai.review.client.dto.InferenceRequest;
import com.bank.ai.review.client.dto.InferenceResponse;
import com.bank.ai.review.dto.AutoReviewRequest;
import com.bank.ai.review.dto.AutoReviewResponse;
import com.bank.ai.review.observability.ReviewMetrics;
import com.bank.common.web.BusinessException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * AutoReviewService 단위 테스트 — InferenceClient mock + SimpleMeterRegistry 로 메트릭 검증.
 */
class AutoReviewServiceTest {

    private InferenceClient    inferenceClient;
    private SimpleMeterRegistry registry;
    private AutoReviewService  service;

    @BeforeEach
    void setUp() {
        inferenceClient = Mockito.mock(InferenceClient.class);
        registry        = new SimpleMeterRegistry();
        service         = new AutoReviewService(inferenceClient, new ReviewMetrics(registry));
    }

    @Test
    void review_정상_응답_시_decision_메트릭_기록() {
        when(inferenceClient.predict(any(InferenceRequest.class)))
                .thenReturn(approveResponse("v2", 0.88));

        AutoReviewResponse res = service.review(normalRequest());

        assertThat(res.decision()).isEqualTo("APPROVE");
        assertThat(res.modelVersion()).isEqualTo("v2");

        Counter decision = registry.find("review.decision.total")
                .tag("decision", "APPROVE").tag("modelVersion", "v2").counter();
        assertThat(decision).isNotNull();
        assertThat(decision.count()).isEqualTo(1.0);

        Timer timer = registry.find("review.duration").tag("modelVersion", "v2").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void review_추론_예외_시_에러_메트릭_기록_후_예외_전파() {
        when(inferenceClient.predict(any()))
                .thenThrow(new BusinessException(com.bank.ai.support.AiErrorCode.INFERENCE_UNAVAILABLE));

        assertThatThrownBy(() -> service.review(normalRequest()))
                .isInstanceOf(BusinessException.class);

        Counter error = registry.find("review.inference.error.total").counter();
        assertThat(error).isNotNull();
        assertThat(error.count()).isEqualTo(1.0);

        // 에러 시에도 타이머는 중지되어야 한다
        Timer timer = registry.find("review.duration").tag("modelVersion", "unknown").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void review_빈_predictions_응답_시_에러_메트릭_기록() {
        when(inferenceClient.predict(any()))
                .thenReturn(new InferenceResponse("v2", List.of()));

        assertThatThrownBy(() -> service.review(normalRequest()))
                .isInstanceOf(BusinessException.class);

        Counter error = registry.find("review.inference.error.total").counter();
        assertThat(error).isNotNull();
        assertThat(error.count()).isEqualTo(1.0);
    }

    @Test
    void review_결측치_입력_시_missing_메트릭_기록() {
        when(inferenceClient.predict(any()))
                .thenReturn(approveResponse("v1", 0.75));

        service.review(requestWithNullDsr());

        Counter missing = registry.find("review.input.missing.total")
                .tag("field", "dsr").counter();
        assertThat(missing).isNotNull();
        assertThat(missing.count()).isEqualTo(1.0);
    }

    @Test
    void review_이상치_입력_시_outlier_메트릭_기록() {
        when(inferenceClient.predict(any()))
                .thenReturn(rejectResponse("v1", 0.82));

        service.review(requestWithOutlierDsr());

        Counter outlier = registry.find("review.input.outlier.total")
                .tag("field", "dsr").counter();
        assertThat(outlier).isNotNull();
        assertThat(outlier.count()).isEqualTo(1.0);
    }

    // ---- 픽스처 ----

    private static InferenceResponse approveResponse(String modelVersion, double score) {
        return new InferenceResponse(modelVersion,
                List.of(new InferenceResponse.Prediction("APPROVE", score,
                        Map.of("APPROVE", score, "CONDITIONAL", 0.08, "REJECT", 1 - score - 0.08))));
    }

    private static InferenceResponse rejectResponse(String modelVersion, double score) {
        return new InferenceResponse(modelVersion,
                List.of(new InferenceResponse.Prediction("REJECT", score,
                        Map.of("REJECT", score, "CONDITIONAL", 0.1, "APPROVE", 1 - score - 0.1))));
    }

    private static AutoReviewRequest normalRequest() {
        return new AutoReviewRequest(
                "M", 35, null, null, null, null, null, null,
                null, null, null, null,
                3, 60000L, 200000L, 15000L, 0L, 15000L,
                0.35, 0.5,
                4000L, 600L, 0, 720,
                "P001", 30000L, 12, "LIVING", false
        );
    }

    private static AutoReviewRequest requestWithNullDsr() {
        return new AutoReviewRequest(
                "F", 28, null, null, null, null, null, null,
                null, null, null, null,
                2, 45000L, 100000L, 10000L, 0L, 10000L,
                null,  // dsr — null
                0.4,
                3000L, 400L, 0, 680,
                "P001", 20000L, 12, "LIVING", false
        );
    }

    private static AutoReviewRequest requestWithOutlierDsr() {
        return new AutoReviewRequest(
                "M", 45, null, null, null, null, null, null,
                null, null, null, null,
                1, 30000L, 50000L, 40000L, 20000L, 20000L,
                1.8,   // dsr — 이상치 (> 1.0)
                0.9,
                2000L, 300L, 2, 400,
                "P002", 50000L, 24, "BUSINESS", true
        );
    }
}
