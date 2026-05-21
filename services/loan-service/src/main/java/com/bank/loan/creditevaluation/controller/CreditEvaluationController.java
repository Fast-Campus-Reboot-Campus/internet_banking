package com.bank.loan.creditevaluation.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.creditevaluation.dto.CreditEvaluationResponse;
import com.bank.loan.creditevaluation.dto.RunCreditEvaluationRequest;
import com.bank.loan.creditevaluation.service.CreditEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "신용평가(CB)", description = "CreditEvaluation - 외부 CB·자동심사 엔진 결과 적재/조회")
@RestController
@RequestMapping("/api/loan-applications/{applId}/credit-evaluation")
@RequiredArgsConstructor
public class CreditEvaluationController {

    private final CreditEvaluationService service;

    @Operation(summary = "신용평가(CB) 실행",
            description = "외부 CB(KCB/NICE)·자동심사 엔진 stub — 결과는 클라이언트 입력. " +
                          "사전조건: 가심사 PASS 완료. 신청당 1건 (appl_id UNIQUE). " +
                          "신청 상태는 본 단계에서 전이하지 않으며 본심사에서 종합한다.")
    @PostMapping
    public ResponseEntity<ApiResponse<CreditEvaluationResponse>> run(
            @PathVariable Long applId,
            @Valid @RequestBody RunCreditEvaluationRequest req) {
        CreditEvaluationResponse saved = service.run(applId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }

    @Operation(summary = "신용평가 결과 조회")
    @GetMapping
    public ApiResponse<CreditEvaluationResponse> get(@PathVariable Long applId) {
        return ApiResponse.ok(service.get(applId));
    }
}
