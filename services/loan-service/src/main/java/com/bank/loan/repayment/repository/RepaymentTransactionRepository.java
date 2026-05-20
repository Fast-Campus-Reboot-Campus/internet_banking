package com.bank.loan.repayment.repository;

import com.bank.loan.repayment.domain.RepaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepaymentTransactionRepository extends JpaRepository<RepaymentTransaction, Long> {

    Optional<RepaymentTransaction> findByIdempotencyKey(String idempotencyKey);

    List<RepaymentTransaction> findByCntrIdAndDeletedAtIsNullOrderByPaidAtAsc(Long cntrId);
}
