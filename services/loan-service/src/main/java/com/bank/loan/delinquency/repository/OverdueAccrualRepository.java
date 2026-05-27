package com.bank.loan.delinquency.repository;

import com.bank.loan.delinquency.domain.OverdueAccrual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OverdueAccrualRepository extends JpaRepository<OverdueAccrual, Long> {

    boolean existsByCntrIdAndAccrualDate(Long cntrId, String accrualDate);

    Optional<OverdueAccrual> findFirstByCntrIdAndAccrualDateLessThanOrderByAccrualDateDesc(
            Long cntrId, String accrualDate);
}
