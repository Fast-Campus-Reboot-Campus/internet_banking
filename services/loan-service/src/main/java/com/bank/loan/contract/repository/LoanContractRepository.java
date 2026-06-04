package com.bank.loan.contract.repository;

import com.bank.loan.contract.domain.LoanContract;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoanContractRepository extends JpaRepository<LoanContract, Long> {

    Optional<LoanContract> findByCntrIdAndDeletedAtIsNull(Long cntrId);

    List<LoanContract> findByCntrStatusCdAndDeletedAtIsNullOrderByCntrIdAsc(String cntrStatusCd);

    List<LoanContract> findByCustomerIdAndDeletedAtIsNullOrderByCntrIdDesc(Long customerId);

    /** common_db 미동기화 활성/종결 계약 — 백필 배치 핫패스. */
    List<LoanContract> findByContractIdIsNullAndCntrStatusCdInAndDeletedAtIsNullOrderByCntrIdAsc(
            Collection<String> statuses, Pageable pageable);

    /** common_db 동기화 후 브리지 컬럼 백필. */
    @Modifying
    @Transactional
    @Query("UPDATE LoanContract c SET c.contractId = :commonId WHERE c.cntrId = :cntrId")
    int updateContractId(@Param("cntrId") Long cntrId, @Param("commonId") Long commonId);
}
