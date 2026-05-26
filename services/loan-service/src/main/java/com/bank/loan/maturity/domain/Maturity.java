package com.bank.loan.maturity.domain;

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
 * 만기 관리. ERD STAGE 8 MATURITY 매핑. cntr_id UNIQUE — 계약당 1건.
 *
 * 라이프사이클:
 *   ACTIVE   계약 체결 시 자동 생성 (original=current=cntr_end_date)
 *   MATURED  current_maturity_date 도래 (별도 배치)
 *   CLOSED   계약 종결 시 (LoanClosure 와 연동)
 *
 * 연장:
 *   extend(N개월) → current_maturity_date += N개월, extension_count++, last_extended_date=today
 *   원본 original_maturity_date 는 불변.
 *
 * 본 단계는 자동 전이(ACTIVE→MATURED) · 만기 알림은 별도 배치 — 후속.
 */
@Getter
@Entity
@Table(name = "maturity")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Maturity extends BaseEntity {

    public static final String STATUS_ACTIVE  = "ACTIVE";
    public static final String STATUS_MATURED = "MATURED";
    public static final String STATUS_CLOSED  = "CLOSED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mat_id")
    private Long matId;

    @Column(name = "cntr_id", nullable = false, unique = true)
    private Long cntrId;

    @Column(name = "original_maturity_date", nullable = false, length = 8)
    private String originalMaturityDate;

    @Column(name = "current_maturity_date", nullable = false, length = 8)
    private String currentMaturityDate;

    @Column(name = "mat_status_cd", nullable = false, length = 50)
    private String matStatusCd;

    @Column(name = "extension_type_cd", length = 50)
    private String extensionTypeCd;

    @Column(name = "extension_count", nullable = false)
    private Integer extensionCount;

    @Column(name = "last_extended_date", length = 8)
    private String lastExtendedDate;

    @Column(name = "extended_period_mo")
    private Integer extendedPeriodMo;

    @Column(name = "notice_status_cd", length = 50)
    private String noticeStatusCd;

    @Column(name = "last_notice_at")
    private OffsetDateTime lastNoticeAt;

    public boolean isExtendable() {
        return STATUS_ACTIVE.equals(matStatusCd) || STATUS_MATURED.equals(matStatusCd);
    }

    public String currentStatus() {
        return matStatusCd;
    }

    public void extend(String newMaturityDate, int periodMo, String typeCd, String today) {
        this.currentMaturityDate = newMaturityDate;
        this.extensionCount = this.extensionCount + 1;
        this.extendedPeriodMo = periodMo;
        this.lastExtendedDate = today;
        if (typeCd != null) this.extensionTypeCd = typeCd;
    }
}
