package com.bank.commonaccount.repository;

import com.bank.commonaccount.domain.CommonAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommonAccountRepository extends JpaRepository<CommonAccount, Long> {

    Optional<CommonAccount> findByAccountNo(String accountNo);

    Optional<CommonAccount> findByAccountNickname(String accountNickname);
}
