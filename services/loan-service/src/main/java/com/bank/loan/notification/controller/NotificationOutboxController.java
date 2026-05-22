package com.bank.loan.notification.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.notification.dto.NotificationOutboxListResponse;
import com.bank.loan.notification.dto.NotificationOutboxResponse;
import com.bank.loan.notification.service.NotificationOutboxQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 운영자용 알림 outbox 조회·재전송 엔드포인트.
 *
 * 디스패치 트리거는 [[NotificationDispatchController]] (`/api/internal/notifications/dispatch`) 가 별도로 담당.
 */
@Tag(name = "알림 outbox", description = "Notification outbox - 운영자 조회·재전송")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationOutboxController {

    private final NotificationOutboxQueryService service;

    @Operation(summary = "알림 outbox 목록",
            description = "eventType / status 필터 + 페이지네이션. size 는 최대 100 으로 캡핑된다.")
    @GetMapping
    public ApiResponse<NotificationOutboxListResponse> list(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ApiResponse.ok(service.list(eventType, status, pageable));
    }

    @Operation(summary = "알림 outbox 단건 조회",
            description = "payload 포함 단건 응답. PII 가 포함될 수 있어 운영자 권한 호출 전제.")
    @GetMapping("/{outboxId}")
    public ApiResponse<NotificationOutboxResponse> get(@PathVariable Long outboxId) {
        return ApiResponse.ok(service.get(outboxId));
    }

    @Operation(summary = "알림 outbox 재전송",
            description = "FAILED/DEAD row 를 PENDING 으로 되돌려 다음 디스패치에 다시 픽업되게 한다. "
                    + "PENDING/SENT 는 LOAN_191. attemptNo 와 lastError 가 초기화되고 status_history 에 NOTI_REQUEUED 기록.")
    @PostMapping("/{outboxId}/retry")
    public ApiResponse<NotificationOutboxResponse> retry(@PathVariable Long outboxId) {
        return ApiResponse.ok(service.retry(outboxId));
    }
}
