package com.bank.common.security.crypto;

/**
 * 개인정보 컬럼(BYTEA *_enc) 암복호화 인터페이스.
 *
 * 본 단계에서는 인터페이스만 정의해 도메인 코드가 미리 의존할 수 있게 한다.
 * 실제 AES-GCM 등의 구현체와 KEK 관리 정책(.env vs KMS) 은 본격 착수 시 추가한다.
 *
 * 구현체가 빈으로 등록되지 않은 상태에서 도메인이 이 빈을 주입받지 않도록 주의한다.
 */
public interface CryptoService {

    /**
     * 평문을 암호화한다. ERD 의 BYTEA *_enc 컬럼에 저장하는 바이트 배열을 반환한다.
     */
    byte[] encrypt(String plaintext);

    /**
     * 암호문을 복호화해 평문을 반환한다.
     */
    String decrypt(byte[] ciphertext);
}
