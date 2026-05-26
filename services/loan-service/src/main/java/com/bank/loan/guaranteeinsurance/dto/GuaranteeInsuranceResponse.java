package com.bank.loan.guaranteeinsurance.dto;

import com.bank.loan.guaranteeinsurance.domain.GuaranteeInsurance;

import java.time.OffsetDateTime;

public record GuaranteeInsuranceResponse(
        Long ginsId,
        Long cntrId,
        String ginsAgencyCd,
        String ginsPolicyNo,
        Long guaranteeAmount,
        Integer guaranteeRatioBps,
        Long premiumAmount,
        String ginsStatusCd,
        String ginsStartDate,
        String ginsEndDate,
        String ginsDocUrl,
        String ginsDocHash,
        OffsetDateTime issuedAt
) {
    public static GuaranteeInsuranceResponse of(GuaranteeInsurance g) {
        return new GuaranteeInsuranceResponse(
                g.getGinsId(), g.getCntrId(),
                g.getGinsAgencyCd(), g.getGinsPolicyNo(),
                g.getGuaranteeAmount(), g.getGuaranteeRatioBps(), g.getPremiumAmount(),
                g.getGinsStatusCd(),
                g.getGinsStartDate(), g.getGinsEndDate(),
                g.getGinsDocUrl(), g.getGinsDocHash(),
                g.getIssuedAt()
        );
    }
}
