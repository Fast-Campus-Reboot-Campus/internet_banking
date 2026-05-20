package com.bank.deposit.repository;

import com.bank.deposit.domain.entity.ContractCommonTermsConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractCommonTermsConsentRepository extends JpaRepository<ContractCommonTermsConsent, Long> {
    List<ContractCommonTermsConsent> findByContractId(Long contractId);
}
