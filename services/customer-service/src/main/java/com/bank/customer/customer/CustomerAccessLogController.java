package com.bank.customer.customer;

import com.bank.common.web.ApiResponse;
import com.bank.customer.customer.dto.AccessLogResponse;
import com.bank.customer.customer.dto.RecordAccessRequest;
import com.bank.customer.customer.service.CustomerAccessLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 고객 조회 접근 감사로그 — 명시적 기록(연락처 열람 등)과 감사 화면 조회.
 *
 * <p>고객 상세 조회({@code GET /internal/customers/{id}})의 자동 기록은
 * {@link CustomerLifecycleController#getCustomerDetail}에서 수행한다.
 * 조회 경로 {@code /internal/customers/access-logs}는 리터럴이라 {@code /{customerId}}보다 우선 매칭된다.
 */
@RestController
@RequestMapping("/api/v1/internal/customers")
@RequiredArgsConstructor
public class CustomerAccessLogController {

    private final CustomerAccessLogService accessLogService;

    /** 명시적 접근 기록(연락처 등 민감정보 열람). 행위 직원은 X-Employee-Id 헤더로 식별. */
    @PostMapping("/{customerId}/access-log")
    public ResponseEntity<ApiResponse<Void>> recordAccess(
            @PathVariable Long customerId,
            @RequestHeader(value = "X-Employee-Id", required = false) String employeeIdHeader,
            @Valid @RequestBody RecordAccessRequest request) {
        accessLogService.record(parseEmployeeId(employeeIdHeader), customerId,
                request.actionCode(), request.reason());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /** 감사로그 조회 — 감사 화면 데이터원. keyword(직원명·고객명·행위), branch(지점 한정) 선택 필터. */
    @GetMapping("/access-logs")
    public ResponseEntity<ApiResponse<Page<AccessLogResponse>>> getAccessLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String branch,
            @PageableDefault(size = 30) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(accessLogService.search(keyword, branch, pageable)));
    }

    /** X-Employee-Id 헤더 → employee_id. 비직원은 게이트웨이가 빈 문자열을 넣으므로 null 로 정규화. */
    private static Long parseEmployeeId(String header) {
        if (header == null || header.isBlank()) return null;
        try {
            return Long.parseLong(header.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
