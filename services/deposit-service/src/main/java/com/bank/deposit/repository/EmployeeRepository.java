package com.bank.deposit.repository;

import com.bank.deposit.domain.entity.Employee;
import com.bank.deposit.domain.enums.EmploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByDepartmentId(Long departmentId);
    List<Employee> findByEmploymentStatus(EmploymentStatus status);
    boolean existsByEmployeeNumber(String employeeNumber);
}
