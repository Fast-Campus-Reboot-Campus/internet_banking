package com.bank.loan.ecl.repository;

import com.bank.loan.ecl.domain.LoanEclSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanEclSummaryRepository extends JpaRepository<LoanEclSummary, Long> {

    boolean existsByCntrIdAndSummaryMonth(Long cntrId, String summaryMonth);

    List<LoanEclSummary> findBySummaryMonthOrderByCntrIdAsc(String summaryMonth);
}
