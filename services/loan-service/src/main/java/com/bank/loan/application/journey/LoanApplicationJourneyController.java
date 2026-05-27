package com.bank.loan.application.journey;

import com.bank.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "신청 진행 상황", description = "LoanApplicationJourney - 신청의 모든 단계 결과를 한 응답에 묶어 조회")
@RestController
@RequestMapping("/api/loan-applications/{applId}/journey")
@RequiredArgsConstructor
public class LoanApplicationJourneyController {

    private final LoanApplicationJourneyService service;

    @Operation(summary = "신청 진행 상황 조회",
            description = "한 신청의 가심사 / 신용평가 / DSR / LTV(담보별) / 본심사 결과를 한 응답에 묶어 반환. "
                    + "수행되지 않은 단계는 null(LTV 는 빈 list). 클라이언트 UI 가 단계별 endpoint 를 "
                    + "따로 호출하지 않고 본 endpoint 한 번으로 진행 상황을 표시할 수 있다.")
    @GetMapping
    public ApiResponse<LoanApplicationJourneyResponse> get(@PathVariable Long applId) {
        return ApiResponse.ok(service.get(applId));
    }
}
