package com.bank.loan.advisory.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * L4 파티셔너 실험 — hot partition 시뮬레이터.
 *
 * reviewerId를 key로 사용할 때 특정 심사관 ID(1, 2, 100)에 메시지가 집중되는
 * hot partition 현상을 재현한다.
 *
 * 실험 순서:
 *   1. use-skew-aware-partitioner=false 상태에서 /internal/advisory/skew-sim 호출
 *      → kafka-consumer-groups.sh 또는 kafka-get-offsets.sh로 파티션별 메시지 수 확인
 *      → hot-key(1, 2, 100)가 한 파티션에 쏠리는 것 관찰
 *
 *   2. use-skew-aware-partitioner=true 재기동 후 동일 호출
 *      → hot-key가 여러 파티션에 분산되는 것 관찰
 *
 *   3. 학습 결론: 순서 보장이 깨지는 trade-off 인지
 *
 * 관찰 명령:
 *   docker exec ib-kafka /opt/kafka/bin/kafka-run-class.sh \
 *     kafka.tools.GetOffsetShell \
 *     --bootstrap-server localhost:9092 \
 *     --topic advisory.test.skew.v1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdvisorySkewSimulator {

    // hot-key 비율: 전체 메시지의 80%가 reviewerId 1, 2, 100에서 발생 (파레토 법칙 모사)
    private static final Map<String, Integer> REVIEWER_WEIGHTS = Map.of(
            "1",   400,   // 40%
            "2",   200,   // 20%
            "100", 200,   // 20%
            "50",   50,   // 5%
            "99",   50,   // 5%
            "999", 100    // 10% (일반 심사관)
    );

    @Qualifier("advisoryKafkaTemplate")
    private final KafkaTemplate<String, String> kafkaTemplate;

    public SimulationResult simulate(int totalMessages) {
        log.info("[skew-sim] 시작 — totalMessages={}", totalMessages);

        String[] keys = buildWeightedKeys();
        int[] partitionCounts = new int[3];

        for (int i = 0; i < totalMessages; i++) {
            String reviewerId = keys[ThreadLocalRandom.current().nextInt(keys.length)];
            String payload = String.format(
                    "{\"seq\":%d,\"reviewerId\":\"%s\",\"eventType\":\"SKEW_TEST\"}", i, reviewerId);

            kafkaTemplate.send(AdvisoryTopicInitializer.TOPIC_SKEW_TEST, reviewerId, payload)
                    .whenComplete((result, ex) -> {
                        if (ex == null && result != null) {
                            int p = result.getRecordMetadata().partition();
                            synchronized (partitionCounts) { partitionCounts[p]++; }
                        }
                    });
        }

        // 비동기 발행이므로 간략 결과만 반환. 정확한 분포는 kafka-get-offsets로 확인.
        log.info("[skew-sim] 발행 완료 — {}건. kafka-get-offsets로 파티션별 분포 확인 필요.", totalMessages);
        return new SimulationResult(totalMessages, REVIEWER_WEIGHTS);
    }

    private String[] buildWeightedKeys() {
        int total = REVIEWER_WEIGHTS.values().stream().mapToInt(Integer::intValue).sum();
        String[] keys = new String[total];
        int idx = 0;
        for (Map.Entry<String, Integer> entry : REVIEWER_WEIGHTS.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                keys[idx++] = entry.getKey();
            }
        }
        return keys;
    }

    public record SimulationResult(int totalMessages, Map<String, Integer> reviewerWeights) {}
}
