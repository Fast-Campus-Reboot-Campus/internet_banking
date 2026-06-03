package com.bank.customer.party.repository;

import com.bank.customer.party.domain.BusinessInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusinessInfoRepository extends JpaRepository<BusinessInfo, Long> {

    List<BusinessInfo> findByPartyIdAndDeletedAtIsNull(Long partyId);

    Optional<BusinessInfo> findByBizRegNoAndDeletedAtIsNull(String bizRegNo);

    boolean existsByBizRegNoAndDeletedAtIsNull(String bizRegNo);
}
