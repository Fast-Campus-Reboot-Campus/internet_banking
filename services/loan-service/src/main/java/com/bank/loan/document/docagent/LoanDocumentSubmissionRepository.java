package com.bank.loan.document.docagent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LoanDocumentSubmissionRepository
        extends JpaRepository<LoanDocumentSubmission, String> {

    List<LoanDocumentSubmission> findByApplId(Long applId);

    Optional<LoanDocumentSubmission> findTopByApplIdAndDocCodeOrderByCreatedAtDesc(
            Long applId, String docCode);

    /** applId 기준 최신 제출 이력 (doc_code별 마지막 결과) */
    @Query("""
        SELECT s FROM LoanDocumentSubmission s
        WHERE s.applId = :applId
          AND s.createdAt = (
              SELECT MAX(s2.createdAt)
              FROM LoanDocumentSubmission s2
              WHERE s2.applId = :applId AND s2.docCode = s.docCode
          )
        """)
    List<LoanDocumentSubmission> findLatestPerDocCode(@Param("applId") Long applId);

    /** NEEDS_RESUBMIT 또는 HOLD 이면서 심사원 미승인인 항목 수 */
    @Query("""
        SELECT COUNT(s) FROM LoanDocumentSubmission s
        WHERE s.applId = :applId
          AND s.verifyStatus IN ('NEEDS_RESUBMIT', 'HOLD')
        """)
    long countUnresolved(@Param("applId") Long applId);
}
