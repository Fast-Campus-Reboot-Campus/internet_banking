package com.bank.loan.payment.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "payment")
public record PaymentServiceProperties(
        @DefaultValue("http://localhost:8080") String url,
        Collection collection,
        Disbursement disbursement
) {
    public record Collection(
            @DefaultValue("088") String bankCode,
            @DefaultValue("00000000000") String accountNo,
            @DefaultValue("한국은행") String holderName
    ) {}

    public record Disbursement(
            @DefaultValue("BANK_DISBURSE_001") String accountId
    ) {}
}
