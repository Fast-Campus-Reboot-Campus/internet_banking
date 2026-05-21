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

/**
 * 보증인 마스터. ERD STAGE 4 GUARANTOR_MASTER 매핑.
 *
 * PII 처리 (idv 와 동일 패턴):
 *   - *_masked 컬럼은 Masking 유틸로 채움
 *   - *_enc 컬럼은 BYTEA NOT NULL 이므로 평문 UTF-8 bytes 로 임시 stub 저장.
 *     Phase 3 CryptoService 구현체 도입 시 AES-GCM 등으로 교체.
 *   - guarantor_ci_hash 는 SHA-256(mobile) 임시 산출 — 실제 운영은 인증기관 발급 CI 사용.
 */
@Getter
@Entity
@Table(name = "guarantor_master")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GuarantorMaster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gmst_id")
    private Long gmstId;

    @Column(name = "guarantor_name_enc", nullable = false)
    private byte[] guarantorNameEnc;

    @Column(name = "guarantor_name_masked", length = 50)
    private String guarantorNameMasked;

    @Column(name = "guarantor_ci_hash", nullable = false, length = 128)
    private String guarantorCiHash;

    @Column(name = "relation_type_cd", length = 50)
    private String relationTypeCd;

    @Column(name = "mobile_no_enc")
    private byte[] mobileNoEnc;

    @Column(name = "mobile_no_masked", length = 20)
    private String mobileNoMasked;
}
