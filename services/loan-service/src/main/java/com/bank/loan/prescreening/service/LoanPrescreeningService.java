package com.bank.loan.prescreening.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.BusinessException;
import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.application.repository.LoanApplicationRepository;
import com.bank.loan.prescreening.domain.LoanPrescreening;
import com.bank.loan.prescreening.dto.LoanPrescreeningResponse;
import com.bank.loan.prescreening.dto.RunPrescreeningRequest;
import com.bank.loan.prescreening.engine.CreditScoreEngine;
import com.bank.loan.prescreening.engine.CreditScoreEngineException;
import com.bank.loan.prescreening.engine.CreditScoreRequest;
import com.bank.loan.prescreening.engine.CreditScoreResult;
import com.bank.loan.prescreening.repository.LoanPrescreeningRepository;
import com.bank.loan.product.domain.LoanProduct;
import com.bank.loan.product.repository.LoanProductRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 가심사 (Prescreening) 서비스 — flows §1.1, §2.1.
 *
 * 외부 신용평가(가심사) 엔진({@link CreditScoreEngine}) 호출로 PASS/REJECT 를 자동 산출.
 * 클라이언트가 prescResultCd 를 명시 전달하면 운영자 override 로 간주해 엔진 호출 없이 그대로 사용.
 *
 * 흐름:
 *   1) 신청 SUBMITTED 검증 (LOAN_047)
 *   2) 중복 가심사 차단 (LOAN_046, appl_id UNIQUE)
 *   3) prescResultCd 미입력 → 엔진 호출, decision/score/grade/한도/거절사유/엔진버전 자동 산출
 *   4) PASS:
 *        estimated_limit = 입력 ?? 엔진 결과 ?? requestedAmount
 *        estimated_rate  = 입력 ?? product.baseRateBps  (엔진은 금리 산출 안 함)
 *        신청 → PRESCREENED
 *   5) REJECT:
 *        estimated 값 null, reject_reason_cd = 입력 ?? 엔진 결과
 *        신청 → REJECTED
 *   6) status_history 양쪽 (LOAN_PRESCREENING null→결과, LOAN_APPLICATION SUBMITTED→다음)
 */
@Service
@RequiredArgsConstructor
public class LoanPrescreeningService {

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_PRESCREENING = "LOAN_PRESCREENING";
    private static final String TARGET_APPLICATION  = "LOAN_APPLICATION";
    private static final String REASON_PRESCREEN_PASS   = "PRESCREEN_PASS";
    private static final String REASON_PRESCREEN_REJECT = "PRESCREEN_REJECT";

    private final LoanPrescreeningRepository repository;
    private final LoanApplicationRepository applicationRepository;
    private final LoanProductRepository productRepository;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;
    private final CreditScoreEngine creditScoreEngine;

    @Transactional
    public LoanPrescreeningResponse run(Long applId, RunPrescreeningRequest req) {
        LoanApplication application = applicationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_012));

        // 중복 가심사 먼저 검증 — "이미 수행됨" 이 더 구체적인 사용자 신호
        if (repository.findByApplIdAndDeletedAtIsNull(applId).isPresent()) {
            throw new BusinessException(LoanErrorCode.LOAN_046);
        }
        if (!application.isPrescreenable()) {
            throw new BusinessException(LoanErrorCode.LOAN_047,
                    "current=" + application.currentStatus());
        }

        // 입력값 우선, 없으면 외부 신용평가 엔진 자동 호출.
        CreditScoreResult engineResult = null;
        String prescResultCd = req.prescResultCd();
        if (prescResultCd == null) {
            try {
                engineResult = creditScoreEngine.evaluate(toEngineRequest(application));
            } catch (CreditScoreEngineException e) {
                throw new BusinessException(LoanErrorCode.LOAN_029, e.getMessage());
            }
            prescResultCd = engineResult.decision();
        }

        boolean pass = LoanPrescreening.RESULT_PASS.equals(prescResultCd);
        OffsetDateTime now = OffsetDateTime.now();
        Long actorId = currentActor.currentActorId();

        Long estimatedLimit = null;
        Integer estimatedRate = null;
        if (pass) {
            estimatedLimit = req.estimatedLimitAmt() != null
                    ? req.estimatedLimitAmt()
                    : (engineResult != null && engineResult.estimatedLimitAmt() != null
                            ? engineResult.estimatedLimitAmt()
                            : application.getRequestedAmount());
            estimatedRate = req.estimatedRateBps() != null
                    ? req.estimatedRateBps()
                    : productRepository.findByProdIdAndDeletedAtIsNull(application.getProdId())
                            .map(LoanProduct::getBaseRateBps)
                            .orElse(null);
        }

        Integer estimatedScore = req.estimatedScore() != null
                ? req.estimatedScore()
                : (engineResult != null ? engineResult.score() : null);
        String estimatedGrade = req.estimatedGrade() != null
                ? req.estimatedGrade()
                : (engineResult != null ? engineResult.grade() : null);
        String engineVersion = req.prescEngineVersion() != null
                ? req.prescEngineVersion()
                : (engineResult != null ? engineResult.engineVersion() : null);
        String rejectReasonCd = pass ? null
                : (req.rejectReasonCd() != null
                        ? req.rejectReasonCd()
                        : (engineResult != null ? engineResult.rejectReasonCd() : null));

        LoanPrescreening saved = repository.save(LoanPrescreening.builder()
                .applId(applId)
                .prescResultCd(prescResultCd)
                .estimatedLimitAmt(estimatedLimit)
                .estimatedRateBps(estimatedRate)
                .estimatedGrade(estimatedGrade)
                .estimatedScore(estimatedScore)
                .rejectReasonCd(rejectReasonCd)
                .prescRemark(req.prescRemark())
                .prescreenedAt(now)
                .prescEngineVersion(engineVersion)
                .build());

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_PRESCREENING, saved.getPrescId(),
                null, prescResultCd,
                pass ? REASON_PRESCREEN_PASS : REASON_PRESCREEN_REJECT,
                pass ? null : "rejectReasonCd=" + rejectReasonCd,
                actorId
        ));

        String applBefore = application.currentStatus();
        if (pass) {
            application.markPrescreened();
        } else {
            application.markRejected();
        }
        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_APPLICATION, applId,
                applBefore, application.currentStatus(),
                pass ? REASON_PRESCREEN_PASS : REASON_PRESCREEN_REJECT,
                "prescId=" + saved.getPrescId(),
                actorId
        ));

        return LoanPrescreeningResponse.of(saved);
    }

    @Transactional(readOnly = true)
    public LoanPrescreeningResponse get(Long applId) {
        applicationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_012));
        return repository.findByApplIdAndDeletedAtIsNull(applId)
                .map(LoanPrescreeningResponse::of)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_045));
    }

    private CreditScoreRequest toEngineRequest(LoanApplication app) {
        String loanTypeCd = productRepository.findByProdIdAndDeletedAtIsNull(app.getProdId())
                .map(LoanProduct::getLoanTypeCd)
                .orElse(null);
        return new CreditScoreRequest(
                app.getCustomerId(),
                loanTypeCd,
                app.getRequestedAmount(),
                app.getRequestedPeriodMo(),
                app.getLoanPurposeCd(),
                app.getEmploymentTypeCd(),
                app.getEstimatedIncomeAmt()
        );
    }
}
