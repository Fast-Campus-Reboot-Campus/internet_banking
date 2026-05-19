package com.bank.loan.application.domain;

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

import java.time.OffsetDateTime;

/**
 * 대출 신청. ERD STAGE 2 LOAN_APPLICATION 매핑.
 */
@Getter
@Entity
@Table(name = "loan_application")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LoanApplication extends BaseEntity {

    public static final String STATUS_SUBMITTED   = "SUBMITTED";
    public static final String STATUS_PRESCREENED = "PRESCREENED";
    public static final String STATUS_REVIEWING   = "REVIEWING";
    public static final String STATUS_APPROVED    = "APPROVED";
    public static final String STATUS_REJECTED    = "REJECTED";
    public static final String STATUS_CANCELED    = "CANCELED";
    public static final String STATUS_WITHDRAWN   = "WITHDRAWN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "appl_id")
    private Long applId;

    @Column(name = "appl_no", nullable = false, length = 30, unique = true)
    private String applNo;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "prod_id", nullable = false)
    private Long prodId;

    @Column(name = "channel_cd", nullable = false, length = 50)
    private String channelCd;

    @Column(name = "requested_amount", nullable = false)
    private Long requestedAmount;

    @Column(name = "requested_period_mo", nullable = false)
    private Integer requestedPeriodMo;

    @Column(name = "loan_purpose_cd", length = 50)
    private String loanPurposeCd;

    @Column(name = "repayment_method_cd", nullable = false, length = 50)
    private String repaymentMethodCd;

    @Column(name = "estimated_income_amt")
    private Long estimatedIncomeAmt;

    @Column(name = "employment_type_cd", length = 50)
    private String employmentTypeCd;

    @Column(name = "appl_status_cd", nullable = false, length = 50)
    private String applStatusCd;

    @Column(name = "applied_at", nullable = false)
    private OffsetDateTime appliedAt;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Column(name = "device", length = 200)
    private String device;

    @Column(name = "idempotency_key", length = 100, unique = true)
    private String idempotencyKey;
}
