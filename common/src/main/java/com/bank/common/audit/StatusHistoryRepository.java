package com.bank.common.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {

    List<StatusHistory> findByTargetDomainCdAndTargetTableCdAndTargetIdOrderByChangedAtAsc(
            String targetDomainCd, String targetTableCd, Long targetId);
}
