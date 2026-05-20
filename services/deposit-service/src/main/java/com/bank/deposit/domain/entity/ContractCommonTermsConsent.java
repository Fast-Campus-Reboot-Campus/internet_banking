package com.bank.deposit.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "contract_common_terms_consents")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContractCommonTermsConsent {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_common_terms_consents_id")
    private Long contractCommonTermsConsentsId;

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(name = "consent_id", nullable = false)
    private Long consentId;

    @Column(name = "created_at", columnDefinition = "CHAR(8)", nullable = false, updatable = false)
    private String createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDate.now().format(DATE_FMT);
    }
}
