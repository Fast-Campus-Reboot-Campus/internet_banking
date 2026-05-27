package com.bank.loan.advisory.kafka;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * advisory 도메인 토픽 명시적 생성.
 *
 * KAFKA_AUTO_CREATE_TOPICS_ENABLE=false 환경에서 서비스 기동 시 토픽이 없으면 직접 생성.
 * 이미 존재하면 무시. 토픽 설정(partition/retention)은 도메인 요건 기준:
 *   - quarantine: 30일 보존 (감사 요건)
 *   - report:      7일 보존 (알림성)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdvisoryTopicInitializer {

    private static final int PARTITIONS     = 3;
    private static final short REPLICATION  = 1;  // dev: 1 / prd: 3

    // 토픽 이름
    private static final String TOPIC_QUARANTINE     = AdvisoryKafkaOutboxMessage.TOPIC_QUARANTINE;
    private static final String TOPIC_REPORT         = AdvisoryKafkaOutboxMessage.TOPIC_REPORT;
    private static final String TOPIC_QUARANTINE_DLQ = TOPIC_QUARANTINE + ".dlq";
    private static final String TOPIC_REPORT_DLQ     = TOPIC_REPORT     + ".dlq";

    // L4 파티셔너 실험용 토픽 (운영 토픽과 분리)
    public static final String TOPIC_SKEW_TEST = "advisory.test.skew.v1";

    // retention: 30일(quarantine/DLQ), 7일(report), 1일(실험용)
    private static final String RETENTION_30D = String.valueOf(30L * 24 * 60 * 60 * 1000);
    private static final String RETENTION_7D  = String.valueOf(7L  * 24 * 60 * 60 * 1000);
    private static final String RETENTION_1D  = String.valueOf(1L  * 24 * 60 * 60 * 1000);

    private final KafkaAdmin kafkaAdmin;

    @PostConstruct
    public void createTopics() {
        List<NewTopic> topics = List.of(
                topic(TOPIC_QUARANTINE,     RETENTION_30D),
                topic(TOPIC_QUARANTINE_DLQ, RETENTION_30D),
                topic(TOPIC_REPORT,         RETENTION_7D),
                topic(TOPIC_REPORT_DLQ,     RETENTION_7D),
                topic(TOPIC_SKEW_TEST,      RETENTION_1D)   // L4 파티셔너 실험용
        );

        try (AdminClient client = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            Set<String> existing = client.listTopics().names().get();
            List<NewTopic> toCreate = topics.stream()
                    .filter(t -> !existing.contains(t.name()))
                    .toList();

            if (toCreate.isEmpty()) {
                log.info("[advisory-topic] 모든 토픽이 이미 존재합니다.");
                return;
            }

            client.createTopics(toCreate).all().get();
            toCreate.forEach(t -> log.info("[advisory-topic] 생성 완료 — name={} partitions={} retention={}ms",
                    t.name(), t.numPartitions(), t.configs().get(TopicConfig.RETENTION_MS_CONFIG)));
        } catch (ExecutionException | InterruptedException e) {
            if (e.getCause() instanceof org.apache.kafka.common.errors.TopicExistsException) {
                log.info("[advisory-topic] 이미 존재하는 토픽 포함 — 무시");
            } else {
                log.error("[advisory-topic] 토픽 생성 실패 — 서비스는 계속 기동", e);
            }
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static NewTopic topic(String name, String retentionMs) {
        return new NewTopic(name, PARTITIONS, REPLICATION)
                .configs(Map.of(
                        TopicConfig.RETENTION_MS_CONFIG,    retentionMs,
                        TopicConfig.CLEANUP_POLICY_CONFIG,  TopicConfig.CLEANUP_POLICY_DELETE
                ));
    }
}
