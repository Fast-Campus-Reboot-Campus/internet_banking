package com.bank.loan.collateral.repository;

import com.bank.loan.collateral.domain.CollateralEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CollateralEvaluationRepository extends JpaRepository<CollateralEvaluation, Long> {

    /**
     * 담보의 최신 완료(DONE) 감정평가 조회 — LTV 산정 시 applied_value 출처.
     */
    Optional<CollateralEvaluation> findFirstByColIdAndEvalStatusCdAndDeletedAtIsNullOrderByEvaluatedAtDesc(
            Long colId, String evalStatusCd);
}
