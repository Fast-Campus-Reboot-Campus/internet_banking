package com.bank.loan.notification.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.BusinessException;
import com.bank.loan.notification.dto.NotificationOutboxListItem;
import com.bank.loan.notification.dto.NotificationOutboxListResponse;
import com.bank.loan.notification.dto.NotificationOutboxResponse;
import com.bank.loan.notification.outbox.NotificationOutbox;
import com.bank.loan.notification.outbox.NotificationOutboxRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 운영자용 알림 outbox 조회·재전송.
 *
 * 조회: eventType / status 조합 필터. 페이지 사이즈는 100 으로 캡핑한다 (plan 03 E).
 *
 * 재전송: FAILED/DEAD 만 허용. PENDING/SENT 는 LOAN_191. requeue 는 도메인 메서드가 담당하고
 * 본 서비스는 status_history 에 NOTI_REQUEUED 만 publish.
 */
@Service
@RequiredArgsConstructor
public class NotificationOutboxQueryService {

    public static final int MAX_PAGE_SIZE = 100;

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_TABLE_CD = "NOTIFICATION_OUTBOX";
    private static final String REASON_REQUEUED = "NOTI_REQUEUED";

    private final NotificationOutboxRepository repository;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;

    @Transactional(readOnly = true)
    public NotificationOutboxListResponse list(String eventTypeCd, String status, Pageable pageable) {
        Pageable capped = cap(pageable);
        Page<NotificationOutbox> page;
        if (eventTypeCd != null && status != null) {
            page = repository.findByEventTypeCdAndStatusAndDeletedAtIsNull(eventTypeCd, status, capped);
        } else if (eventTypeCd != null) {
            page = repository.findByEventTypeCdAndDeletedAtIsNull(eventTypeCd, capped);
        } else if (status != null) {
            page = repository.findByStatusAndDeletedAtIsNull(status, capped);
        } else {
            page = repository.findByDeletedAtIsNull(capped);
        }
        return NotificationOutboxListResponse.of(page.map(NotificationOutboxListItem::of));
    }

    @Transactional(readOnly = true)
    public NotificationOutboxResponse get(Long outboxId) {
        NotificationOutbox row = repository.findByOutboxIdAndDeletedAtIsNull(outboxId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_190));
        return NotificationOutboxResponse.of(row);
    }

    @Transactional
    public NotificationOutboxResponse retry(Long outboxId) {
        NotificationOutbox row = repository.findByOutboxIdAndDeletedAtIsNull(outboxId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_190));

        String before = row.getStatus();
        if (!NotificationOutbox.STATUS_FAILED.equals(before)
                && !NotificationOutbox.STATUS_DEAD.equals(before)) {
            throw new BusinessException(LoanErrorCode.LOAN_191, "current=" + before);
        }

        row.requeue(OffsetDateTime.now());
        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_TABLE_CD, row.getOutboxId(),
                before, NotificationOutbox.STATUS_PENDING,
                REASON_REQUEUED, "operator retry",
                currentActor.currentActorId()
        ));
        return NotificationOutboxResponse.of(row);
    }

    private Pageable cap(Pageable pageable) {
        if (pageable.getPageSize() <= MAX_PAGE_SIZE) return pageable;
        return PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
    }
}
