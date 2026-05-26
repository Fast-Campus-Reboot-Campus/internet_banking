package com.bank.loan.ecl.domain;

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
 * IFRS9 ECL (Expected Credit Loss) 월별 산출 결과.
 *
 * ECL = PD × LGD × EAD
 *   PD  부도확률 (bps, 10000 = 100%)
 *   LGD 손실률 (bps)
 *   EAD 부도시 익스포저 — 원금 잔액 기준
 *
 * IFRS Stage:
 *   STAGE_1  12-month PD (정상)
 *   STAGE_2  Lifetime PD (요주의 — 신용위험 유의미한 증가)
 *   STAGE_3  Credit-impaired (부실)
 *
 * 멱등: UNIQUE(cntr_id, summary_month).
 */
@Getter
@Entity
@Table(name = "loan_ecl_summary")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class LoanEclSummary {

    public static final String STAGE_1 = "STAGE_1";
    public static final String STAGE_2 = "STAGE_2";
    public static final String STAGE_3 = "STAGE_3";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ecl_id")
    private Long eclId;

    @Column(name = "cntr_id", nullable = false)
    private Long cntrId;

    @Column(name = "summary_month", nullable = false, length = 6)
    private String summaryMonth;

    @Column(name = "ifrs_stage_cd", nullable = false, length = 50)
    private String ifrsStageCd;

    @Column(name = "pd_bps", nullable = false)
    private Integer pdBps;

    @Column(name = "lgd_bps", nullable = false)
    private Integer lgdBps;

    @Column(name = "ead", nullable = false)
    private Long ead;

    @Column(name = "ecl", nullable = false)
    private Long ecl;

    @Column(name = "engine_version", nullable = false, length = 50)
    private String engineVersion;

    @Column(name = "calculated_at", nullable = false)
    private OffsetDateTime calculatedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;
}
