package com.bank.customer.customer.repository;

import com.bank.customer.customer.domain.Credential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CredentialRepository extends JpaRepository<Credential, Long> {

    /** 로그인 ID로 활성 자격증명 조회 — 로그인 API에서 사용. */
    Optional<Credential> findByLoginIdAndDeletedAtIsNull(String loginId);

    Optional<Credential> findByCustomerIdAndDeletedAtIsNull(Long customerId);

    boolean existsByLoginIdAndDeletedAtIsNull(String loginId);
}
