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

    /**
     * 중도상환(TYPE_EARLY) 으로 갚은 원금 누계. outstanding 잔액 계산에 사용.
     * SUCCESS 이고 reversal_yn='N' 인 row 만 합산.
     */
    @Query("""
            select coalesce(sum(t.principalAmount), 0)
              from RepaymentTransaction t
             where t.cntrId = :cntrId
               and t.rtxTypeCd = 'EARLY'
               and t.rtxStatusCd = 'SUCCESS'
               and t.reversalYn = 'N'
               and t.deletedAt is null
            """)
    long sumEarlyPrincipal(@Param("cntrId") Long cntrId);
}
