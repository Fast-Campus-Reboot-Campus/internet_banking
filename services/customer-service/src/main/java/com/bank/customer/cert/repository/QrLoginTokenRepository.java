package com.bank.customer.cert.repository;

import com.bank.customer.cert.domain.QrLoginToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QrLoginTokenRepository extends JpaRepository<QrLoginToken, Long> {

    Optional<QrLoginToken> findByQrTokenHash(String qrTokenHash);
}
