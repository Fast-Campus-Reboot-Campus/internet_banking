package com.bank.payment.domain.service;

/**
 * IN(수신) 결제 처리 오케스트레이션 진입점.
 * step③에서 검증·입금·응답송신을 구현한다.
 */
public interface InboundPaymentOrchestrator {

    /**
     * PI DRAFT가 저장된 이후 수신 처리 본체 진입.
     * @param piId 결제지시번호 (txInboundReceive에서 채번된 값)
     */
    void processInbound(String piId);
}
