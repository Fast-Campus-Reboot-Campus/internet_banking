package com.bank.loan.notification.dto;

import com.bank.loan.notification.outbox.NotificationOutbox;

import java.time.OffsetDateTime;

/**
 * 운영자 목록 화면용 슬림 DTO. payload 는 단건 응답에서만 노출한다.
 */
public record NotificationOutboxListItem(
        Long outboxId,
        String eventTypeCd,
        Long referenceId,
        String channelCd,
        String status,
        int attemptNo,
        int maxAttempt,
        OffsetDateTime nextAttemptAt,
        OffsetDateTime sentAt
) {
    public static NotificationOutboxListItem of(NotificationOutbox row) {
        return new NotificationOutboxListItem(
                row.getOutboxId(),
                row.getEventTypeCd(),
                row.getReferenceId(),
                row.getChannelCd(),
                row.getStatus(),
                row.getAttemptNo(),
                row.getMaxAttempt(),
                row.getNextAttemptAt(),
                row.getSentAt()
        );
    }
}
