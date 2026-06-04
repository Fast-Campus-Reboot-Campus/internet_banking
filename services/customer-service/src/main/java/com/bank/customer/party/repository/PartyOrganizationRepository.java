package com.bank.customer.party.repository;

import com.bank.customer.party.domain.PartyOrganization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PartyOrganizationRepository extends JpaRepository<PartyOrganization, Long> {

    Optional<PartyOrganization> findByCorpRegNoAndDeletedAtIsNull(String corpRegNo);
}
