package com.bank.loan.accounting.domain;

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
 * 월별 회계 요약 (EOM 산출).
 *
 * summary_month = YYYYMM. UNIQUE 로 동일 baseMonth 재실행 시 자연 멱등.
 *
 * 매출 합계 = 해당 월의 trasaction (value_date 기준) 일별 합산.
 * 월말 시점 통계 = base_month_end_date 시점의 상태 (스냅샷성).
 *
 * 본 단계 NPL = STAGE_3 합계 (대손 분류는 본 단계 범위 외).
 */
@Getter
@Entity
@Table(name = "monthly_accounting_summary")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class MonthlyAccountingSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mas_id")
    private Long masId;

    @Column(name = "summary_month", nullable = false, unique = true, length = 6)
    private String summaryMonth;

    @Column(name = "base_month_start_date", nullable = false, length = 8)
    private String baseMonthStartDate;

    @Column(name = "base_month_end_date", nullable = false, length = 8)
    private String baseMonthEndDate;

    // 매출 합계
    @Column(name = "interest_revenue", nullable = false)
    private Long interestRevenue;

    @Column(name = "overdue_interest_revenue", nullable = false)
    private Long overdueInterestRevenue;

    @Column(name = "auto_debit_principal", nullable = false)
    private Long autoDebitPrincipal;

    @Column(name = "auto_debit_interest", nullable = false)
    private Long autoDebitInterest;

    @Column(name = "auto_debit_overdue_interest", nullable = false)
    private Long autoDebitOverdueInterest;

    @Column(name = "auto_debit_count", nullable = false)
    private Integer autoDebitCount;

    // 신규 실행
    @Column(name = "new_disbursed_amount", nullable = false)
    private Long newDisbursedAmount;

    @Column(name = "new_disbursed_count", nullable = false)
    private Integer newDisbursedCount;

    // 월말 통계
    @Column(name = "month_end_active_contracts", nullable = false)
    private Integer monthEndActiveContracts;

    @Column(name = "month_end_active_delinquencies", nullable = false)
    private Integer monthEndActiveDelinquencies;

    @Column(name = "month_end_npl_count", nullable = false)
    private Integer monthEndNplCount;

    @Column(name = "month_end_npl_principal", nullable = false)
    private Long monthEndNplPrincipal;

    @Column(name = "summarized_at", nullable = false)
    private OffsetDateTime summarizedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;
}
