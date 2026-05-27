package com.bank.loan.collateral.domain;

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
 * 담보 감정평가. ERD STAGE 4 COLLATERAL_EVALUATION 매핑.
 *
 * 외부 감정평가기관(KAB·한감원 등) 연계는 추후 도입되며,
 * 본 단계에서는 항상 DONE 으로 기록되는 stub 으로 동작한다.
 */
@Getter
@Entity
@Table(name = "collateral_evaluation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CollateralEvaluation extends BaseEntity {

    public static final String STATUS_REQUESTED   = "REQUESTED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_DONE        = "DONE";
    public static final String STATUS_FAILED      = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ceval_col_id")
    private Long cevalColId;

    @Column(name = "col_id", nullable = false)
    private Long colId;

    @Column(name = "eval_method_cd", nullable = false, length = 50)
    private String evalMethodCd;

    @Column(name = "eval_agency_cd", length = 50)
    private String evalAgencyCd;

    @Column(name = "appraised_value", nullable = false)
    private Long appraisedValue;

    @Column(name = "applied_value", nullable = false)
    private Long appliedValue;

    @Column(name = "eval_status_cd", nullable = false, length = 50)
    private String evalStatusCd;

    @Column(name = "eval_report_url", length = 500)
    private String evalReportUrl;

    @Column(name = "eval_report_hash", length = 128)
    private String evalReportHash;

    @Column(name = "evaluated_at")
    private OffsetDateTime evaluatedAt;

    @Column(name = "applied_start_date", length = 8)
    private String appliedStartDate;

    @Column(name = "applied_end_date", length = 8)
    private String appliedEndDate;
}
