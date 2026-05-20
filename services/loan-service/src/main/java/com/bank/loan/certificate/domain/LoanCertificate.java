package com.bank.loan.certificate.domain;

import com.bank.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 증명서. ERD STAGE 9 LOAN_CERTIFICATE 매핑.
 *
 * 발급 시점에 cert_no 자체 채번, status=ISSUED 즉시 확정 (결재 워크플로우는 후속).
 * 다발 발급(재발급)은 새 row — cert_no 만 UNIQUE, cntr_id 는 중복 가능.
 *
 * 유형(cert_type_cd):
 *   BALANCE     대출잔액증명서
 *   DEBT        부채증명서
 *   REPAYMENT   상환증명서
 *
 * 본 단계는 실제 PDF 생성·전송 stub — cert_doc_url/hash 는 nullable.
 */
@Getter
@Entity
@Table(name = "loan_certificate")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LoanCertificate extends BaseEntity {

    public static final String STATUS_ISSUED  = "ISSUED";
    public static final String STATUS_REVOKED = "REVOKED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cert_id")
    private Long certId;

    @Column(name = "cntr_id", nullable = false)
    private Long cntrId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "cert_type_cd", nullable = false, length = 50)
    private String certTypeCd;

    @Column(name = "cert_no", nullable = false, length = 50, unique = true)
    private String certNo;

    @Column(name = "cert_status_cd", nullable = false, length = 50)
    private String certStatusCd;

    @Column(name = "cert_purpose_cd", length = 50)
    private String certPurposeCd;

    @Column(name = "cert_doc_url", length = 500)
    private String certDocUrl;

    @Column(name = "cert_doc_hash", length = 128)
    private String certDocHash;

    @Column(name = "issue_channel_cd", length = 50)
    private String issueChannelCd;

    @Column(name = "issued_at")
    private OffsetDateTime issuedAt;

    @Column(name = "retention_until", length = 8)
    private String retentionUntil;
}
