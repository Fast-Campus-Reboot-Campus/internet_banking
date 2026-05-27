package com.bank.loan.application.journey;

import com.bank.loan.application.dto.LoanApplicationResponse;
import com.bank.loan.creditevaluation.dto.CreditEvaluationResponse;
import com.bank.loan.dsr.dto.DsrCalculationResponse;
import com.bank.loan.ltv.dto.LtvCalculationResponse;
import com.bank.loan.prescreening.dto.LoanPrescreeningResponse;
import com.bank.loan.review.dto.LoanReviewResponse;

import java.util.List;

/**
 * 신청 진행 상황(journey) 한눈 조회 응답.
 *
 * 한 신청에 연결된 모든 단계 결과를 한 응답에 묶어 반환한다.
 * 각 단계는 수행되지 않았으면 null(또는 빈 list). 클라이언트 UI 가 단계별 endpoint 를
 * 따로 호출하지 않고 본 endpoint 한 번으로 진행 상황을 표시할 수 있게 한다.
 *
 *   application    — 신청 본체
 *   prescreening   — 가심사 결과 (1건 또는 null)
 *   creditEvaluation — 신용평가 결과 (1건 또는 null)
 *   dsr            — DSR 산정 결과 (1건 또는 null)
 *   ltv            — LTV 산정 결과 (담보 0..N 건 별 list, 빈 list 가능)
 *   review         — 본심사 결과 (1건 또는 null, 권고/확정/만료 상태 포함)
 */
public record LoanApplicationJourneyResponse(
        LoanApplicationResponse application,
        LoanPrescreeningResponse prescreening,
        CreditEvaluationResponse creditEvaluation,
        DsrCalculationResponse dsr,
        List<LtvCalculationResponse> ltv,
        LoanReviewResponse review
) {
}
