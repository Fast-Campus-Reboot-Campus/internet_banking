package com.bank.loan.accrual.domain;

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
 * 일별 이자 발생. ERD STAGE 7 INTEREST_ACCRUAL 매핑.
 *
 * append-only — 한번 INSERT 되면 수정·삭제 금지 (flows §3 운영규칙).
 * UNIQUE (cntr_id, accrual_date) 제약으로 같은 baseDate 재실행 시 자연 멱등.
 *
 * 본 단계 day_count_basis_cd = ACT/365 고정 (상품별 차등은 후속).
 * 본 단계 iacc_status_cd = ACCRUED. 운영 오류 보정(REVERSED) 은 후속.
 */
@Getter
@Entity
@Table(name = "interest_accrual")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class InterestAccrual {

    public static final String STATUS_ACCRUED  = "ACCRUED";
    public static final String STATUS_REVERSED = "REVERSED";

    public static final String BASIS_ACT_365 = "ACT/365";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "iacc_id")
    private Long iaccId;

    @Column(name = "cntr_id", nullable = false)
    private Long cntrId;

    @Column(name = "accrual_date", nullable = false, length = 8)
    private String accrualDate;

    @Column(name = "principal_balance", nullable = false)
    private Long principalBalance;

    @Column(name = "applied_rate_bps", nullable = false)
    private Integer appliedRateBps;

    @Column(name = "day_count_basis_cd", nullable = false, length = 50)
    private String dayCountBasisCd;

    @Column(name = "daily_interest_amt", nullable = false)
    private Long dailyInterestAmt;

    @Column(name = "cumulative_interest_amt", nullable = false)
    private Long cumulativeInterestAmt;

    @Column(name = "iacc_status_cd", nullable = false, length = 50)
    private String iaccStatusCd;

    @Column(name = "accrued_at", nullable = false)
    private OffsetDateTime accruedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;
}
