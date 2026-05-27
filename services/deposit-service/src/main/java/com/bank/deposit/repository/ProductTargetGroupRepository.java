package com.bank.deposit.repository;

import com.bank.deposit.domain.entity.ProductTargetGroup;
import com.bank.deposit.domain.entity.ProductTargetGroupId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductTargetGroupRepository extends JpaRepository<ProductTargetGroup, ProductTargetGroupId> {
    List<ProductTargetGroup> findByIdProductId(Long productId);
    void deleteById(ProductTargetGroupId id);
}
