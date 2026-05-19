package com.bank.loan.idv.domain;

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
 * 본인확인(IDV). ERD STAGE 2 LOAN_IDENTITY_VERIFICATION 매핑.
 *
 * mobile_no_enc(BYTEA) 는 Phase 3 암호화 구현체가 들어온 뒤 채워진다.
 * 현재는 mobile_no_masked 만 채우고 평문은 저장하지 않는다.
 */
@Getter
@Entity
@Table(name = "loan_identity_verification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LoanIdentityVerification extends BaseEntity {

    public static final String STATUS_REQUESTED = "REQUESTED";
    public static final String STATUS_DONE      = "DONE";
    public static final String STATUS_FAILED    = "FAILED";

    public static final String RESULT_PASS = "PASS";
    public static final String RESULT_FAIL = "FAIL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idv_id")
    private Long idvId;

    @Column(name = "appl_id", nullable = false)
    private Long applId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "idv_method_cd", nullable = false, length = 50)
    private String idvMethodCd;

    @Column(name = "idv_status_cd", nullable = false, length = 50)
    private String idvStatusCd;

    @Column(name = "idv_result_cd", length = 50)
    private String idvResultCd;

    @Column(name = "idv_target_cd", nullable = false, length = 50)
    private String idvTargetCd;

    @Column(name = "ci_hash", length = 128)
    private String ciHash;

    @Column(name = "di_hash", length = 128)
    private String diHash;

    @Column(name = "mobile_no_enc")
    private byte[] mobileNoEnc;

    @Column(name = "mobile_no_masked", length = 20)
    private String mobileNoMasked;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Column(name = "device", length = 200)
    private String device;

    @Column(name = "external_tx_no", length = 100)
    private String externalTxNo;
}
