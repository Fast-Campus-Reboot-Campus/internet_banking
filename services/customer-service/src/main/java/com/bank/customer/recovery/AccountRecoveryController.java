package com.bank.customer.recovery;

import com.bank.common.web.ApiResponse;
import com.bank.customer.recovery.dto.FindIdRequest;
import com.bank.customer.recovery.dto.FindIdResponse;
import com.bank.customer.recovery.dto.ResetPasswordRequest;
import com.bank.customer.recovery.service.AccountRecoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 비로그인 ID 조회 / 사용자암호 재설정. 게이트웨이가 /api/v1/auth/** 를 public 처리하므로 토큰 없이 호출된다.
 * 본인확인은 계좌(번호+비밀번호) 기반 — {@link AccountRecoveryService}.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AccountRecoveryController {

    private final AccountRecoveryService accountRecoveryService;

    @PostMapping("/find-id")
    public ResponseEntity<ApiResponse<FindIdResponse>> findId(@RequestBody FindIdRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(accountRecoveryService.findId(request)));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody ResetPasswordRequest request) {
        accountRecoveryService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
