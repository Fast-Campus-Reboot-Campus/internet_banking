package com.bank.loan.guarantor.domain;

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
 * 보증 약정. ERD STAGE 4 GUARANTOR_AGREEMENT 매핑.
 *
 * 상태 전이:
 *   REGISTERED → SIGNED   (보증인 전자서명 완료. flows §1.1 — 약정 체결 전제조건)
 *   REGISTERED → CANCELED (서명 전 취소)
 *   SIGNED     → CANCELED (사후 해지. 본 단계는 허용. 약정 체결 후 해지 영향은 후속.)
 *
 * gagr_type_cd 후보: JOINT(연대), PARTIAL(부분)
 */
@Getter
@Entity
@Table(name = "guarantor_agreement")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GuarantorAgreement extends BaseEntity {

    public static final String STATUS_REGISTERED = "REGISTERED";
    public static final String STATUS_SIGNED     = "SIGNED";
    public static final String STATUS_CANCELED   = "CANCELED";

    public static final String TYPE_JOINT   = "JOINT";
    public static final String TYPE_PARTIAL = "PARTIAL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gagr_id")
    private Long gagrId;

    @Column(name = "appl_id", nullable = false)
    private Long applId;

    @Column(name = "gmst_id", nullable = false)
    private Long gmstId;

    @Column(name = "gagr_type_cd", nullable = false, length = 50)
    private String gagrTypeCd;

    @Column(name = "guarantee_amount", nullable = false)
    private Long guaranteeAmount;

    @Column(name = "guarantee_ratio_bps")
    private Integer guaranteeRatioBps;

    @Column(name = "gagr_status_cd", nullable = false, length = 50)
    private String gagrStatusCd;

    @Column(name = "consented_at")
    private OffsetDateTime consentedAt;

    @Column(name = "signed_doc_url", length = 500)
    private String signedDocUrl;

    @Column(name = "signed_doc_hash", length = 128)
    private String signedDocHash;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Column(name = "device", length = 200)
    private String device;

    public String currentStatus() {
        return gagrStatusCd;
    }

    public boolean isSignable() {
        return STATUS_REGISTERED.equals(gagrStatusCd);
    }

    public boolean isCancellable() {
        return STATUS_REGISTERED.equals(gagrStatusCd) || STATUS_SIGNED.equals(gagrStatusCd);
    }

    public void markSigned(String signedDocUrl, String signedDocHash, String clientIp, String device) {
        this.gagrStatusCd = STATUS_SIGNED;
        this.signedDocUrl = signedDocUrl;
        this.signedDocHash = signedDocHash;
        this.clientIp = clientIp;
        this.device = device;
    }

    public void markCanceled() {
        this.gagrStatusCd = STATUS_CANCELED;
    }
}
