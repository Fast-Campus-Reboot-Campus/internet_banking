package com.bank.loan.schedule.repository;

import com.bank.loan.schedule.domain.RepaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, Long> {

    List<RepaymentSchedule> findByCntrIdAndRschVersionCdAndDeletedAtIsNullOrderByInstallmentNoAsc(
            Long cntrId, String rschVersionCd);

    boolean existsByCntrIdAndDeletedAtIsNull(Long cntrId);

    Optional<RepaymentSchedule> findByCntrIdAndInstallmentNoAndRschVersionCdAndDeletedAtIsNull(
            Long cntrId, Integer installmentNo, String rschVersionCd);

    List<RepaymentSchedule> findByDueDateAndRschStatusCdAndRschVersionCdAndDeletedAtIsNullOrderByCntrIdAscInstallmentNoAsc(
            String dueDate, String rschStatusCd, String rschVersionCd);

    List<RepaymentSchedule> findByRschStatusCdAndDueDateLessThanAndRschVersionCdAndDeletedAtIsNullOrderByCntrIdAscInstallmentNoAsc(
            String rschStatusCd, String dueDate, String rschVersionCd);

    List<RepaymentSchedule> findByCntrIdAndRschStatusCdAndRschVersionCdAndDeletedAtIsNullOrderByInstallmentNoAsc(
            Long cntrId, String rschStatusCd, String rschVersionCd);

    @Query("""
            select coalesce(max(s.rschVersionCd), '')
              from RepaymentSchedule s
             where s.cntrId = :cntrId
               and s.deletedAt is null
            """)
    String findMaxVersion(@Param("cntrId") Long cntrId);

    @Query("""
            select coalesce(sum(s.scheduledPrincipal), 0)
              from RepaymentSchedule s
             where s.cntrId = :cntrId
               and s.rschStatusCd = 'PAID'
               and s.deletedAt is null
            """)
    long sumPaidPrincipal(@Param("cntrId") Long cntrId);
}
