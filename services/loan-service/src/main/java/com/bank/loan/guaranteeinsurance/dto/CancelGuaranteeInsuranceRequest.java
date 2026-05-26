package com.bank.loan.guaranteeinsurance.dto;

import jakarta.validation.constraints.Size;

public record CancelGuaranteeInsuranceRequest(
        @Size(max = 50) String cancelReasonCd,
        @Size(max = 500) String cancelRemark
) {
}
