package com.bank.loan.creditscore.service;

import com.bank.common.web.BusinessException;
import com.bank.loan.creditscore.dto.CreditScorePreviewRequest;
import com.bank.loan.creditscore.dto.CreditScorePreviewResponse;
import com.bank.loan.prescreening.engine.CreditScoreEngine;
import com.bank.loan.prescreening.engine.CreditScoreEngineException;
import com.bank.loan.prescreening.engine.CreditScoreRequest;
import com.bank.loan.prescreening.engine.CreditScoreResult;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 한도조회(가심사 preview) 서비스 — 신청 전 단계의 외부 신용평가 호출.
 *
 * DB 적재 없음. 같은 어댑터({@link CreditScoreEngine}) 를 가심사와 공유해 결과 일관성을 보장.
 * 결과를 보존하려면 클라이언트가 후속으로 정식 신청 + 가심사 API 를 호출해야 한다.
 */
@Service
@RequiredArgsConstructor
public class CreditScorePreviewService {

    private final CreditScoreEngine creditScoreEngine;

    public CreditScorePreviewResponse preview(CreditScorePreviewRequest req) {
        CreditScoreResult result;
        try {
            result = creditScoreEngine.evaluate(new CreditScoreRequest(
                    req.customerId(),
                    req.loanTypeCd(),
                    req.requestedAmount(),
                    req.requestedPeriodMo(),
                    req.loanPurposeCd(),
                    req.employmentTypeCd(),
                    req.estimatedIncomeAmt()
            ));
        } catch (CreditScoreEngineException e) {
            throw new BusinessException(LoanErrorCode.LOAN_029, e.getMessage());
        }
        return CreditScorePreviewResponse.of(result);
    }
}
