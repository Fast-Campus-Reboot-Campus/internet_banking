package com.bank.loan.creditreport.domain;

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
import org.hibernate.annotations.ColumnTransformer;

import java.time.OffsetDateTime;

/**
 * 신용정보 신고. ERD STAGE 8 CREDIT_INFO_REPORT 매핑.
 *
 * 화면 B-9 신용정보 신고 관리 — KCB/NICE 등 외부 기관 전송.
 *
 * 라이프사이클:
 *   REQUESTED  신고 등록 직후 (전송 대기)
 *   SENT       외부 전송 완료, 응답 대기
 *   ACKED      외부 기관 ACK 수신
 *   FAILED     전송 실패 (재전송 대상)
 *
 * 본 단계는 등록 → 즉시 SENT (전송 stub). ACK callback·재전송은 후속.
 *
 * report_payload JSONB — String 으로 받아 ::jsonb 캐스팅 (@ColumnTransformer).
 */
@Getter
@Entity
@Table(name = "credit_info_report")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CreditInfoReport extends BaseEntity {

    public static final String STATUS_REQUESTED = "REQUESTED";
    public static final String STATUS_SENT      = "SENT";
    public static final String STATUS_ACKED     = "ACKED";
    public static final String STATUS_FAILED    = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "crpt_id")
    private Long crptId;

    @Column(name = "cntr_id", nullable = false)
    private Long cntrId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "crpt_type_cd", nullable = false, length = 50)
    private String crptTypeCd;

    @Column(name = "crpt_agency_cd", nullable = false, length = 50)
    private String crptAgencyCd;

    @Column(name = "crpt_status_cd", nullable = false, length = 50)
    private String crptStatusCd;

    @Column(name = "report_target_cd", nullable = false, length = 50)
    private String reportTargetCd;

    @Column(name = "report_reason_cd", length = 50)
    private String reportReasonCd;

    @Column(name = "report_payload", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String reportPayload;

    @Column(name = "external_tx_no", length = 100)
    private String externalTxNo;

    @Column(name = "reported_at")
    private OffsetDateTime reportedAt;

    @Column(name = "ack_at")
    private OffsetDateTime ackAt;

    public void markSent(String externalTxNo, OffsetDateTime at) {
        this.crptStatusCd = STATUS_SENT;
        this.externalTxNo = externalTxNo;
        this.reportedAt = at;
    }

    public String currentStatus() {
        return crptStatusCd;
    }
}
