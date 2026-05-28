package com.bank.loan.accounting.repository;

import com.bank.loan.accounting.domain.DailyAccountingSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DailyAccountingSummaryRepository extends JpaRepository<DailyAccountingSummary, Long> {

    Optional<DailyAccountingSummary> findBySummaryDate(String summaryDate);

    boolean existsBySummaryDate(String summaryDate);
}
