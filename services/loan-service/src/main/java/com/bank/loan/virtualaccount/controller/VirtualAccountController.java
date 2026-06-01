package com.bank.loan.virtualaccount.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.virtualaccount.dto.VirtualAccountResponse;
import com.bank.loan.virtualaccount.service.VirtualAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 대출 상환용 가상계좌.
 *
 * ⚠️ 미완성(의도적 부분구현): 발급은 동작하나 입금 인식(상환 완결)은 payment 입금통지에
 *    수신계좌번호가 실려야 가능. {@link VirtualAccountService} 참고.
 */
@Tag(name = "가상계좌", description = "대출 상환용 가상계좌 발급")
@RestController
@RequestMapping("/api/loan-contracts/{cntrId}/virtual-account")
@RequiredArgsConstructor
public class VirtualAccountController {

    private final VirtualAccountService virtualAccountService;

    @Operation(summary = "가상계좌 발급(멱등)",
            description = "중도·추가 상환용 가상계좌를 발급한다. 이미 있으면 기존 계좌를 반환한다.")
    @PostMapping
    public ResponseEntity<ApiResponse<VirtualAccountResponse>> issue(@PathVariable Long cntrId) {
        VirtualAccountResponse body = VirtualAccountResponse.of(virtualAccountService.issue(cntrId));
        return ResponseEntity.ok(ApiResponse.ok(body));
    }
}
