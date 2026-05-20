package com.bank.deposit.repository;

import com.bank.deposit.domain.entity.ProductCommonTerm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductCommonTermRepository extends JpaRepository<ProductCommonTerm, Long> {
    List<ProductCommonTerm> findByProductId(Long productId);
    boolean existsByProductIdAndTermsTemplateId(Long productId, Long termsTemplateId);
    void deleteByProductIdAndTermsTemplateId(Long productId, Long termsTemplateId);
}
