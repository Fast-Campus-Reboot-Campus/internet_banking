package com.bank.loan.closure.repository;

import com.bank.loan.closure.domain.LoanClosure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoanClosureRepository extends JpaRepository<LoanClosure, Long> {

    Optional<LoanClosure> findByCntrIdAndDeletedAtIsNull(Long cntrId);
}
