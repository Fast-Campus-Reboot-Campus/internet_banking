package com.bank.customer.banking.repository;

import com.bank.customer.banking.domain.WithdrawalAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WithdrawalAccountRepository extends JpaRepository<WithdrawalAccount, Long> {

    List<WithdrawalAccount> findByCustomerIdAndDeletedAtIsNullOrderByPriorityOrderAsc(Long customerId);

    Optional<WithdrawalAccount> findByWithdrawalAccountIdAndCustomerIdAndDeletedAtIsNull(
            Long withdrawalAccountId, Long customerId);

    boolean existsByCustomerIdAndAccountNumberAndDeletedAtIsNull(Long customerId, String accountNumber);
}
