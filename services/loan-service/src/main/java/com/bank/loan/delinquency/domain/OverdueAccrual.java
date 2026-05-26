package com.bank.loan.delinquency.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

/**
 * 연체 이자 일별 발생. append-only.
 *
 * UNIQUE (cntr_id, accrual_date) 로 동일 baseDate 재실행 시 자연 멱등.
 * 계산식: daily = overduePrincipal × overdueRateBps × 1 / 10000 / 365  (ACT/365)
 */
@Getter
@Entity
@Table(name = "overdue_accrual")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class OverdueAccrual {

    public static final String STATUS_ACCRUED  = "ACCRUED";
    public static final String STATUS_REVERSED = "REVERSED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oa_id")
    private Long oaId;

    @Column(name = "cntr_id", nullable = false)
    private Long cntrId;

    @Column(name = "dlq_id", nullable = false)
    private Long dlqId;

    @Column(name = "accrual_date", nullable = false, length = 8)
    private String accrualDate;

    @Column(name = "overdue_principal", nullable = false)
    private Long overduePrincipal;

    @Column(name = "overdue_rate_bps", nullable = false)
    private Integer overdueRateBps;

    @Column(name = "dlq_days", nullable = false)
    private Integer dlqDays;

    @Column(name = "daily_overdue_interest", nullable = false)
    private Long dailyOverdueInterest;

    @Column(name = "cumulative_overdue_interest", nullable = false)
    private Long cumulativeOverdueInterest;

    @Column(name = "oa_status_cd", nullable = false, length = 50)
    private String oaStatusCd;

    @Column(name = "accrued_at", nullable = false)
    private OffsetDateTime accruedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;
}
