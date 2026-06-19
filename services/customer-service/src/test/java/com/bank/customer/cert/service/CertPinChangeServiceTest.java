package com.bank.customer.cert.service;

import com.bank.common.web.BusinessException;
import com.bank.customer.cert.domain.Certificate;
import com.bank.customer.cert.dto.CertPinChangeRequest;
import com.bank.customer.cert.repository.CertificateRepository;
import com.bank.customer.customer.domain.Credential;
import com.bank.customer.customer.repository.CredentialRepository;
import com.bank.customer.support.CustomerErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CertPinChangeServiceTest {

    @Mock CertificateRepository certificateRepository;
    @Mock CredentialRepository credentialRepository;
    @Mock PasswordEncoder passwordEncoder;

    private CertPinChangeService service;

    @BeforeEach
    void setUp() {
        service = new CertPinChangeService(certificateRepository, credentialRepository, passwordEncoder);
    }

    private CertPinChangeRequest req(String currentPin) {
        return new CertPinChangeRequest("SERIAL-1", currentPin, "654321");
    }

    @Test
    @DisplayName("인증서 없음 — CUST_030")
    void changePin_certNotFound() {
        given(certificateRepository.findByCertificateSerialNumberAndDeletedAtIsNull("SERIAL-1"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.changePin(req("123456")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(CustomerErrorCode.CUST_030));
    }

    @Test
    @DisplayName("certPinHash 보유 — 현재 PIN 일치 시 새 해시로 갱신")
    void changePin_withPinHash_ok() {
        Certificate cert = Certificate.builder().customerId(1L).certPinHash("OLD_HASH").build();
        given(certificateRepository.findByCertificateSerialNumberAndDeletedAtIsNull("SERIAL-1"))
                .willReturn(Optional.of(cert));
        given(passwordEncoder.matches("123456", "OLD_HASH")).willReturn(true);
        given(passwordEncoder.encode("654321")).willReturn("NEW_HASH");

        service.changePin(req("123456"));

        assertThat(cert.getCertPinHash()).isEqualTo("NEW_HASH");
    }

    @Test
    @DisplayName("certPinHash 보유 — 현재 PIN 불일치 시 CUST_033, 해시 미변경")
    void changePin_withPinHash_wrong() {
        Certificate cert = Certificate.builder().customerId(1L).certPinHash("OLD_HASH").build();
        given(certificateRepository.findByCertificateSerialNumberAndDeletedAtIsNull("SERIAL-1"))
                .willReturn(Optional.of(cert));
        given(passwordEncoder.matches("000000", "OLD_HASH")).willReturn(false);

        assertThatThrownBy(() -> service.changePin(req("000000")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(CustomerErrorCode.CUST_033));
        assertThat(cert.getCertPinHash()).isEqualTo("OLD_HASH");
    }

    @Test
    @DisplayName("certPinHash 없음 — 로그인 비밀번호로 fallback 검증 후 갱신")
    void changePin_fallbackToCredential_ok() {
        Certificate cert = Certificate.builder().customerId(7L).certPinHash(null).build();
        Credential credential = Credential.builder().customerId(7L).passwordHash("PW_HASH").build();
        given(certificateRepository.findByCertificateSerialNumberAndDeletedAtIsNull("SERIAL-1"))
                .willReturn(Optional.of(cert));
        given(credentialRepository.findByCustomerIdAndDeletedAtIsNull(7L))
                .willReturn(Optional.of(credential));
        given(passwordEncoder.matches("123456", "PW_HASH")).willReturn(true);
        given(passwordEncoder.encode("654321")).willReturn("NEW_HASH");

        service.changePin(req("123456"));

        assertThat(cert.getCertPinHash()).isEqualTo("NEW_HASH");
    }

    @Test
    @DisplayName("certPinHash 없음 — fallback 대상 Credential 미존재 시 CUST_010")
    void changePin_fallbackMissingCredential() {
        Certificate cert = Certificate.builder().customerId(7L).certPinHash(null).build();
        given(certificateRepository.findByCertificateSerialNumberAndDeletedAtIsNull("SERIAL-1"))
                .willReturn(Optional.of(cert));
        given(credentialRepository.findByCustomerIdAndDeletedAtIsNull(7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.changePin(req("123456")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(CustomerErrorCode.CUST_010));
    }

    @Test
    @DisplayName("certPinHash 없음 — fallback 비밀번호 불일치 시 CUST_033")
    void changePin_fallbackWrong() {
        Certificate cert = Certificate.builder().customerId(7L).certPinHash(null).build();
        Credential credential = Credential.builder().customerId(7L).passwordHash("PW_HASH").build();
        given(certificateRepository.findByCertificateSerialNumberAndDeletedAtIsNull("SERIAL-1"))
                .willReturn(Optional.of(cert));
        given(credentialRepository.findByCustomerIdAndDeletedAtIsNull(7L))
                .willReturn(Optional.of(credential));
        given(passwordEncoder.matches("000000", "PW_HASH")).willReturn(false);

        assertThatThrownBy(() -> service.changePin(req("000000")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(CustomerErrorCode.CUST_033));
    }
}
