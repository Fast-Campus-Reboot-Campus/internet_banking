package com.bank.loan.delinquency.repository;

import com.bank.loan.delinquency.domain.DelinquencyDailySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DelinquencyDailySnapshotRepository extends JpaRepository<DelinquencyDailySnapshot, Long> {

    boolean existsByDlqIdAndSnapshotDate(Long dlqId, String snapshotDate);

    List<DelinquencyDailySnapshot> findByDlqIdOrderBySnapshotDateAsc(Long dlqId);
}
