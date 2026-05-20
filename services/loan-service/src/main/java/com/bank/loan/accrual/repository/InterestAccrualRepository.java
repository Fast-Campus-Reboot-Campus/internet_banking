package com.bank.loan.accrual.repository;

import com.bank.loan.accrual.domain.InterestAccrual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterestAccrualRepository extends JpaRepository<InterestAccrual, Long> {

    boolean existsByCntrIdAndAccrualDate(Long cntrId, String accrualDate);

    Optional<InterestAccrual> findFirstByCntrIdAndAccrualDateLessThanOrderByAccrualDateDesc(
            Long cntrId, String accrualDate);

    List<InterestAccrual> findByCntrIdAndAccrualDateBetweenOrderByAccrualDateAsc(
            Long cntrId, String from, String to);

    List<InterestAccrual> findByCntrIdOrderByAccrualDateAsc(Long cntrId);
}
