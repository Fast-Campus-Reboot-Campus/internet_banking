package com.bank.deposit.dto.response;

import java.util.List;

public record ProductRecommendResponse(
        String customerId,
        int analysisPeriodMonth,
        CashFlowSummary cashFlow,
        List<RecommendedProduct> recommendations
) {}
