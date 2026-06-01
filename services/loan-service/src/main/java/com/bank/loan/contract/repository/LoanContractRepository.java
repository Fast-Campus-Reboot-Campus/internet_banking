package com.bank.loan.contract.repository;

import com.bank.loan.contract.domain.LoanContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanContractRepository extends JpaRepository<LoanContract, Long> {

    Optional<LoanContract> findByCntrIdAndDeletedAtIsNull(Long cntrId);

    List<LoanContract> findByCntrStatusCdAndDeletedAtIsNullOrderByCntrIdAsc(String cntrStatusCd);

    List<LoanContract> findByCustomerIdAndDeletedAtIsNullOrderByCntrIdDesc(Long customerId);
}
