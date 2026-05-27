package com.bank.deposit.repository;

import com.bank.deposit.domain.entity.Account;
import com.bank.deposit.domain.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByCustomerId(String customerId);
    List<Account> findByCustomerIdAndAccountStatus(String customerId, AccountStatus status);
    Optional<Account> findByAccountNumber(String accountNumber);
    Optional<Account> findByContractId(Long contractId);
    boolean existsByAccountNumber(String accountNumber);
}
