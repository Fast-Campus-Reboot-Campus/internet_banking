package com.bank.common.security.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmCryptoServiceTest {

    private static final String KEY_32 = Base64.getEncoder().encodeToString(
            "loan-service-dev-aes-key-32bytes".getBytes());

    @Test
    @DisplayName("32바이트 키 — 정상 생성 및 암복호화 왕복 성공")
    void encrypt_then_decrypt_roundtrip() {
        AesGcmCryptoService service = new AesGcmCryptoService(KEY_32);
        String plaintext = "123-456-789012";

        byte[] ciphertext = service.encrypt(plaintext);
        String decrypted = service.decrypt(ciphertext);

        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("32바이트 키 — 동일 평문도 매번 다른 암호문 생성 (랜덤 nonce)")
    void encrypt_produces_different_ciphertext_each_time() {
        AesGcmCryptoService service = new AesGcmCryptoService(KEY_32);

        byte[] ct1 = service.encrypt("account-no");
        byte[] ct2 = service.encrypt("account-no");

        assertThat(ct1).isNotEqualTo(ct2);
    }

    @Test
    @DisplayName("31바이트 키 — 생성자에서 IllegalArgumentException (fail-fast)")
    void constructor_throws_when_key_is_31_bytes() {
        // 구 CryptoProperties @DefaultValue 에 있던 깨진 키 (31바이트)
        String brokenKey = "bG9hbi1zZXJ2aWNlLWRldi1hZXMta2V5LTMyYnl0ZQ==";
        assertThat(Base64.getDecoder().decode(brokenKey)).hasSize(31);

        assertThatThrownBy(() -> new AesGcmCryptoService(brokenKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    @DisplayName("16바이트 키(AES-128) — 생성자에서 IllegalArgumentException (AES-256 전용)")
    void constructor_throws_when_key_is_16_bytes() {
        String shortKey = Base64.getEncoder().encodeToString("0123456789abcdef".getBytes());

        assertThatThrownBy(() -> new AesGcmCryptoService(shortKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    @DisplayName("변조된 암호문 — decrypt 에서 CryptoException")
    void decrypt_throws_on_tampered_ciphertext() {
        AesGcmCryptoService service = new AesGcmCryptoService(KEY_32);
        byte[] ciphertext = service.encrypt("sensitive");
        ciphertext[ciphertext.length - 1] ^= 0xFF;

        assertThatThrownBy(() -> service.decrypt(ciphertext))
                .isInstanceOf(CryptoException.class);
    }
}
