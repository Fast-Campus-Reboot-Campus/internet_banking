package com.bank.loan.certificate.repository;

import com.bank.loan.certificate.domain.LoanCertificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanCertificateRepository extends JpaRepository<LoanCertificate, Long> {

    Optional<LoanCertificate> findByCertIdAndDeletedAtIsNull(Long certId);

    List<LoanCertificate> findByCntrIdAndDeletedAtIsNullOrderByIssuedAtAsc(Long cntrId);
}
