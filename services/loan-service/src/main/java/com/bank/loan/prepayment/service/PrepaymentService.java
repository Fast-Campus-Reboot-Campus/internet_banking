package com.bank.loan.prepayment.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.BusinessException;
import com.bank.loan.contract.domain.LoanContract;
import com.bank.loan.contract.repository.LoanContractRepository;
import com.bank.loan.prepayment.dto.PrepayRequest;
import com.bank.loan.prepayment.dto.PrepaymentResponse;
import com.bank.loan.repayment.domain.RepaymentTransaction;
import com.bank.loan.repayment.repository.RepaymentTransactionRepository;
import com.bank.loan.schedule.domain.RepaymentSchedule;
import com.bank.loan.schedule.repository.RepaymentScheduleRepository;
import com.bank.loan.schedule.service.EqualPaymentCalculator;
import com.bank.loan.schedule.service.RepaymentScheduleService;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 중도상환(Early Repayment) 서비스 — TYPE_EARLY.
 *
 * 처리 절차:
 *   1) 멱등성 키 검사 — 동일 키 재호출 시 기존 tx 반환
 *   2) 계약 ACTIVE 검증
 *   3) outstanding = contracted - Σ(PAID schedule principal) - Σ(EARLY tx principal)
 *      amount 가 outstanding 을 초과하면 LOAN_094
 *   4) RepaymentTransaction 신규 row (TYPE_EARLY, SUCCESS, principal=amount, interest=0)
 *   5) 잔여 스케줄 재생성 (flows §2.3 동일 패턴):
 *      - 최신 버전의 DUE/OVERDUE 회차들을 모두 SUPERSEDED — status_history append
 *      - newOutstanding > 0 이면 새 버전(V{n+1}) 으로 회차 재계산해 saveAll
 *        같은 installmentNo / dueDate / appliedRateBps 유지, principal/interest 만 재산정
 *      - newOutstanding == 0 이면 새 회차 없음. 계약 종결은 별도 API (LoanClosureService).
 *
 * 단순화 가정 (본 단계):
 *   - 미발생이자(기준일까지 일할 이자) 정산 미고려 — interest_amount = 0
 *   - 중도상환 수수료 미고려 — fee_amount = 0
 *   - 부분상환(회차 일부) 미지원 (별도)
 *   - 역분개 미지원
 *   - EQUAL(원리금균등) 외 상환방식은 LOAN_084 throw
 */
@Service
@RequiredArgsConstructor
public class PrepaymentService {

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_TABLE_CD = "REPAYMENT_SCHEDULE";
    private static final String REASON_SUPERSEDED_BY_PREPAY = "SUPERSEDED_BY_PREPAY";
    private static final String DEFAULT_CHANNEL = "MANUAL";

    private final RepaymentTransactionRepository txRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final LoanContractRepository contractRepository;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;

    @Transactional
    public PrepaymentResponse prepay(Long cntrId, PrepayRequest req, String idempotencyKey) {
        // 1) 멱등성
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = txRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                RepaymentTransaction tx = existing.get();
                return PrepaymentResponse.of(tx, computeOutstandingExcluding(cntrId, tx),
                        0, null, 0);
            }
        }

        // 2) 계약 검증
        LoanContract contract = contractRepository.findByCntrIdAndDeletedAtIsNull(cntrId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_062));
        if (!contract.isActive()) {
            throw new BusinessException(LoanErrorCode.LOAN_092,
                    "current=" + contract.currentStatus());
        }
        if (!RepaymentScheduleService.REPAY_METHOD_EQUAL.equals(contract.getRepaymentMethodCd())) {
            throw new BusinessException(LoanErrorCode.LOAN_084,
                    "repaymentMethodCd=" + contract.getRepaymentMethodCd());
        }
        if (req.amount() == null || req.amount() <= 0) {
            throw new BusinessException(LoanErrorCode.LOAN_093);
        }

        // 3) outstanding 계산
        long paidFromSchedule = scheduleRepository.sumPaidPrincipal(cntrId);
        long paidFromPrepay = txRepository.sumEarlyPrincipal(cntrId);
        long outstanding = contract.getContractedAmount() - paidFromSchedule - paidFromPrepay;
        if (outstanding <= 0) {
            throw new BusinessException(LoanErrorCode.LOAN_094,
                    "outstanding=" + outstanding);
        }
        if (req.amount() > outstanding) {
            throw new BusinessException(LoanErrorCode.LOAN_094,
                    "amount=" + req.amount() + ", outstanding=" + outstanding);
        }

        long newOutstanding = outstanding - req.amount();
        OffsetDateTime now = OffsetDateTime.now();

        // 4) RepaymentTransaction 신규 row
        RepaymentTransaction saved = txRepository.save(RepaymentTransaction.builder()
                .cntrId(cntrId)
                .rschId(null)
                .rtxTypeCd(RepaymentTransaction.TYPE_EARLY)
                .totalAmount(req.amount())
                .principalAmount(req.amount())
                .interestAmount(0L)
                .overdueInterestAmount(0L)
                .feeAmount(0L)
                .currencyCd(contract.getCurrencyCd())
                .channelCd(req.channelCd() == null ? DEFAULT_CHANNEL : req.channelCd())
                .rtxStatusCd(RepaymentTransaction.STATUS_SUCCESS)
                .paidAt(now)
                .valueDate(req.valueDate())
                .balanceAfter(newOutstanding)
                .idempotencyKey(idempotencyKey)
                .reversalYn(RepaymentTransaction.YN_N)
                .build());

        // 5) 스케줄 재생성
        String currentVersion = currentVersionOrInitial(cntrId);
        List<RepaymentSchedule> active = scheduleRepository.findActiveByVersion(cntrId, currentVersion);
        for (RepaymentSchedule s : active) {
            String before = s.currentStatus();
            s.markSuperseded();
            statusHistoryPublisher.publish(StatusChangeEvent.of(
                    DOMAIN_CD, TARGET_TABLE_CD, s.getRschId(),
                    before, RepaymentSchedule.STATUS_SUPERSEDED,
                    REASON_SUPERSEDED_BY_PREPAY,
                    "rtxId=" + saved.getRtxId() + ", newVersion=" + bumpVersion(currentVersion),
                    currentActor.currentActorId()
            ));
        }

        int newCount = 0;
        String newVersion = bumpVersion(currentVersion);
        if (newOutstanding > 0 && !active.isEmpty()) {
            List<EqualPaymentCalculator.Installment> recomputed = EqualPaymentCalculator.calculate(
                    newOutstanding, contract.getTotalRateBps(), active.size());

            List<RepaymentSchedule> toSave = new ArrayList<>(active.size());
            for (int i = 0; i < active.size(); i++) {
                RepaymentSchedule old = active.get(i);
                EqualPaymentCalculator.Installment inst = recomputed.get(i);
                toSave.add(RepaymentSchedule.builder()
                        .cntrId(cntrId)
                        .installmentNo(old.getInstallmentNo())
                        .dueDate(old.getDueDate())
                        .scheduledPrincipal(inst.scheduledPrincipal())
                        .scheduledInterest(inst.scheduledInterest())
                        .scheduledTotal(inst.scheduledTotal())
                        .remainingBalance(inst.remainingBalance())
                        .appliedRateBps(contract.getTotalRateBps())
                        .rschStatusCd(RepaymentSchedule.STATUS_DUE)
                        .rschVersionCd(newVersion)
                        .build());
            }
            scheduleRepository.saveAll(toSave);
            newCount = toSave.size();
        }

        return PrepaymentResponse.of(saved, newOutstanding, active.size(), newVersion, newCount);
    }

    private String currentVersionOrInitial(Long cntrId) {
        String max = scheduleRepository.findMaxVersion(cntrId);
        return (max == null || max.isBlank()) ? RepaymentSchedule.VERSION_INITIAL : max;
    }

    /** "V1" → "V2", "V12" → "V13". 파싱 실패 시 V2 로 보정. */
    private String bumpVersion(String current) {
        if (current == null || current.length() < 2 || current.charAt(0) != 'V') {
            return "V2";
        }
        try {
            int n = Integer.parseInt(current.substring(1));
            return "V" + (n + 1);
        } catch (NumberFormatException e) {
            return "V2";
        }
    }

    /**
     * 멱등 재호출 응답에서 outstanding 표기를 위한 보조 계산.
     * 이미 반영된 tx 의 원금까지 포함된 현재 상태를 반환한다.
     */
    private long computeOutstandingExcluding(Long cntrId, RepaymentTransaction alreadyAppliedTx) {
        long paidFromSchedule = scheduleRepository.sumPaidPrincipal(cntrId);
        long paidFromPrepay = txRepository.sumEarlyPrincipal(cntrId);
        long contracted = contractRepository.findByCntrIdAndDeletedAtIsNull(cntrId)
                .map(LoanContract::getContractedAmount).orElse(0L);
        return Math.max(contracted - paidFromSchedule - paidFromPrepay, 0L);
    }
}
