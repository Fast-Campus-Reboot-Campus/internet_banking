package com.bank.ai.rag.document.repository;

import com.bank.ai.rag.document.domain.RagDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RagDocumentRepository extends JpaRepository<RagDocument, Long> {

    Optional<RagDocument> findByDocIdAndDeletedAtIsNull(Long docId);

    Optional<RagDocument> findBySourceUriAndDeletedAtIsNull(String sourceUri);

    Optional<RagDocument> findByChecksumAndDeletedAtIsNull(String checksum);

    List<RagDocument> findAllByDocTypeCdAndDeletedAtIsNull(String docTypeCd);

    /** 아직 인제스트되지 않은 활성 문서 목록 — 스케줄러가 우선 처리. */
    List<RagDocument> findAllByIngestedAtIsNullAndDeletedAtIsNull();

    @Query("""
        SELECT d FROM RagDocument d
        WHERE d.deletedAt IS NULL
          AND (:docTypeCd IS NULL OR d.docTypeCd = :docTypeCd)
          AND (:sensitivityCd IS NULL OR d.sensitivityCd = :sensitivityCd)
          AND (:asOfDate IS NULL
               OR (d.effectiveFrom IS NULL OR d.effectiveFrom <= :asOfDate)
               AND (d.effectiveTo IS NULL OR d.effectiveTo > :asOfDate))
        ORDER BY d.effectiveFrom DESC
        """)
    List<RagDocument> search(@Param("docTypeCd") String docTypeCd,
                             @Param("sensitivityCd") String sensitivityCd,
                             @Param("asOfDate") String asOfDate);
}
