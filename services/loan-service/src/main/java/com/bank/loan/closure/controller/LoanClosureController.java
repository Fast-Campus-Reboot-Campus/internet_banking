package com.bank.loan.closure.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.closure.dto.CloseLoanRequest;
import com.bank.loan.closure.dto.LoanClosureResponse;
import com.bank.loan.closure.service.LoanClosureService;
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

@Tag(name = "약정종결", description = "LoanClosure - 정상/조기/대위변제/대손")
@RestController
@RequestMapping("/api/loan-contracts/{cntrId}/closure")
@RequiredArgsConstructor
public class LoanClosureController {

    private final LoanClosureService service;

    @Operation(summary = "약정 종결",
            description = "ACTIVE 계약을 CLOSED 로 전이한다. NORMAL/EARLY 는 잔액=0 + 활성 회차 없음 필요. " +
                          "WRITE_OFF/SUBROGATION 은 잔액 검증 면제. 정산금액(final_*)은 서버 자동 산출.")
    @PostMapping
    public ResponseEntity<ApiResponse<LoanClosureResponse>> close(
            @PathVariable Long cntrId,
            @Valid @RequestBody CloseLoanRequest req) {
        LoanClosureResponse saved = service.close(cntrId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }

    @Operation(summary = "약정 종결 조회",
            description = "계약의 종결 정보 단건. 종결 안 됐으면 LOAN_124.")
    @GetMapping
    public ApiResponse<LoanClosureResponse> get(@PathVariable Long cntrId) {
        return ApiResponse.ok(service.get(cntrId));
    }
}
