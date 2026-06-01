package com.bank.loan.document.docagent;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "doc-agent")
public record DocAgentProperties(
        @DefaultValue("http://doc-agent:8080") String baseUrl,
        @DefaultValue("3000") int connectTimeoutMs,
        @DefaultValue("10000") int readTimeoutMs
) {}
