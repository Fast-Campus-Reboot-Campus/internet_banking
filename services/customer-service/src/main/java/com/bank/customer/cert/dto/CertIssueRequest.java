package com.bank.customer.cert.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CertIssueRequest(

        @NotBlank
        String loginId,

        @NotBlank
        String password,

        @NotBlank
        @Pattern(regexp = "CERT_COMMON|CERT_FIN|CERT_AXFUL", message = "certType은 CERT_COMMON, CERT_FIN, CERT_AXFUL 이어야 합니다.")
        String certType,

        @NotBlank
        String certPin
) {

    /** 금융인증서(CERT_FIN)는 숫자 6자리 PIN, 그 외(공동 등)는 8~30자 영숫자특. */
    @AssertTrue(message = "금융인증서 PIN은 숫자 6자리, 그 외 인증서 암호는 8~30자로 입력해 주세요.")
    public boolean isCertPinLengthValid() {
        if (certPin == null) return true; // @NotBlank 가 null 처리
        if ("CERT_FIN".equals(certType)) return certPin.matches("\\d{6}");
        return certPin.length() >= 8 && certPin.length() <= 30;
    }
}
