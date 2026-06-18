package com.bank.customer.cert.service;

import com.bank.common.web.BusinessException;
import com.bank.customer.cert.domain.Certificate;
import com.bank.customer.cert.dto.CertDetailResponse;
import com.bank.customer.cert.repository.CertificateRepository;
import com.bank.customer.support.CustomerErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CertManageServiceTest {

    @Mock CertificateRepository certificateRepository;

    private CertManageService service;

    @BeforeEach
    void setUp() {
        service = new CertManageService(certificateRepository);
    }

    private Certificate cert(Long owner, String status, String pinHash) {
        return Certificate.builder()
                .customerId(owner)
                .certificateSerialNumber("SERIAL-1")
                .certificateTypeCode("CERT_FIN")
                .certificateStatusCode(status)
                .certPinHash(pinHash)
                .build();
    }

    @Test
    @DisplayName("상세 — 소유자 불일치는 CUST_030 (타인 인증서 열람 차단)")
    void getCertDetail_notOwner() {
        given(certificateRepository.findByCertificateSerialNumberAndDeletedAtIsNull("SERIAL-1"))
                .willReturn(Optional.of(cert(2L, Certificate.STATUS_ACTIVE, "HASH")));

        assertThatThrownBy(() -> service.getCertDetail(1L, "SERIAL-1"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(CustomerErrorCode.CUST_030));
    }

    @Test
    @DisplayName("상세 — hasPinSet 은 certPinHash 존재 여부로 결정")
    void getCertDetail_pinRegisteredFlag() {
        given(certificateRepository.findByCertificateSerialNumberAndDeletedAtIsNull("SERIAL-1"))
                .willReturn(Optional.of(cert(1L, Certificate.STATUS_ACTIVE, null)));

        CertDetailResponse res = service.getCertDetail(1L, "SERIAL-1");

        assertThat(res.hasPinSet()).isFalse();
    }

    @Test
    @DisplayName("폐기 — 인증서 없으면 CUST_030")
    void revoke_notFound() {
        given(certificateRepository.findByCertificateSerialNumberAndDeletedAtIsNull("SERIAL-1"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.revoke(1L, "SERIAL-1"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(CustomerErrorCode.CUST_030));
    }

    @Test
    @DisplayName("폐기 — 이미 폐기된 인증서는 CUST_032")
    void revoke_alreadyRevoked() {
        given(certificateRepository.findByCertificateSerialNumberAndDeletedAtIsNull("SERIAL-1"))
                .willReturn(Optional.of(cert(1L, Certificate.STATUS_REVOKED, "HASH")));

        assertThatThrownBy(() -> service.revoke(1L, "SERIAL-1"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(CustomerErrorCode.CUST_032));
    }

    @Test
    @DisplayName("폐기 — 정상 인증서는 REVOKED 로 전이")
    void revoke_ok() {
        Certificate c = cert(1L, Certificate.STATUS_ACTIVE, "HASH");
        given(certificateRepository.findByCertificateSerialNumberAndDeletedAtIsNull("SERIAL-1"))
                .willReturn(Optional.of(c));

        service.revoke(1L, "SERIAL-1");

        assertThat(c.getCertificateStatusCode()).isEqualTo(Certificate.STATUS_REVOKED);
    }
}
