package com.bank.customer.party.repository;

import com.bank.customer.party.domain.PartyPerson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PartyPersonRepository extends JpaRepository<PartyPerson, Long> {

    Optional<PartyPerson> findByPartyIdAndDeletedAtIsNull(Long partyId);

    /** 본인확인 CI값으로 기존 등록 여부 조회 (중복 가입 방지). */
    boolean existsByCiValueAndDeletedAtIsNull(String ciValue);
}
