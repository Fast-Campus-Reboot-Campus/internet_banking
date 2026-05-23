package com.bank.loan.autodebit.service;

import com.bank.loan.autodebit.dto.AutoDebitRunResponse;
import com.bank.loan.calendar.service.BusinessDayService;
import com.bank.loan.repayment.dto.RepayInstallmentRequest;
import com.bank.loan.repayment.service.RepaymentService;
import com.bank.loan.repaymentaccount.domain.RepaymentAccount;
import com.bank.loan.repaymentaccount.repository.RepaymentAccountRepository;
import com.bank.loan.schedule.domain.RepaymentSchedule;
import com.bank.loan.schedule.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 자동이체 배치 (flows §2.2).
 *
 * 실행 시점: 매일 새벽 (별도 스케줄러). 본 단계는 운영자가 baseDate 를 지정해 수동 호출.
 *
 * 처리 대상 (lookup 범위):
 *   REPAYMENT_SCHEDULE.due_date ∈ ( lastBusinessDayBefore(baseDate), baseDate ]
 *   AND rsch_status_cd = DUE AND rsch_version_cd = V1
 *   AND 해당 계약의 REPAYMENT_ACCOUNT.auto_debit_yn = Y AND racct_status_cd = VERIFIED
 *
 * 신규 약정(V8 이후) 은 스케줄 생성 시 휴일 보정으로 dueDate 가 baseDate 와 정확히 일치한다.
 * 구약정의 비영업일 dueDate (예: 토/일 회차) 는 직전 영업일 다음날부터 baseDate 까지의 구간으로 흡수되어
 * 익영업일 배치에서 처리된다 (plan 05).
 *
 * 본 단계는 외부 출금 stub — 항상 SUCCESS 가정. 실패 시뮬레이션·OVERDUE 전이는 후속(#6 연체).
 *
 * 멱등성: 회차당 idempotency_key = "AUTO-{cntrId}-{rschId}-{baseDate}" 자체 채번.
 * 같은 baseDate 재실행 시 RepaymentTransaction.idempotency_key UNIQUE 제약으로 중복 출금 차단.
 *
 * 휴일 보정(BUSINESS_CALENDAR): 호출 시 baseDate 가 비영업일이면 출금을 수행하지 않고 skipReason=NON_BUSINESS_DAY
 * 로 즉시 반환 (flows §2.2 — 휴일에는 INTEREST_ACCRUAL 만 발생, 출금은 익영업일로 이월).
 */
@Service
@RequiredArgsConstructor
public class AutoDebitBatchService {

    private static final Logger log = LoggerFactory.getLogger(AutoDebitBatchService.class);

    private static final String CHANNEL_AUTO_DEBIT = "AUTO_DEBIT";

    private final RepaymentScheduleRepository scheduleRepository;
    private final RepaymentAccountRepository repaymentAccountRepository;
    private final RepaymentService repaymentService;
    private final BusinessDayService businessDayService;

    public AutoDebitRunResponse run(String baseDate) {
        if (!businessDayService.isBusinessDay(baseDate)) {
            log.info("auto-debit skipped: baseDate={} is non-business day", baseDate);
            return AutoDebitRunResponse.skippedNonBusinessDay(baseDate);
        }

        String lastBusinessDay = businessDayService.lastBusinessDayBefore(baseDate);
        List<RepaymentSchedule> candidates = scheduleRepository
                .findDueOrPostponedForAutoDebit(
                        lastBusinessDay, baseDate,
                        RepaymentSchedule.STATUS_DUE, RepaymentSchedule.VERSION_INITIAL);

        int processed = 0;
        int skipped = 0;

        for (RepaymentSchedule schedule : candidates) {
            if (!isAutoDebitEligible(schedule.getCntrId())) {
                skipped++;
                continue;
            }
            String idemKey = buildIdempotencyKey(schedule, baseDate);
            RepayInstallmentRequest req = new RepayInstallmentRequest(
                    schedule.getInstallmentNo(), CHANNEL_AUTO_DEBIT, baseDate);
            try {
                repaymentService.repayInstallment(schedule.getCntrId(), req, idemKey);
                processed++;
            } catch (RuntimeException e) {
                log.warn("auto-debit failed for cntrId={} installmentNo={} baseDate={}: {}",
                        schedule.getCntrId(), schedule.getInstallmentNo(), baseDate, e.toString());
                skipped++;
            }
        }

        return AutoDebitRunResponse.of(baseDate, candidates.size(), processed, skipped);
    }

    private boolean isAutoDebitEligible(Long cntrId) {
        Optional<RepaymentAccount> opt = repaymentAccountRepository.findByCntrIdAndDeletedAtIsNull(cntrId);
        if (opt.isEmpty()) return false;
        RepaymentAccount account = opt.get();
        return account.isVerified() && RepaymentAccount.YN_Y.equals(account.getAutoDebitYn());
    }

    private String buildIdempotencyKey(RepaymentSchedule schedule, String baseDate) {
        return "AUTO-" + schedule.getCntrId() + "-" + schedule.getRschId() + "-" + baseDate;
    }
}
