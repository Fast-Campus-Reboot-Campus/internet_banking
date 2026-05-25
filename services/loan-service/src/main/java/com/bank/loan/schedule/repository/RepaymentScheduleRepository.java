package com.bank.loan.schedule.repository;

import com.bank.loan.schedule.domain.RepaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
     * autodebit 후보 회차 lookup — baseDate 당일 회차 + 직전 비영업일 동안 미수행된 회차 까지.
     * 범위: dueDate ∈ (lastBusinessDayBefore(baseDate), baseDate] AND status=DUE AND version=V1.
     *
     * 신규 약정(V8 마이그레이션 이후) 은 스케줄 생성 시 휴일 보정되므로 dueDate 가 baseDate 와 정확히 일치하지만,
     * 구약정의 비영업일 dueDate 가 다음 영업일 배치에 흡수되도록 범위 lookup 으로 단순화.
     */
    @Query("""
            select s from RepaymentSchedule s
             where s.dueDate > :lastBusinessDay
               and s.dueDate <= :baseDate
               and s.rschStatusCd = :status
               and s.rschVersionCd = :version
               and s.deletedAt is null
             order by s.cntrId asc, s.installmentNo asc
            """)
    List<RepaymentSchedule> findDueOrPostponedForAutoDebit(
            @Param("lastBusinessDay") String lastBusinessDay,
            @Param("baseDate") String baseDate,
            @Param("status") String status,
            @Param("version") String version);

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

    /**
     * 원자적 상태 전이 — allowedStatuses 중 하나인 경우에만 newStatus 로 변경한다.
     *
     * "SELECT → 메모리 확인 → UPDATE" 구조에서 발생하는 Race Condition 대신
     * DB 단일 UPDATE 로 조회·갱신을 원자적으로 처리한다.
     * affected=1 이면 이 요청이 선점 성공, affected=0 이면 다른 스레드가 먼저 전이함.
     *
     * clearAutomatically=true: UPDATE 후 1차 캐시를 비워 이후 조회가 stale 데이터를 반환하지 않도록 한다.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE RepaymentSchedule s
               SET s.rschStatusCd = :newStatus
             WHERE s.rschId        = :rschId
               AND s.rschStatusCd IN :allowedStatuses
               AND s.deletedAt    IS NULL
            """)
    int claimStatusChange(@Param("rschId") Long rschId,
                          @Param("newStatus") String newStatus,
                          @Param("allowedStatuses") List<String> allowedStatuses);
}
