package com.bank.loan.guarantor.repository;

import com.bank.loan.guarantor.domain.GuarantorMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuarantorMasterRepository extends JpaRepository<GuarantorMaster, Long> {

    Optional<GuarantorMaster> findByGmstIdAndDeletedAtIsNull(Long gmstId);

    Optional<GuarantorMaster> findByGuarantorCiHashAndDeletedAtIsNull(String guarantorCiHash);
}
