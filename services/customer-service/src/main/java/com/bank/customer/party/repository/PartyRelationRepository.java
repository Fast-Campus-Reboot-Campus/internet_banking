package com.bank.customer.party.repository;

import com.bank.customer.party.domain.PartyRelation;
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
}
