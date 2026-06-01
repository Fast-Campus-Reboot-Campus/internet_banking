package com.bank.loan.advisory;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "advisory.service")
public record AdvisoryProperties(
        @DefaultValue("http://advisory-service:8085") String baseUrl,
        @DefaultValue("5000") int timeoutMs,
        @DefaultValue("3000") int connectTimeoutMs
) {}
