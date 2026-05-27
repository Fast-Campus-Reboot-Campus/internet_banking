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
    public static final String STATUS_CONTRACTED  = "CONTRACTED";
    public static final String STATUS_CANCELED    = "CANCELED";
    public static final String STATUS_WITHDRAWN   = "WITHDRAWN";
    public static final String STATUS_EXPIRED     = "EXPIRED";

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

    /**
     * 고객이 본인 신청을 취소할 수 있는 단계.
     * 심사 결과(승인·거절) 이후 또는 이미 취소/철회된 건은 본 메서드로 변경하지 않는다.
     */
    public boolean isCancellable() {
        return STATUS_SUBMITTED.equals(applStatusCd)
                || STATUS_PRESCREENED.equals(applStatusCd)
                || STATUS_REVIEWING.equals(applStatusCd);
    }

    public void cancel() {
        this.applStatusCd = STATUS_CANCELED;
    }

    /** 본심사 통과 후 호출. 추후 본심사 API 가 정식 구현되면 그곳에서 직접 호출. */
    public void markApproved() {
        this.applStatusCd = STATUS_APPROVED;
    }

    public boolean isPrescreenable() {
        return STATUS_SUBMITTED.equals(applStatusCd);
    }

    /** 본심사 가능 상태. 가심사 PASS 후(PRESCREENED) 만 본심사 진행. */
    public boolean isReviewable() {
        return STATUS_PRESCREENED.equals(applStatusCd);
    }

    /** 가심사 PASS 시 호출. SUBMITTED → PRESCREENED. */
    public void markPrescreened() {
        this.applStatusCd = STATUS_PRESCREENED;
    }

    /** 가심사 REJECT / 본심사 거절 등 거절 시 호출. */
    public void markRejected() {
        this.applStatusCd = STATUS_REJECTED;
    }

    public boolean isApproved() {
        return STATUS_APPROVED.equals(this.applStatusCd);
    }

    /** 약정 체결 시 LoanContractService 가 호출. */
    public void markContracted() {
        this.applStatusCd = STATUS_CONTRACTED;
    }

    /** 승인 후 유효기간 경과 시 일배치가 호출. APPROVED → EXPIRED. */
    public void markExpired() {
        this.applStatusCd = STATUS_EXPIRED;
    }

    public String currentStatus() {
        return this.applStatusCd;
    }
}
