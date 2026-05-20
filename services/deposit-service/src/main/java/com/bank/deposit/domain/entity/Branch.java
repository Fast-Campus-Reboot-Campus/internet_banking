package com.bank.deposit.domain.entity;

import com.bank.deposit.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "branches")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Branch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long branchId;

    @Column(name = "branch_code", length = 20, nullable = false, unique = true)
    private String branchCode;

    @Column(name = "branch_name", length = 100, nullable = false)
    private String branchName;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(name = "address", length = 300)
    private String address;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "region", length = 50)
    private String region;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "opened_at", columnDefinition = "CHAR(8)")
    private String openedAt;

    @Column(name = "closed_at", columnDefinition = "CHAR(8)")
    private String closedAt;

    public void update(String branchName, String address, String phoneNumber) {
        this.branchName = branchName;
        this.address = address;
        this.phoneNumber = phoneNumber;
    }

    public void deactivate(String closedAt) {
        this.isActive = false;
        this.closedAt = closedAt;
    }
}
