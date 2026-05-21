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
import com.bank.loan.ltv.domain.LtvCalculation;
import com.bank.loan.notification.event.LoanApprovedEvent;
import com.bank.loan.product.domain.LoanProduct;
import com.bank.loan.product.repository.LoanProductRepository;
import com.bank.loan.review.domain.LoanReview;
import com.bank.loan.review.dto.ConfirmReviewRequest;
import com.bank.loan.review.dto.ExpirePendingReviewsResponse;
import com.bank.loan.review.dto.LoanReviewResponse;
import com.bank.loan.review.dto.ReviewStatsResponse;
import com.bank.loan.review.dto.ReviseReviewRequest;
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
import java.util.ArrayList;
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
    private static final String REASON_REVIEW_REVISITED_APPROVED = "REVIEW_REVISITED_APPROVED";
    private static final String REASON_REVIEW_REVISITED_REJECTED = "REVIEW_REVISITED_REJECTED";
    private static final String REASON_AUTO_RECOMMENDED_APPROVED = "AUTO_RECOMMENDED_APPROVED";
    private static final String REASON_AUTO_RECOMMENDED_REJECTED = "AUTO_RECOMMENDED_REJECTED";
    private static final String REASON_REVIEW_CONFIRMED          = "REVIEW_CONFIRMED";
    private static final String REASON_REVIEW_EXPIRED            = "AUTO_RECOMMENDATION_EXPIRED";

    // 자동 결정 거절 사유 코드
    private static final String REJECT_CB  = "CB_REJECT";
    private static final String REJECT_DSR = "DSR_OVER";
    private static final String REJECT_LTV = "LTV_FAIL";

    private final LoanReviewRepository repository;
    private final LoanApplicationRepository applicationRepository;
    private final LoanProductRepository productRepository;
    private final CreditEvaluationRepository creditEvaluationRepository;
    private final DsrCalculationRepository dsrCalculationRepository;
    private final LoanReviewPreconditions preconditions;
    private final ApprovedAmountCalculator approvedAmountCalculator;
    private final LoanReviewCheckLogWriter checkLogWriter;
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

    /**
     * 자동 권고(PENDING_APPROVAL) 상태로 사람 확정을 기다리는 본심사 목록.
     * 가장 오래 대기한 권고가 위에 오도록 reviewedAt 오름차순으로 반환한다.
     */
    @Transactional(readOnly = true)
    public List<LoanReviewResponse> listPending() {
        return repository
                .findByRevStatusCdAndDeletedAtIsNullOrderByReviewedAtAsc(LoanReview.STATUS_PENDING_APPROVAL)
                .stream()
                .map(LoanReviewResponse::of)
                .toList();
    }

    /**
     * 자동 권고 만료 배치. olderThanDays 일 이전(reviewedAt 기준) 의 PENDING_APPROVAL 권고를
     * 일괄 EXPIRED 로 전이하고 status_history 에 이력을 남긴다.
     *
     * 신청 상태는 PRESCREENED 그대로 유지 — 만료된 권고는 운영자가 별도 처리(수동 본심사 등) 필요.
     * 만료된 권고는 confirm 불가 (LOAN_049 가 PENDING_APPROVAL 아님으로 차단).
     */
    @Transactional
    public ExpirePendingReviewsResponse expirePending(int olderThanDays) {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(olderThanDays);
        Long actorId = currentActor.currentActorId();

        List<LoanReview> targets = repository
                .findByRevStatusCdAndReviewedAtBeforeAndDeletedAtIsNull(
                        LoanReview.STATUS_PENDING_APPROVAL, cutoff);

        List<Long> expiredRevIds = new ArrayList<>();
        for (LoanReview rev : targets) {
            rev.expire();
            statusHistoryPublisher.publish(StatusChangeEvent.of(
                    DOMAIN_CD, TARGET_REVIEW, rev.getRevId(),
                    LoanReview.STATUS_PENDING_APPROVAL, LoanReview.STATUS_EXPIRED,
                    REASON_REVIEW_EXPIRED,
                    "cutoffAt=" + cutoff + ", olderThanDays=" + olderThanDays,
                    actorId
            ));
            expiredRevIds.add(rev.getRevId());
        }
        return new ExpirePendingReviewsResponse(targets.size(), expiredRevIds, cutoff);
    }

    /**
     * 본심사 결정 정정(재심사). 신청이 APPROVED/REJECTED 상태일 때만 가능 — 약정 진입 후엔 LOAN_044.
     * 같은 LoanReview row 를 갱신하고, 변경 이력은 status_history 와 ReviewCheckLog 재적재로 보존한다.
     */
    @Transactional
    public LoanReviewResponse revise(Long applId, ReviseReviewRequest req) {
        LoanApplication application = applicationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_012));

        LoanReview review = repository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_042));

        // 약정 진입(CONTRACTED) 이후엔 정정 불가. APPROVED 또는 REJECTED 만 정정 가능.
        String applBefore = application.currentStatus();
        if (!LoanApplication.STATUS_APPROVED.equals(applBefore)
                && !LoanApplication.STATUS_REJECTED.equals(applBefore)) {
            throw new BusinessException(LoanErrorCode.LOAN_044,
                    "current=" + applBefore);
        }

        boolean approved = LoanReview.DECISION_APPROVED.equals(req.revDecisionCd());
        OffsetDateTime now = OffsetDateTime.now();
        Long actorId = currentActor.currentActorId();

        // APPROVED 정정 시 한도/금리/기간 — 입력 우선, 미입력이면 RunReviewRequest 와 동일 규칙으로 산정
        Long approvedAmount = null;
        Integer approvedRate = null;
        Integer approvedPeriod = null;
        CreditEvaluation ceval = null;
        LoanProduct product = productRepository.findByProdIdAndDeletedAtIsNull(application.getProdId())
                .orElse(null);
        if (approved) {
            ceval = creditEvaluationRepository.findByApplIdAndDeletedAtIsNull(applId).orElse(null);
            approvedAmount = req.approvedAmount() != null
                    ? req.approvedAmount()
                    : approvedAmountCalculator.determine(application, ceval, product);
            approvedRate = req.approvedRateBps() != null
                    ? req.approvedRateBps()
                    : (ceval != null && ceval.getEvalRateBps() != null
                            ? ceval.getEvalRateBps()
                            : (product != null ? product.getBaseRateBps() : null));
            approvedPeriod = req.approvedPeriodMo() != null
                    ? req.approvedPeriodMo()
                    : application.getRequestedPeriodMo();
        }

        review.revise(
                req.revDecisionCd(),
                approvedAmount, approvedRate, approvedPeriod,
                approved ? null : req.rejectReasonCd(),
                req.revRemark(),
                req.reviewerId(),
                now
        );

        checkLogWriter.logRevisit(review.getRevId(), approved, req);

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_REVIEW, review.getRevId(),
                LoanReview.STATUS_COMPLETED, LoanReview.STATUS_COMPLETED,
                approved ? REASON_REVIEW_REVISITED_APPROVED : REASON_REVIEW_REVISITED_REJECTED,
                "revisitReasonCd=" + req.revisitReasonCd()
                        + (approved
                                ? ", approvedAmount=" + approvedAmount + ", rateBps=" + approvedRate
                                : ", rejectReasonCd=" + req.rejectReasonCd()),
                actorId
        ));

        String applAfter = approved ? LoanApplication.STATUS_APPROVED : LoanApplication.STATUS_REJECTED;
        if (!applBefore.equals(applAfter)) {
            if (approved) {
                application.markApproved();
            } else {
                application.markRejected();
            }
            statusHistoryPublisher.publish(StatusChangeEvent.of(
                    DOMAIN_CD, TARGET_APPLICATION, applId,
                    applBefore, applAfter,
                    approved ? REASON_REVIEW_REVISITED_APPROVED : REASON_REVIEW_REVISITED_REJECTED,
                    "revId=" + review.getRevId()
                            + ", revisitReasonCd=" + req.revisitReasonCd(),
                    actorId
            ));
        }

        return LoanReviewResponse.of(review);
    }

    /**
     * 본심사 자동 결정(권고). CB·DSR·LTV 결과로 결정 산출만 하고
     * 사람이 {@link #confirm(Long, ConfirmReviewRequest)} 로 확정해야 효과 발생.
     *
     * 저장: revStatusCd = PENDING_APPROVAL, revDecisionCd 권고값, 한도/금리/기간 미리 계산.
     * 신청 상태는 PRESCREENED 그대로 유지 — confirm 시점에 전이된다.
     *
     * 데이터 부족(CB/DSR 미수행 또는 담보 필수인데 LTV 미수행)은 LOAN_038,
     * CB.REVIEW 는 LOAN_048 (수동 본심사 권유).
     *
     * 결정 룰:
     *   CB.REJECT     → REJECTED (CB_REJECT)
     *   DSR.FAIL      → REJECTED (DSR_OVER)
     *   LTV.FAIL      → REJECTED (LTV_FAIL, 담보 필수 케이스)
     *   APPROVE/PASS  → APPROVED, 한도/금리/기간 자동 산정
     */
    @Transactional
    public LoanReviewResponse autoDecide(Long applId) {
        LoanApplication application = applicationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_012));

        if (repository.findByApplIdAndDeletedAtIsNull(applId).isPresent()) {
            throw new BusinessException(LoanErrorCode.LOAN_039);
        }
        if (!application.isReviewable()) {
            throw new BusinessException(LoanErrorCode.LOAN_038,
                    "current=" + application.currentStatus());
        }

        CreditEvaluation ceval = creditEvaluationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_038,
                        "credit-evaluation required"));
        if (CreditEvaluation.DECISION_REVIEW.equals(ceval.getCevalDecisionCd())) {
            throw new BusinessException(LoanErrorCode.LOAN_048,
                    "cevalDecision=REVIEW");
        }

        DsrCalculation dsr = dsrCalculationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_038,
                        "dsr-calculation required"));

        // 사전조건: 본인확인(IDV) PASS — 자동 결정도 동일한 입장
        preconditions.requireIdvPass(applId);

        LoanProduct product = productRepository.findByProdIdAndDeletedAtIsNull(application.getProdId())
                .orElse(null);
        boolean collateralRequired = product != null && product.isCollateralRequired();
        LtvCalculation chosenLtv = null;
        if (collateralRequired) {
            chosenLtv = preconditions.resolveActiveLtvForAuto(applId);
        }

        // 결정 룰 — 우선순위: CB.REJECT → DSR.FAIL → LTV.FAIL → APPROVED
        String decision;
        String rejectReasonCd = null;
        if (CreditEvaluation.DECISION_REJECT.equals(ceval.getCevalDecisionCd())) {
            decision = LoanReview.DECISION_REJECTED;
            rejectReasonCd = REJECT_CB;
        } else if (!DsrCalculation.STATUS_PASS.equals(dsr.getDsrStatusCd())) {
            decision = LoanReview.DECISION_REJECTED;
            rejectReasonCd = REJECT_DSR;
        } else if (collateralRequired && !LtvCalculation.STATUS_PASS.equals(chosenLtv.getLtvStatusCd())) {
            decision = LoanReview.DECISION_REJECTED;
            rejectReasonCd = REJECT_LTV;
        } else {
            decision = LoanReview.DECISION_APPROVED;
        }

        boolean approved = LoanReview.DECISION_APPROVED.equals(decision);
        OffsetDateTime now = OffsetDateTime.now();
        Long actorId = currentActor.currentActorId();

        Long approvedAmount = null;
        Integer approvedRate = null;
        Integer approvedPeriod = null;
        if (approved) {
            approvedAmount = approvedAmountCalculator.determine(application, ceval, product);
            approvedRate = ceval.getEvalRateBps() != null
                    ? ceval.getEvalRateBps()
                    : (product != null ? product.getBaseRateBps() : null);
            approvedPeriod = application.getRequestedPeriodMo();
        }

        // 권고만 — revStatusCd=PENDING_APPROVAL, approvedAt 은 confirm 시점에 채운다.
        LoanReview saved = repository.save(LoanReview.builder()
                .applId(applId)
                .revTypeCd(LoanReview.TYPE_AUTO)
                .revStatusCd(LoanReview.STATUS_PENDING_APPROVAL)
                .revDecisionCd(decision)
                .approvedAmount(approvedAmount)
                .approvedRateBps(approvedRate)
                .approvedPeriodMo(approvedPeriod)
                .rejectReasonCd(rejectReasonCd)
                .revRemark(null)
                .reviewerId(null)
                .reviewedAt(now)
                .approvedAt(null)
                .build());

        checkLogWriter.logAuto(saved.getRevId(), ceval, dsr, chosenLtv, collateralRequired, approved, rejectReasonCd);

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_REVIEW, saved.getRevId(),
                null, LoanReview.STATUS_PENDING_APPROVAL,
                approved ? REASON_AUTO_RECOMMENDED_APPROVED : REASON_AUTO_RECOMMENDED_REJECTED,
                approved
                        ? "auto, approvedAmount=" + approvedAmount + ", rateBps=" + approvedRate
                        : "auto, rejectReasonCd=" + rejectReasonCd,
                actorId
        ));

        // 신청 상태는 confirm 단계에서 전이. 권고 단계엔 PRESCREENED 그대로 유지.

        return LoanReviewResponse.of(saved);
    }

    /**
     * 자동 권고(PENDING_APPROVAL) 결과를 사람이 확정. 권고된 결정 그대로 COMPLETED 로 마감하고
     * 신청 상태를 그 결정에 맞춰 전이한다. 결정·한도 정정이 필요하면 본 endpoint 대신
     * {@link #revise(Long, ReviseReviewRequest)} 사용.
     *
     * 사전조건: 본심사가 PENDING_APPROVAL 상태여야 함 (LOAN_049).
     * 체크로그에 FINAL_DECISION 확정 1건 추가, status_history 양쪽 publish.
     */
    @Transactional
    public LoanReviewResponse confirm(Long applId, ConfirmReviewRequest req) {
        LoanApplication application = applicationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_012));

        LoanReview review = repository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_042));

        if (!review.isPendingApproval()) {
            throw new BusinessException(LoanErrorCode.LOAN_049,
                    "revStatus=" + review.getRevStatusCd());
        }

        OffsetDateTime now = OffsetDateTime.now();
        Long actorId = currentActor.currentActorId();
        boolean approved = review.isApproved();

        review.confirm(req.reviewerId(), now);

        checkLogWriter.logConfirm(review.getRevId(), approved, req.reviewerId(), req.confirmRemark());

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_REVIEW, review.getRevId(),
                LoanReview.STATUS_PENDING_APPROVAL, LoanReview.STATUS_COMPLETED,
                REASON_REVIEW_CONFIRMED,
                "reviewerId=" + req.reviewerId(),
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
                "confirm, revId=" + review.getRevId(),
                actorId
        ));

        return LoanReviewResponse.of(review);
    }
}
