package com.bank.customer.banking.repository;

import com.bank.customer.banking.domain.TransferLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransferLimitRepository extends JpaRepository<TransferLimit, Long> {

    Optional<TransferLimit> findByCustomerIdAndDeletedAtIsNull(Long customerId);
}
