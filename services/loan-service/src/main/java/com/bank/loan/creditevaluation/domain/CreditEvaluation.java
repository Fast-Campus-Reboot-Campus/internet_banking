package com.bank.loan.creditevaluation.domain;

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
 * 신용평가(CB 조회 + 내부 평가 엔진 결과). ERD CREDIT_EVALUATION 매핑. appl_id UNIQUE — 신청당 1건.
 *
 * 본 단계: 외부 CB(KCB/NICE)·자동심사 모델 stub — 클라이언트가 결과(grade/score/decision 등) 를 입력으로 전달.
 *
 * 결정(ceval_decision_cd):
 *   APPROVE  자동 승인 가능 (본심사에서 최종)
 *   REVIEW   수동 심사 필요
 *   REJECT   자동 거절 권고
 *
 * 신청 상태 전이는 본심사에서 종합 — 본 row 적재만 한다.
 */
@Getter
@Entity
@Table(name = "credit_evaluation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CreditEvaluation extends BaseEntity {

    public static final String DECISION_APPROVE = "APPROVE";
    public static final String DECISION_REVIEW  = "REVIEW";
    public static final String DECISION_REJECT  = "REJECT";

    public static final String STATUS_COMPLETED = "COMPLETED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ceval_id")
    private Long cevalId;

    @Column(name = "appl_id", nullable = false, unique = true)
    private Long applId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "ceval_engine", nullable = false, length = 50)
    private String cevalEngine;

    @Column(name = "ceval_engine_version", length = 50)
    private String cevalEngineVersion;

    @Column(name = "ceval_grade", length = 10)
    private String cevalGrade;

    @Column(name = "ceval_score")
    private Integer cevalScore;

    @Column(name = "pd_bps")
    private Integer pdBps;

    @Column(name = "ceval_decision_cd", nullable = false, length = 50)
    private String cevalDecisionCd;

    @Column(name = "eval_limit_amount")
    private Long evalLimitAmount;

    @Column(name = "eval_rate_bps")
    private Integer evalRateBps;

    @Column(name = "ceval_status_cd", nullable = false, length = 50)
    private String cevalStatusCd;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ceval_factors", columnDefinition = "jsonb")
    private String cevalFactors;

    @Column(name = "evaluated_at", nullable = false)
    private OffsetDateTime evaluatedAt;
}
