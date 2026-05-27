package com.bank.deposit.repository;

import com.bank.deposit.domain.entity.Transaction;
import com.bank.deposit.domain.enums.TransactionStatus;
import com.bank.deposit.domain.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findByAccountId(Long accountId, Pageable pageable);
    Page<Transaction> findByAccountIdAndTransactionAtBetween(Long accountId, OffsetDateTime start, OffsetDateTime end, Pageable pageable);
    List<Transaction> findByContractIdAndTransactionType(Long contractId, TransactionType type);
    Optional<Transaction> findByTransactionNumber(String transactionNumber);

    List<Transaction> findByAccountIdInAndTransactionAtBetweenAndStatus(
            List<Long> accountIds, OffsetDateTime start, OffsetDateTime end, TransactionStatus status);
}