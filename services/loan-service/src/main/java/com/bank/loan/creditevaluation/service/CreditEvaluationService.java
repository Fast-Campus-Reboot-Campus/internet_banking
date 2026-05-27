package com.bank.loan.creditevaluation.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.BusinessException;
import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.application.repository.LoanApplicationRepository;
import com.bank.loan.creditevaluation.domain.CreditEvaluation;
import com.bank.loan.creditevaluation.dto.CreditEvaluationResponse;
import com.bank.loan.creditevaluation.dto.RunCreditEvaluationRequest;
import com.bank.loan.creditevaluation.repository.CreditEvaluationRepository;
import com.bank.loan.prescreening.domain.LoanPrescreening;
import com.bank.loan.prescreening.repository.LoanPrescreeningRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 신용평가(CB) 서비스 — flows §2.1.
 *
 * 외부 CB(KCB/NICE) · 내부 자동심사 엔진 stub — 결과(grade/score/decision) 는 클라이언트 입력.
 *
 * 흐름:
 *   1) 신청 존재 검증 (LOAN_012)
 *   2) 중복 신용평가 차단 (LOAN_033, appl_id UNIQUE)
 *   3) 사전조건: 가심사 PASS 완료 (LOAN_032)
 *   4) CREDIT_EVALUATION row 적재 (status=COMPLETED)
 *   5) status_history 발행 (LOAN_CREDIT_EVALUATION null→COMPLETED)
 *
 * 신청 상태 전이는 본심사에서 종합 — 본 단계는 row 적재만 한다.
 */
@Service
@RequiredArgsConstructor
public class CreditEvaluationService {

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_CEVAL = "CREDIT_EVALUATION";
    private static final String REASON_CEVAL_COMPLETED = "CEVAL_COMPLETED";

    private final CreditEvaluationRepository repository;
    private final LoanApplicationRepository applicationRepository;
    private final LoanPrescreeningRepository prescreeningRepository;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;

    @Transactional
    public CreditEvaluationResponse run(Long applId, RunCreditEvaluationRequest req) {
        LoanApplication application = applicationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_012));

        // 중복 신용평가 먼저 검증 — "이미 수행됨" 이 더 구체적인 사용자 신호
        if (repository.findByApplIdAndDeletedAtIsNull(applId).isPresent()) {
            throw new BusinessException(LoanErrorCode.LOAN_033);
        }

        // 사전조건: 가심사 PASS 완료
        LoanPrescreening prescreening = prescreeningRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_032,
                        "prescreening required"));
        if (!prescreening.isPass()) {
            throw new BusinessException(LoanErrorCode.LOAN_032,
                    "prescreening=" + prescreening.getPrescResultCd());
        }

        OffsetDateTime now = OffsetDateTime.now();
        Long actorId = currentActor.currentActorId();

        CreditEvaluation saved = repository.save(CreditEvaluation.builder()
                .applId(applId)
                .customerId(application.getCustomerId())
                .cevalEngine(req.cevalEngine())
                .cevalEngineVersion(req.cevalEngineVersion())
                .cevalGrade(req.cevalGrade())
                .cevalScore(req.cevalScore())
                .pdBps(req.pdBps())
                .cevalDecisionCd(req.cevalDecisionCd())
                .evalLimitAmount(req.evalLimitAmount())
                .evalRateBps(req.evalRateBps())
                .cevalStatusCd(CreditEvaluation.STATUS_COMPLETED)
                .cevalFactors(req.cevalFactors())
                .evaluatedAt(now)
                .build());

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_CEVAL, saved.getCevalId(),
                null, CreditEvaluation.STATUS_COMPLETED,
                REASON_CEVAL_COMPLETED,
                "decision=" + req.cevalDecisionCd() + ", engine=" + req.cevalEngine(),
                actorId
        ));

        return CreditEvaluationResponse.of(saved);
    }

    @Transactional(readOnly = true)
    public CreditEvaluationResponse get(Long applId) {
        applicationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_012));
        return repository.findByApplIdAndDeletedAtIsNull(applId)
                .map(CreditEvaluationResponse::of)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_034));
    }
}
