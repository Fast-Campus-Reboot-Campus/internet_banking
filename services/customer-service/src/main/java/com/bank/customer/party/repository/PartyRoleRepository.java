package com.bank.customer.party.repository;

import com.bank.customer.party.domain.PartyRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartyRoleRepository extends JpaRepository<PartyRole, Long> {

    List<PartyRole> findByPartyIdAndDeletedAtIsNull(Long partyId);

    List<PartyRole> findByPartyIdAndRoleStatusCodeAndDeletedAtIsNull(Long partyId, String statusCode);

    Optional<PartyRole> findByPartyIdAndRoleTypeCodeAndRoleStatusCodeAndDeletedAtIsNull(
            Long partyId, String roleTypeCode, String statusCode);
}
