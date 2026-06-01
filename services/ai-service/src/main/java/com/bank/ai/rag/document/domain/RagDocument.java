package com.bank.ai.rag.document.domain;

import com.bank.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * RAG 원본 문서 단위. 1 PDF·HWP·DOCX = 1 row.
 * 재임베딩 판정은 checksum 비교로 수행한다.
 */
@Getter
@Entity
@Table(name = "rag_document")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class RagDocument extends BaseEntity {

    // --- 상태 코드 ---
    public static final String SENSITIVITY_PUBLIC     = "PUBLIC";
    public static final String SENSITIVITY_INTERNAL   = "INTERNAL";
    public static final String SENSITIVITY_RESTRICTED = "RESTRICTED";

    public static final String DOC_TYPE_LAW              = "LAW";
    public static final String DOC_TYPE_SUPERVISION_GUIDE = "SUPERVISION_GUIDE";
    public static final String DOC_TYPE_POLICY            = "POLICY";
    public static final String DOC_TYPE_INTERNAL_RULE     = "INTERNAL_RULE";
    public static final String DOC_TYPE_PRODUCT_TERMS     = "PRODUCT_TERMS";
    public static final String DOC_TYPE_FAQ               = "FAQ";
    public static final String DOC_TYPE_FAIR_LENDING      = "FAIR_LENDING";
    public static final String DOC_TYPE_BIAS_CASE         = "BIAS_CASE";
    public static final String DOC_TYPE_REVIEW_CASE       = "REVIEW_CASE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doc_id")
    private Long docId;

    @Column(name = "doc_type_cd", nullable = false, length = 50)
    private String docTypeCd;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "source_uri", nullable = false, length = 500)
    private String sourceUri;

    @Column(name = "jurisdiction", nullable = false, length = 50)
    private String jurisdiction;

    @Column(name = "sensitivity_cd", nullable = false, length = 20)
    private String sensitivityCd;

    /** 문서 버전 식별자 (법령 호수, 약관 버전 등). BaseEntity 의 @Version 과 별개 */
    @Column(name = "doc_version", length = 50)
    private String docVersion;

    /** 시행일 YYYYMMDD */
    @Column(name = "effective_from", length = 8)
    private String effectiveFrom;

    /** 폐지·개정일 YYYYMMDD. null = 현재 유효 */
    @Column(name = "effective_to", length = 8)
    private String effectiveTo;

    /** SHA-256 체크섬. 재임베딩 판정용 */
    @Column(name = "checksum", nullable = false, length = 64)
    private String checksum;

    @Column(name = "ingested_at")
    private OffsetDateTime ingestedAt;

    @Builder
    public RagDocument(String docTypeCd, String title, String sourceUri,
                       String jurisdiction, String sensitivityCd,
                       String docVersion, String effectiveFrom, String effectiveTo,
                       String checksum) {
        this.docTypeCd    = docTypeCd;
        this.title        = title;
        this.sourceUri    = sourceUri;
        this.jurisdiction = jurisdiction != null ? jurisdiction : "KR";
        this.sensitivityCd = sensitivityCd != null ? sensitivityCd : SENSITIVITY_PUBLIC;
        this.docVersion   = docVersion;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo  = effectiveTo;
        this.checksum     = checksum;
    }

    public void markIngested(OffsetDateTime at) {
        this.ingestedAt = at;
    }

    public void updateChecksum(String newChecksum) {
        this.checksum = newChecksum;
        this.ingestedAt = null;
    }

    public boolean isSameContent(String incomingChecksum) {
        return this.checksum.equals(incomingChecksum);
    }
}
