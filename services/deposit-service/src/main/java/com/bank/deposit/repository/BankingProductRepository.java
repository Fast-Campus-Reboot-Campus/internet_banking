package com.bank.deposit.repository;

import com.bank.deposit.domain.entity.BankingProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BankingProductRepository extends JpaRepository<BankingProduct, Long> {
    Optional<BankingProduct> findByProductId(Long productId);
    boolean existsByProductId(Long productId);
}
