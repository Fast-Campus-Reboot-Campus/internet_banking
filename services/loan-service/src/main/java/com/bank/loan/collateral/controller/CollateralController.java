package com.bank.loan.collateral.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.collateral.dto.CollateralResponse;
import com.bank.loan.collateral.dto.CreateCollateralRequest;
import com.bank.loan.collateral.service.CollateralService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "담보", description = "Collateral - 대출 담보")
@RestController
@RequestMapping("/api/loan-applications/{applId}/collaterals")
@RequiredArgsConstructor
public class CollateralController {

    private final CollateralService service;

    @Operation(summary = "담보 등록",
            description = "신청에 담보를 등록한다. 초기 상태 REGISTERED, col_no 자동 생성.")
    @PostMapping
    public ResponseEntity<ApiResponse<CollateralResponse>> register(
            @PathVariable Long applId,
            @Valid @RequestBody CreateCollateralRequest req) {
        CollateralResponse saved = service.register(applId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }
}
