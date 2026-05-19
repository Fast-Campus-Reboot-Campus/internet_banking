package com.bank.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

/**
 * 등록계 엔티티의 공통 감사 컬럼. ERD 의 공통 감사 컬럼 7종을 매핑한다.
 *   created_at / created_by / updated_at / updated_by / deleted_at / deleted_by / version
 *
 * Soft delete 는 SoftDeletable 인터페이스로 노출하고 softDelete() 호출 시점에
 * deleted_at / deleted_by 가 채워진다. 조회 측은 SoftDeleteSpecifications.activeOnly()
 * 또는 repository 메서드에서 명시적으로 deleted_at IS NULL 필터를 적용한다.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity implements SoftDeletable {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Override
    public void softDelete(Long actorId) {
        if (this.deletedAt != null) return;
        this.deletedAt = OffsetDateTime.now();
        this.deletedBy = actorId;
    }

    @Override
    public boolean isDeleted() {
        return deletedAt != null;
    }
}
