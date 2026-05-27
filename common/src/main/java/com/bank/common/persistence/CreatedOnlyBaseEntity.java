package com.bank.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

/**
 * Append-only 이력·스냅샷 테이블용 베이스.
 * STATUS_HISTORY, INTEREST_ACCRUAL, RATE_CHANGE_HISTORY,
 * DELINQUENCY_DAILY_SNAPSHOT, REVIEW_CHECK_LOG 등이 사용.
 * Soft delete / 수정 대상이 아니므로 created_at / created_by 두 컬럼만 보유한다.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class CreatedOnlyBaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;
}
