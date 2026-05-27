package com.bank.ai.rag.chunk.repository;

import com.bank.ai.rag.chunk.domain.RagChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RagChunkRepository extends JpaRepository<RagChunk, Long> {

    List<RagChunk> findAllByDocIdOrderByChunkSeq(Long docId);

    int countByDocId(Long docId);

    @Modifying
    @Query("DELETE FROM RagChunk c WHERE c.docId = :docId")
    void deleteAllByDocId(@Param("docId") Long docId);
}
