package com.bank.loan.dsr.domain;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * DSR(총부채원리금상환비율) 산정 결과. ERD DSR_CALCULATION 매핑. appl_id UNIQUE — 신청당 1건.
 *
 * dsr_ratio_bps = total_annual_repay_amt / annual_income_amt × 10000
 * dsr_status_cd  PASS : ratio ≤ limit
 *                FAIL : ratio  > limit
 *
 * 본 단계는 한도 산정(신청별) 의 핵심 — 본심사에서 LTV·CB 와 종합한다.
 */
@Getter
@Entity
@Table(name = "dsr_calculation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DsrCalculation extends BaseEntity {

    public static final String STATUS_PASS = "PASS";
    public static final String STATUS_FAIL = "FAIL";

    /** 기본 DSR 규제 한도 (40% = 4000bps). */
    public static final int DEFAULT_DSR_LIMIT_BPS = 4_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dsr_id")
    private Long dsrId;

    @Column(name = "appl_id", nullable = false, unique = true)
    private Long applId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "annual_income_amt", nullable = false)
    private Long annualIncomeAmt;

    @Column(name = "existing_principal_total", nullable = false)
    private Long existingPrincipalTotal;

    @Column(name = "existing_annual_repay_amt", nullable = false)
    private Long existingAnnualRepayAmt;

    @Column(name = "new_annual_repay_amt", nullable = false)
    private Long newAnnualRepayAmt;

    @Column(name = "total_annual_repay_amt", nullable = false)
    private Long totalAnnualRepayAmt;

    @Column(name = "dsr_ratio_bps", nullable = false)
    private Integer dsrRatioBps;

    @Column(name = "dsr_limit_bps", nullable = false)
    private Integer dsrLimitBps;

    @Column(name = "dsr_status_cd", nullable = false, length = 50)
    private String dsrStatusCd;

    @Column(name = "dsr_reg_type_cd", length = 50)
    private String dsrRegTypeCd;

    @Column(name = "calculated_at", nullable = false)
    private OffsetDateTime calculatedAt;

    @Column(name = "calc_engine_version", length = 50)
    private String calcEngineVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dsr_detail", columnDefinition = "jsonb")
    private String dsrDetail;
}
