package com.bank.customer.party.repository;

import com.bank.customer.party.domain.PartyRelation;
import com.bank.customer.party.dto.AgentReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PartyRelationRepository extends JpaRepository<PartyRelation, Long> {

    /** from 기준 활성 관계 목록 */
    List<PartyRelation> findByFromPartyIdAndRelationEndDateIsNullAndDeletedAtIsNull(Long fromPartyId);

    /** to 기준 활성 관계 목록 */
    List<PartyRelation> findByToPartyIdAndRelationEndDateIsNullAndDeletedAtIsNull(Long toPartyId);

    /** 특정 타입의 활성 관계 존재 여부 */
    boolean existsByFromPartyIdAndToPartyIdAndRelationTypeCodeAndRelationEndDateIsNullAndDeletedAtIsNull(
            Long fromPartyId, Long toPartyId, String relationTypeCode);

    /** 양방향 관계 조회 (from 또는 to가 partyId인 경우) */
    @Query("""
            SELECT pr FROM PartyRelation pr
            WHERE (pr.fromPartyId = :partyId OR pr.toPartyId = :partyId)
              AND pr.relationEndDate IS NULL
              AND pr.deletedAt IS NULL
            """)
    List<PartyRelation> findAllActiveRelations(@Param("partyId") Long partyId);

    /**
     * 대리인 위임장 검토 대기 목록 — review_status='PENDING'. 대리인(toParty) 이름을 Party 조인으로 가져온다.
     * relation_id 역순 고정 정렬.
     */
    @Query(value = """
            SELECT new com.bank.customer.party.dto.AgentReviewResponse(
                pr.relationId, pr.fromPartyId, pr.toPartyId, p.partyName,
                pr.relationTypeCode, pr.relationDetailCode, pr.representationScope,
                pr.proofUrl, pr.relationStartDate, pr.relationReviewStatusCode)
            FROM PartyRelation pr JOIN Party p ON p.partyId = pr.toPartyId
            WHERE pr.relationReviewStatusCode = 'PENDING'
              AND pr.relationEndDate IS NULL
              AND pr.deletedAt IS NULL
            ORDER BY pr.relationId DESC
            """,
            countQuery = """
            SELECT COUNT(pr)
            FROM PartyRelation pr JOIN Party p ON p.partyId = pr.toPartyId
            WHERE pr.relationReviewStatusCode = 'PENDING'
              AND pr.relationEndDate IS NULL
              AND pr.deletedAt IS NULL
            """)
    Page<AgentReviewResponse> searchPendingReviews(Pageable pageable);
}
