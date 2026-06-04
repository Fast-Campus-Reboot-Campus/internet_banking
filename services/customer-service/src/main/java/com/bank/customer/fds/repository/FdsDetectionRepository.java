package com.bank.customer.fds.repository;

import com.bank.customer.fds.domain.FdsDetection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FdsDetectionRepository extends JpaRepository<FdsDetection, Long> {

    /** PENDING 상태 탐지 목록 — 직원 검토용 */
    Page<FdsDetection> findByFdsDetectionStatusCodeOrderByFdsDetectedAtDesc(
            String statusCode, Pageable pageable);

    /** 특정 고객의 탐지 이력 */
    List<FdsDetection> findByCustomerIdOrderByFdsDetectedAtDesc(Long customerId);
}
