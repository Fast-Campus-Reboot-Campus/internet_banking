package com.bank.loan.advisory.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.utils.Utils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hot-key 분산 파티셔너.
 *
 * 문제: reviewerId를 partition key로 쓸 때 특정 심사관(VIP/팀장)에게 데이터가 집중
 *       → 한 파티션에 부하 쏠림 (hot partition)
 *
 * 해결:
 *   - hot-key(HOT_REVIEWER_IDS)에 해당하면 round-robin으로 모든 파티션에 분산
 *   - 그 외는 기본 murmur2 해시 파티셔닝 (같은 key는 같은 파티션 → 순서 보장)
 *
 * 트레이드오프:
 *   - hot-key는 파티션 분산으로 순서 보장이 깨짐
 *   - 1금융권에서 순서가 중요한 도메인(계좌 잔액 변동 등)에는 적용 불가
 *   - advisory 이벤트는 집계/감사 목적이므로 순서 보장 불필요 → 적용 가능
 *
 * 사용: AdvisoryKafkaProducerConfig에서 PARTITIONER_CLASS_CONFIG로 등록
 *       advisory.producer.use-skew-aware-partitioner=true 시 활성화
 */
@Slf4j
public class SkewAwarePartitioner implements Partitioner {

    // 실제 환경에서는 DB나 설정 파일에서 로드. 학습용으로 하드코딩.
    private static final Set<String> HOT_REVIEWER_IDS = Set.of("1", "2", "100");

    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);

    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                         Object value, byte[] valueBytes, Cluster cluster) {
        List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);
        int numPartitions = partitions.size();

        if (keyBytes == null || numPartitions == 1) {
            return 0;
        }

        String keyStr = new String(keyBytes);

        if (HOT_REVIEWER_IDS.contains(keyStr)) {
            // hot-key: round-robin으로 전체 파티션에 분산
            int partition = Math.abs(roundRobinCounter.getAndIncrement() % numPartitions);
            log.debug("[skew-partitioner] hot-key '{}' → round-robin partition={}", keyStr, partition);
            return partition;
        }

        // 일반 key: murmur2 해시 (기본 DefaultPartitioner와 동일)
        return Math.abs(Utils.murmur2(keyBytes)) % numPartitions;
    }

    @Override
    public void close() {}

    @Override
    public void configure(Map<String, ?> configs) {}
}
