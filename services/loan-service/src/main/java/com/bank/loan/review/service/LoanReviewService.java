package com.bank.loan.review.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.BusinessException;
import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.application.repository.LoanApplicationRepository;
import com.bank.loan.collateral.domain.Collateral;
import com.bank.loan.collateral.repository.CollateralRepository;
import com.bank.loan.creditevaluation.domain.CreditEvaluation;
import com.bank.loan.creditevaluation.repository.CreditEvaluationRepository;
import com.bank.loan.dsr.domain.DsrCalculation;
import com.bank.loan.dsr.repository.DsrCalculationRepository;
import com.bank.loan.ltv.domain.LtvCalculation;
import com.bank.loan.ltv.repository.LtvCalculationRepository;
import com.bank.loan.product.domain.LoanProduct;
import com.bank.loan.product.repository.LoanProductRepository;
import com.bank.loan.review.domain.LoanReview;
import com.bank.loan.review.domain.ReviewCheckLog;
import com.bank.loan.review.dto.LoanReviewResponse;
import com.bank.loan.review.dto.ReviseReviewRequest;
import com.bank.loan.review.dto.RunReviewRequest;
import com.bank.loan.review.repository.LoanReviewRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

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

    // 자동 결정 거절 사유 코드
    private static final String REJECT_CB  = "CB_REJECT";
    private static final String REJECT_DSR = "DSR_OVER";
    private static final String REJECT_LTV = "LTV_FAIL";

    private final LoanReviewRepository repository;
    private final LoanApplicationRepository applicationRepository;
    private final LoanProductRepository productRepository;
    private final CreditEvaluationRepository creditEvaluationRepository;
    private final DsrCalculationRepository dsrCalculationRepository;
    private final CollateralRepository collateralRepository;
    private final LtvCalculationRepository ltvCalculationRepository;
    private final ReviewCheckLogger reviewCheckLogger;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;

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

        // 사전조건 4: 담보 필수 상품이면 활성 담보별 LTV PASS 검증
        LoanProduct product = productRepository.findByProdIdAndDeletedAtIsNull(application.getProdId())
                .orElse(null);
        if (product != null && product.isCollateralRequired()) {
            requireAllActiveCollateralsLtvPass(applId);
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
                    : determineApprovedAmount(application, ceval, product);
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

        logChecks(saved.getRevId(), ceval, dsr, product, approved, req);

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
                    : determineApprovedAmount(application, ceval, product);
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

        logRevisitChecks(review.getRevId(), approved, req);

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
     * 본심사 자동 결정 — 외부 호출이나 운영자 입력 없이 CB·DSR·LTV 결과만으로 결정.
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

        LoanProduct product = productRepository.findByProdIdAndDeletedAtIsNull(application.getProdId())
                .orElse(null);
        boolean collateralRequired = product != null && product.isCollateralRequired();
        LtvCalculation chosenLtv = null;
        if (collateralRequired) {
            chosenLtv = resolveActiveLtvForAuto(applId);
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
        OffsetDateTime approvedAt = null;
        if (approved) {
            approvedAmount = determineApprovedAmount(application, ceval, product);
            approvedRate = ceval.getEvalRateBps() != null
                    ? ceval.getEvalRateBps()
                    : (product != null ? product.getBaseRateBps() : null);
            approvedPeriod = application.getRequestedPeriodMo();
            approvedAt = now;
        }

        LoanReview saved = repository.save(LoanReview.builder()
                .applId(applId)
                .revTypeCd(LoanReview.TYPE_AUTO)
                .revStatusCd(LoanReview.STATUS_COMPLETED)
                .revDecisionCd(decision)
                .approvedAmount(approvedAmount)
                .approvedRateBps(approvedRate)
                .approvedPeriodMo(approvedPeriod)
                .rejectReasonCd(rejectReasonCd)
                .revRemark(null)
                .reviewerId(null)
                .reviewedAt(now)
                .approvedAt(approvedAt)
                .build());

        logAutoChecks(saved.getRevId(), ceval, dsr, chosenLtv, collateralRequired, approved, rejectReasonCd);

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_REVIEW, saved.getRevId(),
                null, LoanReview.STATUS_COMPLETED,
                approved ? REASON_REVIEW_APPROVED : REASON_REVIEW_REJECTED,
                approved
                        ? "auto, approvedAmount=" + approvedAmount + ", rateBps=" + approvedRate
                        : "auto, rejectReasonCd=" + rejectReasonCd,
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
                "auto, revId=" + saved.getRevId(),
                actorId
        ));

        return LoanReviewResponse.of(saved);
    }

    /**
     * 담보 필수 상품에서 자동 결정용 LTV 선택 — 활성 담보 1건의 LTV 를 반환.
     * 담보 자체 미첨부 또는 LTV 미수행 시 LOAN_038 (데이터 부족 — 자동 결정 불가).
     */
    private LtvCalculation resolveActiveLtvForAuto(Long applId) {
        List<Collateral> all = collateralRepository.findByApplIdAndDeletedAtIsNullOrderByCreatedAtAsc(applId);
        List<Collateral> active = all.stream()
                .filter(c -> !Collateral.STATUS_RELEASED.equals(c.currentStatus())
                          && !Collateral.STATUS_REJECTED.equals(c.currentStatus()))
                .toList();
        if (active.isEmpty()) {
            throw new BusinessException(LoanErrorCode.LOAN_038,
                    "collateral required but none attached");
        }
        Collateral first = active.get(0);
        return ltvCalculationRepository.findByColIdAndDeletedAtIsNull(first.getColId())
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_038,
                        "ltv required colId=" + first.getColId()));
    }

    /**
     * 자동 결정용 체크로그 5건. 입력 데이터가 검증을 통과한 사유/위반한 사유를 항목별로 기록.
     */
    private void logAutoChecks(Long revId, CreditEvaluation ceval, DsrCalculation dsr,
                               LtvCalculation ltv, boolean collateralRequired,
                               boolean approved, String autoRejectReasonCd) {
        reviewCheckLogger.log(revId,
                ReviewCheckLog.ITEM_PRESCREEN_PASS,
                ReviewCheckLog.RESULT_PASS,
                "application=PRESCREENED, auto",
                null);

        reviewCheckLogger.log(revId,
                ReviewCheckLog.ITEM_CB_DECISION,
                CreditEvaluation.DECISION_APPROVE.equals(ceval.getCevalDecisionCd())
                        ? ReviewCheckLog.RESULT_PASS
                        : ReviewCheckLog.RESULT_FAIL,
                "decision=" + ceval.getCevalDecisionCd() + ", engine=" + ceval.getCevalEngine(),
                null);

        reviewCheckLogger.log(revId,
                ReviewCheckLog.ITEM_DSR_CHECK,
                DsrCalculation.STATUS_PASS.equals(dsr.getDsrStatusCd())
                        ? ReviewCheckLog.RESULT_PASS
                        : ReviewCheckLog.RESULT_FAIL,
                "ratioBps=" + dsr.getDsrRatioBps() + ", limit=" + dsr.getDsrLimitBps(),
                null);

        if (collateralRequired) {
            reviewCheckLogger.log(revId,
                    ReviewCheckLog.ITEM_LTV_CHECK,
                    LtvCalculation.STATUS_PASS.equals(ltv.getLtvStatusCd())
                            ? ReviewCheckLog.RESULT_PASS
                            : ReviewCheckLog.RESULT_FAIL,
                    "ltvStatus=" + ltv.getLtvStatusCd() + ", colId=" + ltv.getColId(),
                    null);
        } else {
            reviewCheckLogger.log(revId,
                    ReviewCheckLog.ITEM_LTV_CHECK,
                    ReviewCheckLog.RESULT_N_A,
                    "collateral not required",
                    null);
        }

        reviewCheckLogger.log(revId,
                ReviewCheckLog.ITEM_FINAL_DECISION,
                approved ? ReviewCheckLog.RESULT_PASS : ReviewCheckLog.RESULT_FAIL,
                "auto, decision=" + (approved ? "APPROVED" : "REJECTED")
                        + (approved ? "" : ", rejectReasonCd=" + autoRejectReasonCd),
                null);
    }

    /**
     * 정정 시점 체크로그 재적재. 자동 5건과 동일한 항목을 다시 기록하되 remark 에 revisit 표시를 남긴다.
     * 기존 5건은 그대로 두고 누적 — append-only 원칙.
     */
    private void logRevisitChecks(Long revId, boolean approved, ReviseReviewRequest req) {
        Long checkerId = req.reviewerId();
        String revisitTag = "revisit(" + req.revisitReasonCd() + ")";

        reviewCheckLogger.log(revId,
                ReviewCheckLog.ITEM_FINAL_DECISION,
                approved ? ReviewCheckLog.RESULT_PASS : ReviewCheckLog.RESULT_FAIL,
                revisitTag + ", decision=" + req.revDecisionCd()
                        + (approved ? "" : ", rejectReasonCd=" + req.rejectReasonCd()),
                checkerId);
    }

    /**
     * 본심사 결정 시점에 사전조건 검증 결과 5건을 REVIEW_CHECK_LOG 에 자동 적재.
     */
    private void logChecks(Long revId, CreditEvaluation ceval, DsrCalculation dsr,
                           LoanProduct product, boolean approved, RunReviewRequest req) {
        Long checkerId = req.reviewerId();

        reviewCheckLogger.log(revId,
                ReviewCheckLog.ITEM_PRESCREEN_PASS,
                ReviewCheckLog.RESULT_PASS,
                "application=PRESCREENED",
                checkerId);

        reviewCheckLogger.log(revId,
                ReviewCheckLog.ITEM_CB_DECISION,
                CreditEvaluation.DECISION_APPROVE.equals(ceval.getCevalDecisionCd())
                        ? ReviewCheckLog.RESULT_PASS
                        : ReviewCheckLog.RESULT_REVIEW,
                "decision=" + ceval.getCevalDecisionCd() + ", engine=" + ceval.getCevalEngine(),
                checkerId);

        reviewCheckLogger.log(revId,
                ReviewCheckLog.ITEM_DSR_CHECK,
                ReviewCheckLog.RESULT_PASS,
                "ratioBps=" + dsr.getDsrRatioBps() + ", limit=" + dsr.getDsrLimitBps(),
                checkerId);

        if (product != null && product.isCollateralRequired()) {
            reviewCheckLogger.log(revId,
                    ReviewCheckLog.ITEM_LTV_CHECK,
                    ReviewCheckLog.RESULT_PASS,
                    "all active collaterals LTV PASS",
                    checkerId);
        } else {
            reviewCheckLogger.log(revId,
                    ReviewCheckLog.ITEM_LTV_CHECK,
                    ReviewCheckLog.RESULT_N_A,
                    "collateral not required",
                    checkerId);
        }

        reviewCheckLogger.log(revId,
                ReviewCheckLog.ITEM_FINAL_DECISION,
                approved ? ReviewCheckLog.RESULT_PASS : ReviewCheckLog.RESULT_FAIL,
                "decision=" + req.revDecisionCd()
                        + (approved ? "" : ", rejectReasonCd=" + req.rejectReasonCd()),
                checkerId);
    }

    /**
     * 담보 필수 상품에서 활성 담보(RELEASED/REJECTED 제외) 별 LTV PASS 검증.
     * 활성 담보 0건 또는 LTV 미수행·FAIL 1건이라도 있으면 LOAN_038.
     */
    private void requireAllActiveCollateralsLtvPass(Long applId) {
        List<Collateral> all = collateralRepository.findByApplIdAndDeletedAtIsNullOrderByCreatedAtAsc(applId);
        List<Collateral> active = all.stream()
                .filter(c -> !Collateral.STATUS_RELEASED.equals(c.currentStatus())
                          && !Collateral.STATUS_REJECTED.equals(c.currentStatus()))
                .toList();
        if (active.isEmpty()) {
            throw new BusinessException(LoanErrorCode.LOAN_038,
                    "collateral required but none attached");
        }
        for (Collateral c : active) {
            LtvCalculation ltv = ltvCalculationRepository.findByColIdAndDeletedAtIsNull(c.getColId())
                    .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_038,
                            "ltv required colId=" + c.getColId()));
            if (!LtvCalculation.STATUS_PASS.equals(ltv.getLtvStatusCd())) {
                throw new BusinessException(LoanErrorCode.LOAN_038,
                        "ltv FAIL colId=" + c.getColId());
            }
        }
    }

    /**
     * 승인 한도 자동 산정 — 신청금액 / CB 한도 / 상품 max 중 최솟값.
     */
    private long determineApprovedAmount(LoanApplication application,
                                         CreditEvaluation ceval,
                                         LoanProduct product) {
        long candidate = application.getRequestedAmount();
        if (ceval.getEvalLimitAmount() != null) {
            candidate = Math.min(candidate, ceval.getEvalLimitAmount());
        }
        if (product != null && product.getMaxAmount() != null) {
            candidate = Math.min(candidate, product.getMaxAmount());
        }
        return candidate;
    }
}
