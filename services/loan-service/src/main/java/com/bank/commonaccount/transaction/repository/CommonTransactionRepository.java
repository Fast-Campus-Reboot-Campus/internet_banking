package com.bank.commonaccount.transaction.repository;

import com.bank.commonaccount.transaction.domain.CommonTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommonTransactionRepository extends JpaRepository<CommonTransaction, Long> {

    /** 자연키(transaction_no)로 조회 — write-through 멱등 dedupe. */
    Optional<CommonTransaction> findByTransactionNo(String transactionNo);
}
