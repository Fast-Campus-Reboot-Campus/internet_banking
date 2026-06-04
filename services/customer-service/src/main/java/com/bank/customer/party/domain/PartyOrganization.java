package com.bank.customer.party.domain;

import com.bank.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기업관계자 상세정보 (party_organization 테이블).
 * party_id 를 공유 PK로 사용 — Party 저장 후 동일 ID로 생성.
 */
@Entity
@Table(name = "party_organization")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PartyOrganization extends BaseEntity {

    public static final String SUBTYPE_CORPORATION     = "CORPORATION";
    public static final String SUBTYPE_NON_CORPORATION = "NON_CORPORATION";

    @Id
    @Column(name = "party_id")
    private Long partyId;

    /** CORPORATION | NON_CORPORATION */
    @Column(name = "org_subtype_code", nullable = false, length = 20)
    private String orgSubtypeCode;

    /** 법인등록번호 (14자리, CORPORATION 필수) */
    @Column(name = "corp_reg_no", length = 14)
    private String corpRegNo;

    @Column(name = "corp_formal_name", length = 200)
    private String corpFormalName;

    @Column(name = "corp_formal_english_name", length = 400)
    private String corpFormalEnglishName;

    @Column(name = "hq_country_code", length = 3)
    private String hqCountryCode;

    /** 외국 법인등록번호 AES-256 암호화 */
    @Column(name = "foreign_corp_reg_no_encrypted", length = 255)
    private String foreignCorpRegNoEncrypted;

    @Column(name = "corp_type_code", length = 20)
    private String corpTypeCode;

    @Column(name = "non_corp_type_code", length = 10)
    private String nonCorpTypeCode;

    @Column(name = "ownership_type_code", length = 10)
    private String ownershipTypeCode;

    @Column(name = "representative_type_code", length = 10)
    private String representativeTypeCode;

    /** YYYYMMDD */
    @Column(name = "establishment_date", length = 8)
    private String establishmentDate;

    @Column(name = "dissolution_date", length = 8)
    private String dissolutionDate;

    @Column(name = "capital_amount")
    private Long capitalAmount;

    @Column(name = "fiscal_month")
    private Short fiscalMonth;

    @Column(name = "establishment_purpose", length = 500)
    private String establishmentPurpose;

    @Column(name = "member_count")
    private Integer memberCount;

    @Column(name = "charter_url", length = 500)
    private String charterUrl;

    public boolean isCorporation() {
        return SUBTYPE_CORPORATION.equals(orgSubtypeCode);
    }

    public void updateCapital(Long capitalAmount) {
        this.capitalAmount = capitalAmount;
    }
}
