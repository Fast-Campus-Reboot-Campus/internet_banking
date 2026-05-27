package com.bank.loan.notification.outbox;

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
 * 알림 outbox. listener 가 이벤트 수신 시 (sms/kakao/email/push 각각) 한 row 씩 적재한다.
 * dispatch 배치가 채널별 어댑터에 위임해 외부 송신을 수행.
 *
 * 라이프사이클: PENDING → SENT (성공) | FAILED → DEAD (재시도 상한 초과).
 * 멱등 키 (`eventTypeCd + referenceId + channelCd`) 로 동일 이벤트 재발행을 차단한다.
 */
@Getter
@Entity
@Table(name = "notification_outbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NotificationOutbox extends BaseEntity {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENT    = "SENT";
    public static final String STATUS_FAILED  = "FAILED";
    public static final String STATUS_DEAD    = "DEAD";

    public static final int DEFAULT_MAX_ATTEMPT = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbox_id")
    private Long outboxId;

    /** 이벤트 종류. listener 가 publish 한 이벤트별 식별자. (APPLICATION_SUBMITTED, INSTALLMENT_PAID, ...) */
    @Column(name = "event_type_cd", nullable = false, length = 50)
    private String eventTypeCd;

    /** 이벤트에 묶인 도메인 PK (applId / cntrId / rtxId / clos_id 등). */
    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    /** 발송 채널. SMS / KAKAO_ALIMTALK / EMAIL / APP_PUSH. */
    @Column(name = "channel_cd", nullable = false, length = 50)
    private String channelCd;

    /** 템플릿 렌더 데이터. PII 가 포함될 수 있음 — 11 plan 의 PII 암호화 적용 대상. */
    @Column(name = "payload", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String payload;

    @Column(name = "status", nullable = false, length = 50)
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

    /** 동일 이벤트 재발행 차단용. `eventTypeCd:referenceId:channelCd`. UNIQUE. */
    @Column(name = "idempotency_key", nullable = false, length = 200, unique = true)
    private String idempotencyKey;

    public static String idempotencyKeyOf(String eventTypeCd, Long referenceId, String channelCd) {
        return eventTypeCd + ":" + referenceId + ":" + channelCd;
    }

    public void markSent(OffsetDateTime at) {
        this.status = STATUS_SENT;
        this.sentAt = at;
    }

    /**
     * 외부 송신 실패. attemptNo++, 백오프 nextAttemptAt = now + 2^attemptNo 분.
     * maxAttempt 도달 시 DEAD 로 전이.
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

    public void delayNextAttempt(OffsetDateTime nextAt) {
        this.nextAttemptAt = nextAt;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
