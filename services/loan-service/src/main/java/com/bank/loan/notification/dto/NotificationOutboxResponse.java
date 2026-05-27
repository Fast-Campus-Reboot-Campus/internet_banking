package com.bank.loan.notification.dto;

import com.bank.loan.notification.outbox.NotificationOutbox;

import java.time.OffsetDateTime;

/**
 * 단건 응답 (조회 + 재전송 응답 공용).
 *
 * payload 는 PII 가 포함될 수 있어 단건 조회·재전송 응답에서만 노출한다.
 * 목록은 [[NotificationOutboxListItem]] 사용.
 */
public record NotificationOutboxResponse(
        Long outboxId,
        String eventTypeCd,
        Long referenceId,
        String channelCd,
        String payload,
        String status,
        int attemptNo,
        int maxAttempt,
        OffsetDateTime nextAttemptAt,
        String lastError,
        OffsetDateTime sentAt,
        String idempotencyKey
) {
    public static NotificationOutboxResponse of(NotificationOutbox row) {
        return new NotificationOutboxResponse(
                row.getOutboxId(),
                row.getEventTypeCd(),
                row.getReferenceId(),
                row.getChannelCd(),
                row.getPayload(),
                row.getStatus(),
                row.getAttemptNo(),
                row.getMaxAttempt(),
                row.getNextAttemptAt(),
                row.getLastError(),
                row.getSentAt(),
                row.getIdempotencyKey()
        );
    }
}
