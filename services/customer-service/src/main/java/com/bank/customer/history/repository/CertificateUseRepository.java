package com.bank.customer.history.repository;

import com.bank.customer.history.domain.CertificateUse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

public interface CertificateUseRepository extends JpaRepository<CertificateUse, Long> {

    /** 특정 인증서의 최근 N분 내 실패 횟수 — FDS 탐지용 (인증서 기준) */
    @Query("""
            SELECT COUNT(cu)
            FROM CertificateUse cu
            WHERE cu.certificateId = :certificateId
              AND cu.verificationResultCode <> 'SUCCESS'
              AND cu.usedAt >= :since
            """)
    long countFailuresSince(@Param("certificateId") Long certificateId,
                            @Param("since") OffsetDateTime since);

    /** 특정 고객의 최근 N분 내 인증서 실패 횟수 — FDS CERT_FAILURE_COUNT 룰용 */
    @Query("""
            SELECT COUNT(cu)
            FROM CertificateUse cu
            WHERE cu.customerId = :customerId
              AND cu.verificationResultCode <> 'SUCCESS'
              AND cu.usedAt >= :since
            """)
    long countCertFailuresByCustomerSince(@Param("customerId") Long customerId,
                                          @Param("since") OffsetDateTime since);
}
