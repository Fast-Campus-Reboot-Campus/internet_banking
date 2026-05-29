package com.bank.loan.review.service;

import com.bank.common.web.BusinessException;
import com.bank.loan.collateral.domain.Collateral;
import com.bank.loan.collateral.repository.CollateralRepository;
import com.bank.loan.document.domain.LoanDocument;
import com.bank.loan.document.repository.LoanDocumentRepository;
import com.bank.loan.idv.domain.LoanIdentityVerification;
import com.bank.loan.idv.repository.LoanIdentityVerificationRepository;
import com.bank.loan.ltv.domain.LtvCalculation;
import com.bank.loan.ltv.repository.LtvCalculationRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 본심사 진입 사전조건 검증 묶음 — flows §2.1 "본인확인 + 동의 + LTV" 충족 여부.
 * LoanReviewService 가 수동·자동·정정 경로에서 공통으로 호출한다.
 */
@Component
@RequiredArgsConstructor
public class LoanReviewPreconditions {

    private final LoanIdentityVerificationRepository idvRepository;
    private final CollateralRepository collateralRepository;
    private final LtvCalculationRepository ltvCalculationRepository;
    private final LoanDocumentRepository documentRepository;

    public void requireIdvPass(Long applId) {
        boolean ok = idvRepository.existsByApplIdAndIdvResultCdAndDeletedAtIsNull(
                applId, LoanIdentityVerification.RESULT_PASS);
        if (!ok) {
            throw new BusinessException(LoanErrorCode.LOAN_038, "idv-verification required");
        }
    }

    /** 활성 담보(RELEASED/REJECTED 제외) 가 모두 LTV PASS 인지 검증 — 수동·정정 경로용. */
    public void requireAllActiveCollateralsLtvPass(Long applId) {
        List<Collateral> active = activeCollaterals(applId);
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

    /** 자동 결정용 LTV 선택 — 첫 번째 활성 담보의 LTV row. 미수행 시 LOAN_038. */
    public LtvCalculation resolveActiveLtvForAuto(Long applId) {
        List<Collateral> active = activeCollaterals(applId);
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
     * doc-agent 검증 미완료 서류 차단 — REJECTED(NEEDS_RESUBMIT) 상태 서류가 있으면 본심사 진입 불가.
     * AUTO_PASS → VERIFIED, NEEDS_RESUBMIT → REJECTED 로 매핑됨 (LoanDocument.applyVerifyResult).
     */
    public void requireDocumentsCleared(Long applId) {
        long rejected = documentRepository
                .findByApplIdAndDeletedAtIsNullOrderBySubmittedAtAsc(applId)
                .stream()
                .filter(d -> LoanDocument.STATUS_REJECTED.equals(d.getDocStatusCd()))
                .count();
        if (rejected > 0) {
            throw new BusinessException(LoanErrorCode.LOAN_055,
                    "rejectedDocCount=" + rejected);
        }
    }

    private List<Collateral> activeCollaterals(Long applId) {
        return collateralRepository.findByApplIdAndDeletedAtIsNullOrderByCreatedAtAsc(applId).stream()
                .filter(c -> !Collateral.STATUS_RELEASED.equals(c.currentStatus())
                          && !Collateral.STATUS_REJECTED.equals(c.currentStatus()))
                .toList();
    }
}
