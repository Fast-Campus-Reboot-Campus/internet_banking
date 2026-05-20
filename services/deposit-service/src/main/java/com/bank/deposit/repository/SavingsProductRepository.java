package com.bank.deposit.repository;

import com.bank.deposit.domain.entity.SavingsProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SavingsProductRepository extends JpaRepository<SavingsProduct, Long> {
    Optional<SavingsProduct> findByProductId(Long productId);
    boolean existsByProductId(Long productId);
}
