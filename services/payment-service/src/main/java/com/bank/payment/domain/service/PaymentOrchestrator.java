package com.bank.payment.domain.service;

/**
 * 결제 오케스트레이션 진입점. P-028 5단계 흐름의 단일 진입 메서드.
 * 내부 단계(txStep1/step2/step3/txStep4)는 구현 디테일 (인터페이스 비노출).
 * 구현체는 Stage 5-3에서 (PaymentOrchestratorImpl + PaymentTransactionService 분리).
 */
public interface PaymentOrchestrator {

    /**
     * 결제(이체) 처리. 자행은 동기 완결 (COMPLETED 반환).
     * @param command 이체 지시 + 신원 + 멱등키
     * @return 처리 결과 (결제지시번호/거래번호/상태/완료시각)
     */
    PaymentResult processPayment(PaymentCommand command);
}
