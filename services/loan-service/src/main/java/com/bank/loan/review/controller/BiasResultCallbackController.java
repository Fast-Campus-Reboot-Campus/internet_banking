package com.bank.loan.review.controller;

import com.bank.loan.review.dto.BiasResultCallbackRequest;
import com.bank.loan.review.service.LoanReviewBiasResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * review-ai-gateway → loan-service 편향 검증 결과 콜백 수신.
 * 내부 서비스 전용 — 외부 노출 X.
 */
@RestController
@RequestMapping("/api/loans/reviews")
@RequiredArgsConstructor
public class BiasResultCallbackController {

    private final LoanReviewBiasResultService biasResultService;

    @PostMapping("/{revId}/bias-result")
    public ResponseEntity<Void> receive(@PathVariable Long revId,
                                        @RequestBody BiasResultCallbackRequest req) {
        biasResultService.apply(revId, req);
        return ResponseEntity.ok().build();
    }
}
