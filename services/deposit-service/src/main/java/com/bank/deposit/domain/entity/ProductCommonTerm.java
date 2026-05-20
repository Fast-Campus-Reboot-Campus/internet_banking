package com.bank.deposit.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "product_common_terms",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "terms_template_id"}))
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductCommonTerm {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_common_terms_id")
    private Long productCommonTermsId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "terms_template_id", nullable = false)
    private Long termsTemplateId;

    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private Boolean isRequired = false;

    @Column(name = "created_at", columnDefinition = "CHAR(8)", nullable = false, updatable = false)
    private String createdAt;

    @Column(name = "updated_at", columnDefinition = "CHAR(8)")
    private String updatedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDate.now().format(DATE_FMT);
    }
}
