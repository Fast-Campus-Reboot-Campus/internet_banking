package com.bank.loan.application.repository;

import com.bank.loan.application.domain.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    Optional<LoanApplication> findByIdempotencyKey(String idempotencyKey);

    Optional<LoanApplication> findByApplIdAndDeletedAtIsNull(Long applId);
}
