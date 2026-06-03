package com.bank.customer.party.repository;

import com.bank.customer.party.domain.ComplianceInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ComplianceInfoRepository extends JpaRepository<ComplianceInfo, Long> {

    Optional<ComplianceInfo> findByPartyIdAndDeletedAtIsNull(Long partyId);

    /** 제재 대상자 목록 (직원 모니터링용) */
    @Query("""
            SELECT c FROM ComplianceInfo c
            WHERE c.isSanctionedYn = 'T'
              AND c.deletedAt IS NULL
            """)
    List<ComplianceInfo> findAllSanctioned();

    /** KYC 만료 예정 목록 */
    @Query("""
            SELECT c FROM ComplianceInfo c
            WHERE c.kycExpiryDate <= :targetDate
              AND c.kycStatusCode = 'COMPLETED'
              AND c.deletedAt IS NULL
            """)
    List<ComplianceInfo> findKycExpiringBefore(
            @org.springframework.data.repository.query.Param("targetDate") String targetDate);
}
