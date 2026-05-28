package com.bank.loan.rag.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

/** D3-2 증분 조회용 리포지토리. */
public interface LoanReviewRepository extends JpaRepository<LoanReview, Long> {

    /**
     * 특정 시각 이후 갱신된 결정 완료 케이스 조회 (soft-deleted 제외).
     * SimilarCaseExporter 가 전일 증분 범위 지정에 사용.
     */
    @Query("""
            SELECT lr FROM LoanReview lr
            WHERE lr.updatedAt >= :since
              AND lr.revDecisionCd IS NOT NULL
              AND lr.deletedAt IS NULL
            ORDER BY lr.updatedAt ASC
            """)
    List<LoanReview> findExportable(@Param("since") OffsetDateTime since);
}
