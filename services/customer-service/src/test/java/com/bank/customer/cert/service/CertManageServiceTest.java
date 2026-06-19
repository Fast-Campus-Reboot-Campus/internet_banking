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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
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
    @DisplayName("상세 — 소유자 일치 시 코드/한글명·hasPinSet 까지 전체 매핑")
    void getCertDetail_ownerMatch_fullMapping() {
        Certificate c = Certificate.builder()
                .customerId(1L)
                .certificateSerialNumber("SERIAL-1")
                .certificateTypeCode("CERT_FIN")
                .certificateIssuerName("AXful인증센터")
                .certificateSubjectDn("CN=홍길동")
                .certificateIssuerDn("CN=AXful CA")
                .certificateIssuedDate("20240101")
                .certificateExpiryDate("20250101")
                .certificateStatusCode(Certificate.STATUS_ACTIVE)
                .certificatePurposeCode("BANKING")
                .certPinHash("HASH")
                .build();
        given(certificateRepository.findByCertificateSerialNumberAndDeletedAtIsNull("SERIAL-1"))
                .willReturn(Optional.of(c));

        CertDetailResponse res = service.getCertDetail(1L, "SERIAL-1");

        assertThat(res.serialNumber()).isEqualTo("SERIAL-1");
        assertThat(res.certType()).isEqualTo("CERT_FIN");
        assertThat(res.certTypeName()).isEqualTo("금융인증서");      // 코드→한글명 매핑
        assertThat(res.issuerName()).isEqualTo("AXful인증센터");
        assertThat(res.subjectDn()).isEqualTo("CN=홍길동");
        assertThat(res.issuerDn()).isEqualTo("CN=AXful CA");
        assertThat(res.issuedDate()).isEqualTo("20240101");
        assertThat(res.expiryDate()).isEqualTo("20250101");
        assertThat(res.status()).isEqualTo(Certificate.STATUS_ACTIVE);
        assertThat(res.statusName()).isEqualTo("정상");            // 코드→한글명 매핑
        assertThat(res.purposeCode()).isEqualTo("BANKING");
        assertThat(res.hasPinSet()).isTrue();
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
        assertThat(c.getCertificateRevokeReasonCode()).isEqualTo("USER_REQUEST");  // 폐기 사유 기록
        assertThat(c.getCertificateRevokedAt()).isNotNull();                       // 폐기 시각 기록
    }
}
