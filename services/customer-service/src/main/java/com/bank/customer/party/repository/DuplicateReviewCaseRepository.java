package com.bank.customer.party.repository;

import com.bank.customer.party.domain.DuplicateReviewCase;
import com.bank.customer.party.dto.DuplicateReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DuplicateReviewCaseRepository extends JpaRepository<DuplicateReviewCase, Long> {

    /**
     * 중복고객 검토 대기 큐 — review_status='PENDING'. 신규·기존 party 이름을 Party 2회 조인으로 가져온다.
     * case_id 역순 고정 정렬.
     */
    @Query(value = """
            SELECT new com.bank.customer.party.dto.DuplicateReviewResponse(
                d.duplicateReviewCaseId, d.newPartyId, np.partyName,
                d.existingPartyId, ep.partyName, d.matchTypeCode, d.reviewStatusCode, d.detectedAt)
            FROM DuplicateReviewCase d
            JOIN Party np ON np.partyId = d.newPartyId
            JOIN Party ep ON ep.partyId = d.existingPartyId
            WHERE d.reviewStatusCode = 'PENDING'
              AND d.deletedAt IS NULL
            ORDER BY d.duplicateReviewCaseId DESC
            """,
            countQuery = """
            SELECT COUNT(d)
            FROM DuplicateReviewCase d
            WHERE d.reviewStatusCode = 'PENDING'
              AND d.deletedAt IS NULL
            """)
    Page<DuplicateReviewResponse> searchPending(Pageable pageable);
}
