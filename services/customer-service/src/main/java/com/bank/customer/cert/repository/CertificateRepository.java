package com.bank.customer.cert.repository;

import com.bank.customer.cert.domain.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findByCertificateSerialNumberAndDeletedAtIsNull(String serialNumber);

    @Modifying
    @Query("UPDATE Certificate c SET c.certificateStatusCode = 'REVOKED' WHERE c.customerId = :customerId AND c.certificateTypeCode = :certType AND c.certificateStatusCode = 'ACTIVE' AND c.deletedAt IS NULL")
    void revokeAllActive(Long customerId, String certType);

    java.util.List<Certificate> findByCustomerIdAndDeletedAtIsNull(Long customerId);
}
