package com.bank.loan.execution.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.BusinessException;
import com.bank.loan.contract.domain.LoanContract;
import com.bank.loan.contract.repository.LoanContractRepository;
import com.bank.loan.execution.domain.LoanExecution;
import com.bank.loan.execution.dto.DrawdownRequest;
import com.bank.loan.execution.dto.LoanExecutionResponse;
import com.bank.loan.execution.repository.LoanExecutionRepository;
import com.bank.loan.repaymentaccount.domain.RepaymentAccount;
import com.bank.loan.repaymentaccount.repository.RepaymentAccountRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 자금 인출 (Drawdown) 서비스.
 *
 * 흐름:
 *   1) 멱등성 키가 이미 처리됐으면 기존 응답 그대로 반환
 *   2) 계약 활성 검증 (LOAN_062 / LOAN_063)
 *   3) 누적 인출 + 신청 ≤ contracted_amount 검증 (LOAN_064)
 *   4) loan_execution INSERT (status=DONE, executed_at=now, journal_entry_no 자체 채번)
 *   5) 최초 인출이면 계약 상태 SIGNED → ACTIVE 전이 + status_history
 *
 * journal_entry_no 및 transaction_id 자체 채번 — 결제·회계 도메인 도입 시 외부 거래 ID 로 교체.
 */
@Service
@RequiredArgsConstructor
public class LoanExecutionService {

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_CONTRACT = "LOAN_CONTRACT";
    private static final String DEFAULT_CURRENCY = "KRW";
    private static final String REASON_FIRST_DRAWDOWN = "FIRST_DRAWDOWN";

    private final LoanExecutionRepository repository;
    private final LoanContractRepository contractRepository;
    private final RepaymentAccountRepository repaymentAccountRepository;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;

    @Transactional
    public LoanExecutionResponse drawdown(Long cntrId, DrawdownRequest req, String idempotencyKey) {
        // 1) 멱등성
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = repository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                long cumul = repository.sumDoneAmount(existing.get().getCntrId());
                return LoanExecutionResponse.of(existing.get(), cumul);
            }
        }

        // 2) 계약 검증
        LoanContract contract = contractRepository.findByCntrIdAndDeletedAtIsNull(cntrId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_062));
        if (!contract.isDrawdownAllowed()) {
            throw new BusinessException(LoanErrorCode.LOAN_063);
        }

        // 2-1) 상환계좌 사전조건 — flows §1.1 CONTRACTED→DISBURSED
        RepaymentAccount racct = repaymentAccountRepository.findByCntrIdAndDeletedAtIsNull(cntrId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_080));
        if (!racct.isVerified()) {
            throw new BusinessException(LoanErrorCode.LOAN_083,
                    "racctStatusCd=" + racct.currentStatus());
        }

        // 3) 한도 검증
        long drawnSoFar = repository.sumDoneAmount(contract.getCntrId());
        long requested = req.executedAmount();
        if (drawnSoFar + requested > contract.getContractedAmount()) {
            throw new BusinessException(LoanErrorCode.LOAN_064,
                    "drawnSoFar(" + drawnSoFar + ") + requested(" + requested
                            + ") > contracted(" + contract.getContractedAmount() + ")");
        }

        // 4) 실행 INSERT
        OffsetDateTime now = OffsetDateTime.now();
        LoanExecution saved = repository.save(LoanExecution.builder()
                .cntrId(contract.getCntrId())
                .transactionId(null) // 공통 거래원장 미구현 — 추후 결제 도메인 도입 시 채움
                .executedAmount(requested)
                .currencyCd(req.currencyCd() == null ? DEFAULT_CURRENCY : req.currencyCd())
                .execStatusCd(LoanExecution.STATUS_DONE)
                .disbursementBankCd(req.disbursementBankCd())
                .disbursementAccountMasked(req.maskedAccount())
                .executedAt(now)
                .valueDate(req.valueDate())
                .feeAmount(req.feeAmount() == null ? 0L : req.feeAmount())
                .idempotencyKey(idempotencyKey)
                .journalEntryNo("JE-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                .build());

        // 5) 최초 인출 시 계약 상태 전이
        if (LoanContract.STATUS_SIGNED.equals(contract.currentStatus())) {
            String before = contract.currentStatus();
            contract.markActiveOnFirstDrawdown();
            statusHistoryPublisher.publish(StatusChangeEvent.of(
                    DOMAIN_CD, TARGET_CONTRACT, contract.getCntrId(),
                    before, LoanContract.STATUS_ACTIVE,
                    REASON_FIRST_DRAWDOWN, "execId=" + saved.getExecId(),
                    currentActor.currentActorId()
            ));
        }

        long cumul = drawnSoFar + requested;
        return LoanExecutionResponse.of(saved, cumul);
    }
}
