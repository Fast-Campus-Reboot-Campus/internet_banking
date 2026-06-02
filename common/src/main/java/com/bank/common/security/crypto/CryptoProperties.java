package com.bank.common.security.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "crypto")
public record CryptoProperties(
        // 32바이트 AES-256 키 — Base64 인코딩. 반드시 CRYPTO_KEY_BASE64 환경변수로 주입.
        // 기본값 없음: 미주입 시 바인딩 실패 → 부팅 중단 (fail-fast)
        String keyBase64
) {}
