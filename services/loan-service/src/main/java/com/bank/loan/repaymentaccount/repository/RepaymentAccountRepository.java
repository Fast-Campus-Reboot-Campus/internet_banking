package com.bank.loan.repaymentaccount.repository;

import com.bank.loan.repaymentaccount.domain.RepaymentAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RepaymentAccountRepository extends JpaRepository<RepaymentAccount, Long> {

    Optional<RepaymentAccount> findByCntrIdAndDeletedAtIsNull(Long cntrId);
}
