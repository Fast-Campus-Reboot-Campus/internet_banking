package com.bank.loan.maturity.repository;

import com.bank.loan.maturity.domain.Maturity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MaturityRepository extends JpaRepository<Maturity, Long> {

    Optional<Maturity> findByCntrIdAndDeletedAtIsNull(Long cntrId);
}
