package com.bank.loan.collateral.domain;

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
 * 담보. ERD STAGE 4 COLLATERAL 매핑.
 */
@Getter
@Entity
@Table(name = "collateral")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Collateral extends BaseEntity {

    public static final String STATUS_REGISTERED = "REGISTERED";
    public static final String STATUS_EVALUATED  = "EVALUATED";
    public static final String STATUS_APPROVED   = "APPROVED";
    public static final String STATUS_RELEASED   = "RELEASED";
    public static final String STATUS_REJECTED   = "REJECTED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "col_id")
    private Long colId;

    @Column(name = "appl_id", nullable = false)
    private Long applId;

    @Column(name = "col_type_cd", nullable = false, length = 50)
    private String colTypeCd;

    @Column(name = "col_status_cd", nullable = false, length = 50)
    private String colStatusCd;

    @Column(name = "col_no", nullable = false, length = 30, unique = true)
    private String colNo;

    @Column(name = "col_name", length = 200)
    private String colName;

    @Column(name = "col_address", length = 500)
    private String colAddress;

    @Column(name = "col_registry_no", length = 100)
    private String colRegistryNo;

    @Column(name = "declared_value")
    private Long declaredValue;

    @Column(name = "currency_cd", nullable = false, length = 10)
    private String currencyCd;

    @Column(name = "ownership_type_cd", length = 50)
    private String ownershipTypeCd;

    @Column(name = "senior_lien_yn", nullable = false, length = 1)
    private String seniorLienYn;

    @Column(name = "senior_lien_amount")
    private Long seniorLienAmount;

    public boolean isReleased() {
        return STATUS_RELEASED.equals(colStatusCd);
    }

    public String currentStatus() {
        return colStatusCd;
    }
}
