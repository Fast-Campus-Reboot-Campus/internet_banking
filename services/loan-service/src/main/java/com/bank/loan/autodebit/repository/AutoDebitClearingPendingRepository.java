package com.bank.loan.autodebit.repository;

import com.bank.loan.autodebit.domain.AutoDebitClearingPending;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AutoDebitClearingPendingRepository
        extends JpaRepository<AutoDebitClearingPending, Long> {

    Optional<AutoDebitClearingPending> findByPiId(String piId);
}
