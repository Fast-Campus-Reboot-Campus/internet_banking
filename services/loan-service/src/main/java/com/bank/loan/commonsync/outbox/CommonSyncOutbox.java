package com.bank.loan.commonsync.outbox;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * common_db write-through outbox (loan_db 적재).
 *
 * 라이프사이클:
 *   PENDING  도메인 트랜잭션에서 적재
 *   DONE     CommonSyncDispatchService 가 common_db upsert + 브리지 백필 성공 후 전이
 *   FAILED   upsert/백필 실패 — attemptNo++, nextAttemptAt 백오프
 *   DEAD     attemptNo >= maxAttempt
 *
 * target_type_cd: PRODUCT / CONTRACT / TRANSACTION
 * source_id     : loan_db 원본 PK (prod_id / cntr_id / exec_id or rtx_id)
 * source_no     : common_db 자연키 (product_cd / contract_no / transaction_no)
 * payload       : 동기화에 필요한 필드 스냅샷 (JSONB)
 * common_id     : upsert 후 common_db 가 채번한 PK — 브리지 백필 대상 값
 */
@Getter
@Entity
@Table(name = "common_sync_outbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CommonSyncOutbox extends BaseEntity {

    public static final String TARGET_PRODUCT     = "PRODUCT";
    public static final String TARGET_CONTRACT    = "CONTRACT";
    public static final String TARGET_TRANSACTION = "TRANSACTION";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_DONE    = "DONE";
    public static final String STATUS_FAILED  = "FAILED";
    public static final String STATUS_DEAD    = "DEAD";

    public static final int DEFAULT_MAX_ATTEMPT = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbox_id")
    private Long outboxId;

    @Column(name = "target_type_cd", length = 20, nullable = false)
    private String targetTypeCd;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "source_no", length = 50)
    private String sourceNo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload;

    @Column(name = "common_id")
    private Long commonId;

    @Column(name = "status", length = 50, nullable = false)
    private String status;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo;

    @Column(name = "max_attempt", nullable = false)
    private int maxAttempt;

    @Column(name = "next_attempt_at", nullable = false)
    private OffsetDateTime nextAttemptAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "synced_at")
    private OffsetDateTime syncedAt;

    @Column(name = "idempotency_key", length = 100, nullable = false, unique = true)
    private String idempotencyKey;

    public void markDone(Long resolvedCommonId, OffsetDateTime at) {
        this.status = STATUS_DONE;
        this.commonId = resolvedCommonId;
        this.syncedAt = at;
    }

    /**
     * 백오프: now + 2^attemptNo 분 (1, 2, 4, 8, 16 ...).
     * attemptNo 가 maxAttempt 에 도달하면 DEAD 로 전이.
     */
    public void markFailed(String error, OffsetDateTime now) {
        this.attemptNo = this.attemptNo + 1;
        this.lastError = truncate(error, 500);
        if (this.attemptNo >= this.maxAttempt) {
            this.status = STATUS_DEAD;
            this.nextAttemptAt = now;
        } else {
            this.status = STATUS_FAILED;
            this.nextAttemptAt = now.plusMinutes(1L << Math.min(this.attemptNo, 10));
        }
    }

    public void requeue(OffsetDateTime now) {
        this.status = STATUS_PENDING;
        this.attemptNo = 0;
        this.nextAttemptAt = now;
        this.lastError = null;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
