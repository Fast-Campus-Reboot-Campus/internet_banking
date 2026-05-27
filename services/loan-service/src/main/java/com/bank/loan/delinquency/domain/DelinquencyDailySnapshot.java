package com.bank.loan.delinquency.domain;

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
 * 연체 일별 스냅샷. ERD STAGE 8 DELINQUENCY_DAILY_SNAPSHOT 매핑.
 *
 * append-only — 한번 작성되면 수정·삭제 금지 (flows §3 운영규칙).
 * UNIQUE (dlq_id, snapshot_date) 제약으로 같은 baseDate 재실행 시 자연 멱등.
 */
@Getter
@Entity
@Table(name = "delinquency_daily_snapshot")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class DelinquencyDailySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dlqs_id")
    private Long dlqsId;

    @Column(name = "dlq_id", nullable = false)
    private Long dlqId;

    @Column(name = "cntr_id", nullable = false)
    private Long cntrId;

    @Column(name = "snapshot_date", nullable = false, length = 8)
    private String snapshotDate;

    @Column(name = "dlq_days", nullable = false)
    private Integer dlqDays;

    @Column(name = "dlq_principal_amt", nullable = false)
    private Long dlqPrincipalAmt;

    @Column(name = "dlq_interest_amt", nullable = false)
    private Long dlqInterestAmt;

    @Column(name = "dlq_total_amt", nullable = false)
    private Long dlqTotalAmt;

    @Column(name = "overdue_rate_bps", nullable = false)
    private Integer overdueRateBps;

    @Column(name = "dlq_stage_cd", nullable = false, length = 50)
    private String dlqStageCd;

    @Column(name = "snapshotted_at", nullable = false)
    private OffsetDateTime snapshottedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;
}
