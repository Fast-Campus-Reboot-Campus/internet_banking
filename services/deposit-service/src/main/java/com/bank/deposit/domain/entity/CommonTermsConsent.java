package com.bank.deposit.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

@Entity
@Table(name = "common_terms_consent")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class CommonTermsConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "consent_id")
    private Long consentId;

    @Column(name = "terms_template_id2")
    private Long termsTemplateId;

    @Column(name = "terms_version_id2")
    private Long termsVersionId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "biz_div_cd", length = 10)
    private String bizDivCd;

    @Column(name = "consent_target_id")
    private Long consentTargetId;

    @Column(name = "consent_status_cd", length = 10)
    private String consentStatusCd;

    @Column(name = "agreed_yn", columnDefinition = "CHAR(1)")
    private String agreedYn;

    @Column(name = "agreed_at", columnDefinition = "TEXT")
    private String agreedAt;

    @Column(name = "consent_method_cd", length = 10)
    private String consentMethodCd;

    @Column(name = "consent_tool", length = 500)
    private String consentTool;

    @Column(name = "signed_doc_url", length = 500)
    private String signedDocUrl;

    @Column(name = "signed_doc_hash", length = 64)
    private String signedDocHash;

    // PostgreSQL INET 타입 — JPA에서 String으로 매핑
    @Column(name = "client_ip", columnDefinition = "INET")
    private String clientIp;

    @Column(name = "withdrawn_yn", columnDefinition = "CHAR(1)")
    @Builder.Default
    private String withdrawnYn = "N";

    @Column(name = "withdrawn_at")
    private OffsetDateTime withdrawnAt;

    @Column(name = "withdrawn_reason", length = 500)
    private String withdrawnReason;

    @Column(name = "retention_until", columnDefinition = "VARCHAR(8)")
    private String retentionUntil;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;
}
