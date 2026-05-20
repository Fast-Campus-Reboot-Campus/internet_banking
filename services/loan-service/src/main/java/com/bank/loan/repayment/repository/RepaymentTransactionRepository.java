package com.bank.loan.repayment.repository;

import com.bank.loan.repayment.domain.RepaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RepaymentTransactionRepository extends JpaRepository<RepaymentTransaction, Long> {

    Optional<RepaymentTransaction> findByIdempotencyKey(String idempotencyKey);

    List<RepaymentTransaction> findByCntrIdAndDeletedAtIsNullOrderByPaidAtAsc(Long cntrId);

    @Query("""
            select coalesce(sum(t.interestAmount), 0)
              from RepaymentTransaction t
             where t.cntrId = :cntrId
               and t.rtxStatusCd = 'SUCCESS'
               and t.reversalYn = 'N'
               and t.deletedAt is null
            """)
    long sumInterestAmount(@Param("cntrId") Long cntrId);
}
