package com.bank.loan.creditreport.repository;

import com.bank.loan.creditreport.domain.CreditInfoReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CreditInfoReportRepository extends JpaRepository<CreditInfoReport, Long> {

    Optional<CreditInfoReport> findByCrptIdAndDeletedAtIsNull(Long crptId);

    List<CreditInfoReport> findByCntrIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long cntrId);
}
