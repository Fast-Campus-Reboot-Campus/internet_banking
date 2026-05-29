package com.bank.loan.autodebit.domain;

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
 * 자동이체 타행 청산(CLEARING) 대기 매핑.
 *
 * payment.* Kafka 이벤트에는 piId 만 실려오므로, CLEARING 응답 시점에
 * piId ↔ 회차(cntrId/rschId/installmentNo/baseDate)·멱등키를 저장해 둔다.
 * 완결/실패 이벤트 수신 시 piId 로 조회해 상환을 완결한다.
 */
@Getter
@Entity
@Table(name = "auto_debit_clearing_pending")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class AutoDebitClearingPending {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_DONE    = "DONE";
    public static final String STATUS_FAILED  = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pending_id")
    private Long pendingId;

    @Column(name = "pi_id", nullable = false, unique = true, length = 100)
    private String piId;

    @Column(name = "cntr_id", nullable = false)
    private Long cntrId;

    @Column(name = "rsch_id", nullable = false)
    private Long rschId;

    @Column(name = "installment_no", nullable = false)
    private Integer installmentNo;

    @Column(name = "base_date", nullable = false, length = 8)
    private String baseDate;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    public static AutoDebitClearingPending of(String piId, Long cntrId, Long rschId,
                                              Integer installmentNo, String baseDate,
                                              String idempotencyKey) {
        return AutoDebitClearingPending.builder()
                .piId(piId)
                .cntrId(cntrId)
                .rschId(rschId)
                .installmentNo(installmentNo)
                .baseDate(baseDate)
                .idempotencyKey(idempotencyKey)
                .status(STATUS_PENDING)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    public boolean isPending() {
        return STATUS_PENDING.equals(this.status);
    }

    /** 완결/실패 이벤트 처리 후 대기 상태를 해소한다. */
    public void resolve(boolean completed) {
        this.status = completed ? STATUS_DONE : STATUS_FAILED;
        this.resolvedAt = OffsetDateTime.now();
    }
}
