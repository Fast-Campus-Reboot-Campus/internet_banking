package com.bank.loan.dsr.repository;

import com.bank.loan.dsr.domain.DsrCalculation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DsrCalculationRepository extends JpaRepository<DsrCalculation, Long> {

    Optional<DsrCalculation> findByApplIdAndDeletedAtIsNull(Long applId);
}
