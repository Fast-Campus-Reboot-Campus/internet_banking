package com.bank.loan.review.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.BusinessException;
import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.application.repository.LoanApplicationRepository;
import com.bank.loan.creditevaluation.domain.CreditEvaluation;
import com.bank.loan.creditevaluation.repository.CreditEvaluationRepository;
import com.bank.loan.dsr.domain.DsrCalculation;
import com.bank.loan.dsr.repository.DsrCalculationRepository;
import com.bank.loan.guarantor.service.GuarantorPolicyValidator;
import com.bank.loan.notification.event.LoanApprovedEvent;
import com.bank.loan.product.domain.LoanProduct;
import com.bank.loan.product.repository.LoanProductRepository;
import com.bank.loan.review.domain.LoanReview;
import com.bank.loan.review.dto.LoanReviewResponse;
import com.bank.loan.review.dto.ReviewStatsResponse;
import com.bank.loan.review.dto.RunReviewRequest;
import com.bank.loan.review.repository.LoanReviewRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 본심사(Underwriting) 서비스 — flows §1.1, §2.1 의 "REVIEWING → APPROVED/REJECTED".
 *
 * 흐름:
 *   1) 신청 존재 검증 (LOAN_012)
 *   2) 중복 본심사 차단 (LOAN_039, appl_id UNIQUE)
 *   3) 사전조건: PRESCREENED 상태 + CB(APPROVE/REVIEW) + DSR PASS (LOAN_038)
 *   4) APPROVED:
 *        approved_amount/rate/period 자동 산정 (입력값 우선)
 *        신청 → APPROVED, approved_at 기록
 *   5) REJECTED:
 *        reject_reason_cd 기록
 *        신청 → REJECTED
 *   6) status_history 양쪽 (LOAN_REVIEW null→COMPLETED, LOAN_APPLICATION PRESCREENED→다음)
 */
@Service
@RequiredArgsConstructor
public class LoanReviewService {

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_REVIEW = "LOAN_REVIEW";
    private static final String TARGET_APPLICATION = "LOAN_APPLICATION";
    private static final String REASON_REVIEW_APPROVED = "REVIEW_APPROVED";
    private static final String REASON_REVIEW_REJECTED = "REVIEW_REJECTED";

    private final LoanReviewRepository repository;
    private final LoanApplicationRepository applicationRepository;
    private final LoanProductRepository productRepository;
    private final CreditEvaluationRepository creditEvaluationRepository;
    private final DsrCalculationRepository dsrCalculationRepository;
    private final LoanReviewPreconditions preconditions;
    private final ApprovedAmountCalculator approvedAmountCalculator;
    private final LoanReviewCheckLogWriter checkLogWriter;
    private final GuarantorPolicyValidator guarantorPolicyValidator;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public LoanReviewResponse run(Long applId, RunReviewRequest req) {
        LoanApplication application = applicationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_012));

        if (repository.findByApplIdAndDeletedAtIsNull(applId).isPresent()) {
            throw new BusinessException(LoanErrorCode.LOAN_039);
        }

        // 사전조건 1: 신청 상태 PRESCREENED
        if (!application.isReviewable()) {
            throw new BusinessException(LoanErrorCode.LOAN_038,
                    "current=" + application.currentStatus());
        }

        // 사전조건 2: CB 완료 + decision != REJECT
        CreditEvaluation ceval = creditEvaluationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_038,
                        "credit-evaluation required"));
        if (CreditEvaluation.DECISION_REJECT.equals(ceval.getCevalDecisionCd())) {
            throw new BusinessException(LoanErrorCode.LOAN_038,
                    "cevalDecision=REJECT");
        }

        // 사전조건 3: DSR PASS
        DsrCalculation dsr = dsrCalculationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_038,
                        "dsr-calculation required"));
        if (!DsrCalculation.STATUS_PASS.equals(dsr.getDsrStatusCd())) {
            throw new BusinessException(LoanErrorCode.LOAN_038,
                    "dsrStatus=" + dsr.getDsrStatusCd());
        }

        // 사전조건 4: 본인확인(IDV) PASS — docs/loan_flows.md 본심사 진입 명세
        preconditions.requireIdvPass(applId);

        // 사전조건 5: 담보 필수 상품이면 활성 담보별 LTV PASS 검증
        LoanProduct product = productRepository.findByProdIdAndDeletedAtIsNull(application.getProdId())
                .orElse(null);
        if (product != null && product.isCollateralRequired()) {
            preconditions.requireAllActiveCollateralsLtvPass(applId);
        }

        // 사전조건 6: 보증 필수 상품이면 활성 SIGNED 보증인 수 >= minGuarantorCount
        if (product != null && !guarantorPolicyValidator.satisfies(application, product)) {
            throw new BusinessException(LoanErrorCode.LOAN_038,
                    "guarantorRequired: signedCount < minGuarantorCount=" + product.getMinGuarantorCount());
        }

        boolean approved = LoanReview.DECISION_APPROVED.equals(req.revDecisionCd());
        OffsetDateTime now = OffsetDateTime.now();
        Long actorId = currentActor.currentActorId();

        Long approvedAmount = null;
        Integer approvedRate = null;
        Integer approvedPeriod = null;
        OffsetDateTime approvedAt = null;
        if (approved) {
            approvedAmount = req.approvedAmount() != null
                    ? req.approvedAmount()
                    : approvedAmountCalculator.determine(application, ceval, product);
            approvedRate = req.approvedRateBps() != null
                    ? req.approvedRateBps()
                    : (ceval.getEvalRateBps() != null
                            ? ceval.getEvalRateBps()
                            : (product != null ? product.getBaseRateBps() : null));
            approvedPeriod = req.approvedPeriodMo() != null
                    ? req.approvedPeriodMo()
                    : application.getRequestedPeriodMo();
            approvedAt = now;
        }

        LoanReview saved = repository.save(LoanReview.builder()
                .applId(applId)
                .revTypeCd(req.revTypeCd())
                .revStatusCd(LoanReview.STATUS_COMPLETED)
                .revDecisionCd(req.revDecisionCd())
                .approvedAmount(approvedAmount)
                .approvedRateBps(approvedRate)
                .approvedPeriodMo(approvedPeriod)
                .rejectReasonCd(approved ? null : req.rejectReasonCd())
                .revRemark(req.revRemark())
                .reviewerId(req.reviewerId())
                .reviewedAt(now)
                .approvedAt(approvedAt)
                .build());

        checkLogWriter.logManual(saved.getRevId(), ceval, dsr, product, approved, req);

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_REVIEW, saved.getRevId(),
                null, LoanReview.STATUS_COMPLETED,
                approved ? REASON_REVIEW_APPROVED : REASON_REVIEW_REJECTED,
                approved
                        ? "approvedAmount=" + approvedAmount + ", rateBps=" + approvedRate
                        : "rejectReasonCd=" + req.rejectReasonCd(),
                actorId
        ));

        String applBefore = application.currentStatus();
        if (approved) {
            application.markApproved();
        } else {
            application.markRejected();
        }
        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_APPLICATION, applId,
                applBefore, application.currentStatus(),
                approved ? REASON_REVIEW_APPROVED : REASON_REVIEW_REJECTED,
                "revId=" + saved.getRevId(),
                actorId
        ));

        if (approved) {
            eventPublisher.publishEvent(new LoanApprovedEvent(
                    applId, saved.getRevId(),
                    application.getCustomerId(), approvedAmount
            ));
        }

        return LoanReviewResponse.of(saved);
    }

    @Transactional(readOnly = true)
    public LoanReviewResponse get(Long applId) {
        applicationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_012));
        return repository.findByApplIdAndDeletedAtIsNull(applId)
                .map(LoanReviewResponse::of)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_042));
    }

    /**
     * 본심사 결정 통계 — 기간 내 row 를 revTypeCd × revDecisionCd, revStatusCd, rejectReasonCd 로
     * 집계해 운영 가시성 응답으로 반환한다. 기간은 yyyyMMdd, to 는 inclusive(그 날 23:59:59 까지).
     */
    @Transactional(readOnly = true)
    public ReviewStatsResponse stats(String fromYyyyMMdd, String toYyyyMMdd) {
        LocalDate fromDate = LocalDate.parse(fromYyyyMMdd, DateTimeFormatter.BASIC_ISO_DATE);
        LocalDate toDate = LocalDate.parse(toYyyyMMdd, DateTimeFormatter.BASIC_ISO_DATE);
        OffsetDateTime fromAt = fromDate.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime toAt = toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

        List<LoanReview> rows = repository
                .findByReviewedAtGreaterThanEqualAndReviewedAtLessThanAndDeletedAtIsNull(fromAt, toAt);

        Map<String, Long> byTypeDecision = new LinkedHashMap<>();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        Map<String, Long> byRejectReason = new LinkedHashMap<>();

        for (LoanReview r : rows) {
            String typeDec = r.getRevTypeCd() + "_"
                    + (r.getRevDecisionCd() != null ? r.getRevDecisionCd() : "NONE");
            byTypeDecision.merge(typeDec, 1L, Long::sum);
            byStatus.merge(r.getRevStatusCd(), 1L, Long::sum);
            if (r.getRejectReasonCd() != null) {
                byRejectReason.merge(r.getRejectReasonCd(), 1L, Long::sum);
            }
        }

        return new ReviewStatsResponse(fromYyyyMMdd, toYyyyMMdd, rows.size(),
                byTypeDecision, byStatus, byRejectReason);
    }

}
