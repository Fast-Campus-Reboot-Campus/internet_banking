package com.bank.loan.idv.repository;

import com.bank.loan.idv.domain.LoanIdentityVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoanIdentityVerificationRepository
        extends JpaRepository<LoanIdentityVerification, Long> {

    Optional<LoanIdentityVerification> findByIdvIdAndDeletedAtIsNull(Long idvId);

    boolean existsByApplIdAndIdvResultCdAndDeletedAtIsNull(Long applId, String idvResultCd);
}
