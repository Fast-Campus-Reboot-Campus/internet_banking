package com.bank.deposit.repository;

import com.bank.deposit.domain.entity.Product;
import com.bank.deposit.domain.enums.ProductStatus;
import com.bank.deposit.domain.enums.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByProductType(ProductType productType);
    List<Product> findByProductStatus(ProductStatus productStatus);
    List<Product> findByProductTypeAndProductStatus(ProductType productType, ProductStatus productStatus);
}
