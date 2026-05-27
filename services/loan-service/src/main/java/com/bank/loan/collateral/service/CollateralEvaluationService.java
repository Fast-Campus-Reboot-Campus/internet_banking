package com.bank.loan.collateral.service;

import com.bank.common.web.BusinessException;
import com.bank.loan.collateral.domain.Collateral;
import com.bank.loan.collateral.domain.CollateralEvaluation;
import com.bank.loan.collateral.dto.CollateralEvaluationResponse;
import com.bank.loan.collateral.dto.EvaluateCollateralRequest;
import com.bank.loan.collateral.repository.CollateralEvaluationRepository;
import com.bank.loan.collateral.repository.CollateralRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 담보 감정평가 서비스. 외부 감정평가기관 연계는 stub — 항상 DONE.
 *
 * 평가 행만 INSERT 하고 담보(collateral) 상태는 본 서비스에서 자동 전이하지 않는다.
 * 상태 전이는 운영 규칙에 따라 별도 호출로 수행한다.
 */
@Service
@RequiredArgsConstructor
public class CollateralEvaluationService {

    private final CollateralEvaluationRepository repository;
    private final CollateralRepository collateralRepository;

    @Transactional
    public CollateralEvaluationResponse evaluate(Long colId, EvaluateCollateralRequest req) {
        Collateral collateral = collateralRepository.findByColIdAndDeletedAtIsNull(colId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_050));

        if (collateral.isReleased()) {
            throw new BusinessException(LoanErrorCode.LOAN_051);
        }

        long appliedValue = req.appliedValue() != null ? req.appliedValue() : req.appraisedValue();

        CollateralEvaluation saved = repository.save(CollateralEvaluation.builder()
                .colId(collateral.getColId())
                .evalMethodCd(req.evalMethodCd())
                .evalAgencyCd(req.evalAgencyCd())
                .appraisedValue(req.appraisedValue())
                .appliedValue(appliedValue)
                .evalStatusCd(CollateralEvaluation.STATUS_DONE)
                .evalReportUrl(req.evalReportUrl())
                .evalReportHash(req.evalReportHash())
                .evaluatedAt(OffsetDateTime.now())
                .appliedStartDate(req.appliedStartDate())
                .appliedEndDate(req.appliedEndDate())
                .build());

        return CollateralEvaluationResponse.of(saved);
    }
}
