package com.bank.loan.product.repository;

import com.bank.loan.product.domain.LoanProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface LoanProductRepository extends JpaRepository<LoanProduct, Long>, JpaSpecificationExecutor<LoanProduct> {

    Optional<LoanProduct> findByProdCdAndDeletedAtIsNull(String prodCd);

    Optional<LoanProduct> findByProdIdAndDeletedAtIsNull(Long prodId);

    boolean existsByProdCdAndDeletedAtIsNull(String prodCd);
}
