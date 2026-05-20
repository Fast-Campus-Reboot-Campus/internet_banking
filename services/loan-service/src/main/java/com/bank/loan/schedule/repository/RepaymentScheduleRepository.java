package com.bank.loan.schedule.repository;

import com.bank.loan.schedule.domain.RepaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
