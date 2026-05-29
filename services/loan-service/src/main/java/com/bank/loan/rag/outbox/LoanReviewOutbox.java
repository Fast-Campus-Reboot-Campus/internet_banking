package com.bank.loan.rag.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;

import java.time.OffsetDateTime;

/**
 * 유사 케이스 적재용 LOAN_REVIEW outbox — Phase E (E3-4).
 *
 * <p>결정 완료된 심사 건의 PII-free 케이스 청크를 적재. polling worker 가 Kafka
 * topic({@code loan-review.case-indexed.v1})으로 발행 후 {@link #markSent} 한다.
 *
 * <p>라이프사이클: PENDING → SENT | FAILED → DEAD(재시도 상한 초과).
 * 멱등 키({@code eventTypeCd:aggregateId})로 동일 케이스 중복 발행 차단.
 */
@Getter
@Entity
@Table(name = "loan_review_outbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LoanReviewOutbox {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENT    = "SENT";
    public static final String STATUS_FAILED  = "FAILED";
    public static final String STATUS_DEAD    = "DEAD";

    public static final String EVENT_TYPE_CASE_INDEXED = "CASE_INDEXED";
    public static final int DEFAULT_MAX_ATTEMPT = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbox_id")
    private Long outboxId;

    /** LOAN_REVIEW rev_id. */
    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "event_type_cd", nullable = false, length = 50)
    private String eventTypeCd;

    /** PII 마스킹된 케이스 청크 페이로드 (JSON). */
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    @ColumnTransformer(write = "?::jsonb")
    private String payload;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo;

    @Column(name = "max_attempt", nullable = false)
    private int maxAttempt;

    @Column(name = "next_attempt_at", nullable = false)
    private OffsetDateTime nextAttemptAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "idempotency_key", nullable = false, length = 200, unique = true)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static String idempotencyKeyOf(String eventTypeCd, Long aggregateId) {
        return eventTypeCd + ":" + aggregateId;
    }

    /** CASE_INDEXED 이벤트 신규 적재 — PENDING 초기 상태. */
    public static LoanReviewOutbox caseIndexed(Long aggregateId, String payload) {
        OffsetDateTime now = OffsetDateTime.now();
        return LoanReviewOutbox.builder()
                .aggregateId(aggregateId)
                .eventTypeCd(EVENT_TYPE_CASE_INDEXED)
                .payload(payload)
                .status(STATUS_PENDING)
                .attemptNo(0)
                .maxAttempt(DEFAULT_MAX_ATTEMPT)
                .nextAttemptAt(now)
                .idempotencyKey(idempotencyKeyOf(EVENT_TYPE_CASE_INDEXED, aggregateId))
                .build();
    }

    public void markSent(OffsetDateTime at) {
        this.status = STATUS_SENT;
        this.sentAt = at;
    }

    /**
     * 발행 실패 — attemptNo++, 백오프 nextAttemptAt = now + 2^attemptNo 분.
     * maxAttempt 도달 시 DEAD.
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

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
