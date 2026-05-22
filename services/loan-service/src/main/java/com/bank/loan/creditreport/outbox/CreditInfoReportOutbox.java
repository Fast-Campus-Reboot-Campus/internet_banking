package com.bank.loan.creditreport.outbox;

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
 * 신용정보 신고 outbox.
 *
 * 라이프사이클:
 *   PENDING  submit() 시 신고 row 와 함께 적재 (도메인 트랜잭션)
 *   SENT     dispatch 배치가 외부 어댑터 호출 성공 후 전이
 *   FAILED   외부 호출 실패 — attemptNo++, nextAttemptAt 백오프
 *   DEAD     attemptNo >= maxAttempt — 운영자 retry 전까지 보존
 *
 * 같은 crpt_id 에 대해 한 시점에 하나의 outbox row 만 활성 (PENDING 또는 FAILED).
 * 재시도는 본 row 를 갱신 — 새 row 만들지 않는다.
 */
@Getter
@Entity
@Table(name = "credit_info_report_outbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CreditInfoReportOutbox extends BaseEntity {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENT    = "SENT";
    public static final String STATUS_FAILED  = "FAILED";
    public static final String STATUS_DEAD    = "DEAD";

    public static final int DEFAULT_MAX_ATTEMPT = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbox_id")
    private Long outboxId;

    @Column(name = "crpt_id", nullable = false)
    private Long crptId;

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

    /**
     * 외부 전송 성공. SENT 로 전이 + sent_at 채움. last_error 는 보존 (감사 추적).
     */
    public void markSent(OffsetDateTime at) {
        this.status = STATUS_SENT;
        this.sentAt = at;
    }

    /**
     * 외부 전송 실패. attemptNo++, 백오프 계산해 nextAttemptAt 갱신.
     * attemptNo 가 maxAttempt 도달 시 DEAD 로 전이.
     *
     * 백오프: now + 2^attemptNo 분 (1, 2, 4, 8, 16 ...)
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

    /**
     * 운영자 재전송. attemptNo 리셋 + 즉시 PENDING.
     */
    public void requeue(OffsetDateTime now) {
        this.status = STATUS_PENDING;
        this.attemptNo = 0;
        this.nextAttemptAt = now;
        this.lastError = null;
    }

    /**
     * dispatch 배치가 한 row 를 처리하기 직전, 같은 배치 사이클 내 중복 픽업을 막기 위해
     * 일시적으로 다음 시도 시각을 한 사이클 뒤로 미뤄둔다. 결과 수신 후 markSent/markFailed 가 덮어쓴다.
     */
    public void delayNextAttempt(OffsetDateTime nextAt) {
        this.nextAttemptAt = nextAt;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
