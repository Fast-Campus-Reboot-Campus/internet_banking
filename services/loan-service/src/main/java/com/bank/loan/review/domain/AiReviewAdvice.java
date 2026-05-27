package com.bank.loan.review.domain;

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
 * AI 심사 조언. append-only — 삭제·수정 없음.
 *
 * advice_type_cd:
 *   BIAS_CHECK        편향/규정 준수 검증 (편향 에이전트)
 *   SUMMARY           CB.REVIEW 사유 요약 (LLM)
 *   REJECTION_LETTER  거절 통지 초안 (LLM)
 *   REVISIT_REASON    정정 사유 추천 (LLM)
 *   GAP_REPORT        권고 vs 결정 갭 분석 (LLM 배치)
 *
 * severity_cd (BIAS_CHECK 전용):
 *   BLOCKED  명백한 규정 위반 — acknowledge 차단
 *   HIGH / MEDIUM / LOW / NONE
 */
@Getter
@Entity
@Table(name = "ai_review_advice")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AiReviewAdvice {

    public static final String TYPE_BIAS_CHECK       = "BIAS_CHECK";
    public static final String TYPE_SUMMARY          = "SUMMARY";
    public static final String TYPE_REJECTION_LETTER = "REJECTION_LETTER";
    public static final String TYPE_REVISIT_REASON   = "REVISIT_REASON";
    public static final String TYPE_GAP_REPORT       = "GAP_REPORT";

    public static final String SEVERITY_BLOCKED = "BLOCKED";
    public static final String SEVERITY_HIGH    = "HIGH";
    public static final String SEVERITY_MEDIUM  = "MEDIUM";
    public static final String SEVERITY_LOW     = "LOW";
    public static final String SEVERITY_NONE    = "NONE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "advice_id")
    private Long adviceId;

    @Column(name = "rev_id", nullable = false)
    private Long revId;

    @Column(name = "advice_type_cd", nullable = false, length = 40)
    private String adviceTypeCd;

    @Column(name = "severity_cd", length = 20)
    private String severityCd;

    @Column(name = "advice_body", nullable = false, columnDefinition = "TEXT")
    private String adviceBody;

    @Column(name = "model", length = 80)
    private String model;

    @Column(name = "model_version", length = 40)
    private String modelVersion;

    @Column(name = "prompt_hash", length = 64)
    private String promptHash;

    @Column(name = "input_token")
    private Integer inputToken;

    @Column(name = "output_token")
    private Integer outputToken;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    public boolean isBlocked() {
        return SEVERITY_BLOCKED.equals(severityCd);
    }
}
