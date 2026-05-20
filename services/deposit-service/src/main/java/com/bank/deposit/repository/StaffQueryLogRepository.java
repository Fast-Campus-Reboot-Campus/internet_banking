package com.bank.deposit.repository;

import com.bank.deposit.domain.entity.StaffQueryLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffQueryLogRepository extends JpaRepository<StaffQueryLog, Long> {
    List<StaffQueryLog> findByEmployeeIdOrderByQueryAtDesc(Long employeeId);
    List<StaffQueryLog> findByCustomerIdOrderByQueryAtDesc(String customerId);
}
