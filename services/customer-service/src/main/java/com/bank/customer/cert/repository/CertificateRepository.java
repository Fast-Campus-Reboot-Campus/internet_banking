package com.bank.customer.cert.repository;

import com.bank.customer.cert.domain.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findByCertificateSerialNumberAndDeletedAtIsNull(String serialNumber);
}
