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
    /** 재시도 상한 초과. 운영자 수동 retry 전까지 보존. */
    public static final String STATUS_DEAD      = "DEAD";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "crpt_id")
    private Long crptId;

    @Column(name = "cntr_id", nullable = false)
    private Long cntrId;

    /** 연체 발화 신고일 때 출처 dlq 추적용. 자동 발화 외 (수동/약정/종결) 신고는 null. */
    @Column(name = "dlq_id")
    private Long dlqId;

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

    /** 외부 기관이 부여한 ACK 추적 번호. ACK callback 으로 채워진다. */
    @Column(name = "external_ack_no", length = 100)
    private String externalAckNo;

    public void markSent(String externalTxNo, OffsetDateTime at) {
        this.crptStatusCd = STATUS_SENT;
        this.externalTxNo = externalTxNo;
        this.reportedAt = at;
    }

    /** 외부 기관 ACK 수신 — 신고 종결. SENT 상태에서만 호출되어야 한다 (서비스 가드). */
    public void markAcked(OffsetDateTime at, String externalAckNo) {
        this.crptStatusCd = STATUS_ACKED;
        this.ackAt = at;
        this.externalAckNo = externalAckNo;
    }

    /** 외부 전송 실패 — 재시도 대상. 실패 사유는 outbox row 의 lastError 에 적재. */
    public void markFailed() {
        this.crptStatusCd = STATUS_FAILED;
    }

    /** 재시도 상한 초과 — 운영자 retry API 로만 복귀. */
    public void markDead() {
        this.crptStatusCd = STATUS_DEAD;
    }

    /** FAILED/DEAD → REQUESTED 재큐잉. 다음 dispatch 배치가 다시 집어가도록 한다. */
    public void markRequeued() {
        this.crptStatusCd = STATUS_REQUESTED;
        this.reportedAt = null;
        this.externalTxNo = null;
    }

    public String currentStatus() {
        return crptStatusCd;
    }
}
