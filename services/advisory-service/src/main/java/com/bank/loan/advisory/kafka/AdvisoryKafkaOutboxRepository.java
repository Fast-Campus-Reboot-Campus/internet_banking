package com.bank.loan.advisory.kafka;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AdvisoryKafkaOutboxRepository extends JpaRepository<AdvisoryKafkaOutboxMessage, Long> {

    @Query("SELECT m FROM AdvisoryKafkaOutboxMessage m WHERE m.status = 'PENDING' ORDER BY m.createdAt ASC")
    List<AdvisoryKafkaOutboxMessage> findPending();
}
