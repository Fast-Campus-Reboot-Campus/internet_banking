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
 * 직원 (employee 테이블) — party_person 과 같은 위치의 party 서브타입 디테일.
 *
 * <p>직원 party 는 {@code party_role.role_type_code = 'EMPLOYEE'} 로 표시되고,
 * 직급(grade_code)·지점(branch_code) 등 직원 전용 속성은 여기에 저장한다.
 * grade_code 는 {@link com.bank.common.security.BankRole} enum 의 이름을 그대로 쓴다.
 */
@Entity
@Table(name = "employee")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Employee extends BaseEntity {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_CLOSED = "CLOSED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "party_id", nullable = false)
    private Long partyId;

    @Column(name = "branch_code", nullable = false, length = 10)
    private String branchCode;

    /** {@link com.bank.common.security.BankRole} 이름 (예: HQ_RISK, BRANCH_MANAGER) */
    @Column(name = "grade_code", nullable = false, length = 30)
    private String gradeCode;

    @Column(name = "status_code", nullable = false, length = 20)
    private String statusCode;

    public boolean isActive() { return STATUS_ACTIVE.equals(statusCode); }
}
