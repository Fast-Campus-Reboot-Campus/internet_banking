package com.bank.loan.reversal.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.reversal.dto.ReverseRepaymentRequest;
import com.bank.loan.reversal.dto.ReversalResponse;
import com.bank.loan.reversal.service.ReversalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "역분개", description = "Reversal - 회차 상환 거래 정정 (TYPE_REVERSAL)")
@RestController
@RequestMapping("/api/loan-contracts/{cntrId}/repayments/{rtxId}/reversal")
@RequiredArgsConstructor
public class ReversalController {

    private final ReversalService service;

    @Operation(summary = "상환 거래 역분개",
            description = "지정된 SCHEDULED + SUCCESS 거래를 정정한다. " +
                          "원본 row 는 보존하고 reversal_yn=Y / reversal_target_rtx_id 로 연결된 새 row 가 발급된다. " +
                          "대응 회차는 PAID → DUE 로 되돌아간다. 중도상환(EARLY) 정정은 본 단계 미지원.")
    @PostMapping
    public ResponseEntity<ApiResponse<ReversalResponse>> reverse(
            @PathVariable Long cntrId,
            @PathVariable Long rtxId,
            @Valid @RequestBody(required = false) ReverseRepaymentRequest req,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        ReverseRepaymentRequest body = (req == null) ? new ReverseRepaymentRequest(null, null) : req;
        ReversalResponse result = service.reverse(cntrId, rtxId, body, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result));
    }
}
