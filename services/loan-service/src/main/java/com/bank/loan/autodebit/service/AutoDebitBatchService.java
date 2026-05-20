package com.bank.loan.autodebit.service;

import com.bank.loan.autodebit.dto.AutoDebitRunResponse;
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
 * 처리 대상:
 *   REPAYMENT_SCHEDULE.due_date = baseDate AND rsch_status_cd = DUE AND rsch_version_cd = V1
 *   AND 해당 계약의 REPAYMENT_ACCOUNT.auto_debit_yn = Y AND racct_status_cd = VERIFIED
 *
 * 본 단계는 외부 출금 stub — 항상 SUCCESS 가정. 실패 시뮬레이션·OVERDUE 전이는 후속(#6 연체).
 *
 * 멱등성: 회차당 idempotency_key = "AUTO-{cntrId}-{rschId}-{baseDate}" 자체 채번.
 * 같은 baseDate 재실행 시 RepaymentTransaction.idempotency_key UNIQUE 제약으로 중복 출금 차단.
 *
 * 휴일 보정(BUSINESS_CALENDAR) 은 본 단계 외 — 호출자가 영업일을 baseDate 로 넘긴다고 가정.
 */
@Service
@RequiredArgsConstructor
public class AutoDebitBatchService {

    private static final Logger log = LoggerFactory.getLogger(AutoDebitBatchService.class);

    private static final String CHANNEL_AUTO_DEBIT = "AUTO_DEBIT";

    private final RepaymentScheduleRepository scheduleRepository;
    private final RepaymentAccountRepository repaymentAccountRepository;
    private final RepaymentService repaymentService;

    public AutoDebitRunResponse run(String baseDate) {
        List<RepaymentSchedule> candidates = scheduleRepository
                .findByDueDateAndRschStatusCdAndRschVersionCdAndDeletedAtIsNullOrderByCntrIdAscInstallmentNoAsc(
                        baseDate, RepaymentSchedule.STATUS_DUE, RepaymentSchedule.VERSION_INITIAL);

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
