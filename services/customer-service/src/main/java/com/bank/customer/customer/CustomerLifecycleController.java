package com.bank.customer.customer;

import com.bank.common.web.ApiResponse;
import com.bank.customer.customer.domain.CustomerGradeHistory;
import com.bank.customer.customer.domain.CustomerStatusHistory;
import com.bank.customer.customer.dto.ChangeGradeRequest;
import com.bank.customer.customer.dto.UpdateCreditRatingRequest;
import com.bank.customer.customer.service.CustomerLifecycleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    /** 고객 등급 변경 — 다중 필드 → RequestBody */
    @PatchMapping("/internal/customers/{customerId}/grade")
    public ResponseEntity<ApiResponse<Void>> changeGrade(
            @PathVariable Long customerId,
            @Valid @RequestBody ChangeGradeRequest request) {
        lifecycleService.changeGrade(customerId,
                request.newGradeCode(), request.reasonCode(),
                request.reasonDetail(), request.systemTriggered());
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
            @RequestParam(required = false) String reasonDetail,
            @RequestParam(defaultValue = "false") boolean systemTriggered) {
        lifecycleService.makeDormant(customerId, reasonDetail, systemTriggered);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /** 재활성화 — 단일 선택적 파람 → RequestParam 유지 */
    @PatchMapping("/internal/customers/{customerId}/reactivate")
    public ResponseEntity<ApiResponse<Void>> reactivate(
            @PathVariable Long customerId,
            @RequestParam(required = false) String reasonDetail) {
        lifecycleService.reactivate(customerId, reasonDetail);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
