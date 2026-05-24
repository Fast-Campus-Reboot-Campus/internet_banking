package com.bank.payment.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InboundPaymentOrchestratorImpl implements InboundPaymentOrchestrator {

    @Override
    public void processInbound(String piId) {
        log.info("TODO step③: inbound processing for {}", piId);
    }
}
