package com.bank.customer.identity.repository;

import com.bank.customer.identity.domain.IdentityVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IdentityVerificationRepository extends JpaRepository<IdentityVerification, Long> {

    List<IdentityVerification> findByCustomerIdOrderByIdentityVerifiedAtDesc(Long customerId);
}
