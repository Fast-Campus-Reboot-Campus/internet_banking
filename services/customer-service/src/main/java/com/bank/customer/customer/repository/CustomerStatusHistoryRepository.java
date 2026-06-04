package com.bank.customer.customer.repository;

import com.bank.customer.customer.domain.CustomerStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerStatusHistoryRepository extends JpaRepository<CustomerStatusHistory, Long> {

    /** 최신 이력 1건 조회 — 상태 전이 시 previousHistoryId 설정에 사용. */
    Optional<CustomerStatusHistory> findTopByCustomerIdOrderByCustomerStatusHistoryIdDesc(
            Long customerId);

    /** 전체 상태 이력 최신순 */
    List<CustomerStatusHistory> findByCustomerIdOrderByCustomerStatusHistoryIdDesc(Long customerId);
}
