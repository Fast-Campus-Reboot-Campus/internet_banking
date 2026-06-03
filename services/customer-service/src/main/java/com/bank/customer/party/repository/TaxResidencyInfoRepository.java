package com.bank.customer.party.repository;

import com.bank.customer.party.domain.TaxResidencyInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaxResidencyInfoRepository extends JpaRepository<TaxResidencyInfo, Long> {

    List<TaxResidencyInfo> findByPartyIdAndDeletedAtIsNull(Long partyId);
}
