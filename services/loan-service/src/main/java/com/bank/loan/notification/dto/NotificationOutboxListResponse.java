package com.bank.loan.notification.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record NotificationOutboxListResponse(
        List<NotificationOutboxListItem> items,
        long totalCount,
        int page,
        int size
) {
    public static NotificationOutboxListResponse of(Page<NotificationOutboxListItem> p) {
        return new NotificationOutboxListResponse(
                p.getContent(), p.getTotalElements(), p.getNumber(), p.getSize());
    }
}
