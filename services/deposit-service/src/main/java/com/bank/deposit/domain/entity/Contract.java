package com.bank.deposit.domain.entity;

import com.bank.deposit.common.BaseEntity;
import com.bank.deposit.domain.enums.ContractStatus;
import com.bank.deposit.domain.enums.JoinChannel;
import com.bank.deposit.domain.enums.TaxBenefitType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "deposit_contracts")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Contract extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long contractId;

    @Column(name = "contract_number", length = 50, nullable = false, unique = true)
    private String contractNumber;

    @Column(name = "customer_id", length = 30, nullable = false)
    private String customerId;

    @Column(name = "banking_product_id", nullable = false)
    private Long productId;

    @Column(name = "is_monthly_payment", nullable = false)
    @Builder.Default
    private Boolean isMonthlyPayment = false;

    @Column(name = "payment_count_total")
    private Integer paymentCountTotal;

    /** 월 납입일 (YYYYMMDD 또는 DD 형식 최대 6자리, DB: VARCHAR(6)) */
    @Column(name = "monthly_payment_day", columnDefinition = "VARCHAR(6)")
    private String monthlyPaymentDay;

    @Column(name = "join_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal joinAmount;

    @Column(name = "contract_interest_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal contractInterestRate;

    @Column(name = "total_preferential_rate", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalPreferentialRate = BigDecimal.ZERO;

    @Column(name = "final_interest_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal finalInterestRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_benefit_type", nullable = false)
    @Builder.Default
    private TaxBenefitType taxBenefitType = TaxBenefitType.GENERAL;

    @Column(name = "applied_tax_rate", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal appliedTaxRate = new BigDecimal("15.40");

    @Column(name = "expected_interest_amount", precision = 18, scale = 2)
    private BigDecimal expectedInterestAmount;

    @Column(name = "contract_period_month", nullable = false)
    private Integer contractPeriodMonth;

    @Column(name = "started_at", columnDefinition = "CHAR(8)", nullable = false)
    private String startedAt;

    @Column(name = "maturity_at", columnDefinition = "CHAR(8)")
    private String maturityAt;

    @Column(name = "terminated_at", columnDefinition = "CHAR(8)")
    private String terminatedAt;

    @Column(name = "termination_reason", length = 200)
    private String terminationReason;

    @Column(name = "is_auto_renewal", nullable = false)
    @Builder.Default
    private Boolean isAutoRenewal = false;

    @Column(name = "auto_transfer_enabled", nullable = false)
    @Builder.Default
    private Boolean autoTransferEnabled = false;

    @Column(name = "auto_transfer_day")
    private Integer autoTransferDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_status", nullable = false)
    @Builder.Default
    private ContractStatus contractStatus = ContractStatus.ACTIVE;

    @Column(name = "status_changed_at", columnDefinition = "CHAR(8)")
    private String statusChangedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "join_channel", nullable = false)
    private JoinChannel joinChannel;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "branch_code", length = 20)
    private String branchCode;

    @Column(name = "branch_name", length = 100)
    private String branchName;

    @Column(name = "manager_id")
    private Long managerId;

    @Column(name = "manager_name", length = 100)
    private String managerName;

    @Column(name = "is_proxy_joined", nullable = false)
    @Builder.Default
    private Boolean isProxyJoined = false;

    @Column(name = "is_power_of_attorney_verified", nullable = false)
    @Builder.Default
    private Boolean isPowerOfAttorneyVerified = false;

    @Column(name = "power_of_attorney_file_url", length = 500)
    private String powerOfAttorneyFileUrl;

    @Column(name = "terms_file_url", length = 500)
    private String termsFileUrl;

    @Column(name = "contract_file_url", length = 500)
    private String contractFileUrl;

    public void terminate(String terminatedAt, String reason) {
        this.contractStatus = ContractStatus.TERMINATED;
        this.terminatedAt = terminatedAt;
        this.terminationReason = reason;
        this.statusChangedAt = terminatedAt;
    }

    public void mature(String statusChangedAt) {
        this.contractStatus = ContractStatus.MATURED;
        this.statusChangedAt = statusChangedAt;
    }

    public void changeStatus(ContractStatus status, String statusChangedAt) {
        this.contractStatus = status;
        this.statusChangedAt = statusChangedAt;
    }

    public void updateAutoTransferDay(Integer autoTransferDay) {
        this.autoTransferDay = autoTransferDay;
    }

    public void updateDepositSettings(Boolean autoTransferEnabled, Integer autoTransferDay) {
        if (autoTransferEnabled != null) this.autoTransferEnabled = autoTransferEnabled;
        if (autoTransferDay != null) this.autoTransferDay = autoTransferDay;
    }
}
