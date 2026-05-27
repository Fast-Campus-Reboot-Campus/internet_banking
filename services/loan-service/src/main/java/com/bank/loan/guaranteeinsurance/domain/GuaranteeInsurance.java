package com.bank.loan.guaranteeinsurance.domain;

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
 * 보증보험. ERD STAGE 6 GUARANTEE_INSURANCE 매핑.
 *
 * 상태 전이:
 *   ISSUED   → CANCELED (해지)
 *   ISSUED   → EXPIRED  (gins_end_date 경과 — 배치는 후속)
 *
 * flows §1.1: CONTRACTED → DISBURSED 전제조건(필요시): gins_status_cd=ISSUED.
 *
 * 외부기관(SGI/HUG/HF 등) 발급은 본 단계 stub — request 즉시 ISSUED 처리.
 * 실제 운영은 외부 콜백/폴링으로 두 단계 분리 가능.
 */
@Getter
@Entity
@Table(name = "guarantee_insurance")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GuaranteeInsurance extends BaseEntity {

    public static final String STATUS_ISSUED   = "ISSUED";
    public static final String STATUS_CANCELED = "CANCELED";
    public static final String STATUS_EXPIRED  = "EXPIRED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gins_id")
    private Long ginsId;

    @Column(name = "cntr_id", nullable = false)
    private Long cntrId;

    @Column(name = "gins_agency_cd", nullable = false, length = 50)
    private String ginsAgencyCd;

    @Column(name = "gins_policy_no", nullable = false, length = 50, unique = true)
    private String ginsPolicyNo;

    @Column(name = "guarantee_amount", nullable = false)
    private Long guaranteeAmount;

    @Column(name = "guarantee_ratio_bps", nullable = false)
    private Integer guaranteeRatioBps;

    @Column(name = "premium_amount", nullable = false)
    private Long premiumAmount;

    @Column(name = "gins_status_cd", nullable = false, length = 50)
    private String ginsStatusCd;

    @Column(name = "gins_start_date", nullable = false, length = 8)
    private String ginsStartDate;

    @Column(name = "gins_end_date", nullable = false, length = 8)
    private String ginsEndDate;

    @Column(name = "gins_doc_url", length = 500)
    private String ginsDocUrl;

    @Column(name = "gins_doc_hash", length = 128)
    private String ginsDocHash;

    @Column(name = "issued_at")
    private OffsetDateTime issuedAt;

    public String currentStatus() {
        return ginsStatusCd;
    }

    public boolean isCancellable() {
        return STATUS_ISSUED.equals(ginsStatusCd);
    }

    public void markCanceled() {
        this.ginsStatusCd = STATUS_CANCELED;
    }

    /** 일배치가 호출: ISSUED → EXPIRED (gins_end_date 경과). 다른 상태는 호출자에서 차단. */
    public void markExpired() {
        this.ginsStatusCd = STATUS_EXPIRED;
    }
}
