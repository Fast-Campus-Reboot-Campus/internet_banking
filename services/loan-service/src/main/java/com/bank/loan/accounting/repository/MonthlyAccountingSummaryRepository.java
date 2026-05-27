package com.bank.loan.accounting.repository;

import com.bank.loan.accounting.domain.MonthlyAccountingSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MonthlyAccountingSummaryRepository extends JpaRepository<MonthlyAccountingSummary, Long> {

    Optional<MonthlyAccountingSummary> findBySummaryMonth(String summaryMonth);

    boolean existsBySummaryMonth(String summaryMonth);
}
