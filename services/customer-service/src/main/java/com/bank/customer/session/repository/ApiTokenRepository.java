package com.bank.customer.session.repository;

import com.bank.customer.session.domain.ApiToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiTokenRepository extends JpaRepository<ApiToken, Long> {

    List<ApiToken> findBySessionIdAndTokenRevokedAtIsNull(String sessionId);
}
