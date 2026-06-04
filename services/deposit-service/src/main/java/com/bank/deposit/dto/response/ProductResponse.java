package com.bank.deposit.dto.response;

import com.bank.deposit.domain.entity.Product;
import com.bank.deposit.domain.enums.ProductStatus;
import com.bank.deposit.domain.enums.ProductType;

import java.math.BigDecimal;

public record ProductResponse(
        Long productId,
        ProductType productType,
        String productName,
        String description,
        Long departmentId,
        BigDecimal baseInterestRate,
        BigDecimal bestRate,
        BigDecimal minJoinAmount,
        BigDecimal maxJoinAmount,
        Integer minPeriodMonth,
        Integer maxPeriodMonth,
        Boolean isEarlyTerminationAllowed,
        Boolean isTaxBenefitAvailable,
        Boolean isAutoRenewalAvailable,
        Boolean isPassbookIssued,
        String releasedAt,
        String endedAt,
        ProductStatus productStatus
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getProductId(),
                product.getProductType(),
                product.getProductName(),
                product.getDescription(),
                product.getDepartmentId(),
                product.getBaseInterestRate(),
                null,
                product.getMinJoinAmount(),
                product.getMaxJoinAmount(),
                product.getMinPeriodMonth(),
                product.getMaxPeriodMonth(),
                product.getIsEarlyTerminationAllowed(),
                product.getIsTaxBenefitAvailable(),
                product.getIsAutoRenewalAvailable(),
                product.getIsPassbookIssued(),
                product.getReleasedAt(),
                product.getEndedAt(),
                product.getProductStatus()
        );
    }

    public static ProductResponse from(Product product, BigDecimal bestRate) {
        return new ProductResponse(
                product.getProductId(),
                product.getProductType(),
                product.getProductName(),
                product.getDescription(),
                product.getDepartmentId(),
                product.getBaseInterestRate(),
                bestRate,
                product.getMinJoinAmount(),
                product.getMaxJoinAmount(),
                product.getMinPeriodMonth(),
                product.getMaxPeriodMonth(),
                product.getIsEarlyTerminationAllowed(),
                product.getIsTaxBenefitAvailable(),
                product.getIsAutoRenewalAvailable(),
                product.getIsPassbookIssued(),
                product.getReleasedAt(),
                product.getEndedAt(),
                product.getProductStatus()
        );
    }
}
