package com.bank.loan.creditreport.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.creditreport.dto.CreditInfoReportDispatchSummary;
import com.bank.loan.creditreport.service.CreditInfoReportDispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 신용정보 신고 outbox 디스패치 트리거 (internal).
 *
 * 실 운영에서는 분 단위 스케줄러가 호출. 본 단계는 운영자/스케줄러 공용 엔드포인트만 마련.
 */
@Tag(name = "신용정보배치", description = "Credit info report dispatch (internal)")
@RestController
@RequestMapping("/api/internal/credit-info-reports")
@RequiredArgsConstructor
public class CreditInfoReportDispatchController {

    private final CreditInfoReportDispatchService dispatchService;

    @Operation(summary = "신용정보 신고 outbox 디스패치",
            description = "PENDING/FAILED 상태이면서 nextAttemptAt <= now 인 outbox row 를 한 페이지(200)" +
                          "단위로 픽업해 어댑터에 전송한다. 결과에 따라 SENT/FAILED/DEAD 로 전이.")
    @PostMapping("/dispatch")
    public ApiResponse<CreditInfoReportDispatchSummary> dispatch() {
        return ApiResponse.ok(dispatchService.dispatch());
    }
}
