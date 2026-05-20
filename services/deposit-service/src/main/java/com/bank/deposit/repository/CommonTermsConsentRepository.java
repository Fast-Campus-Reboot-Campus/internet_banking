package com.bank.deposit.repository;

import com.bank.deposit.domain.entity.CommonTermsConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommonTermsConsentRepository extends JpaRepository<CommonTermsConsent, Long> {
    List<CommonTermsConsent> findByConsentTargetIdAndBizDivCd(Long consentTargetId, String bizDivCd);
}
