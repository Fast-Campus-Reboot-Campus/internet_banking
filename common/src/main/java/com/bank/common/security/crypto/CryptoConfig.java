package com.bank.common.security.crypto;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CryptoProperties.class)
public class CryptoConfig {

    @Bean
    public CryptoService cryptoService(CryptoProperties props) {
        return new AesGcmCryptoService(props.keyBase64());
    }
}
