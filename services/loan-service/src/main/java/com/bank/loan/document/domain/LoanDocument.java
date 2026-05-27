package com.bank.loan.document.domain;

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
 * 대출 서류. ERD STAGE 3 LOAN_DOCUMENT 매핑.
 */
@Getter
@Entity
@Table(name = "loan_document")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LoanDocument extends BaseEntity {

    public static final String STATUS_UPLOADED  = "UPLOADED";
    public static final String STATUS_VERIFIED  = "VERIFIED";
    public static final String STATUS_REJECTED  = "REJECTED";
    public static final String STATUS_DELETED   = "DELETED";

    public static final String SOURCE_MOBILE   = "MOBILE";
    public static final String SOURCE_COUNTER  = "COUNTER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doc_id")
    private Long docId;

    @Column(name = "appl_id", nullable = false)
    private Long applId;

    @Column(name = "doc_type_cd", nullable = false, length = 50)
    private String docTypeCd;

    @Column(name = "doc_status_cd", nullable = false, length = 50)
    private String docStatusCd;

    @Column(name = "doc_source_cd", length = 50)
    private String docSourceCd;

    @Column(name = "doc_name", length = 200)
    private String docName;

    @Column(name = "doc_url", length = 500)
    private String docUrl;

    @Column(name = "doc_hash", length = 128)
    private String docHash;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "verify_result_cd", length = 50)
    private String verifyResultCd;

    @Column(name = "retention_until", length = 8)
    private String retentionUntil;

    /** 상태 전이만 수행. soft delete (deleted_at/by) 는 BaseEntity.softDelete() 로 별도 호출. */
    public void markDeleted() {
        this.docStatusCd = STATUS_DELETED;
    }
}
