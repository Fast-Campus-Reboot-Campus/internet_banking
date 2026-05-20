package com.bank.deposit.repository;

import com.bank.deposit.domain.entity.ProductInterestRate;
import com.bank.deposit.domain.enums.RateType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductInterestRateRepository extends JpaRepository<ProductInterestRate, Long> {
    List<ProductInterestRate> findByProductId(Long productId);
    List<ProductInterestRate> findByProductIdAndIsActive(Long productId, Boolean isActive);
    List<ProductInterestRate> findByProductIdAndRateType(Long productId, RateType rateType);
}
