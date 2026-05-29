package com.bank.loan.payment.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "payment")
public record PaymentServiceProperties(
        @DefaultValue("http://localhost:8080") String url
) {
}
