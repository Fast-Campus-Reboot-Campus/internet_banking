package com.bank.loan.advisory.gateway;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "advisory.ai-gateway")
public class AiGatewayProperties {

    private String baseUrl = "http://localhost:8088";
    private long timeoutMs = 35000;
    private long connectTimeoutMs = 3000;
}
