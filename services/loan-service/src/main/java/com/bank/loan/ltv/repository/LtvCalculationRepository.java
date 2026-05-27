package com.bank.loan.ltv.repository;

import com.bank.loan.ltv.domain.LtvCalculation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LtvCalculationRepository extends JpaRepository<LtvCalculation, Long> {

    Optional<LtvCalculation> findByColIdAndDeletedAtIsNull(Long colId);
}
