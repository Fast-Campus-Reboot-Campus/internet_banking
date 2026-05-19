package com.bank.loan.collateral.repository;

import com.bank.loan.collateral.domain.Collateral;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollateralRepository extends JpaRepository<Collateral, Long> {

    Optional<Collateral> findByColIdAndDeletedAtIsNull(Long colId);

    List<Collateral> findByApplIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long applId);
}
