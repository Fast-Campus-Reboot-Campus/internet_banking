package com.bank.loan.ratechange.domain;

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
 * 금리 변경 이력. ERD STAGE 7 RATE_CHANGE_HISTORY 매핑.
 *
 * append-only — 한번 작성되면 수정·삭제 금지 (flows §2.3, §3 운영규칙).
 * 한 계약에 N 건이 시간순으로 쌓이며, 각 row 는 적용시작일(applied_start_date) 기준의
 * 금리 변경 사건을 기록한다.
 *
 * 자주 쓰이는 사유 코드(rate_change_reason_cd):
 *   BASE_RATE_RESET      기준금리 리셋
 *   PREF_CHANGE          우대조건 충족·실효
 *   DELINQ_PENALTY       연체 가산금리 부과
 *   PRODUCT_POLICY       상품정책 변경
 */
@Getter
@Entity
@Table(name = "rate_change_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class RateChangeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rchg_id")
    private Long rchgId;

    @Column(name = "cntr_id", nullable = false)
    private Long cntrId;

    @Column(name = "rate_change_reason_cd", nullable = false, length = 50)
    private String rateChangeReasonCd;

    @Column(name = "previous_rate_bps", nullable = false)
    private Integer previousRateBps;

    @Column(name = "new_rate_bps", nullable = false)
    private Integer newRateBps;

    @Column(name = "base_rate_bps", nullable = false)
    private Integer baseRateBps;

    @Column(name = "spread_bps", nullable = false)
    private Integer spreadBps;

    @Column(name = "preferential_rate_bps", nullable = false)
    private Integer preferentialRateBps;

    @Column(name = "applied_start_date", nullable = false, length = 8)
    private String appliedStartDate;

    @Column(name = "applied_end_date", length = 8)
    private String appliedEndDate;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;

    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;
}
