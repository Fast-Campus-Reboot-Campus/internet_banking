package com.bank.deposit.repository;

import com.bank.deposit.domain.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByDepartmentId(Long departmentId);
    List<Branch> findByIsActive(Boolean isActive);
    boolean existsByBranchCode(String branchCode);
}
