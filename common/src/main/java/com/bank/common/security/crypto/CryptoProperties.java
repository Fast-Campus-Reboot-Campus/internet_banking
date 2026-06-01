package com.bank.common.security.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "crypto")
public record CryptoProperties(
        // 32바이트 AES-256 키 — Base64 인코딩. 운영 환경에서는 반드시 환경변수로 주입.
        @DefaultValue("bG9hbi1zZXJ2aWNlLWRldi1hZXMta2V5LTMyYnl0ZQ==") String keyBase64
) {}
