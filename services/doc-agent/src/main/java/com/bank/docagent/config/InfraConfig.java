package com.bank.docagent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class InfraConfig {

    @Bean
    public RestClient restClient(
        @Value("${doc-agent.inference.connect-timeout-seconds:5}") int connectTimeout,
        @Value("${doc-agent.inference.read-timeout-seconds:30}")   int readTimeout
    ) {
        return RestClient.builder()
            .requestInterceptor((req, body, execution) -> execution.execute(req, body))
            .build();
    }
}
