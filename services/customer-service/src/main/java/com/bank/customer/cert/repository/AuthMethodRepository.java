package com.bank.customer.cert.repository;

import com.bank.customer.cert.domain.AuthMethod;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthMethodRepository extends JpaRepository<AuthMethod, Long> {
}
