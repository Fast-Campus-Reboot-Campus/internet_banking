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
 * 외부 가심사 엔진 stub — 결과(PASS/REJECT) 는 클라이언트 입력.
 *
 * 흐름:
 *   1) 신청 SUBMITTED 검증 (LOAN_047)
 *   2) 중복 가심사 차단 (LOAN_046, appl_id UNIQUE)
 *   3) PASS:
 *        estimated_limit = req or requestedAmount
 *        estimated_rate  = req or product.baseRateBps
 *        신청 → PRESCREENED
 *   4) REJECT:
 *        estimated 값 null, reject_reason_cd 기록
 *        신청 → REJECTED
 *   5) status_history 양쪽 (LOAN_PRESCREENING null→결과, LOAN_APPLICATION SUBMITTED→다음)
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

        boolean pass = LoanPrescreening.RESULT_PASS.equals(req.prescResultCd());
        OffsetDateTime now = OffsetDateTime.now();
        Long actorId = currentActor.currentActorId();

        Long estimatedLimit = null;
        Integer estimatedRate = null;
        if (pass) {
            estimatedLimit = req.estimatedLimitAmt() != null
                    ? req.estimatedLimitAmt()
                    : application.getRequestedAmount();
            estimatedRate = req.estimatedRateBps() != null
                    ? req.estimatedRateBps()
                    : productRepository.findByProdIdAndDeletedAtIsNull(application.getProdId())
                            .map(LoanProduct::getBaseRateBps)
                            .orElse(null);
        }

        LoanPrescreening saved = repository.save(LoanPrescreening.builder()
                .applId(applId)
                .prescResultCd(req.prescResultCd())
                .estimatedLimitAmt(estimatedLimit)
                .estimatedRateBps(estimatedRate)
                .estimatedGrade(req.estimatedGrade())
                .estimatedScore(req.estimatedScore())
                .rejectReasonCd(pass ? null : req.rejectReasonCd())
                .prescRemark(req.prescRemark())
                .prescreenedAt(now)
                .prescEngineVersion(req.prescEngineVersion())
                .build());

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_PRESCREENING, saved.getPrescId(),
                null, req.prescResultCd(),
                pass ? REASON_PRESCREEN_PASS : REASON_PRESCREEN_REJECT,
                pass ? null : "rejectReasonCd=" + req.rejectReasonCd(),
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
}
