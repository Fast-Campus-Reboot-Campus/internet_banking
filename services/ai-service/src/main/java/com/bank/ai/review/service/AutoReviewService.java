package com.bank.ai.review.service;

import com.bank.ai.review.client.InferenceClient;
import com.bank.ai.review.client.dto.InferenceRequest;
import com.bank.ai.review.client.dto.InferenceResponse;
import com.bank.ai.review.dto.AutoReviewRequest;
import com.bank.ai.review.dto.AutoReviewResponse;
import com.bank.ai.support.AiErrorCode;
import com.bank.common.web.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 자동심사 단건 추론. inference-server 를 호출하고 응답을 정규화한다.
 *
 * Java camelCase → Python snake_case 키 변환은 여기서 수행 (학습 측이 신뢰의 원천).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoReviewService {

    private final InferenceClient inferenceClient;

    public AutoReviewResponse review(AutoReviewRequest req) {
        Map<String, Object> features = toFeatureMap(req);
        InferenceRequest payload = new InferenceRequest(List.of(features));
        InferenceResponse res = inferenceClient.predict(payload);

        if (res == null || res.predictions() == null || res.predictions().isEmpty()) {
            log.error("inference returned empty predictions");
            throw new BusinessException(AiErrorCode.INFERENCE_FAILED);
        }
        InferenceResponse.Prediction p = res.predictions().get(0);
        return new AutoReviewResponse(res.modelVersion(), p.decision(), p.score(), p.proba());
    }

    private Map<String, Object> toFeatureMap(AutoReviewRequest r) {
        Map<String, Object> m = new LinkedHashMap<>();
        // Layer 1
        m.put("sex", r.sex());
        m.put("age", r.age());
        m.put("marital_status", r.maritalStatus());
        m.put("military_status", r.militaryStatus());
        m.put("family_type", r.familyType());
        m.put("housing_type", r.housingType());
        m.put("education_level", r.educationLevel());
        m.put("bachelors_field", r.bachelorsField());
        m.put("occupation", r.occupation());
        m.put("district", r.district());
        m.put("province", r.province());
        m.put("applicant_segment", r.applicantSegment());
        // Layer 2
        m.put("income_quintile", r.incomeQuintile());
        m.put("annual_income_kw", r.annualIncomeKw());
        m.put("total_asset_kw", r.totalAssetKw());
        m.put("total_debt_kw", r.totalDebtKw());
        m.put("collateral_debt_kw", r.collateralDebtKw());
        m.put("credit_debt_kw", r.creditDebtKw());
        m.put("dsr", r.dsr());
        m.put("ltv", r.ltv());
        m.put("monthly_cashflow_mean_kw", r.monthlyCashflowMeanKw());
        m.put("monthly_cashflow_std_kw", r.monthlyCashflowStdKw());
        m.put("delinquency_history_24m", r.delinquencyHistory24m());
        m.put("credit_score_proxy", r.creditScoreProxy());
        // Layer 3
        m.put("product_code", r.productCode());
        m.put("requested_amount_kw", r.requestedAmountKw());
        m.put("requested_period_mo", r.requestedPeriodMo());
        m.put("purpose_cd", r.purposeCd());
        m.put("purpose_red_flag", r.purposeRedFlag());
        return m;
    }
}
