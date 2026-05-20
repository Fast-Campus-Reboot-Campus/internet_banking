package com.bank.loan.repayment.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.BusinessException;
import com.bank.loan.contract.domain.LoanContract;
import com.bank.loan.contract.repository.LoanContractRepository;
import com.bank.loan.repayment.domain.RepaymentTransaction;
import com.bank.loan.repayment.dto.RepayInstallmentRequest;
import com.bank.loan.repayment.dto.RepaymentTransactionListResponse;
import com.bank.loan.repayment.dto.RepaymentTransactionResponse;
import com.bank.loan.repayment.repository.RepaymentTransactionRepository;
import com.bank.loan.schedule.domain.RepaymentSchedule;
import com.bank.loan.schedule.repository.RepaymentScheduleRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 상환 처리 (수동/창구) 서비스.
 *
 * 본 단계 시나리오: 회차 정확액 정상 상환 1건 (rtx_type_cd=SCHEDULED, status=SUCCESS).
 *   - 분배: 회차 스케줄의 scheduled_principal / scheduled_interest 그대로 사용
 *   - 연체이자·수수료 0 (연체 라이프사이클 미구현)
 *   - 부분상환·중도상환·역분개·자동이체는 후속
 *
 * 멱등성:
 *   - Idempotency-Key 헤더로 보호. 동일 키 재호출 시 기존 tx 그대로 반환.
 *
 * 상태 전이:
 *   REPAYMENT_SCHEDULE: DUE/OVERDUE → PAID (status_history 기록)
 *   REPAYMENT_TRANSACTION: 신규 row, status=SUCCESS
 *
 * 분배 순서(flows §2.2): 연체이자 → 정상이자 → 원금 → 수수료.
 * 본 단계는 정상 회차만 다루므로 (정상이자 + 원금)만 0이 아닌 값이 채워진다.
 */
@Service
@RequiredArgsConstructor
public class RepaymentService {

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_TABLE_CD = "REPAYMENT_SCHEDULE";
    private static final String REASON_INSTALLMENT_PAID = "INSTALLMENT_PAID";
    private static final String DEFAULT_CHANNEL = "MANUAL";

    private final RepaymentTransactionRepository txRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final LoanContractRepository contractRepository;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;

    @Transactional
    public RepaymentTransactionResponse repayInstallment(Long cntrId, RepayInstallmentRequest req, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = txRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return RepaymentTransactionResponse.of(existing.get());
            }
        }

        LoanContract contract = contractRepository.findByCntrIdAndDeletedAtIsNull(cntrId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_062));

        RepaymentSchedule schedule = scheduleRepository
                .findByCntrIdAndInstallmentNoAndRschVersionCdAndDeletedAtIsNull(
                        cntrId, req.installmentNo(), RepaymentSchedule.VERSION_INITIAL)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_090,
                        "cntrId=" + cntrId + ", installmentNo=" + req.installmentNo()));

        if (schedule.isPaid()) {
            throw new BusinessException(LoanErrorCode.LOAN_091,
                    "installmentNo=" + req.installmentNo());
        }
        if (!schedule.isPayable()) {
            throw new BusinessException(LoanErrorCode.LOAN_091,
                    "current=" + schedule.currentStatus());
        }

        OffsetDateTime now = OffsetDateTime.now();
        RepaymentTransaction saved = txRepository.save(RepaymentTransaction.builder()
                .cntrId(cntrId)
                .rschId(schedule.getRschId())
                .rtxTypeCd(RepaymentTransaction.TYPE_SCHEDULED)
                .totalAmount(schedule.getScheduledTotal())
                .principalAmount(schedule.getScheduledPrincipal())
                .interestAmount(schedule.getScheduledInterest())
                .overdueInterestAmount(0L)
                .feeAmount(0L)
                .currencyCd(contract.getCurrencyCd())
                .channelCd(req.channelCd() == null ? DEFAULT_CHANNEL : req.channelCd())
                .rtxStatusCd(RepaymentTransaction.STATUS_SUCCESS)
                .paidAt(now)
                .valueDate(req.valueDate())
                .balanceAfter(schedule.getRemainingBalance())
                .idempotencyKey(idempotencyKey)
                .reversalYn(RepaymentTransaction.YN_N)
                .build());

        String before = schedule.currentStatus();
        schedule.markPaid();
        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_TABLE_CD, schedule.getRschId(),
                before, RepaymentSchedule.STATUS_PAID,
                REASON_INSTALLMENT_PAID,
                "rtxId=" + saved.getRtxId(),
                currentActor.currentActorId()
        ));

        return RepaymentTransactionResponse.of(saved);
    }

    @Transactional(readOnly = true)
    public RepaymentTransactionListResponse list(Long cntrId) {
        contractRepository.findByCntrIdAndDeletedAtIsNull(cntrId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_062));

        List<RepaymentTransactionResponse> items = txRepository
                .findByCntrIdAndDeletedAtIsNullOrderByPaidAtAsc(cntrId)
                .stream()
                .map(RepaymentTransactionResponse::of)
                .toList();
        return RepaymentTransactionListResponse.of(cntrId, items);
    }
}
