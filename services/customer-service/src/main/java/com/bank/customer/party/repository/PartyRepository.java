package com.bank.customer.party.repository;

import com.bank.customer.party.domain.Party;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PartyRepository extends JpaRepository<Party, Long> {

    Optional<Party> findByPartyIdAndDeletedAtIsNull(Long partyId);
}
