package com.bank.deposit.controller;

import com.bank.deposit.dto.request.VerifyOwnerRequest;
import com.bank.deposit.dto.response.VerifyOwnerResponse;
import com.bank.deposit.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서비스 간 내부 호출용 계좌 API. 게이트웨이로 외부 노출하지 않는다(서비스 네트워크 내부 전용).
 * customer-service 의 비로그인 ID 조회/사용자암호 재설정 본인확인에서 계좌 소유를 검증한다.
 * (deposit-service 는 context-path 가 /api 이므로 매핑은 /internal/accounts → 실제 /api/internal/accounts)
 */
@RestController
@RequestMapping("/internal/accounts")
@RequiredArgsConstructor
public class InternalAccountController {

    private final AccountService accountService;

    @PostMapping("/verify-owner")
    public VerifyOwnerResponse verifyOwner(@RequestBody VerifyOwnerRequest req) {
        return accountService.verifyOwner(req.accountNumber(), req.accountPassword())
                .map(customerId -> new VerifyOwnerResponse(true, customerId))
                .orElseGet(() -> new VerifyOwnerResponse(false, null));
    }
}
