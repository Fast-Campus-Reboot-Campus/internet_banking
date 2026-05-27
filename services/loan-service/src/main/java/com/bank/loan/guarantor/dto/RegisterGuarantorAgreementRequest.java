package com.bank.loan.guarantor.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 보증 약정 등록 요청.
 *
 * 보증인 정보(name/mobile/relation)와 약정 정보(type/amount/ratio) 를 동시에 받는다.
 * 같은 mobile 의 보증인이 이미 등록돼 있으면 GuarantorMaster row 는 재사용한다.
 *
 *   guarantorName       성명 (평문 — 저장 시 마스킹/암호화 stub 으로 분리 보관)
 *   guarantorMobileNo   휴대전화번호 (숫자 7~15자, ci_hash 임시 산출 base)
 *   relationTypeCd      관계 코드 (예: SPOUSE, PARENT, FRIEND)
 *   gagrTypeCd          JOINT(연대) / PARTIAL(부분)
 *   guaranteeAmount     보증 금액 (1 이상)
 *   guaranteeRatioBps   보증 비율 bps (0~10000, 옵션)
 */
public record RegisterGuarantorAgreementRequest(
        @NotBlank @Size(max = 50) String guarantorName,
        @NotBlank @Pattern(regexp = "\\d{7,15}") String guarantorMobileNo,
        @Size(max = 50) String relationTypeCd,
        @NotBlank @Pattern(regexp = "JOINT|PARTIAL") String gagrTypeCd,
        @NotNull @Min(1) Long guaranteeAmount,
        Integer guaranteeRatioBps
) {
}
