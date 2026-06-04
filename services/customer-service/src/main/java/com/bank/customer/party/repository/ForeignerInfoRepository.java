package com.bank.customer.party.repository;

import com.bank.customer.party.domain.ForeignerInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ForeignerInfoRepository extends JpaRepository<ForeignerInfo, Long> {

    Optional<ForeignerInfo> findByPassportNoAndDeletedAtIsNull(String passportNo);
}
