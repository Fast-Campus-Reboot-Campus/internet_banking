package com.bank.customer.party.repository;

import com.bank.customer.party.domain.SanctionScreeningHit;
import com.bank.customer.party.dto.SanctionHitResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SanctionScreeningHitRepository extends JpaRepository<SanctionScreeningHit, Long> {

    /**
     * 제재 스크리닝 검토 대기 큐 — screening_status='PENDING'. 이름·인적사항을 Party·PartyPerson 조인으로 가져온다.
     * 일치율 높은 순(우선검토) 정렬.
     */
    @Query(value = """
            SELECT new com.bank.customer.party.dto.SanctionHitResponse(
                h.sanctionScreeningHitId, h.partyId, p.partyName, pp.birthDate, pp.nationalityCode,
                h.hitTypeCode, h.matchRate, h.screeningStatusCode, h.detectedAt)
            FROM SanctionScreeningHit h
            JOIN Party p ON p.partyId = h.partyId
            LEFT JOIN PartyPerson pp ON pp.partyId = h.partyId
            WHERE h.screeningStatusCode = 'PENDING'
              AND h.deletedAt IS NULL
            ORDER BY h.matchRate DESC, h.sanctionScreeningHitId DESC
            """,
            countQuery = """
            SELECT COUNT(h)
            FROM SanctionScreeningHit h
            WHERE h.screeningStatusCode = 'PENDING'
              AND h.deletedAt IS NULL
            """)
    Page<SanctionHitResponse> searchPending(Pageable pageable);
}
