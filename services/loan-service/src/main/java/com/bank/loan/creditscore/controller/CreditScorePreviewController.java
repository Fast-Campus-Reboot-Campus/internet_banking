package com.bank.loan.creditscore.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.creditscore.dto.CreditScorePreviewRequest;
import com.bank.loan.creditscore.dto.CreditScorePreviewResponse;
import com.bank.loan.creditscore.service.CreditScorePreviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "한도조회",
        description = "CreditScorePreview - 신청 전 단계 외부 신용평가 결과 미리보기 (DB 적재 없음)")
@RestController
@RequestMapping("/api/credit-score")
@RequiredArgsConstructor
public class CreditScorePreviewController {

    private final CreditScorePreviewService service;

    @Operation(summary = "한도조회 (외부 신용평가 미리보기)",
            description = "신청 row 생성 없이 외부 신용평가 엔진을 호출해 예상 한도/점수/등급을 회신. "
                    + "1회성 신용조회 동의(consentYn=Y) 필수. "
                    + "결과를 보존하려면 후속으로 정식 신청 + 가심사 API 호출 필요.")
    @PostMapping("/preview")
    public ApiResponse<CreditScorePreviewResponse> preview(
            @Valid @RequestBody CreditScorePreviewRequest req) {
        return ApiResponse.ok(service.preview(req));
    }
}
