package com.bank.loan.maturity.repository;

import com.bank.loan.maturity.domain.Maturity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaturityRepository extends JpaRepository<Maturity, Long> {

    Optional<Maturity> findByCntrIdAndDeletedAtIsNull(Long cntrId);

    /**
     * 만기 도래 배치 대상: matStatusCd = ACTIVE AND currentMaturityDate <= baseDate.
     * YYYYMMDD 사전식 비교가 곧 날짜 비교.
     */
    List<Maturity> findByMatStatusCdAndCurrentMaturityDateLessThanEqualAndDeletedAtIsNullOrderByCntrIdAsc(
            String matStatusCd, String baseDate);
}
