package com.bank.deposit.service;

import com.bank.deposit.domain.entity.Account;
import com.bank.deposit.domain.entity.Transaction;
import com.bank.deposit.domain.enums.*;
import com.bank.deposit.exception.BusinessException;
import com.bank.deposit.exception.ErrorCode;
import com.bank.deposit.repository.AccountRepository;
import com.bank.deposit.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final Clock clock;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public Page<Transaction> findByAccount(Long accountId, OffsetDateTime startDate, OffsetDateTime endDate, Pageable pageable) {
        if (startDate != null && endDate != null) {
            return transactionRepository.findByAccountIdAndTransactionAtBetween(accountId, startDate, endDate, pageable);
        }
        return transactionRepository.findByAccountId(accountId, pageable);
    }

    public Transaction findById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND));
    }

    @Transactional
    public Transaction deposit(Long accountId, BigDecimal amount, TransactionChannel channelType,
                               String transactionMemo, String depositorCustomerId, String depositorName) {
        Account account = getActiveAccount(accountId);
        BigDecimal before = account.getBalance();
        account.deposit(amount, clock);

        return transactionRepository.save(Transaction.builder()
                .transactionNumber(generateTxnNumber("DEP"))
                .accountId(accountId)
                .transactionType(TransactionType.DEPOSIT)
                .directionType(DirectionType.IN)
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(account.getBalance())
                .availableBalanceAfter(account.getBalance())
                .channelType(channelType != null ? channelType : TransactionChannel.INTERNET)
                .transactionAt(OffsetDateTime.now(clock))
                .postedAt(OffsetDateTime.now(clock))
                .transactionMemo(transactionMemo)
                .depositorCustomerId(depositorCustomerId)
                .depositorName(depositorName)
                .build());
    }

    @Transactional
    public Transaction withdraw(Long accountId, BigDecimal amount, TransactionChannel channelType, String transactionMemo) {
        Account account = getActiveAccount(accountId);
        BigDecimal before = account.getBalance();
        account.withdraw(amount, clock);

        return transactionRepository.save(Transaction.builder()
                .transactionNumber(generateTxnNumber("WDR"))
                .accountId(accountId)
                .transactionType(TransactionType.WITHDRAW)
                .directionType(DirectionType.OUT)
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(account.getBalance())
                .availableBalanceAfter(account.getBalance())
                .channelType(channelType != null ? channelType : TransactionChannel.INTERNET)
                .transactionAt(OffsetDateTime.now(clock))
                .postedAt(OffsetDateTime.now(clock))
                .transactionMemo(transactionMemo)
                .build());
    }

    /**
     * 내부/외부 이체.
     *
     * <p>수정 사항:
     * <ul>
     * <li>수신 계좌가 없으면 {@link ErrorCode#ACCOUNT_NOT_FOUND} 예외 — 돈 증발 방지</li>
     * <li>toAccountNo 가 toAccountId 의 accountNumber 와 일치하는지 검증</li>
     * <li>수신 측 transferType 을 요청 값 그대로 반영 (EXTERNAL → EXTERNAL)</li>
     * </ul>
     */
    @Transactional
    public Transaction transfer(Long fromAccountId, Long toAccountId, String toAccountNo,
                                BigDecimal amount, TransferType transferType,
                                String counterpartyBankCode, String counterpartyBankName,
                                String counterpartyName, TransactionChannel channelType, String transactionMemo) {
        Account source = getActiveAccount(fromAccountId);
        BigDecimal before = source.getBalance();
        source.withdraw(amount, clock);

        TransferType resolvedType = transferType != null ? transferType : TransferType.INTERNAL;
        OffsetDateTime now = OffsetDateTime.now(clock