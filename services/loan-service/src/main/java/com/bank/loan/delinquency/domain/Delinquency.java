package com.bank.loan.delinquency.domain;

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
 * 연체 (계약별 활성 1건). ERD STAGE 8 DELINQUENCY 매핑.
 *
 * 상태 전이:
 *   ACTIVE → RESOLVED (모든 OVERDUE 회차가 PAID 로 전이되면)
 *
 * dlq_stage_cd: 연체일수 기준 5/30/90일 임계치.
 *   STAGE_0:   1~4일
 *   STAGE_1:   5~29일
 *   STAGE_2:  30~89일
 *   STAGE_3:  90일+
 */
@Getter
@Entity
@Table(name = "delinquency")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Delinquency extends BaseEntity {

    public static final String STATUS_ACTIVE   = "ACTIVE";
    public static final String STATUS_RESOLVED = "RESOLVED";

    public static final String STAGE_0 = "STAGE_0";
    public static final String STAGE_1 = "STAGE_1";
    public static final String STAGE_2 = "STAGE_2";
    public static final String STAGE_3 = "STAGE_3";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dlq_id")
    private Long dlqId;

    @Column(name = "cntr_id", nullable = false)
    private Long cntrId;

    @Column(name = "dlq_status_cd", nullable = false, length = 50)
    private String dlqStatusCd;

    @Column(name = "dlq_start_date", nullable = false, length = 8)
    private String dlqStartDate;

    @Column(name = "dlq_end_date", length = 8)
    private String dlqEndDate;

    @Column(name = "dlq_days", nullable = false)
    private Integer dlqDays;

    @Column(name = "dlq_principal_amt", nullable = false)
    private Long dlqPrincipalAmt;

    @Column(name = "dlq_interest_amt", nullable = false)
    private Long dlqInterestAmt;

    @Column(name = "dlq_total_amt", nullable = false)
    private Long dlqTotalAmt;

    @Column(name = "overdue_rate_bps", nullable = false)
    private Integer overdueRateBps;

    @Column(name = "dlq_stage_cd", nullable = false, length = 50)
    private String dlqStageCd;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    public boolean isActive() {
        return STATUS_ACTIVE.equals(dlqStatusCd);
    }

    public String currentStatus() {
        return dlqStatusCd;
    }

    /** rollover 시 갱신: 연체일·집계금액·stage 재산정. */
    public void updateDailyAggregate(int dlqDays, long principalAmt, long interestAmt, String stage) {
        this.dlqDays = dlqDays;
        this.dlqPrincipalAmt = principalAmt;
        this.dlqInterestAmt = interestAmt;
        this.dlqTotalAmt = principalAmt + interestAmt;
        this.dlqStageCd = stage;
    }

    /** 모든 연체 회차가 PAID 된 시점에 호출. */
    public void markResolved(String endDate, OffsetDateTime at) {
        this.dlqStatusCd = STATUS_RESOLVED;
        this.dlqEndDate = endDate;
        this.resolvedAt = at;
    }

    public static String stageOf(int dlqDays) {
        if (dlqDays >= 90) return STAGE_3;
        if (dlqDays >= 30) return STAGE_2;
        if (dlqDays >= 5)  return STAGE_1;
        return STAGE_0;
    }
}
