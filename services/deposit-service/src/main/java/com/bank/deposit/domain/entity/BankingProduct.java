package com.bank.deposit.domain.entity;

import com.bank.deposit.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "banking_products")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BankingProduct extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bankingProductId;

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Column(name = "banking_product_type", length = 50, nullable = false)
    private String bankingProductType;

    @Column(name = "min_join_amount", precision = 18, scale = 2)
    private BigDecimal minJoinAmount;

    @Column(name = "max_join_amount", precision = 18, scale = 2)
    private BigDecimal maxJoinAmount;

    @Column(name = "min_contract_period_month")
    private Integer minContractPeriodMonth;

    @Column(name = "max_contract_period_month")
    private Integer maxContractPeriodMonth;

    @Column(name = "auto_transfer_available_yn", nullable = false)
    @Builder.Default
    private Boolean autoTransferAvailableYn = false;

    @Column(name = "auto_renewal_available_yn", nullable = false)
    @Builder.Default
    private Boolean autoRenewalAvailableYn = false;

    public void update(String bankingProductType, BigDecimal minJoinAmount, BigDecimal maxJoinAmount,
                       Integer minContractPeriodMonth, Integer maxContractPeriodMonth,
                       Boolean autoTransferAvailableYn, Boolean autoRenewalAvailableYn) {
        if (bankingProductType != null) this.bankingProductType = bankingProductType;
        if (minJoinAmount != null) this.minJoinAmount = minJoinAmount;
        if (maxJoinAmount != null) this.maxJoinAmount = maxJoinAmount;
        if (minContractPeriodMonth != null) this.minContractPeriodMonth = minContractPeriodMonth;
        if (maxContractPeriodMonth != null) this.maxContractPeriodMonth = maxContractPeriodMonth;
        if (autoTransferAvailableYn != null) this.autoTransferAvailableYn = autoTransferAvailableYn;
        if (autoRenewalAvailableYn != null) this.autoRenewalAvailableYn = autoRenewalAvailableYn;
    }
}
