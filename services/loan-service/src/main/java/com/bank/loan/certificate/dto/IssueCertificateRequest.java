package com.bank.loan.certificate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 증명서 발급 요청.
 *
 * 본 단계는 즉시 발급 (status=ISSUED). PDF 생성은 stub — docUrl/docHash 는 nullable.
 */
public record IssueCertificateRequest(

        @NotBlank @Size(max = 50) String certTypeCd,

        @Size(max = 50) String certPurposeCd,

        @Size(max = 50) String issueChannelCd,

        @Size(max = 500) String certDocUrl,

        @Size(max = 128) String certDocHash,

        @Pattern(regexp = "\\d{8}") String retentionUntil
) {
}
