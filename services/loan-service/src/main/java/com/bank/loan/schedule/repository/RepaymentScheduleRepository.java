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

    @Query("""
            select case when count(s) > 0 then true else false end
              from RepaymentSchedule s
             where s.cntrId = :cntrId
               and s.rschStatusCd in ('DUE','OVERDUE')
               and s.deletedAt is null
            """)
    boolean existsActiveInstallment(@Param("cntrId") Long cntrId);

    /**
     * 특정 버전에서 DUE/OVERDUE 인 회차를 installmentNo 오름차순으로 반환.
     * 중도상환 시 SUPERSEDED 대상 + 다음 버전 재생성 베이스가 된다.
     */
    @Query("""
            select s
              from RepaymentSchedule s
             where s.cntrId = :cntrId
               and s.rschVersionCd = :version
               and s.rschStatusCd in ('DUE','OVERDUE')
               and s.deletedAt is null
             order by s.installmentNo asc
            """)
    List<RepaymentSchedule> findActiveByVersion(@Param("cntrId") Long cntrId,
                                                @Param("version") String rschVersionCd);
}
