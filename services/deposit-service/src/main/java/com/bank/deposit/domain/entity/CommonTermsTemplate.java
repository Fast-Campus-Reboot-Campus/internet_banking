package com.bank.deposit.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

@Entity
@Table(name = "common_terms_template")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class CommonTermsTemplate {

    @Id
    @Column(name = "terms_template_id")
    private Long termsTemplateId;

    @Column(name = "terms_no", length = 50, unique = true)
    private String termsNo;

    @Column(name = "terms_name", length = 200)
    private String termsName;

    @Column(name = "terms_category_cd", length = 10)
    private String termsCategoryCd;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "required_yn", columnDefinition = "CHAR(1)")
    private String requiredYn;

    @Column(name = "biz_div_cd", length = 50)
    private String bizDivCd;

    @Column(name = "active_yn", columnDefinition = "CHAR(1)")
    private String activeYn;

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
