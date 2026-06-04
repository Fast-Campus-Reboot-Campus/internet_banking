package com.bank.commonaccount.contract.repository;

import com.bank.commonaccount.contract.domain.CommonContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommonContractRepository extends JpaRepository<CommonContract, Long> {

    /** 자연키(contract_no)로 조회 — write-through 멱등 dedupe. */
    Optional<CommonContract> findByContractNo(String contractNo);
}
