package com.bank.loan.ltv.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.BusinessException;
import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.application.repository.LoanApplicationRepository;
import com.bank.loan.collateral.domain.Collateral;
import com.bank.loan.collateral.domain.CollateralEvaluation;
import com.bank.loan.collateral.repository.CollateralEvaluationRepository;
import com.bank.loan.collateral.repository.CollateralRepository;
import com.bank.loan.ltv.domain.LtvCalculation;
import com.bank.loan.ltv.dto.LtvCalculationResponse;
import com.bank.loan.ltv.dto.RunLtvCalculationRequest;
import com.bank.loan.ltv.repository.LtvCalculationRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * LTV(담보가치비율) 산정 서비스 — flows §1.1, §2.1 의 "LTV 산출 잡".
 *
 * 흐름:
 *   1) 담보 존재 검증 (LOAN_050)
 *   2) 담보 상태 검증 (RELEASED/REJECTED 차단 → LOAN_051 재사용 의미 다름 → LOAN_052 로 묶음)
 *   3) 중복 LTV 차단 (LOAN_053, col_id 당 1건)
 *   4) 사전조건: 담보 감정평가 DONE 존재 (LOAN_052)
 *   5) LTV 계산:
 *        applied_col_value  = 최신 감정평가 applied_value (입력 override 가능)
 *        senior_lien_amount = 담보의 senior_lien_amount (입력 override 가능)
 *        requested_amount   = 신청의 requested_amount   (입력 override 가능)
 *        max_loan_amount    = max(applied_col_value × limit_bps/10000 - senior_lien, 0)
 *        ltv_ratio_bps      = requested_amount / applied_col_value × 10000
 *        status             = PASS (requested ≤ max_loan) | FAIL
 *   6) status_history 발행
 */
@Service
@RequiredArgsConstructor
public class LtvCalculationService {

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_LTV = "LTV_CALCULATION";
    private static final String REASON_LTV_CALCULATED = "LTV_CALCULATED";

    private final LtvCalculationRepository repository;
    private final CollateralRepository collateralRepository;
    private final CollateralEvaluationRepository collateralEvaluationRepository;
    private final LoanApplicationRepository applicationRepository;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;

    @Transactional
    public LtvCalculationResponse run(Long colId, RunLtvCalculationRequest req) {
        Collateral collateral = collateralRepository.findByColIdAndDeletedAtIsNull(colId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_050));

        // 담보 상태 검증 — 해제·반려된 담보는 LTV 무의미
        String colStatus = collateral.currentStatus();
        if (Collateral.STATUS_RELEASED.equals(colStatus) || Collateral.STATUS_REJECTED.equals(colStatus)) {
            throw new BusinessException(LoanErrorCode.LOAN_052,
                    "colStatus=" + colStatus);
        }

        if (repository.findByColIdAndDeletedAtIsNull(colId).isPresent()) {
            throw new BusinessException(LoanErrorCode.LOAN_053);
        }

        // 사전조건: 담보 감정평가 DONE 1건 이상
        CollateralEvaluation eval = collateralEvaluationRepository
                .findFirstByColIdAndEvalStatusCdAndDeletedAtIsNullOrderByEvaluatedAtDesc(
                        colId, CollateralEvaluation.STATUS_DONE)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_052,
                        "collateral evaluation DONE required"));

        LoanApplication application = applicationRepository
                .findByApplIdAndDeletedAtIsNull(collateral.getApplId())
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_012));

        long appliedColValue = req.appliedColValue() != null
                ? req.appliedColValue()
                : eval.getAppliedValue();
        Long seniorLien = req.seniorLienAmount() != null
                ? req.seniorLienAmount()
                : collateral.getSeniorLienAmount();
        long seniorLienSafe = seniorLien != null ? seniorLien : 0L;
        long requested = req.requestedAmount() != null
                ? req.requestedAmount()
                : application.getRequestedAmount();
        int limitBps = req.ltvLimitBps() != null
                ? req.ltvLimitBps()
                : LtvCalculation.DEFAULT_LTV_LIMIT_BPS;

        long ltvCeiling = Math.round(appliedColValue * (limitBps / 10_000d));
        long maxLoan = Math.max(ltvCeiling - seniorLienSafe, 0L);

        int ratioBps = appliedColValue > 0
                ? (int) Math.round((double) requested / appliedColValue * 10_000d)
                : Integer.MAX_VALUE;
        String statusCd = (requested <= maxLoan) ? LtvCalculation.STATUS_PASS : LtvCalculation.STATUS_FAIL;

        OffsetDateTime now = OffsetDateTime.now();
        Long actorId = currentActor.currentActorId();

        LtvCalculation saved = repository.save(LtvCalculation.builder()
                .applId(collateral.getApplId())
                .colId(colId)
                .appliedColValue(appliedColValue)
                .seniorLienAmount(seniorLien)
                .requestedAmount(requested)
                .ltvRatioBps(ratioBps)
                .ltvLimitBps(limitBps)
                .maxLoanAmount(maxLoan)
                .ltvStatusCd(statusCd)
                .calculatedAt(now)
                .calcEngineVersion(req.calcEngineVersion())
                .build());

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_LTV, saved.getLtvId(),
                null, statusCd,
                REASON_LTV_CALCULATED,
                "ratioBps=" + ratioBps + ", maxLoan=" + maxLoan,
                actorId
        ));

        return LtvCalculationResponse.of(saved);
    }

    @Transactional(readOnly = true)
    public LtvCalculationResponse get(Long colId) {
        collateralRepository.findByColIdAndDeletedAtIsNull(colId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_050));
        return repository.findByColIdAndDeletedAtIsNull(colId)
                .map(LtvCalculationResponse::of)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_054));
    }
}
