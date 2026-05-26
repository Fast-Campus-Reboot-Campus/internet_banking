package com.bank.loan.contract.domain;

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
 * 대출 계약. ERD STAGE 6 LOAN_CONTRACT 매핑.
 *
 * 상태 전이:
 *   SIGNED  → ACTIVE (최초 자금 인출 시점)
 *   ACTIVE  → CLOSED (계약 종결)
 *
 * rev_id 는 본심사 API 도입 이전이라 nullable. 도입 후 NOT NULL 로 좁힐 것.
 */
@Getter
@Entity
@Table(name = "loan_contract")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LoanContract extends BaseEntity {

    public static final String STATUS_SIGNED = "SIGNED";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_CLOSED = "CLOSED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cntr_id")
    private Long cntrId;

    @Column(name = "cntr_no", nullable = false, length = 30, unique = true)
    private String cntrNo;

    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "appl_id", nullable = false)
    private Long applId;

    @Column(name = "rev_id")
    private Long revId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "prod_id", nullable = false)
    private Long prodId;

    @Column(name = "contracted_amount", nullable = false)
    private Long contractedAmount;

    @Column(name = "currency_cd", nullable = false, length = 10)
    private String currencyCd;

    @Column(name = "contracted_period_mo", nullable = false)
    private Integer contractedPeriodMo;

    @Column(name = "total_rate_bps", nullable = false)
    private Integer totalRateBps;

    @Column(name = "base_rate_bps", nullable = false)
    private Integer baseRateBps;

    @Column(name = "spread_bps", nullable = false)
    private Integer spreadBps;

    @Column(name = "preferential_rate_bps", nullable = false)
    private Integer preferentialRateBps;

    @Column(name = "rate_type_cd", nullable = false, length = 50)
    private String rateTypeCd;

    @Column(name = "repayment_method_cd", nullable = false, length = 50)
    private String repaymentMethodCd;

    @Column(name = "cntr_status_cd", nullable = false, length = 50)
    private String cntrStatusCd;

    @Column(name = "cntr_start_date", nullable = false, length = 8)
    private String cntrStartDate;

    @Column(name = "cntr_end_date", nullable = false, length = 8)
    private String cntrEndDate;

    @Column(name = "cntr_doc_url", length = 500)
    private String cntrDocUrl;

    @Column(name = "cntr_doc_hash", length = 128)
    private String cntrDocHash;

    @Column(name = "signed_at")
    private OffsetDateTime signedAt;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Column(name = "device", length = 200)
    private String device;

    public boolean isDrawdownAllowed() {
        return STATUS_SIGNED.equals(cntrStatusCd) || STATUS_ACTIVE.equals(cntrStatusCd);
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equals(cntrStatusCd);
    }

    public void markActiveOnFirstDrawdown() {
        if (STATUS_SIGNED.equals(cntrStatusCd)) {
            this.cntrStatusCd = STATUS_ACTIVE;
        }
    }

    public String currentStatus() {
        return cntrStatusCd;
    }

    /** 금리 변경. 적용 시점·이력 관리는 RateChangeHistory 가 담당. */
    public void updateRate(int newBaseBps, int newSpreadBps, int newPreferentialBps, int newTotalBps) {
        this.baseRateBps = newBaseBps;
        this.spreadBps = newSpreadBps;
        this.preferentialRateBps = newPreferentialBps;
        this.totalRateBps = newTotalBps;
    }

    public boolean isClosable() {
        return STATUS_ACTIVE.equals(cntrStatusCd);
    }

    public boolean isClosed() {
        return STATUS_CLOSED.equals(cntrStatusCd);
    }

    /** 약정 종결 — ACTIVE → CLOSED. 다른 상태에서는 호출자에서 차단. */
    public void markClosed() {
        this.cntrStatusCd = STATUS_CLOSED;
    }
}
