package com.bank.customer.session.repository;

import com.bank.customer.session.domain.LoginSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoginSessionRepository extends JpaRepository<LoginSession, String> {

    List<LoginSession> findByCustomerIdAndSessionStatusCodeOrderBySessionExpiryAtDesc(
            Long customerId, String statusCode);

    Optional<LoginSession> findBySessionIdAndCustomerId(String sessionId, Long customerId);
}
