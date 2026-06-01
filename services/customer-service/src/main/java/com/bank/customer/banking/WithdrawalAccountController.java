package com.bank.customer.banking;

import com.bank.common.web.ApiResponse;
import com.bank.customer.banking.dto.RegisterWithdrawalAccountRequest;
import com.bank.customer.banking.dto.UpdateOrderRequest;
import com.bank.customer.banking.dto.WithdrawalAccountResponse;
import com.bank.customer.banking.service.WithdrawalAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/banking/withdrawal-accounts")
@RequiredArgsConstructor
public class WithdrawalAccountController {

    private final WithdrawalAccountService withdrawalAccountService;

    /** 출금계좌 목록 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<WithdrawalAccountResponse>>> list(
            @RequestHeader("X-Customer-Id") Long customerId) {
        return ResponseEntity.ok(ApiResponse.ok(withdrawalAccountService.list(customerId)));
    }

    /** 출금계좌 등록 */
    @PostMapping
    public ResponseEntity<ApiResponse<WithdrawalAccountResponse>> register(
            @RequestHeader("X-Customer-Id") Long customerId,
            @Valid @RequestBody RegisterWithdrawalAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(withdrawalAccountService.register(customerId, request)));
    }

    /** 출금계좌 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @RequestHeader("X-Customer-Id") Long customerId,
            @PathVariable Long id) {
        withdrawalAccountService.delete(customerId, id);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    /** 순위 변경 */
    @PutMapping("/order")
    public ResponseEntity<ApiResponse<Void>> updateOrder(
            @RequestHeader("X-Customer-Id") Long customerId,
            @Valid @RequestBody UpdateOrderRequest request) {
        withdrawalAccountService.updateOrder(customerId, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
