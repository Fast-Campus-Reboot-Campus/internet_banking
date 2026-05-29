package com.bank.loan.advisory;

public record AdvisoryReportSummary(
        Long advrId,
        Long revId,
        String advisoryTypeCd,
        String severityCd,
        String advrStatusCd,
        String advrTitle,
        String advrSummary,
        String targetReviewerId
) {}
