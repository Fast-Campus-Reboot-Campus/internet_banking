package com.bank.deposit.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

@Entity
@Table(name = "common_terms_version")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class CommonTermsVersion {

    @Id
    @Column(name = "terms_version_id")
    private Long termsVersionId;

    @Column(name = "terms_template_id")
    private Long termsTemplateId;

    @Column(name = "version_no", length = 20)
    private String versionNo;

    @Column(name = "version_name", length = 200)
    private String versionName;

    @Column(name = "content_url", length = 500)
    private String contentUrl;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "summary_text", columnDefinition = "TEXT")
    private String summaryText;

    @Column(name = "change_summary", columnDefinition = "TEXT")
    private String changeSummary;

    @Column(name = "effective_start_date", columnDefinition = "VARCHAR(8)")
    private String effectiveStartDate;

    @Column(name = "effective_end_date", columnDefinition = "VARCHAR(8)")
    private String effectiveEndDate;

    @Column(name = "approved_at", columnDefinition = "VARCHAR(8)")
    private String approvedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "regulatory_filing_no", length = 50)
    private String regulatoryFilingNo;

    @Column(name = "terms_version_status_cd", length = 10)
    private String termsVersionStatusCd;

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
