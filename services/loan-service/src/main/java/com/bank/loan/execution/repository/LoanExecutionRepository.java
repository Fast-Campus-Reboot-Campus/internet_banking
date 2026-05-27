package com.bank.loan.execution.repository;

import com.bank.loan.execution.domain.LoanExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface LoanExecutionRepository extends JpaRepository<LoanExecution, Long> {

    Optional<LoanExecution> findByIdempotencyKey(String idempotencyKey);

    /** DONE 상태 인출 합계. null 안전. */
    @Query("SELECT COALESCE(SUM(e.executedAmount), 0) FROM LoanExecution e " +
            "WHERE e.cntrId = :cntrId AND e.execStatusCd = 'DONE' AND e.deletedAt IS NULL")
    long sumDoneAmount(@Param("cntrId") Long cntrId);

    /** 활성·DONE 트랜치 수. */
    @Query("SELECT COUNT(e) FROM LoanExecution e " +
            "WHERE e.cntrId = :cntrId AND e.execStatusCd = 'DONE' AND e.deletedAt IS NULL")
    long countDone(@Param("cntrId") Long cntrId);

    @Query("SELECT MAX(e.executedAt) FROM LoanExecution e " +
            "WHERE e.cntrId = :cntrId AND e.execStatusCd = 'DONE' AND e.deletedAt IS NULL")
    Optional<OffsetDateTime> findLastExecutedAt(@Param("cntrId") Long cntrId);
}
