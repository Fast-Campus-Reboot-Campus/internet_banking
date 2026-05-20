package com.bank.deposit.domain.entity;

import com.bank.deposit.common.BaseEntity;
import com.bank.deposit.domain.enums.EmployeeRole;
import com.bank.deposit.domain.enums.EmploymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "employees")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Employee extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long employeeId;

    @Column(name = "employee_number", length = 20, nullable = false, unique = true)
    private String employeeNumber;

    @Column(name = "employee_name", length = 100, nullable = false)
    private String employeeName;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "position", length = 50)
    private String position;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private EmployeeRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false)
    @Builder.Default
    private EmploymentStatus employmentStatus = EmploymentStatus.ACTIVE;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "joined_at", columnDefinition = "CHAR(8)")
    private String joinedAt;

    @Column(name = "resigned_at", columnDefinition = "CHAR(8)")
    private String resignedAt;

    public void resign(String resignedAt) {
        this.employmentStatus = EmploymentStatus.RESIGNED;
        this.resignedAt = resignedAt;
    }
}
