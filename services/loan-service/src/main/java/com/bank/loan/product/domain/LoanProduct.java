package com.bank.loan.product.domain;

import com.bank.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대출 상품 마스터. ERD STAGE 1 LOAN_PRODUCT 매핑.
 */
@Getter
@Entity
@Table(name = "loan_product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LoanProduct extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prod_id")
    private Long prodId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "prod_cd", nullable = false, length = 30, unique = true)
    private String prodCd;

    @Column(name = "prod_name", nullable = false, length = 200)
    private String prodName;

    @Column(name = "loan_type_cd", nullable = false, length = 50)
    private String loanTypeCd;

    @Column(name = "target_customer_cd", length = 50)
    private String targetCustomerCd;

    @Column(name = "repayment_method_cd", nullable = false, length = 50)
    private String repaymentMethodCd;

    @Column(name = "rate_type_cd", nullable = false, length = 50)
    private String rateTypeCd;

    @Column(name = "base_rate_bps", nullable = false)
    private Integer baseRateBps;

    @Column(name = "min_rate_bps")
    private Integer minRateBps;

    @Column(name = "max_rate_bps")
    private Integer maxRateBps;

    @Column(name = "min_amount", nullable = false)
    private Long minAmount;

    @Column(name = "max_amount", nullable = false)
    private Long maxAmount;

    @Column(name = "min_period_mo", nullable = false)
    private Integer minPeriodMo;

    @Column(name = "max_period_mo", nullable = false)
    private Integer maxPeriodMo;

    @Column(name = "collateral_required_yn", nullable = false, length = 1)
    private String collateralRequiredYn;

    @Column(name = "guarantor_required_yn", nullable = false, length = 1)
    private String guarantorRequiredYn;

    @Column(name = "sale_start_date", length = 8)
    private String saleStartDate;

    @Column(name = "sale_end_date", length = 8)
    private String saleEndDate;

    @Column(name = "prod_status_cd", nullable = false, length = 50)
    private String prodStatusCd;

    @Column(name = "prod_terms_url", length = 500)
    private String prodTermsUrl;

    @Column(name = "prod_terms_hash", length = 128)
    private String prodTermsHash;

    /**
     * 부분 수정. 각 인자 null 이면 해당 필드는 변경하지 않는다.
     * prodCd / productId 는 식별자라 본 메서드에서 변경 불가.
     */
    public void update(
            String prodName,
            String loanTypeCd, String targetCustomerCd,
            String repaymentMethodCd, String rateTypeCd,
            Integer baseRateBps, Integer minRateBps, Integer maxRateBps,
            Long minAmount, Long maxAmount,
            Integer minPeriodMo, Integer maxPeriodMo,
            String collateralRequiredYn, String guarantorRequiredYn,
            String saleStartDate, String saleEndDate,
            String prodTermsUrl, String prodTermsHash,
            String prodStatusCd
    ) {
        if (prodName != null) this.prodName = prodName;
        if (loanTypeCd != null) this.loanTypeCd = loanTypeCd;
        if (targetCustomerCd != null) this.targetCustomerCd = targetCustomerCd;
        if (repaymentMethodCd != null) this.repaymentMethodCd = repaymentMethodCd;
        if (rateTypeCd != null) this.rateTypeCd = rateTypeCd;
        if (baseRateBps != null) this.baseRateBps = baseRateBps;
        if (minRateBps != null) this.minRateBps = minRateBps;
        if (maxRateBps != null) this.maxRateBps = maxRateBps;
        if (minAmount != null) this.minAmount = minAmount;
        if (maxAmount != null) this.maxAmount = maxAmount;
        if (minPeriodMo != null) this.minPeriodMo = minPeriodMo;
        if (maxPeriodMo != null) this.maxPeriodMo = maxPeriodMo;
        if (collateralRequiredYn != null) this.collateralRequiredYn = collateralRequiredYn;
        if (guarantorRequiredYn != null) this.guarantorRequiredYn = guarantorRequiredYn;
        if (saleStartDate != null) this.saleStartDate = saleStartDate;
        if (saleEndDate != null) this.saleEndDate = saleEndDate;
        if (prodTermsUrl != null) this.prodTermsUrl = prodTermsUrl;
        if (prodTermsHash != null) this.prodTermsHash = prodTermsHash;
        if (prodStatusCd != null) this.prodStatusCd = prodStatusCd;
    }
}
