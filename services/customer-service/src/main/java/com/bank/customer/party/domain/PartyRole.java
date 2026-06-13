package com.bank.customer.party.domain;

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

/**
 * 관계자역할 (party_role 테이블).
 * 하나의 party 가 여러 역할을 가질 수 있다 (예: 고객이면서 대출보증인).
 */
@Entity
@Table(name = "party_role")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PartyRole extends BaseEntity {

    public static final String TYPE_CUSTOMER   = "CUSTOMER";
    public static final String TYPE_GUARANTOR  = "GUARANTOR";
    public static final String TYPE_TRUSTEE    = "TRUSTEE";
    public static final String TYPE_AGENT      = "AGENT";
    public static final String TYPE_BENEFICIARY = "BENEFICIARY";
    public static final String TYPE_EMPLOYEE    = "EMPLOYEE";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_CLOSED = "CLOSED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "party_id", nullable = false)
    private Long partyId;

    @Column(name = "role_type_code", nullable = false, length = 20)
    private String roleTypeCode;

    @Column(name = "role_status_code", nullable = false, length = 20)
    private String roleStatusCode;

    /** YYYYMMDD */
    @Column(name = "role_start_date", nullable = false, length = 8)
    private String roleStartDate;

    @Column(name = "role_end_date", length = 8)
    private String roleEndDate;

    @Column(name = "role_end_reason_code", length = 20)
    private String roleEndReasonCode;

    public boolean isActive() { return STATUS_ACTIVE.equals(roleStatusCode); }

    public void close(String endDate, String reasonCode) {
        this.roleStatusCode    = STATUS_CLOSED;
        this.roleEndDate       = endDate;
        this.roleEndReasonCode = reasonCode;
    }
}
