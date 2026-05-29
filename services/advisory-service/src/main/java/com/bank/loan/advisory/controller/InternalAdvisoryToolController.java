package com.bank.loan.advisory.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.advisory.dto.CohortStatsResponse;
import com.bank.loan.advisory.dto.PolicyCitationResponse;
import com.bank.loan.advisory.dto.ReviewerHistoryResponse;
import com.bank.loan.advisory.dto.SimilarCaseResponse;
import com.bank.loan.advisory.kafka.AdvisoryKafkaQuotaManager;
import com.bank.loan.advisory.kafka.AdvisorySkewSimulator;
import com.bank.loan.advisory.service.AdvisoryToolQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@Tag(name = "어드바이저리", description = "Advisory - LLM tool-use 전용 조회 (internal)")
@RestController
@RequestMapping("/api/internal/advisory")
@RequiredArgsConstructor
public class InternalAdvisoryToolController {

    private final AdvisoryToolQueryService service;
    private final AdvisorySkewSimulator skewSimulator;
    private final AdvisoryKafkaQuotaManager quotaManager;

    @Operation(summary = "정책 인용 검색",
            description = "query 텍스트로 활성 정책 문서 청크를 벡터 검색한다. LLM get_policy_citation tool 전용.")
    @GetMapping("/policy-citations")
    public ApiResponse<PolicyCitationResponse> getPolicyCitations(
            @RequestParam String query) {
        return ApiResponse.ok(service.queryCitations(query));
    }

    @Operation(summary = "심사관 결정 이력 조회",
            description = "최근 N일간 심사관 결정 이력(승인·거절 건수, 승인율)을 반환한다. LLM get_reviewer_history tool 전용.")
    @GetMapping("/reviewer-history")
    public ApiResponse<ReviewerHistoryResponse> getReviewerHistory(
            @RequestParam Long reviewerId,
            @RequestParam(defaultValue = "90") int days) {
        return ApiResponse.ok(service.queryReviewerHistory(reviewerId, days));
    }

    @Operation(summary = "[L4 실험] Kafka 파티션 skew 시뮬레이션",
            description = "reviewerId를 key로 N건 발행. 파티셔너 전후 파티션 분포 비교용. " +
                    "use-skew-aware-partitioner=false/true 재기동 후 비교. " +
                    "관찰: docker exec ib-kafka /opt/kafka/bin/kafka-run-class.sh " +
                    "kafka.tools.GetOffsetShell --bootstrap-server localhost:9092 --topic advisory.test.skew.v1")
    @PostMapping("/skew-sim")
    public ApiResponse<AdvisorySkewSimulator.SimulationResult> runSkewSimulation(
            @RequestParam(defaultValue = "1000") int messages) {
        return ApiResponse.ok(skewSimulator.simulate(messages));
    }

    @Operation(summary = "유사 과거 사례 검색 (rev_id 기준)",
            description = "rev_id 에 연결된 최신 리포트 기준으로 유사 프로파일 종결 사례를 반환한다. LLM get_similar_cases tool 전용.")
    @GetMapping("/similar-cases")
    public ApiResponse<SimilarCaseResponse> getSimilarCases(
            @RequestParam Long revId,
            @RequestParam(defaultValue = "5") int topK) {
        return ApiResponse.ok(service.querySimilarCasesByRevId(revId, topK));
    }

    @Operation(summary = "코호트 편향 통계 조회",
            description = "특정 코호트의 최근 스냅샷 승인·거절률 통계를 반환한다. LLM get_cohort_stats tool 전용.")
    @GetMapping("/cohort-stats")
    public ApiResponse<CohortStatsResponse> getCohortStats(
            @RequestParam String dimension,
            @RequestParam String value) {
        return ApiResponse.ok(service.queryCohortStats(dimension, value));
    }

    // ---- L5 Quota / Throttling 실험 엔드포인트 ----

    @Operation(summary = "[L5 실험] Kafka Quota 설정",
            description = "특정 client-id에 producer/consumer byte rate 또는 request percentage quota를 설정한다. " +
                    "설정 즉시 브로커에 반영됨 (재기동 불필요). " +
                    "기본 대상: advisory-producer / advisory-quarantine-notifier. " +
                    "관찰: docker exec ib-kafka /opt/kafka/bin/kafka-configs.sh " +
                    "--bootstrap-server localhost:9092 --entity-type clients --describe")
    @PostMapping("/quota")
    public ApiResponse<Map<String, Object>> setQuota(
            @RequestParam(defaultValue = AdvisoryKafkaQuotaManager.CLIENT_ID_PRODUCER) String clientId,
            @RequestParam(required = false) Double producerByteRate,
            @RequestParam(required = false) Double consumerByteRate,
            @RequestParam(required = false) Double requestPercentage)
            throws ExecutionException, InterruptedException {
        quotaManager.setClientQuota(clientId, producerByteRate, consumerByteRate, requestPercentage);
        Map<String, Double> current = quotaManager.describeClientQuota(clientId);
        return ApiResponse.ok(Map.of("clientId", clientId, "appliedQuotas", current));
    }

    @Operation(summary = "[L5 실험] Kafka Quota 조회",
            description = "현재 설정된 모든 client-id 기준 quota를 반환한다. " +
                    "quota가 없는 client는 목록에 나타나지 않는다.")
    @GetMapping("/quota")
    public ApiResponse<Map<String, Map<String, Double>>> describeQuotas()
            throws ExecutionException, InterruptedException {
        return ApiResponse.ok(quotaManager.describeAllQuotas());
    }

    @Operation(summary = "[L5 실험] Kafka Quota 해제",
            description = "특정 client-id의 quota를 모두 제거해 브로커 기본값(무제한)으로 복귀시킨다.")
    @DeleteMapping("/quota")
    public ApiResponse<String> removeQuota(
            @RequestParam(defaultValue = AdvisoryKafkaQuotaManager.CLIENT_ID_PRODUCER) String clientId)
            throws ExecutionException, InterruptedException {
        quotaManager.removeClientQuota(clientId);
        return ApiResponse.ok("quota 해제 완료 — clientId=" + clientId);
    }
}
