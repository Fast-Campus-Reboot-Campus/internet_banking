package com.bank.loan.prescreening.repository;

import com.bank.loan.prescreening.domain.LoanPrescreening;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoanPrescreeningRepository extends JpaRepository<LoanPrescreening, Long> {

    Optional<LoanPrescreening> findByApplIdAndDeletedAtIsNull(Long applId);
}
