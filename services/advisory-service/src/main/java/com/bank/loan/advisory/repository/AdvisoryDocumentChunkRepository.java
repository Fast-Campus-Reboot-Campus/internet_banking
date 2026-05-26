package com.bank.loan.advisory.repository;

import com.bank.loan.advisory.domain.AdvisoryDocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdvisoryDocumentChunkRepository extends JpaRepository<AdvisoryDocumentChunk, Long> {

    List<AdvisoryDocumentChunk> findByDocIdOrderByChunkSeqAsc(Long docId);

    int countByDocId(Long docId);
}
