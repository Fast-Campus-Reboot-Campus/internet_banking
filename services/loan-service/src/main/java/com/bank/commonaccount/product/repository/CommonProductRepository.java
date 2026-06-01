package com.bank.commonaccount.product.repository;

import com.bank.commonaccount.product.domain.CommonProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommonProductRepository extends JpaRepository<CommonProduct, Long> {

    /** 자연키(product_cd)로 조회 — write-through 멱등 dedupe. */
    Optional<CommonProduct> findByProductCd(String productCd);
}
