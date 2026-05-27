package com.bank.loan.delinquency.repository;

import com.bank.loan.delinquency.domain.Delinquency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DelinquencyRepository extends JpaRepository<Delinquency, Long> {

    Optional<Delinquency> findByCntrIdAndDlqStatusCdAndDeletedAtIsNull(Long cntrId, String dlqStatusCd);

    List<Delinquency> findByDlqStatusCdAndDeletedAtIsNull(String dlqStatusCd);
}
