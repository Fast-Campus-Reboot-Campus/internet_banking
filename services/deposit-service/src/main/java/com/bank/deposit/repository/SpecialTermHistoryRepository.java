package com.bank.deposit.repository;

import com.bank.deposit.domain.entity.SpecialTermHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpecialTermHistoryRepository extends JpaRepository<SpecialTermHistory, Long> {
    List<SpecialTermHistory> findBySpecialTermIdOrderByCreatedAtDesc(Long specialTermId);
}
