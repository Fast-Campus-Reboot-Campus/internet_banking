package com.bank.loan.ltv.domain;

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
 * LTV(담보가치비율) 산정 결과. ERD LTV_CALCULATION 매핑.
 *
 * max_loan_amount = max(applied_col_value × limit_bps/10000 - senior_lien_amount, 0)
 * ltv_ratio_bps   = requested_amount / applied_col_value × 10000
 * ltv_status_cd   PASS : requested_amount ≤ max_loan_amount
 *                 FAIL : requested_amount > max_loan_amount
 *
 * 본 단계는 담보 필수 상품의 한도 산정 — 본심사에서 DSR·CB 와 종합한다.
 * 담보당 1건 (MVP — 재계산 시 기존 row 갱신은 추후 도입).
 */
@Getter
@Entity
@Table(name = "ltv_calculation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LtvCalculation extends BaseEntity {

    public static final String STATUS_PASS = "PASS";
    public static final String STATUS_FAIL = "FAIL";

    /** 기본 LTV 한도 (70% = 7000bps) — 주택담보 보편치. */
    public static final int DEFAULT_LTV_LIMIT_BPS = 7_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ltv_id")
    private Long ltvId;

    @Column(name = "appl_id", nullable = false)
    private Long applId;

    @Column(name = "col_id", nullable = false)
    private Long colId;

    @Column(name = "applied_col_value", nullable = false)
    private Long appliedColValue;

    @Column(name = "senior_lien_amount")
    private Long seniorLienAmount;

    @Column(name = "requested_amount", nullable = false)
    private Long requestedAmount;

    @Column(name = "ltv_ratio_bps", nullable = false)
    private Integer ltvRatioBps;

    @Column(name = "ltv_limit_bps", nullable = false)
    private Integer ltvLimitBps;

    @Column(name = "max_loan_amount", nullable = false)
    private Long maxLoanAmount;

    @Column(name = "ltv_status_cd", nullable = false, length = 50)
    private String ltvStatusCd;

    @Column(name = "calculated_at", nullable = false)
    private OffsetDateTime calculatedAt;

    @Column(name = "calc_engine_version", length = 50)
    private String calcEngineVersion;
}
