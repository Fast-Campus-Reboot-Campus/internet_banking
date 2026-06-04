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
 * 외국인정보 (foreigner_info 테이블).
 * party_person 1:1 공유 PK.
 */
@Entity
@Table(name = "foreigner_info")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ForeignerInfo extends BaseEntity {

    @Id
    @Column(name = "party_id")
    private Long partyId;

    /** 외국인등록번호 AES-256 암호화 */
    @Column(name = "foreigner_no_encrypted", length = 255)
    private String foreignerNoEncrypted;

    @Column(name = "passport_no", length = 20)
    private String passportNo;

    @Column(name = "passport_country_code", length = 3)
    private String passportCountryCode;

    /** YYYYMMDD */
    @Column(name = "passport_expiry_date", length = 8)
    private String passportExpiryDate;

    @Column(name = "stay_qualification_code", length = 10)
    private String stayQualificationCode;

    @Column(name = "stay_expiry_date", length = 8)
    private String stayExpiryDate;

    @Column(name = "recent_entry_date", length = 8)
    private String recentEntryDate;

    @Column(name = "stay_address", length = 500)
    private String stayAddress;

    public void updatePassport(String passportNo, String countryCode, String expiryDate) {
        this.passportNo          = passportNo;
        this.passportCountryCode = countryCode;
        this.passportExpiryDate  = expiryDate;
    }

    public void updateStay(String stayQualificationCode, String stayExpiryDate) {
        this.stayQualificationCode = stayQualificationCode;
        this.stayExpiryDate        = stayExpiryDate;
    }
}
