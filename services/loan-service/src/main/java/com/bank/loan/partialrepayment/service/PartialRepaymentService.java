package com.bank.loan.partialrepayment.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.BusinessException;
import com.bank.loan.accrual.repository.InterestAccrualRepository;
import com.bank.loan.contract.domain.LoanContract;
import com.bank.loan.contract.repository.LoanContractRepository;
import com.bank.loan.partialrepayment.dto.PartialRepayRequest;
import com.bank.loan.partialrepayment.dto.PartialRepaymentResponse;
import com.bank.loan.repayment.domain.RepaymentTransaction;
import com.bank.loan.repayment.repository.RepaymentTransactionRepository;
import com.bank.loan.schedule.domain.RepaymentSchedule;
import com.bank.loan.schedule.repository.RepaymentScheduleRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 회차 부분상환(Partial Repayment) 서비스 — TYPE_PARTIAL.
 *
 * 처리 절차:
 *   1) 멱등성 키 검사
 *   2) 계약 + 최신 버전 회차 조회. 회차 상태가 DUE/OVERDUE/PARTIAL_PAID 아니면 LOAN_091.
 *   3) cumulative = sumPaidByRschId(rschId), remaining = scheduledTotal - cumulative
 *      amount > remaining 이면 LOAN_098.
 *   4) 분배 — 순차 분배 (flows §2.2 정석: 연체이자 → 정상이자 → 원금 → 수수료).
 *      본 단계는 연체이자/수수료 0 가정이므로 "이자 먼저 → 원금" 단순화 적용.
 *        actualInterest = 회차 기간(prev_due_date, due_date] InterestAccrual.daily_interest_amt 합
 *                         (배치 미실행 시 scheduled_interest 로 fallback)
 *        paidInterestCumulative = 이 회차에 이미 갚힌 누적 이자
 *        remainingInterest = max(0, actualInterest - paidInterestCumulative)
 *        interestPortion = min(amount, remainingInterest)
 *        principalPortion = amount - interestPortion
 *   5) RepaymentTransaction 신규 row (TYPE_PARTIAL, SUCCESS).
 *   6) cumulative + amount == scheduledTotal 이면 markPaid(), 아니면 markPartialPaid().
 *      회차 status 변경 시에만 status_history publish (PARTIAL_PAID → PARTIAL_PAID 는 발행 안 함).
 *
 * 연체이자/수수료 본격 처리(분배 순서 1·4단계) 는 본 단계 외 — Delinquency·LoanProduct 정책 연계 후속.
 */
@Service
@RequiredArgsConstructor
public class PartialRepaymentService {

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_TABLE_CD = "REPAYMENT_SCHEDULE";
    private static final String REASON_PARTIAL_PAID = "INSTALLMENT_PARTIAL_PAID";
    private static final String REASON_FULLY_PAID   = "INSTALLMENT_PAID";
    private static final String DEFAULT_CHANNEL = "MANUAL";

    private final RepaymentTransactionRepository txRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final LoanContractRepository contractRepository;
    private final InterestAccrualRepository accrualRepository;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;

    @Transactional
    public PartialRepaymentResponse repay(Long cntrId, PartialRepayRequest req, String idempotencyKey) {
        // 1) 멱등성
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = txRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                RepaymentTransaction tx = existing.get();
                RepaymentSchedule schedule = scheduleRepository.findById(tx.getRschId())
                        .filter(s -> s.getDeletedAt() == null)
                        .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_090));
                long cumulative = txRepository.sumPaidByRschId(schedule.getRschId());
                return PartialRepaymentResponse.of(tx, schedule, cumulative);
            }
        }

        // 2) 계약·회차 조회
        LoanContract contract = contractRepository.findByCntrIdAndDeletedAtIsNull(cntrId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_062));

        String version = resolveLatestVersion(cntrId);
        RepaymentSchedule schedule = scheduleRepository
                .findByCntrIdAndInstallmentNoAndRschVersionCdAndDeletedAtIsNull(
                        cntrId, req.installmentNo(), version)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_090,
                        "cntrId=" + cntrId + ", installmentNo=" + req.installmentNo()
                                + ", version=" + version));
        if (!schedule.isPartialPayable()) {
            throw new BusinessException(LoanErrorCode.LOAN_091,
                    "current=" + schedule.currentStatus());
        }

        // 3) 잔액 검증
        long cumulative = txRepository.sumPaidByRschId(schedule.getRschId());
        long remaining = schedule.getScheduledTotal() - cumulative;
        if (req.amount() > remaining) {
            throw new BusinessException(LoanErrorCode.LOAN_098,
                    "amount=" + req.amount() + ", remaining=" + remaining);
        }

        // 4) 분배 — 이자 먼저 → 원금 (순차)
        long actualInterest = computeAccruedInterest(contract, schedule);
        long paidInterestCumulative = txRepository.sumPaidInterestByRschId(schedule.getRschId());
        long remainingInterest = Math.max(0L, actualInterest - paidInterestCumulative);
        long interestPortion = Math.min(req.amount(), remainingInterest);
        long principalPortion = req.amount() - interestPortion;

        OffsetDateTime now = OffsetDateTime.now();
        long newCumulative = cumulative + req.amount();
        boolean fullyPaid = (newCumulative == schedule.getScheduledTotal());

        // 5) tx 저장
        RepaymentTransaction saved = txRepository.save(RepaymentTransaction.builder()
                .cntrId(cntrId)
                .rschId(schedule.getRschId())
                .rtxTypeCd(RepaymentTransaction.TYPE_PARTIAL)
                .totalAmount(req.amount())
                .principalAmount(principalPortion)
                .interestAmount(interestPortion)
                .overdueInterestAmount(0L)
                .feeAmount(0L)
                .currencyCd(contract.getCurrencyCd())
                .channelCd(req.channelCd() == null ? DEFAULT_CHANNEL : req.channelCd())
                .rtxStatusCd(RepaymentTransaction.STATUS_SUCCESS)
                .paidAt(now)
                .valueDate(req.valueDate())
                .balanceAfter(schedule.getScheduledTotal() - newCumulative)
                .idempotencyKey(idempotencyKey)
                .reversalYn(RepaymentTransaction.YN_N)
                .build());

        // 6) 회차 상태 전이 (변경 시에만 publish)
        String before = schedule.currentStatus();
        if (fullyPaid) {
            schedule.markPaid();
            statusHistoryPublisher.publish(StatusChangeEvent.of(
                    DOMAIN_CD, TARGET_TABLE_CD, schedule.getRschId(),
                    before, RepaymentSchedule.STATUS_PAID,
                    REASON_FULLY_PAID,
                    "rtxId=" + saved.getRtxId() + " (final partial)",
                    currentActor.currentActorId()
            ));
        } else if (!RepaymentSchedule.STATUS_PARTIAL_PAID.equals(before)) {
            schedule.markPartialPaid();
            statusHistoryPublisher.publish(StatusChangeEvent.of(
                    DOMAIN_CD, TARGET_TABLE_CD, schedule.getRschId(),
                    before, RepaymentSchedule.STATUS_PARTIAL_PAID,
                    REASON_PARTIAL_PAID,
                    "rtxId=" + saved.getRtxId() + ", cumulative=" + newCumulative,
                    currentActor.currentActorId()
            ));
        }
        // before == PARTIAL_PAID && !fullyPaid 인 경우는 status 변경 없음 → publish 생략

        return PartialRepaymentResponse.of(saved, schedule, newCumulative);
    }

    /**
     * 회차 귀속 기간(prev_due_date, due_date] InterestAccrual.daily_interest_amt 합.
     * 첫 회차는 cntr_start_date 기준, 이전 회차는 같은 버전에서 조회.
     * accrual 배치가 한 번도 안 돌았으면 scheduled_interest 로 fallback.
     * (RepaymentService.computeInterestPortion 과 동일 산식)
     */
    private long computeAccruedInterest(LoanContract contract, RepaymentSchedule schedule) {
        String fromExclusive;
        if (schedule.getInstallmentNo() == 1) {
            fromExclusive = contract.getCntrStartDate();
        } else {
            fromExclusive = scheduleRepository
                    .findByCntrIdAndInstallmentNoAndRschVersionCdAndDeletedAtIsNull(
                            schedule.getCntrId(),
                            schedule.getInstallmentNo() - 1,
                            schedule.getRschVersionCd())
                    .map(RepaymentSchedule::getDueDate)
                    .orElse(contract.getCntrStartDate());
        }
        long actual = accrualRepository.sumDailyInterestInRange(
                schedule.getCntrId(), fromExclusive, schedule.getDueDate());
        return actual > 0 ? actual : schedule.getScheduledInterest();
    }

    private String resolveLatestVersion(Long cntrId) {
        String max = scheduleRepository.findMaxVersion(cntrId);
        return (max == null || max.isBlank()) ? RepaymentSchedule.VERSION_INITIAL : max;
    }
}
