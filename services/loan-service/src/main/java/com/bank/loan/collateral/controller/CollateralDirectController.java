package com.bank.loan.collateral.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.collateral.dto.CollateralEvaluationResponse;
import com.bank.loan.collateral.dto.CollateralResponse;
import com.bank.loan.collateral.dto.EvaluateCollateralRequest;
import com.bank.loan.collateral.dto.ReleaseCollateralRequest;
import com.bank.loan.collateral.dto.UpdateCollateralRequest;
import com.bank.loan.collateral.service.CollateralEvaluationService;
import com.bank.loan.collateral.service.CollateralService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 담보 ID 기반 직접 접근 엔드포인트. 수정·해제 등 신청 경로 없이 colId 로 식별.
 */
@Tag(name = "담보", description = "Collateral - 직접 접근")
@RestController
@RequestMapping("/api/collaterals")
@RequiredArgsConstructor
public class CollateralDirectController {

    private final CollateralService service;
    private final CollateralEvaluationService evaluationService;

    @Operation(summary = "담보 수정",
            description = "지정된 필드만 부분 수정. col_no / appl_id / col_status_cd 는 변경 불가. 해제된 담보는 수정 불가.")
    @PatchMapping("/{colId}")
    public ApiResponse<CollateralResponse> update(@PathVariable Long colId,
                                                   @Valid @RequestBody UpdateCollateralRequest req) {
        return ApiResponse.ok(service.update(colId, req));
    }

    @Operation(summary = "담보 해제",
            description = "담보 상태를 RELEASED 로 전이한다. status_history 에 사유·해제일 기록.")
    @PostMapping("/{colId}/release")
    public ApiResponse<CollateralResponse> release(@PathVariable Long colId,
                                                    @Valid @RequestBody ReleaseCollateralRequest req) {
        return ApiResponse.ok(service.release(colId, req));
    }

    @Operation(summary = "담보 감정평가",
            description = "외부 감정평가기관 연계는 stub. applied_value 미지정 시 appraised_value 와 동일 적용. 담보 상태는 본 API 에서 자동 전이하지 않음.")
    @PostMapping("/{colId}/evaluations")
    public ResponseEntity<ApiResponse<CollateralEvaluationResponse>> evaluate(
            @PathVariable Long colId,
            @Valid @RequestBody EvaluateCollateralRequest req) {
        CollateralEvaluationResponse saved = evaluationService.evaluate(colId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }
}
