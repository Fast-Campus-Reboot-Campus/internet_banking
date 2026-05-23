package com.bank.payment.domain.service;

import com.bank.payment.domain.PaymentInstruction;

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

    /**
     * F2 KFTC 거절 보상. CLEARING→REVERSING→FAILED + 역분개4건 + B-5 출금취소 + CT REJECTED.
     * Kafka consumer(kftc.network.response REJECT/PAYMENT_REJECT)에서 호출.
     * @param freshPi DB 재조회한 PI (CLEARING 또는 REVERSING 상태)
     * @param clearingNo KFTC 청산식별번호
     * @param rejectCode KFTC responseCode (예: 'E2001')
     * @param rejectMessage KFTC 거절메시지
     * @param rejectedAt KFTC 거절시각 (yyyyMMddHHmmss)
     * @return FAILED 결제결과
     */
    PaymentResult processKftcReject(PaymentInstruction freshPi, String clearingNo,
                                     String rejectCode, String rejectMessage, String rejectedAt);

    /**
     * F3 BOK 거절 보상. CLEARING→REVERSING→FAILED + 역분개4건 + B-5 출금취소 + BST REJECTED.
     * processKftcReject의 BOK판. Kafka consumer(bok.network.response SETTLEMENT_REJECT)에서 호출.
     * @param freshPi DB 재조회한 PI (CLEARING 또는 REVERSING 상태)
     * @param bokReferenceNo BOK 참조번호 (BST 조회키)
     * @param rejectCode BOK responseCode (예: 'B1001')
     * @param rejectMessage BOK 거절메시지
     * @param rejectedAt BOK 거절시각
     * @return FAILED 결제결과
     */
    PaymentResult processBokReject(PaymentInstruction freshPi, String bokReferenceNo,
                                    String rejectCode, String rejectMessage, String rejectedAt);
}
