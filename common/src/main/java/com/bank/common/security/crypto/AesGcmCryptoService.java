package com.bank.common.security.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM 암복호화 구현체.
 *
 * 저장 포맷: [12바이트 nonce][암호문 + 16바이트 GCM 인증태그]
 * 키는 {@code crypto.key-base64} 환경변수로 주입 (Base64 인코딩된 32바이트).
 */
public class AesGcmCryptoService implements CryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS   = 128;

    private final SecretKey secretKey;
    private final SecureRandom random = new SecureRandom();

    public AesGcmCryptoService(String keyBase64) {
        byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "crypto.key-base64 must decode to exactly 32 bytes (AES-256), got " + keyBytes.length);
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public byte[] encrypt(String plaintext) {
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            random.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, nonce));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            byte[] result = new byte[NONCE_BYTES + ciphertext.length];
            System.arraycopy(nonce,       0, result, 0,           NONCE_BYTES);
            System.arraycopy(ciphertext,  0, result, NONCE_BYTES, ciphertext.length);
            return result;
        } catch (Exception e) {
            throw new CryptoException("encrypt failed", e);
        }
    }

    @Override
    public String decrypt(byte[] ciphertext) {
        if (ciphertext == null || ciphertext.length <= NONCE_BYTES) {
            throw new CryptoException("invalid ciphertext length: " + (ciphertext == null ? "null" : ciphertext.length));
        }
        try {
            byte[] nonce      = Arrays.copyOfRange(ciphertext, 0, NONCE_BYTES);
            byte[] encrypted  = Arrays.copyOfRange(ciphertext, NONCE_BYTES, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, nonce));
            return new String(cipher.doFinal(encrypted), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new CryptoException("decrypt failed", e);
        }
    }
}
