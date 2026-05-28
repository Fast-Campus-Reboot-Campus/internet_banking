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
 * 일일 회계 요약 (EOD 산출).
 *
 * 본격 복식부기 전표는 본 단계 범위 외. 일별 합계만 적재해서 리포팅 기반을 제공한다.
 * UNIQUE(summary_date) 로 동일 baseDate 재실행 시 자연 멱등.
 *
 *   interest_revenue            정상 이자 발생 합계 (interest_accrual)
 *   overdue_interest_revenue    연체 이자 발생 합계 (overdue_accrual)
 *   auto_debit_*                자동이체 출금 (repayment_transaction.channel=AUTO_DEBIT, value_date=summary_date)
 *   disbursed_*                 신규 실행 (loan_execution.exec_status=DONE, value_date=summary_date)
 *   active_contract_count       summary_date 시점 ACTIVE 약정 수
 *   active_delinquency_count    summary_date 시점 ACTIVE 연체 수
 */
@Getter
@Entity
@Table(name = "daily_accounting_summary")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class DailyAccountingSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "das_id")
    private Long dasId;

    @Column(name = "summary_date", nullable = false, unique = true, length = 8)
    private String summaryDate;

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

    @Column(name = "disbursed_amount", nullable = false)
    private Long disbursedAmount;

    @Column(name = "disbursed_count", nullable = false)
    private Integer disbursedCount;

    @Column(name = "active_contract_count", nullable = false)
    private Integer activeContractCount;

    @Column(name = "active_delinquency_count", nullable = false)
    private Integer activeDelinquencyCount;

    @Column(name = "summarized_at", nullable = false)
    private OffsetDateTime summarizedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;
}
