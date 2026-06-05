package com.bank.loan.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * loan-service 도메인 토픽 명시적 생성.
 *
 * KAFKA_AUTO_CREATE_TOPICS_ENABLE=false 환경에서 서비스 기동 시 토픽이 없으면 직접 생성.
 * 이미 존재하면 무시. replication-factor 는 kafka.replication-factor 프로퍼티로 환경별 주입.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoanTopicInitializer {

    private static final int    PARTITIONS     = 3;
    private static final String RETENTION_7D   = String.valueOf(7L * 24 * 60 * 60 * 1000);

    private static final String TOPIC_LOAN_DOMAIN_EVENTS = "loan-domain-events";
    private static final String TOPIC_DOC_AGENT_ROUTED   = "doc-agent.routed";
    private static final String TOPIC_DOC_AGENT_FRAUD    = "doc-agent.fraud.audit";

    @Value("${kafka.replication-factor:1}")  // dev: 1 / prd: 3
    private short replicationFactor;

    private final KafkaAdmin kafkaAdmin;

    @PostConstruct
    public void createTopics() {
        List<NewTopic> topics = List.of(
                topic(TOPIC_LOAN_DOMAIN_EVENTS),
                topic(TOPIC_DOC_AGENT_ROUTED),
                topic(TOPIC_DOC_AGENT_FRAUD)
        );

        try (AdminClient client = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            Set<String> existing = client.listTopics().names().get();
            List<NewTopic> toCreate = topics.stream()
                    .filter(t -> !existing.contains(t.name()))
                    .toList();

            if (toCreate.isEmpty()) {
                log.info("[loan-topic] 모든 토픽이 이미 존재합니다.");
                return;
            }

            client.createTopics(toCreate).all().get();
            toCreate.forEach(t -> log.info("[loan-topic] 생성 완료 — name={} partitions={} retention={}ms",
                    t.name(), t.numPartitions(), t.configs().get(TopicConfig.RETENTION_MS_CONFIG)));
        } catch (ExecutionException | InterruptedException e) {
            if (e.getCause() instanceof org.apache.kafka.common.errors.TopicExistsException) {
                log.info("[loan-topic] 이미 존재하는 토픽 포함 — 무시");
            } else {
                log.error("[loan-topic] 토픽 생성 실패 — 서비스는 계속 기동", e);
            }
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private NewTopic topic(String name) {
        return new NewTopic(name, PARTITIONS, replicationFactor)
                .configs(Map.of(
                        TopicConfig.RETENTION_MS_CONFIG,   RETENTION_7D,
                        TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE
                ));
    }
}
