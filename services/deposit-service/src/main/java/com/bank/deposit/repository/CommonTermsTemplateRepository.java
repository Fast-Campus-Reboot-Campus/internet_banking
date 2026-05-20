package com.bank.deposit.repository;

import com.bank.deposit.domain.entity.CommonTermsTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommonTermsTemplateRepository extends JpaRepository<CommonTermsTemplate, Long> {
    List<CommonTermsTemplate> findByActiveYn(String activeYn);
}
