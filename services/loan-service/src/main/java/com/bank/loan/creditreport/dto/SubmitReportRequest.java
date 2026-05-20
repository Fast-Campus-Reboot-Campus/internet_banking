package com.bank.loan.creditreport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 신용정보 신고 요청.
 *
 * 본 단계는 즉시 외부 전송(stub) — REQUESTED → SENT 한 트랜잭션 안에서 전이.
 * reportPayload 는 사전에 만들어진 JSON 문자열. 외부 기관 포맷 변환은 발신자 책임.
 */
public record SubmitReportRequest(

        @NotBlank @Size(max = 50) String reportTypeCd,

        @NotBlank @Size(max = 50) String agencyCd,

        @NotBlank @Size(max = 50) String reportTargetCd,

        @Size(max = 50) String reportReasonCd,

        String reportPayload
) {
}
