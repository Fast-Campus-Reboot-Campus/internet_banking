package com.bank.deposit.repository;

import com.bank.deposit.domain.entity.CommonTermsVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommonTermsVersionRepository extends JpaRepository<CommonTermsVersion, Long> {
    List<CommonTermsVersion> findByTermsTemplateId(Long termsTemplateId);
}
