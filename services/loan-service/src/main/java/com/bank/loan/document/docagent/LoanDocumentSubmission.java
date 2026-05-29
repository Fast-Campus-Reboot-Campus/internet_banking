package com.bank.loan.document.docagent;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * doc-agent 검증 결과 수신 이력 — loan_document_submission 테이블.
 * doc-agent.routed 토픽으로 수신한 AUTO_PASS / NEEDS_RESUBMIT / HOLD 결과 저장.
 */
@Getter
@Entity
@Table(name = "loan_document_submission")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LoanDocumentSubmission {

    public static final String STATUS_AUTO_PASS      = "AUTO_PASS";
    public static final String STATUS_NEEDS_RESUBMIT = "NEEDS_RESUBMIT";
    public static final String STATUS_HOLD           = "HOLD";
    public static final String STATUS_REVIEWER_PASS  = "REVIEWER_PASS";  // 심사원이 수동 승인한 상태

    @Id
    @Column(name = "submission_id", length = 36)
    private String submissionId;

    @Column(name = "appl_id")
    private Long applId;

    @Column(name = "doc_code", nullable = false, length = 50)
    private String docCode;

    @Column(name = "verify_status", length = 50)
    private String verifyStatus;

    @Column(name = "occurred_at")
    private OffsetDateTime occurredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    /** 심사원 수동 승인 — HOLD 서류를 검토 후 통과 처리 */
    public void markReviewerPass(Long reviewerId) {
        this.verifyStatus = STATUS_REVIEWER_PASS;
        this.reviewedBy   = reviewerId;
        this.reviewedAt   = OffsetDateTime.now();
    }

    /** 본심사 진입 가능 여부 — AUTO_PASS 또는 심사원 수동 승인 */
    public boolean isCleared() {
        return STATUS_AUTO_PASS.equals(verifyStatus) || STATUS_REVIEWER_PASS.equals(verifyStatus);
    }
}
