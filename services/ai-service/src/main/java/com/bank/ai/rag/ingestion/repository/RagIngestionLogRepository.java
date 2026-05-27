package com.bank.ai.rag.ingestion.repository;

import com.bank.ai.rag.ingestion.domain.RagIngestionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RagIngestionLogRepository extends JpaRepository<RagIngestionLog, Long> {

    List<RagIngestionLog> findAllByDocIdOrderByStartedAtDesc(Long docId);
}
