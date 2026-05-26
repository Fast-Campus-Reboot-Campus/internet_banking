package com.bank.loan.creditevaluation.repository;

import com.bank.loan.creditevaluation.domain.CreditEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CreditEvaluationRepository extends JpaRepository<CreditEvaluation, Long> {

    Optional<CreditEvaluation> findByApplIdAndDeletedAtIsNull(Long applId);
}
