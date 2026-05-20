package com.bank.loan.ratechange.repository;

import com.bank.loan.ratechange.domain.RateChangeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RateChangeHistoryRepository extends JpaRepository<RateChangeHistory, Long> {

    List<RateChangeHistory> findByCntrIdOrderByChangedAtAsc(Long cntrId);
}
