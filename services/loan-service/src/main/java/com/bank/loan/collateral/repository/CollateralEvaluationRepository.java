package com.bank.loan.collateral.repository;

import com.bank.loan.collateral.domain.CollateralEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollateralEvaluationRepository extends JpaRepository<CollateralEvaluation, Long> {
}
