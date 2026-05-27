package com.bank.loan.advisory.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterClientQuotasResult;
import org.apache.kafka.clients.admin.DescribeClientQuotasResult;
import org.apache.kafka.common.quota.ClientQuotaAlteration;
import org.apache.kafka.common.quota.ClientQuotaEntity;
import org.apache.kafka.common.quota.ClientQuotaFilter;
import org.apache.kafka.common.quota.ClientQuotaFilterComponent;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * L5 실험 — Kafka Client Quota 설정·조회·해제.
 *
 * Kafka 브로커는 client-id 또는 user principal 단위로 처리량(bytes/s)과
 * CPU 요청 비율을 제한할 수 있다. 이 서비스는 AdminClient로 동적 quota를
 * 설정하고 즉시 효과를 확인할 수 있다 (브로커 재기동 불필요).
 *
 * 실험 관찰:
 *   - throttle 발생 시 producer.metrics()[throttle-time-avg] 상승
 *   - kafka-consumer-groups.sh로 consumer lag 증가 확인
 *   - kafka-configs.sh --entity-type clients --describe 로 현재 quota 확인
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdvisoryKafkaQuotaManager {

    // advisory producer/consumer client-id (AdvisoryKafkaProducerConfig, AdvisoryKafkaConsumerConfig)
    public static final String CLIENT_ID_PRODUCER = "advisory-producer";
    public static final String CLIENT_ID_CONSUMER = "advisory-quarantine-notifier";

    // Quota 종류
    public static final String QUOTA_PRODUCER_BYTES  = "producer_byte_rate";  // bytes/s
    public static final String QUOTA_CONSUMER_BYTES  = "consumer_byte_rate";  // bytes/s
    public static final String QUOTA_REQUEST_PERCENT = "request_percentage";   // % of thread time

    private final KafkaAdmin kafkaAdmin;

    /**
     * 특정 client-id에 producer/consumer byte rate 및 request percentage quota 설정.
     * null로 전달한 항목은 변경하지 않는다.
     */
    public void setClientQuota(String clientId,
                               Double producerByteRate,
                               Double consumerByteRate,
                               Double requestPercentage) throws ExecutionException, InterruptedException {
        ClientQuotaEntity entity = clientEntity(clientId);
        List<ClientQuotaAlteration.Op> ops = new ArrayList<>();
        if (producerByteRate  != null) ops.add(new ClientQuotaAlteration.Op(QUOTA_PRODUCER_BYTES,  producerByteRate));
        if (consumerByteRate  != null) ops.add(new ClientQuotaAlteration.Op(QUOTA_CONSUMER_BYTES,  consumerByteRate));
        if (requestPercentage != null) ops.add(new ClientQuotaAlteration.Op(QUOTA_REQUEST_PERCENT, requestPercentage));

        alter(entity, ops);
        log.info("[quota] 설정 — clientId={} producer={}B/s consumer={}B/s request={}%",
                clientId, producerByteRate, consumerByteRate, requestPercentage);
    }

    /**
     * 특정 client-id의 quota를 모두 해제 (브로커 기본값 무제한으로 복귀).
     */
    public void removeClientQuota(String clientId) throws ExecutionException, InterruptedException {
        ClientQuotaEntity entity = clientEntity(clientId);
        List<ClientQuotaAlteration.Op> ops = List.of(
                new ClientQuotaAlteration.Op(QUOTA_PRODUCER_BYTES,  null),
                new ClientQuotaAlteration.Op(QUOTA_CONSUMER_BYTES,  null),
                new ClientQuotaAlteration.Op(QUOTA_REQUEST_PERCENT, null)
        );
        alter(entity, ops);
        log.info("[quota] 해제 — clientId={}", clientId);
    }

    /**
     * 현재 설정된 모든 client-id 기준 quota를 반환.
     * Map<clientId, Map<quotaKey, value>>
     */
    public Map<String, Map<String, Double>> describeAllQuotas() throws ExecutionException, InterruptedException {
        try (AdminClient client = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            DescribeClientQuotasResult result = client.describeClientQuotas(
                    ClientQuotaFilter.contains(List.of(
                            ClientQuotaFilterComponent.ofEntityType(ClientQuotaEntity.CLIENT_ID)
                    ))
            );
            Map<ClientQuotaEntity, Map<String, Double>> raw = result.entities().get();
            Map<String, Map<String, Double>> out = new HashMap<>();
            raw.forEach((entity, quotas) -> {
                String clientId = entity.entries().getOrDefault(ClientQuotaEntity.CLIENT_ID, "<default>");
                out.put(clientId, quotas);
            });
            return out;
        }
    }

    /**
     * 특정 client-id의 quota만 조회.
     */
    public Map<String, Double> describeClientQuota(String clientId) throws ExecutionException, InterruptedException {
        try (AdminClient client = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            DescribeClientQuotasResult result = client.describeClientQuotas(
                    ClientQuotaFilter.containsOnly(List.of(
                            ClientQuotaFilterComponent.ofEntity(ClientQuotaEntity.CLIENT_ID, clientId)
                    ))
            );
            Map<ClientQuotaEntity, Map<String, Double>> raw = result.entities().get();
            return raw.values().stream().findFirst().orElse(Collections.emptyMap());
        }
    }

    private void alter(ClientQuotaEntity entity, Collection<ClientQuotaAlteration.Op> ops)
            throws ExecutionException, InterruptedException {
        try (AdminClient client = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            AlterClientQuotasResult result = client.alterClientQuotas(
                    List.of(new ClientQuotaAlteration(entity, ops))
            );
            result.all().get();
        }
    }

    private static ClientQuotaEntity clientEntity(String clientId) {
        return new ClientQuotaEntity(Map.of(ClientQuotaEntity.CLIENT_ID, clientId));
    }

    public record QuotaInfo(String clientId, Map<String, Double> quotas) {}
}
