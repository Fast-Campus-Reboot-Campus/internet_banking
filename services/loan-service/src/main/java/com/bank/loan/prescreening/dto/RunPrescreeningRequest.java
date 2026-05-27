package com.bank.loan.prescreening.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 가심사 실행 요청.
 *
 * prescResultCd 미지정 시 서버가 외부 신용평가 엔진({@link com.bank.loan.prescreening.engine.CreditScoreEngine})
 * 을 호출해 자동 결정한다. 운영자/배치가 결과를 강제로 override 해야 할 때만 명시 전달.
 *
 * 자동 호출 시 score/grade/estimatedLimit/rejectReasonCd/engineVersion 도 엔진 결과로 채워지며,
 * 입력값이 있으면 입력 우선.
 *
 * estimatedRateBps 미지정 시 서버가 product.baseRateBps 로 자동 추정 (엔진은 금리를 산출하지 않음).
 * REJECT 시 estimatedLimitAmt / estimatedRateBps 는 null 로 저장.
 */
public record RunPrescreeningRequest(

        @Pattern(regexp = "PASS|REJECT") String prescResultCd,

        @Min(0) Long estimatedLimitAmt,
        @Min(0) Integer estimatedRateBps,

        @Size(max = 10) String estimatedGrade,
        @Min(0) Integer estimatedScore,

        @Size(max = 50) String rejectReasonCd,
        @Size(max = 500) String prescRemark,

        @Size(max = 50) String prescEngineVersion
) {
}
