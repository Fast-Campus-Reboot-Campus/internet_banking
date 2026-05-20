package com.bank.deposit.service;

import com.bank.deposit.domain.entity.Account;
import com.bank.deposit.domain.enums.AccountStatus;
import com.bank.deposit.domain.enums.ProductType;
import com.bank.deposit.domain.enums.SavingType;
import com.bank.deposit.exception.BusinessException;
import com.bank.deposit.exception.ErrorCode;
import com.bank.deposit.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public List<Account> findByCustomer(String customerId) {
        return accountRepository.findByCustomerId(customerId);
    }

    public Account findById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    public Account findActive(Long id) {
        Account account = findById(id);
        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
        return account;
    }

    @Transactional
    public Account create(String customerId, Long contractId, ProductType accountType,
                          SavingType savingType, String accountAlias, String accountPassword) {
        if (accountRepository.findByContractId(contractId).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE, "이미 계좌가 생성된 계약입니다.");
        }

        String today = LocalDate.now().format(DATE_FMT);
        String accountNumber = generateAccountNumber(today);

        return accountRepository.save(Account.builder()
                .accountNumber(accountNumber)
                .customerId(customerId)
                .contractId(contractId)
                .accountType(accountType)
                .savingType(savingType)
                .accountAlias(accountAlias)
                .accountPassword(accountPassword)
                .openedAt(today)
                .build());
    }

    @Transactional
    public Account changeStatus(Long id, AccountStatus status) {
        Account account = findById(id);
        account.changeStatus(status, LocalDate.now().format(DATE_FMT));
        return account;
    }

    @Transactional
    public Account updateLimits(Long id, BigDecimal dailyWithdrawLimit,
                                Integer dailyWithdrawCountLimit, BigDecimal atmWithdrawLimit) {
        Account account = findById(id);
        account.updateLimits(dailyWithdrawLimit, dailyWithdrawCountLimit, atmWithdrawLimit);
        return account;
    }

    @Transactional
    public Account updateAlias(Long id, String alias) {
        Account account = findById(id);
        account.updateAlias(alias);
        return account;
    }

    private String generateAccountNumber(String today) {
        String accountNumber;
        do {
            accountNumber = "ACC-" + today + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }
}
