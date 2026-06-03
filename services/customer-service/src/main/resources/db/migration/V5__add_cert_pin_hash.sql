-- 인증서 암호(PIN)를 로그인 비밀번호와 분리
ALTER TABLE certificate
    ADD COLUMN cert_pin_hash VARCHAR(255);
