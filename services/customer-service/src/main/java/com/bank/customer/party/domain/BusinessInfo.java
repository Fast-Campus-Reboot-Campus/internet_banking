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
 * 사업자정보 (business_info 테이블).
 * party_organization 1:N. 동일 법인이 사업장 여러 개를 가질 수 있음.
 */
@Entity
@Table(name = "business_info")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BusinessInfo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "business_info_id")
    private Long businessInfoId;

    @Column(name = "party_id", nullable = false)
    private Long partyId;

    /** 사업자등록번호 (12자리, 하이픈 포함) */
    @Column(name = "biz_reg_no", nullable = false, length = 12)
    private String bizRegNo;

    @Column(name = "biz_status_code", nullable = false, length = 20)
    private String bizStatusCode;

    @Column(name = "trade_name", nullable = false, length = 200)
    private String tradeName;

    @Column(name = "english_trade_name", length = 400)
    private String englishTradeName;

    /** YYYYMMDD */
    @Column(name = "opening_date", nullable = false, length = 8)
    private String openingDate;

    @Column(name = "closing_date", length = 8)
    private String closingDate;

    /** 국세청 업종코드 6자리 */
    @Column(name = "nts_industry_code", nullable = false, length = 6)
    private String ntsIndustryCode;

    /** 한국표준산업분류 5자리 */
    @Column(name = "ksic_code", nullable = false, length = 5)
    private String ksicCode;

    @Column(name = "biz_type_code", length = 10)
    private String bizTypeCode;

    @Column(name = "biz_item_code", nullable = false, length = 10)
    private String bizItemCode;

    @Column(name = "tax_type_code", nullable = false, length = 10)
    private String taxTypeCode;

    public boolean isActive() { return "ACTIVE".equals(bizStatusCode); }

    public void close(String closingDate) {
        this.bizStatusCode = "CLOSED";
        this.closingDate   = closingDate;
    }
}
