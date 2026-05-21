package com.bank.loan.reversal.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.BusinessException;
import com.bank.loan.repayment.domain.RepaymentTransaction;
import com.bank.loan.repayment.repository.RepaymentTransactionRepository;
import com.bank.loan.reversal.dto.ReverseRepaymentRequest;
import com.bank.loan.reversal.dto.ReversalResponse;
import com.bank.loan.schedule.domain.RepaymentSchedule;
import com.bank.loan.schedule.repository.RepaymentScheduleRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 상환 거래 역분개(Reversal) 서비스 — TYPE_REVERSAL.
 *
 * 처리 절차:
 *   1) 멱등성 키 검사 — 동일 키 재호출 시 기존 reversal row 반환
 *   2) 대상 tx 조회 (cntrId 일치 + SUCCESS + reversalYn=N)
 *   3) 본 단계 지원 범위: rtxTypeCd = SCHEDULED 만 허용 (EARLY 역분개는 스케줄 V 재되돌리기가
 *      필요해 별도 작업으로 분리). 그 외 LOAN_096.
 *   4) 이미 활성 reversal 이 존재하면 LOAN_097.
 *   5) 새 RepaymentTransaction row (TYPE_REVERSAL, reversal_yn=Y, reversal_target_rtx_id=원본).
 *      금액은 원본과 동일 양수 — 회계 반대분개(common_transaction) 는 본 단계 외.
 *   6) 대응 RepaymentSchedule (PAID) 가 있으면 DUE 로 되돌리고 status_history append.
 *      EARLY 원본은 rschId 가 null 이라 단계 3 에서 이미 차단.
 *
 * 정정의 정정(역분개의 역분개) 는 미지원 — 동일 원본에 활성 reversal 이 한 건이라도 있으면 LOAN_097.
 */
@Service
@RequiredArgsConstructor
public class ReversalService {

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_TABLE_CD = "REPAYMENT_SCHEDULE";
    private static final String REASON_REVERSED = "REPAYMENT_REVERSED";
    private static final String DEFAULT_CHANNEL = "MANUAL";

    private final RepaymentTransactionRepository txRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;

    @Transactional
    public ReversalResponse reverse(Long cntrId, Long rtxId, ReverseRepaymentRequest req,
                                    String idempotencyKey) {
        // 1) 멱등성
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = txRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                RepaymentTransaction tx = existing.get();
                return ReversalResponse.of(tx, tx.getRschId());
            }
        }

        // 2) 대상 tx 검증
        RepaymentTransaction target = txRepository.findByRtxIdAndDeletedAtIsNull(rtxId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_095));
        if (!target.getCntrId().equals(cntrId)) {
            throw new BusinessException(LoanErrorCode.LOAN_095,
                    "cntrId mismatch: tx.cntrId=" + target.getCntrId() + ", path=" + cntrId);
        }
        if (!RepaymentTransaction.STATUS_SUCCESS.equals(target.getRtxStatusCd())) {
            throw new BusinessException(LoanErrorCode.LOAN_096,
                    "status=" + target.getRtxStatusCd());
        }
        if (RepaymentTransaction.YN_Y.equals(target.getReversalYn())) {
            throw new BusinessException(LoanErrorCode.LOAN_096,
                    "target is itself a reversal row");
        }

        // 3) SCHEDULED 만 허용 (본 단계)
        if (!RepaymentTransaction.TYPE_SCHEDULED.equals(target.getRtxTypeCd())) {
            throw new BusinessException(LoanErrorCode.LOAN_096,
                    "type=" + target.getRtxTypeCd() + " (only SCHEDULED supported)");
        }

        // 4) 중복 reversal 차단
        if (txRepository.existsActiveReversal(target.getRtxId())) {
            throw new BusinessException(LoanErrorCode.LOAN_097);
        }

        OffsetDateTime now = OffsetDateTime.now();

        // 5) 새 reversal row
        RepaymentTransaction reversal = txRepository.save(RepaymentTransaction.builder()
                .cntrId(cntrId)
                .rschId(target.getRschId())
                .rtxTypeCd(RepaymentTransaction.TYPE_REVERSAL)
                .totalAmount(target.getTotalAmount())
                .principalAmount(target.getPrincipalAmount())
                .interestAmount(target.getInterestAmount())
                .overdueInterestAmount(target.getOverdueInterestAmount())
                .feeAmount(target.getFeeAmount())
                .currencyCd(target.getCurrencyCd())
                .channelCd(DEFAULT_CHANNEL)
                .rtxStatusCd(RepaymentTransaction.STATUS_SUCCESS)
                .paidAt(now)
                .valueDate(null)
                .balanceAfter(null)
                .idempotencyKey(idempotencyKey)
                .reversalYn(RepaymentTransaction.YN_Y)
                .reversalTargetRtxId(target.getRtxId())
                .build());

        // 6) 대응 스케줄 되돌림 (SCHEDULED 는 rschId 보장)
        Long restoredRschId = null;
        if (target.getRschId() != null) {
            RepaymentSchedule schedule = scheduleRepository.findById(target.getRschId())
                    .filter(s -> s.getDeletedAt() == null)
                    .orElse(null);
            if (schedule != null && schedule.isPaid()) {
                String before = schedule.currentStatus();
                schedule.markDue();
                statusHistoryPublisher.publish(StatusChangeEvent.of(
                        DOMAIN_CD, TARGET_TABLE_CD, schedule.getRschId(),
                        before, RepaymentSchedule.STATUS_DUE,
                        REASON_REVERSED,
                        "reversalRtxId=" + reversal.getRtxId()
                                + (req.reversalReasonCd() == null ? "" : ", reason=" + req.reversalReasonCd())
                                + (req.reversalRemark()   == null ? "" : ", remark=" + req.reversalRemark()),
                        currentActor.currentActorId()
                ));
                restoredRschId = schedule.getRschId();
            }
        }

        return ReversalResponse.of(reversal, restoredRschId);
    }
}
