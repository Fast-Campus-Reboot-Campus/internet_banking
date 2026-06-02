package com.bank.loan.commonsync.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.commonsync.dto.CommonSyncDispatchSummary;
import com.bank.loan.commonsync.service.CommonSyncBackfillService;
import com.bank.loan.commonsync.service.CommonSyncDispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * common_db 동기화 디스패치 + 백필 운영 엔드포인트 (internal).
 *
 * 실 운영에서는 분 단위 스케줄러가 dispatch 를 호출.
 * backfill/products 는 서비스 최초 배포 또는 재동기화 필요 시 1회성으로 호출.
 */
@Tag(name = "공통DB동기화배치", description = "CommonSync dispatch / backfill (internal)")
@RestController
@RequestMapping("/api/internal/common-sync")
@RequiredArgsConstructor
@Validated
public class CommonSyncDispatchController {

    private final CommonSyncDispatchService dispatchService;
    private final CommonSyncBackfillService backfillService;

    @Operation(summary = "common_sync_outbox 디스패치",
            description = "PENDING/FAILED 상태이면서 nextAttemptAt <= now 인 outbox 를 pageSize 건 픽업해 " +
                          "common_db upsert + loan_db 브리지 백필을 수행한다. " +
                          "기본 200건. 결과에 따라 DONE/FAILED/DEAD 로 전이.")
    @PostMapping("/dispatch")
    public ApiResponse<CommonSyncDispatchSummary> dispatch(
            @RequestParam(defaultValue = "200") @Min(1) int pageSize) {
        return ApiResponse.ok(dispatchService.dispatch(pageSize));
    }

    @Operation(summary = "상품 common_db 백필",
            description = "loan_product.product_id 가 null 인 (common_product 미동기화) 상품을 " +
                          "pageSize 건 픽업해 common_sync_outbox 에 적재한다. " +
                          "서비스 최초 배포 또는 재동기화 필요 시 호출. " +
                          "적재 후 /dispatch 를 호출해야 실제 동기화가 진행된다.")
    @PostMapping("/backfill/products")
    public ApiResponse<Integer> backfillProducts(
            @RequestParam(defaultValue = "500") @Min(1) int pageSize) {
        return ApiResponse.ok(backfillService.backfillProducts(pageSize));
    }

    @Operation(summary = "계약 common_db 백필",
            description = "ACTIVE/CLOSED 상태이면서 loan_contract.contract_id 가 null 인 " +
                          "(common_contract 미동기화) 계약을 pageSize 건 픽업해 common_sync_outbox 에 적재한다. " +
                          "SIGNED 계약은 대상 외 (아직 ACTIVE 전이 전). " +
                          "적재 후 /dispatch 를 호출해야 실제 동기화가 진행된다.")
    @PostMapping("/backfill/contracts")
    public ApiResponse<Integer> backfillContracts(
            @RequestParam(defaultValue = "500") @Min(1) int pageSize) {
        return ApiResponse.ok(backfillService.backfillContracts(pageSize));
    }
}
