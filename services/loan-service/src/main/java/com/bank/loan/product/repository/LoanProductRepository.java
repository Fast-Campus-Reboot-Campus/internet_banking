package com.bank.loan.product.repository;

import com.bank.loan.product.domain.LoanProduct;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface LoanProductRepository extends JpaRepository<LoanProduct, Long>, JpaSpecificationExecutor<LoanProduct> {

    Optional<LoanProduct> findByProdCdAndDeletedAtIsNull(String prodCd);

    Optional<LoanProduct> findByProdIdAndDeletedAtIsNull(Long prodId);

    boolean existsByProdCdAndDeletedAtIsNull(String prodCd);

    /** common_db 미동기화 상품 — 백필 배치 핫패스. */
    List<LoanProduct> findByProductIdIsNullAndDeletedAtIsNullOrderByProdIdAsc(Pageable pageable);

    /** common_db 동기화 후 브리지 컬럼 백필. */
    @Modifying
    @Transactional
    @Query("UPDATE LoanProduct p SET p.productId = :commonId WHERE p.prodId = :prodId")
    int updateProductId(@Param("prodId") Long prodId, @Param("commonId") Long commonId);
}
