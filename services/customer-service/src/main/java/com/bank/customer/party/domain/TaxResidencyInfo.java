package com.bank.customer.party.domain;

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
 * 납세거주정보 (tax_residency_info 테이블).
 * party 1:N. FATCA/CRS 보고를 위한 납세지 정보.
 */
@Entity
@Table(name = "tax_residency_info")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TaxResidencyInfo extends BaseEntity {

    public static final String TYPE_DOMESTIC = "DOMESTIC";
    public static final String TYPE_FOREIGN  = "FOREIGN";
    public static final String TYPE_DUAL     = "DUAL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tax_residency_id")
    private Long taxResidencyId;

    @Column(name = "party_id", nullable = false)
    private Long partyId;

    /** DOMESTIC | FOREIGN | DUAL */
    @Column(name = "resident_type_code", nullable = false, length = 20)
    private String residentTypeCode;

    /** ISO 3166-1 alpha-3 */
    @Column(name = "tax_country_code", length = 3)
    private String taxCountryCode;

    /** 외국 납세자번호 (TIN) */
    @Column(name = "foreign_tin", length = 50)
    private String foreignTin;

    /** 원천징수율 bps (1% = 100) */
    @Column(name = "withholding_rate_bps")
    private Integer withholdingRateBps;

    /** YYYYMMDD — 납세거주 확인일 */
    @Column(name = "tax_residency_confirm_date", nullable = false, length = 8)
    private String taxResidencyConfirmDate;

    public boolean isDomestic() { return TYPE_DOMESTIC.equals(residentTypeCode); }
}
