package com.bank.loan.creditreport.dto;

import com.bank.loan.creditreport.domain.CreditInfoReport;

import java.time.OffsetDateTime;

public record CreditInfoReportResponse(
        Long crptId,
        Long cntrId,
        Long dlqId,
        Long customerId,
        String crptTypeCd,
        String crptAgencyCd,
        String crptStatusCd,
        String reportTargetCd,
        String reportReasonCd,
        String reportPayload,
        String externalTxNo,
        OffsetDateTime reportedAt,
        OffsetDateTime ackAt
) {
    public static CreditInfoReportResponse of(CreditInfoReport r) {
        return new CreditInfoReportResponse(
                r.getCrptId(), r.getCntrId(), r.getDlqId(), r.getCustomerId(),
                r.getCrptTypeCd(), r.getCrptAgencyCd(), r.getCrptStatusCd(),
                r.getReportTargetCd(), r.getReportReasonCd(),
                r.getReportPayload(),
                r.getExternalTxNo(),
                r.getReportedAt(), r.getAckAt()
        );
    }
}
