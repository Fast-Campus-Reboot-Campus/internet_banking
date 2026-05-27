package com.bank.customer.customer.repository;

import com.bank.customer.customer.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerIdAndDeletedAtIsNull(Long customerId);

    Optional<Customer> findByPartyIdAndCustomerStatusCodeNotAndDeletedAtIsNull(
            Long partyId, String excludedStatus);

    boolean existsByPartyIdAndCustomerStatusCodeNotAndDeletedAtIsNull(
            Long partyId, String excludedStatus);
}
