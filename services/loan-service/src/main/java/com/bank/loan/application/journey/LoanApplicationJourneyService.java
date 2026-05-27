package com.bank.loan.application.journey;

import com.bank.common.web.BusinessException;
import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.application.dto.LoanApplicationResponse;
import com.bank.loan.application.repository.LoanApplicationRepository;
import com.bank.loan.collateral.domain.Collateral;
import com.bank.loan.collateral.repository.CollateralRepository;
import com.bank.loan.creditevaluation.dto.CreditEvaluationResponse;
import com.bank.loan.creditevaluation.repository.CreditEvaluationRepository;
import com.bank.loan.dsr.dto.DsrCalculationResponse;
import com.bank.loan.dsr.repository.DsrCalculationRepository;
import com.bank.loan.ltv.dto.LtvCalculationResponse;
import com.bank.loan.ltv.repository.LtvCalculationRepository;
import com.bank.loan.prescreening.dto.LoanPrescreeningResponse;
import com.bank.loan.prescreening.repository.LoanPrescreeningRepository;
import com.bank.loan.review.dto.LoanReviewResponse;
import com.bank.loan.review.repository.LoanReviewRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 신청 진행 상황(journey) 조회 서비스.
 *
 * 한 신청에 연결된 가심사 / CB / DSR / LTV(담보별) / 본심사 row 를 한 응답으로 묶어 반환한다.
 * 각 단계가 없으면 null(LTV 는 빈 list). 도메인 간 결합을 늘리지 않기 위해 LTV 는
 * 담보(Collateral)를 경유해 조회 — LTV 도메인은 신청을 모르고 담보만 안다.
 */
@Service
@RequiredArgsConstructor
public class LoanApplicationJourneyService {

    private final LoanApplicationRepository applicationRepository;
    private final LoanPrescreeningRepository prescreeningRepository;
    private final CreditEvaluationRepository creditEvaluationRepository;
    private final DsrCalculationRepository dsrCalculationRepository;
    private final CollateralRepository collateralRepository;
    private final LtvCalculationRepository ltvCalculationRepository;
    private final LoanReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public LoanApplicationJourneyResponse get(Long applId) {
        LoanApplication application = applicationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_012));

        LoanApplicationResponse applResponse = LoanApplicationResponse.of(application);

        LoanPrescreeningResponse presc = prescreeningRepository.findByApplIdAndDeletedAtIsNull(applId)
                .map(LoanPrescreeningResponse::of).orElse(null);

        CreditEvaluationResponse ceval = creditEvaluationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .map(CreditEvaluationResponse::of).orElse(null);

        DsrCalculationResponse dsr = dsrCalculationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .map(DsrCalculationResponse::of).orElse(null);

        // LTV — 담보별 (담보 → LTV row 조회, LTV 미수행 담보는 응답에서 제외)
        List<Collateral> collaterals = collateralRepository
                .findByApplIdAndDeletedAtIsNullOrderByCreatedAtAsc(applId);
        List<LtvCalculationResponse> ltvList = collaterals.stream()
                .map(c -> ltvCalculationRepository.findByColIdAndDeletedAtIsNull(c.getColId()).orElse(null))
                .filter(Objects::nonNull)
                .map(LtvCalculationResponse::of)
                .toList();

        LoanReviewResponse review = reviewRepository.findByApplIdAndDeletedAtIsNull(applId)
                .map(LoanReviewResponse::of).orElse(null);

        return new LoanApplicationJourneyResponse(applResponse, presc, ceval, dsr, ltvList, review);
    }
}
