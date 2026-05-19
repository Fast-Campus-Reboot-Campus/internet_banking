package com.bank.loan.product.repository;

import com.bank.loan.product.domain.LoanProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {

    Optional<LoanProduct> findByProdCdAndDeletedAtIsNull(String prodCd);

    boolean existsByProdCdAndDeletedAtIsNull(String prodCd);
}
