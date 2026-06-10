package com.bank.deposit.dto.response;

import com.bank.deposit.domain.entity.Product;
import com.bank.deposit.domain.entity.TargetGroup;
import com.bank.deposit.domain.enums.ProductStatus;
import com.bank.deposit.domain.enums.ProductType;

import java.math.BigDecimal;
import java.util.List;

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
        ProductStatus productStatus,
        List<TargetGroupInfo> targetGroups
) {
    public record TargetGroupInfo(
            Long targetGroupId,
            String targetGroupName,
            Integer minAge,
            Integer maxAge
    ) {
        public static TargetGroupInfo from(TargetGroup tg) {
            return new TargetGroupInfo(tg.getTargetGroupId(), tg.getTargetGroupName(), tg.getMinAge(), tg.getMaxAge());
        }
    }

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
                product.getProductStatus(),
                List.of()
        );
    }

    public static ProductResponse from(Product product, BigDecimal bestRate) {
        return from(product, bestRate, List.of());
    }

    public static ProductResponse from(Product product, BigDecimal bestRate, List<TargetGroupInfo> targetGroups) {
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
                product.getProductStatus(),
                targetGroups
        );
    }
}
