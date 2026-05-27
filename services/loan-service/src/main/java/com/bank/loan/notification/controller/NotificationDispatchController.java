package com.bank.loan.notification.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.notification.dto.NotificationDispatchSummary;
import com.bank.loan.notification.service.NotificationDispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 알림 outbox 디스패치 트리거 (internal).
 *
 * 분 단위 스케줄러 또는 운영자가 호출.
 */
@Tag(name = "알림배치", description = "Notification dispatch (internal)")
@RestController
@RequestMapping("/api/internal/notifications")
@RequiredArgsConstructor
public class NotificationDispatchController {

    private final NotificationDispatchService dispatchService;

    @Operation(summary = "알림 outbox 디스패치",
            description = "PENDING/FAILED 상태이면서 nextAttemptAt <= now 인 outbox row 를 한 페이지(200) " +
                          "단위로 픽업해 채널 어댑터에 전송. SENT/FAILED/DEAD 로 전이.")
    @PostMapping("/dispatch")
    public ApiResponse<NotificationDispatchSummary> dispatch() {
        return ApiResponse.ok(dispatchService.dispatch());
    }
}
