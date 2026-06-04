package com.bank.customer.code.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 전 도메인 공통 코드 마스터 (cust_code_master 테이블).
 * PK: (code_group_id, code_value) 복합키.
 */
@Entity
@Table(name = "cust_code_master")
@IdClass(CustCodeMaster.CustCodeId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CustCodeMaster {

    @Id
    @Column(name = "code_group_id", length = 30)
    private String codeGroupId;

    @Id
    @Column(name = "code_value", length = 20)
    private String codeValue;

    @Column(name = "code_name", nullable = false, length = 100)
    private String codeName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder;

    /** YYYYMMDD */
    @Column(name = "effective_start_date", nullable = false, length = 8)
    private String effectiveStartDate;

    @Column(name = "effective_end_date", length = 8)
    private String effectiveEndDate;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public boolean isActive(String today) {
        if (effectiveEndDate == null) return true;
        return today.compareTo(effectiveEndDate) <= 0;
    }

    // ── 복합 PK 클래스 ────────────────────────────────────────────────────────
    public static class CustCodeId implements Serializable {
        private String codeGroupId;
        private String codeValue;

        public CustCodeId() {}
        public CustCodeId(String codeGroupId, String codeValue) {
            this.codeGroupId = codeGroupId;
            this.codeValue   = codeValue;
        }
    }
}
