package com.bank.loan.dsr.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.BusinessException;
import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.application.repository.LoanApplicationRepository;
import com.bank.loan.creditevaluation.repository.CreditEvaluationRepository;
import com.bank.loan.dsr.domain.DsrCalculation;
import com.bank.loan.dsr.dto.DsrCalculationResponse;
import com.bank.loan.dsr.dto.RunDsrCalculationRequest;
import com.bank.loan.dsr.repository.DsrCalculationRepository;
import com.bank.loan.product.domain.LoanProduct;
import com.bank.loan.product.repository.LoanProductRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * DSR(한도) 산정 서비스 — flows §1.1, §2.1 의 "DSR 산출 잡".
 *
 * 흐름:
 *   1) 신청 존재 검증 (LOAN_012)
 *   2) 중복 DSR 차단 (LOAN_036, appl_id UNIQUE)
 *   3) 사전조건: 신용평가 완료 (LOAN_035) — CB 결과 받은 후 한도 산정
 *   4) DSR 계산:
 *        new_annual_repay_amt 미지정 시 baseRateBps 기반 단순 추정
 *        total = existing + new
 *        ratio_bps = total / annual_income × 10000
 *        status   = PASS (ratio ≤ limit) | FAIL
 *   5) status_history 발행
 *
 * 신청 상태 전이는 본심사에서 종합 — 본 단계는 row 적재만 한다.
 */
@Service
@RequiredArgsConstructor
public class DsrCalculationService {

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_DSR = "DSR_CALCULATION";
    private static final String REASON_DSR_CALCULATED = "DSR_CALCULATED";

    private final DsrCalculationRepository repository;
    private final LoanApplicationRepository applicationRepository;
    private final LoanProductRepository productRepository;
    private final CreditEvaluationRepository creditEvaluationRepository;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;

    @Transactional
    public DsrCalculationResponse run(Long applId, RunDsrCalculationRequest req) {
        LoanApplication application = applicationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_012));

        if (repository.findByApplIdAndDeletedAtIsNull(applId).isPresent()) {
            throw new BusinessException(LoanErrorCode.LOAN_036);
        }

        // 사전조건: 신용평가 완료
        if (creditEvaluationRepository.findByApplIdAndDeletedAtIsNull(applId).isEmpty()) {
            throw new BusinessException(LoanErrorCode.LOAN_035, "credit-evaluation required");
        }

        long existingPrincipal = req.existingPrincipalTotal() != null ? req.existingPrincipalTotal() : 0L;
        long existingRepay     = req.existingAnnualRepayAmt() != null ? req.existingAnnualRepayAmt() : 0L;
        long newRepay = req.newAnnualRepayAmt() != null
                ? req.newAnnualRepayAmt()
                : estimateNewAnnualRepay(application);
        long total = existingRepay + newRepay;

        int ratioBps = req.annualIncomeAmt() > 0
                ? (int) Math.round((double) total / req.annualIncomeAmt() * 10_000d)
                : Integer.MAX_VALUE;
        int limitBps = req.dsrLimitBps() != null ? req.dsrLimitBps() : DsrCalculation.DEFAULT_DSR_LIMIT_BPS;
        String statusCd = (ratioBps <= limitBps) ? DsrCalculation.STATUS_PASS : DsrCalculation.STATUS_FAIL;

        OffsetDateTime now = OffsetDateTime.now();
        Long actorId = currentActor.currentActorId();

        DsrCalculation saved = repository.save(DsrCalculation.builder()
                .applId(applId)
                .customerId(application.getCustomerId())
                .annualIncomeAmt(req.annualIncomeAmt())
                .existingPrincipalTotal(existingPrincipal)
                .existingAnnualRepayAmt(existingRepay)
                .newAnnualRepayAmt(newRepay)
                .totalAnnualRepayAmt(total)
                .dsrRatioBps(ratioBps)
                .dsrLimitBps(limitBps)
                .dsrStatusCd(statusCd)
                .dsrRegTypeCd(req.dsrRegTypeCd())
                .calculatedAt(now)
                .calcEngineVersion(req.calcEngineVersion())
                .dsrDetail(req.dsrDetail())
                .build());

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_DSR, saved.getDsrId(),
                null, statusCd,
                REASON_DSR_CALCULATED,
                "ratioBps=" + ratioBps + ", limitBps=" + limitBps,
                actorId
        ));

        return DsrCalculationResponse.of(saved);
    }

    @Transactional(readOnly = true)
    public DsrCalculationResponse get(Long applId) {
        applicationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_012));
        return repository.findByApplIdAndDeletedAtIsNull(applId)
                .map(DsrCalculationResponse::of)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_037));
    }

    /**
     * 신규대출의 연간 원리금 단순 추정.
     *   원금 / 기간(년) + 원금 × baseRate (단순 평균 이자)
     * 본격 계산은 상환 스케줄 생성기와 같은 엔진을 써야 하나, 한도 산정 단계에서는 보수적 추정으로 충분.
     */
    private long estimateNewAnnualRepay(LoanApplication application) {
        long principal = application.getRequestedAmount();
        int months = Math.max(application.getRequestedPeriodMo(), 1);
        double years = months / 12.0;
        int rateBps = productRepository.findByProdIdAndDeletedAtIsNull(application.getProdId())
                .map(LoanProduct::getBaseRateBps)
                .orElse(500); // fallback 5%
        double annualPrincipal = principal / Math.max(years, 1.0);
        double annualInterest  = principal * (rateBps / 10_000d);
        return Math.round(annualPrincipal + annualInterest);
    }
}
