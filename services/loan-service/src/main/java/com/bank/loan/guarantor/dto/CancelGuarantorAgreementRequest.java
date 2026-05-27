package com.bank.loan.guarantor.dto;

import jakarta.validation.constraints.Size;

public record CancelGuarantorAgreementRequest(
        @Size(max = 50) String cancelReasonCd,
        @Size(max = 500) String cancelRemark
) {
}
