package com.bank.loan.closure.domain;

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
 * 약정 종결. ERD STAGE 9 LOAN_CLOSURE 매핑. cntr_id UNIQUE — 계약당 1건.
 *
 * 종결 유형(clos_type_cd):
 *   NORMAL        정상 만기
 *   EARLY         조기상환 (잔액 0 + 만기 이전)
 *   WRITE_OFF     대손 (잔액 미회수, 운영 결정)
 *   SUBROGATION   대위변제 (보증인·보험사 대납)
 *
 * 본 단계 clos_status_cd 는 COMPLETED 만 (REQUESTED → 별도 승인 워크플로우는 후속).
 * 정산 금액(final_*) 은 서비스가 산출하며 사용자 입력은 fee/prepayment 만 받는다.
 */
@Getter
@Entity
@Table(name = "loan_closure")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LoanClosure extends BaseEntity {

    public static final String STATUS_REQUESTED = "REQUESTED";
    public static final String STATUS_COMPLETED = "COMPLETED";

    public static final String TYPE_NORMAL      = "NORMAL";
    public static final String TYPE_EARLY       = "EARLY";
    public static final String TYPE_WRITE_OFF   = "WRITE_OFF";
    public static final String TYPE_SUBROGATION = "SUBROGATION";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "clos_id")
    private Long closId;

    @Column(name = "cntr_id", nullable = false, unique = true)
    private Long cntrId;

    @Column(name = "clos_type_cd", nullable = false, length = 50)
    private String closTypeCd;

    @Column(name = "clos_reason_cd", length = 50)
    private String closReasonCd;

    @Column(name = "clos_status_cd", nullable = false, length = 50)
    private String closStatusCd;

    @Column(name = "final_principal_amt", nullable = false)
    private Long finalPrincipalAmt;

    @Column(name = "final_interest_amt", nullable = false)
    private Long finalInterestAmt;

    @Column(name = "final_fee_amt", nullable = false)
    private Long finalFeeAmt;

    @Column(name = "prepayment_fee_amt", nullable = false)
    private Long prepaymentFeeAmt;

    @Column(name = "total_settled_amt", nullable = false)
    private Long totalSettledAmt;

    @Column(name = "clos_date", nullable = false, length = 8)
    private String closDate;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "clos_doc_url", length = 500)
    private String closDocUrl;

    @Column(name = "clos_doc_hash", length = 128)
    private String closDocHash;

    @Column(name = "write_off_amount")
    private Long writeOffAmount;

    @Column(name = "subrogation_amount")
    private Long subrogationAmount;

    @Column(name = "subrogation_party_ref", length = 200)
    private String subrogationPartyRef;

    @Column(name = "write_off_reason_cd", length = 50)
    private String writeOffReasonCd;

    public static boolean requiresZeroBalance(String type) {
        return TYPE_NORMAL.equals(type) || TYPE_EARLY.equals(type);
    }

    /** SUBROGATION 시에만 보증보험·연대보증인 사전조건 검증. */
    public static boolean requiresGuaranteeCheck(String type) {
        return TYPE_SUBROGATION.equals(type);
    }

    public static boolean isWriteOff(String type) {
        return TYPE_WRITE_OFF.equals(type);
    }

    public static boolean isSubrogation(String type) {
        return TYPE_SUBROGATION.equals(type);
    }
}
