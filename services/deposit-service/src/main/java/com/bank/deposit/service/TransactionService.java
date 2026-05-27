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
     *   <li>수신 계좌가 없으면 {@link ErrorCode#ACCOUNT_NOT_FOUND} 예외 — 돈 증발 방지</li>
     *   <li>toAccountNo 가 toAccountId 의 accountNumber 와 일치하는지 검증</li>
     *   <li>수신 측 transferType 을 요청 값 그대로 반영 (EXTERNAL → EXTERNAL)</li>
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
        OffsetDateTime now = OffsetDateTime.now(clock);

        Transaction outTx = transactionRepository.save(Transaction.builder()
                .transactionNumber(generateTxnNumber("TRF"))
                .accountId(fromAccountId)
                .transactionType(TransactionType.TRANSFER)
                .directionType(DirectionType.OUT)
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(source.getBalance())
                .availableBalanceAfter(source.getBalance())
                .transferType(resolvedType)
                .counterpartyAccountId(toAccountId)
                .counterpartyAccountNo(toAccountNo)
                .counterpartyBankCode(counterpartyBankCode)
                .counterpartyBankName(counterpartyBankName)
                .counterpartyName(counterpartyName)
                .channelType(channelType != null ? channelType : TransactionChannel.INTERNET)
                .transactionAt(now)
                .postedAt(now)
                .transferRequestedAt(now)
                .transferCompletedAt(now)
                .transactionMemo(transactionMemo)
                .build());

        // 내부 이체: 수신 계좌가 반드시 존재해야 한다 (없으면 잔액 증발 버그)
        if (toAccountId != null) {
            Account target = accountRepository.findById(toAccountId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

            // 계좌번호 일치 검증
            if (toAccountNo != null && !toAccountNo.equals(target.getAccountNumber())) {
                throw new BusinessException(ErrorCode.INVALID_STATUS,
                        "계좌번호(" + toAccountNo + ")가 계좌 ID(" + toAccountId + ")와 일치하지 않습니다.");
            }

            BigDecimal targetBefore = target.getBalance();
            target.deposit(amount, clock);
            transactionRepository.save(Transaction.builder()
                    .transactionNumber(generateTxnNumber("TRF"))
                    .accountId(toAccountId)
                    .transactionType(TransactionType.TRANSFER)
                    .directionType(DirectionType.IN)
                    .amount(amount)
                    .balanceBefore(targetBefore)
                    .balanceAfter(target.getBalance())
                    .availableBalanceAfter(target.getBalance())
                    .transferType(resolvedType)   // EXTERNAL 이체면 수신측도 EXTERNAL
                    .counterpartyAccountId(fromAccountId)
                    .channelType(TransactionChannel.SYSTEM)
                    .transactionAt(now)
                    .postedAt(now)
                    .transactionSummary("이체 수신")
                    .build());
        }
        return outTx;
    }

    @Transactional
    public Transaction savingsPayment(Long accountId, Long contractId, BigDecimal amount,
                                      Integer paymentRound, TransactionChannel channelType) {
        Account account = getActiveAccount(accountId);
        BigDecimal before = account.getBalance();
        account.deposit(amount, clock);
        account.addPaidAmount(amount);

        return transactionRepository.save(Transaction.builder()
                .transactionNumber(generateTxnNumber("SAV"))
                .accountId(accountId)
                .contractId(contractId)
                .transactionType(TransactionType.SAVINGS_PAYMENT)
                .directionType(DirectionType.IN)
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(account.getBalance())
                .availableBalanceAfter(account.getBalance())
                .channelType(channelType != null ? channelType : TransactionChannel.SYSTEM)
                .paymentRound(paymentRound)
                .transactionAt(OffsetDateTime.now(clock))
                .postedAt(OffsetDateTime.now(clock))
                .build());
    }

    @Transactional
    public Transaction reversal(Long transactionId, TransactionChannel channelType) {
        Transaction original = findById(transactionId);
        if (original.getStatus() == TransactionStatus.CANCELED) {
            throw new BusinessException(ErrorCode.ALREADY_CANCELED);
        }

        Account account = getActiveAccount(original.getAccountId());
        BigDecimal before = account.getBalance();
        DirectionType reverseDirection = original.getDirectionType() == DirectionType.IN
                ? DirectionType.OUT : DirectionType.IN;

        if (reverseDirection == DirectionType.OUT) {
            account.withdraw(original.getAmount(), clock);
        } else {
            account.deposit(original.getAmount(), clock);
        }

        original.cancel();

        return transactionRepository.save(Transaction.builder()
                .transactionNumber(generateTxnNumber("REV"))
                .accountId(original.getAccountId())
                .contractId(original.getContractId())
                .transactionType(TransactionType.REVERSAL)
                .directionType(reverseDirection)
                .amount(original.getAmount())
                .balanceBefore(before)
                .balanceAfter(account.getBalance())
                .availableBalanceAfter(account.getBalance())
                .channelType(channelType != null ? channelType : TransactionChannel.SYSTEM)
                .originalTransactionId(transactionId)
                .transactionAt(OffsetDateTime.now(clock))
                .postedAt(OffsetDateTime.now(clock))
                .transactionSummary("거래 취소")
                .build());
    }

    private Account getActiveAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
        return account;
    }

    private String generateTxnNumber(String prefix) {
        return prefix + "-" + LocalDate.now(clock).format(DATE_FMT) + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
