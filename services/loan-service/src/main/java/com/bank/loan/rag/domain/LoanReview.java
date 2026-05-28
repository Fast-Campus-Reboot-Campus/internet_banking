package com.bank.loan.rag.domain;

import com.bank.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * loan_review 엔티티 — D3-2 SimilarCaseExporter 전용 읽기 매핑.
 *
 * <p>auto-loan-review 가 {@code PATCH /reviews/{revId}/report} 로 결과를 업데이트하고,
 * SimilarCaseExporter 가 전일 완료 케이스를 증분 조회해 임베딩 코퍼스로 내보낸다.
 */
@Getter
@Entity
@Table(name = "loan_review")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoanReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rev_id")
    private Long revId;

    @Column(name = "appl_id", nullable = false)
    private Long applId;

    @Column(name = "rev_type_cd", nullable = false)
    private String revTypeCd;

    @Column(name = "rev_status_cd", nullable = false)
    private String revStatusCd;

    @Column(name = "rev_decision_cd")
    private String revDecisionCd;

    @Column(name = "approved_amount")
    private Long approvedAmount;

    @Column(name = "reject_reason_cd")
    private String rejectReasonCd;
}
