package com.bank.loan.advisory.kafka;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvisoryConsumedEventRepository extends JpaRepository<AdvisoryConsumedEvent, Long> {

    boolean existsByTopicAndPartitionAndKafkaOffset(String topic, int partition, long kafkaOffset);
}
