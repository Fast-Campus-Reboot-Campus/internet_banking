package com.bank.customer.fds;

import com.bank.common.web.ApiResponse;
import com.bank.customer.fds.dto.FdsDetectionResponse;
import com.bank.customer.fds.dto.FdsIncidentRequest;
import com.bank.customer.fds.dto.FdsIncidentResponse;
import com.bank.customer.fds.dto.FdsRuleRequest;
import com.bank.customer.fds.dto.FdsRuleResponse;
import com.bank.customer.fds.service.FdsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * FDS 관리 API — 직원 전용.
 * 룰 관리 / 탐지 검토 / 사고 처리 엔드포인트를 제공한다.
 */
@RestController
@RequestMapping("/api/v1/internal/fds")
@RequiredArgsConstructor
public class FdsController {

    private final FdsService fdsService;

    // -------------------------------------------------------------------------
    // 룰 관리
    // -------------------------------------------------------------------------

    @PostMapping("/rules")
    public ResponseEntity<ApiResponse<FdsRuleResponse>> createRule(
            @Valid @RequestBody FdsRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(fdsService.createRule(request)));
    }

    @GetMapping("/rules")
    public ResponseEntity<ApiResponse<List<FdsRuleResponse>>> listRules() {
        return ResponseEntity.ok(ApiResponse.ok(fdsService.listRules()));
    }

    @PatchMapping("/rules/{ruleId}/activate")
    public ResponseEntity<ApiResponse<FdsRuleResponse>> activateRule(
            @PathVariable Long ruleId) {
        return ResponseEntity.ok(ApiResponse.ok(fdsService.activateRule(ruleId)));
    }

    @PatchMapping("/rules/{ruleId}/deactivate")
    public ResponseEntity<ApiResponse<FdsRuleResponse>> deactivateRule(
            @PathVariable Long ruleId) {
        return ResponseEntity.ok(ApiResponse.ok(fdsService.deactivateRule(ruleId)));
    }

    // -------------------------------------------------------------------------
    // 탐지 검토
    // -------------------------------------------------------------------------

    @GetMapping("/detections/pending")
    public ResponseEntity<ApiResponse<Page<FdsDetectionResponse>>> listPendingDetections(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(fdsService.listPendingDetections(pageable)));
    }

    @PatchMapping("/detections/{detectionId}/confirm")
    public ResponseEntity<ApiResponse<FdsDetectionResponse>> confirmDetection(
            @PathVariable Long detectionId) {
        return ResponseEntity.ok(ApiResponse.ok(fdsService.confirmDetection(detectionId)));
    }

    @PatchMapping("/detections/{detectionId}/false-positive")
    public ResponseEntity<ApiResponse<FdsDetectionResponse>> markFalsePositive(
            @PathVariable Long detectionId) {
        return ResponseEntity.ok(ApiResponse.ok(fdsService.markFalsePositive(detectionId)));
    }

    // -------------------------------------------------------------------------
    // 사고 처리
    // -------------------------------------------------------------------------

    @PostMapping("/incidents")
    public ResponseEntity<ApiResponse<FdsIncidentResponse>> openIncident(
            @Valid @RequestBody FdsIncidentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(fdsService.openIncident(request)));
    }

    @GetMapping("/incidents/open")
    public ResponseEntity<ApiResponse<Page<FdsIncidentResponse>>> listOpenIncidents(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(fdsService.listOpenIncidents(pageable)));
    }

    @PatchMapping("/incidents/{incidentId}/close")
    public ResponseEntity<ApiResponse<FdsIncidentResponse>> closeIncident(
            @PathVariable Long incidentId) {
        return ResponseEntity.ok(ApiResponse.ok(fdsService.closeIncident(incidentId)));
    }

    @PatchMapping("/incidents/{incidentId}/report-fss")
    public ResponseEntity<ApiResponse<FdsIncidentResponse>> reportToFss(
            @PathVariable Long incidentId) {
        return ResponseEntity.ok(ApiResponse.ok(fdsService.reportToFss(incidentId)));
    }
}
