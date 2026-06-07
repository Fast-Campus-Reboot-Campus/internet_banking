package com.bank.customer.customer;

import com.bank.common.web.ApiResponse;
import com.bank.customer.customer.domain.CustomerGradeHistory;
import com.bank.customer.customer.domain.CustomerStatusHistory;
import com.bank.customer.customer.dto.ChangeGradeRequest;
import com.bank.customer.customer.dto.CloseCustomerRequest;
import com.bank.customer.customer.dto.CustomerDetailResponse;
import com.bank.customer.customer.dto.CustomerSummaryResponse;
import com.bank.customer.customer.dto.JoinStatsResponse;
import com.bank.customer.customer.dto.UpdateCreditRatingRequest;
import com.bank.customer.customer.domain.CustomerAccessLog;
import com.bank.customer.customer.service.CustomerAccessLogService;
import com.bank.customer.customer.service.CustomerLifecycleService;
import com.bank.customer.customer.service.CustomerQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CustomerLifecycleController {

    private final CustomerLifecycleService lifecycleService;
    private final CustomerQueryService     queryService;
    private final CustomerAccessLogService accessLogService;

    // ── 고객용 이력 조회 ──────────────────────────────────────────────────────

    @GetMapping("/customers/me/status-history")
    public ResponseEntity<ApiResponse<List<CustomerStatusHistory>>> getStatusHistory(
            @RequestHeader("X-Customer-Id") Long customerId) {
        return ResponseEntity.ok(ApiResponse.ok(lifecycleService.getStatusHistory(customerId)));
    }

    @GetMapping("/customers/me/grade-history")
    public ResponseEntity<ApiResponse<List<CustomerGradeHistory>>> getGradeHistory(
            @RequestHeader("X-Customer-Id") Long customerId) {
        return ResponseEntity.ok(ApiResponse.ok(lifecycleService.getGradeHistory(customerId)));
    }

    // ── 직원용 관리 API (게이트웨이가 X-User-Id, X-User-Role 주입) ─────────────

    /**
     * 고객 목록·검색 — 모든 list 화면의 진입점.
     * keyword(이름·전화 부분일치)·status·grade는 선택 필터, customer_id 역순 고정 정렬.
     */
    @GetMapping("/internal/customers")
    public ResponseEntity<ApiResponse<Page<CustomerSummaryResponse>>> searchCustomers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String grade,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                queryService.searchCustomers(keyword, status, grade, pageable)));
    }

    /** 가입 현황 통계 — 가입 대시보드의 데이터원(customer 집계) */
    @GetMapping("/internal/customers/join-stats")
    public ResponseEntity<ApiResponse<JoinStatsResponse>> getJoinStats() {
        return ResponseEntity.ok(ApiResponse.ok(queryService.getJoinStats()));
    }

    /**
     * 고객(회원) 상세 — 회원 상세 화면의 데이터원.
     * customer·party·party_person을 합쳐 반환한다. 주민번호·계좌·동의이력은 제외(별 도메인/별 엔드포인트).
     */
    @GetMapping("/internal/customers/{customerId}")
    public ResponseEntity<ApiResponse<CustomerDetailResponse>> getCustomerDetail(
            @PathVariable Long customerId,
            @RequestHeader(value = "X-Employee-Id", required = false) String employeeIdHeader,
            @RequestHeader(value = "X-Access-Reason", required = false) String reason) {
        CustomerDetailResponse detail = queryService.getCustomerDetail(customerId);
        // 상세 조회는 접근 감사로그에 자동 기록한다(행위 직원 = X-Employee-Id).
        accessLogService.record(parseEmployeeId(employeeIdHeader), customerId,
                CustomerAccessLog.ACTION_CUSTOMER_DETAIL, reason);
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

    /** 고객 등급 변경 — 다중 필드 → RequestBody */
    @PatchMapping("/internal/customers/{customerId}/grade")
    public ResponseEntity<ApiResponse<Void>> changeGrade(
            @PathVariable Long customerId,
            @RequestHeader(value = "X-Employee-Id", required = false) String employeeIdHeader,
            @Valid @RequestBody ChangeGradeRequest request) {
        lifecycleService.changeGrade(customerId,
                request.newGradeCode(), request.reasonCode(),
                request.reasonDetail(), request.systemTriggered(),
                parseEmployeeId(employeeIdHeader));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /** 신용등급 업데이트 — 다중 필드 → RequestBody */
    @PatchMapping("/internal/customers/{customerId}/credit-rating")
    public ResponseEntity<ApiResponse<Void>> updateCreditRating(
            @PathVariable Long customerId,
            @Valid @RequestBody UpdateCreditRatingRequest request) {
        lifecycleService.updateCreditRating(customerId,
                request.ratingCode(), request.evaluationDate(), request.agencyCode());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /** 휴면 전환 — 단일 선택적 파람 → RequestParam 유지 */
    @PatchMapping("/internal/customers/{customerId}/dormant")
    public ResponseEntity<ApiResponse<Void>> makeDormant(
            @PathVariable Long customerId,
            @RequestHeader(value = "X-Employee-Id", required = false) String employeeIdHeader,
            @RequestParam(required = false) String reasonDetail,
            @RequestParam(defaultValue = "false") boolean systemTriggered) {
        lifecycleService.makeDormant(customerId, reasonDetail, systemTriggered,
                parseEmployeeId(employeeIdHeader));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /** 정지 — 단일 선택적 파람 → RequestParam 유지 */
    @PatchMapping("/internal/customers/{customerId}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspend(
            @PathVariable Long customerId,
            @RequestHeader(value = "X-Employee-Id", required = false) String employeeIdHeader,
            @RequestParam(required = false) String reasonDetail) {
        lifecycleService.suspend(customerId, reasonDetail, parseEmployeeId(employeeIdHeader));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /** 해지(탈퇴) — 다중 필드 → RequestBody */
    @PatchMapping("/internal/customers/{customerId}/close")
    public ResponseEntity<ApiResponse<Void>> close(
            @PathVariable Long customerId,
            @RequestHeader(value = "X-Employee-Id", required = false) String employeeIdHeader,
            @Valid @RequestBody CloseCustomerRequest request) {
        lifecycleService.close(customerId, request.closeReasonCode(), request.reasonDetail(),
                parseEmployeeId(employeeIdHeader));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /** 재활성화(휴면·정지 해제) — 단일 선택적 파람 → RequestParam 유지 */
    @PatchMapping("/internal/customers/{customerId}/reactivate")
    public ResponseEntity<ApiResponse<Void>> reactivate(
            @PathVariable Long customerId,
            @RequestHeader(value = "X-Employee-Id", required = false) String employeeIdHeader,
            @RequestParam(required = false) String reasonDetail) {
        lifecycleService.reactivate(customerId, reasonDetail, parseEmployeeId(employeeIdHeader));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * 게이트웨이가 주입한 X-Employee-Id 헤더를 직원 employee_id 로 파싱한다.
     * 직원이 아니면 게이트웨이가 빈 문자열을 넣으므로(또는 헤더 부재) null 로 정규화한다.
     */
    private static Long parseEmployeeId(String header) {
        if (header == null || header.isBlank()) return null;
        try {
            return Long.parseLong(header.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
