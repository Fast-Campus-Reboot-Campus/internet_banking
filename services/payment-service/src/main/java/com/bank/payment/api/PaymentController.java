package com.bank.payment.api;

import com.bank.payment.api.dto.PaymentRequest;
import com.bank.payment.api.dto.PaymentResponse;
import com.bank.payment.domain.service.PaymentCommand;
import com.bank.payment.domain.service.PaymentOrchestrator;
import com.bank.payment.domain.service.PaymentResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 결제 API. POST /api/v1/payments.
 * 헤더(신원/멱등키) + 본문(이체 지시) → Command 조립 → Orchestrator → Result → Response 매핑.
 * 자행 동기 완결 → 200 OK.
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentOrchestrator paymentOrchestrator;

    public PaymentController(PaymentOrchestrator paymentOrchestrator) {
        this.paymentOrchestrator = paymentOrchestrator;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Auth-Token-Id") String authTokenId,
            @RequestBody PaymentRequest request) {

        // api 입력 → 도메인 입력 번역 (Command 조립)
        PaymentCommand command = new PaymentCommand(
                request.senderAccountId(),
                request.receiverBankCode(),
                request.receiverAccountNo(),
                request.receiverHolderName(),
                request.transferAmount(),
                request.receiverMemo(),
                request.senderMemo(),
                request.channel(),
                request.receiverPassbookSenderDisplay(),
                userId,
                authTokenId,
                idempotencyKey
        );

        // 오케스트레이션
        PaymentResult result = paymentOrchestrator.processPayment(command);

        // 도메인 출력 → api 출력 매핑 (COMPLETED=null, FAILED=원인코드)
        PaymentResponse response = new PaymentResponse(
                result.paymentInstructionId(),
                result.transactionNo(),
                result.status(),
                result.completedAt(),
                result.failureCategory()
        );

        // 자행 동기 완결 → 200 OK (COMPLETED/FAILED 모두 비즈니스 정상결과)
        // 타행 청산 대기 → 202 Accepted (KFTC 응답 비동기, CLEARING 상태)
        if ("CLEARING".equals(result.status())) {
            return ResponseEntity.accepted().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
