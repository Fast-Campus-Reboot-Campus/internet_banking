package com.bank.loan.review.repository;

import com.bank.loan.review.domain.ReviewCheckLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewCheckLogRepository extends JpaRepository<ReviewCheckLog, Long> {

    List<ReviewCheckLog> findByRevIdOrderByCheckedAtAscRchkIdAsc(Long revId);
}
