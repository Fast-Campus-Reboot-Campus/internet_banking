package com.bank.loan.consent.repository;

import com.bank.loan.consent.domain.CreditConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CreditConsentRepository extends JpaRepository<CreditConsent, Long> {

    Optional<CreditConsent> findByCsntIdAndDeletedAtIsNull(Long csntId);
}
