package com.bank.customer.customer.repository;

import com.bank.customer.customer.domain.CustomerGradeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerGradeHistoryRepository extends JpaRepository<CustomerGradeHistory, Long> {

    List<CustomerGradeHistory> findByCustomerIdOrderByCustomerGradeHistoryIdDesc(Long customerId);

    Optional<CustomerGradeHistory> findTopByCustomerIdOrderByCustomerGradeHistoryIdDesc(Long customerId);
}
