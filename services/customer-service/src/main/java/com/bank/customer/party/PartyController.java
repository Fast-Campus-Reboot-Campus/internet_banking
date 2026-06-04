package com.bank.customer.party;

import com.bank.common.web.ApiResponse;
import com.bank.customer.party.domain.ComplianceInfo;
import com.bank.customer.party.dto.AddPartyRelationRequest;
import com.bank.customer.party.dto.PartyRelationResponse;
import com.bank.customer.party.dto.PartyRoleResponse;
import com.bank.customer.party.service.PartyManageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PartyController {

    private final PartyManageService partyManageService;

    // ── 역할 ──────────────────────────────────────────────────────────────────

    /** 내 역할 목록 */
    @GetMapping("/customers/me/roles")
    public ResponseEntity<ApiResponse<List<PartyRoleResponse>>> getMyRoles(
            @RequestHeader("X-Customer-Id") Long customerId) {
        Long partyId = partyManageService.resolvePartyId(customerId);
        return ResponseEntity.ok(ApiResponse.ok(partyManageService.getRoles(partyId)));
    }

    /** 역할 종료 (직원용) */
    @PatchMapping("/internal/party/roles/{roleId}/close")
    public ResponseEntity<ApiResponse<PartyRoleResponse>> closeRole(
            @PathVariable Long roleId,
            @RequestParam(required = false) String endDate,
            @RequestParam String reasonCode) {
        return ResponseEntity.ok(ApiResponse.ok(
                partyManageService.closeRole(roleId, endDate, reasonCode)));
    }

    // ── 관계 ──────────────────────────────────────────────────────────────────

    /** 관계자 관계 목록 (직원용) */
    @GetMapping("/internal/party/{partyId}/relations")
    public ResponseEntity<ApiResponse<List<PartyRelationResponse>>> getRelations(
            @PathVariable Long partyId) {
        return ResponseEntity.ok(ApiResponse.ok(partyManageService.getRelations(partyId)));
    }

    /** 관계 등록 (직원용) */
    @PostMapping("/internal/party/{fromPartyId}/relations")
    public ResponseEntity<ApiResponse<PartyRelationResponse>> addRelation(
            @PathVariable Long fromPartyId,
            @Valid @RequestBody AddPartyRelationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(partyManageService.addRelation(fromPartyId, request)));
    }

    /** 관계 종료 (직원용) */
    @PatchMapping("/internal/party/relations/{relationId}/end")
    public ResponseEntity<ApiResponse<PartyRelationResponse>> endRelation(
            @PathVariable Long relationId,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String reasonCode) {
        return ResponseEntity.ok(ApiResponse.ok(
                partyManageService.endRelation(relationId, endDate, reasonCode)));
    }

    // ── 컴플라이언스 ──────────────────────────────────────────────────────────

    /** 컴플라이언스 정보 조회 (직원용) */
    @GetMapping("/internal/party/{partyId}/compliance")
    public ResponseEntity<ApiResponse<ComplianceInfo>> getCompliance(
            @PathVariable Long partyId) {
        return ResponseEntity.ok(ApiResponse.ok(partyManageService.getCompliance(partyId)));
    }

    /** AML 위험도 변경 (직원용) */
    @PatchMapping("/internal/party/{partyId}/compliance/aml-risk")
    public ResponseEntity<ApiResponse<Void>> updateAmlRisk(
            @PathVariable Long partyId,
            @RequestParam String riskLevel) {
        partyManageService.updateAmlRisk(partyId, riskLevel);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /** KYC 완료 처리 (직원용) */
    @PatchMapping("/internal/party/{partyId}/compliance/kyc-complete")
    public ResponseEntity<ApiResponse<Void>> completeKyc(
            @PathVariable Long partyId,
            @RequestParam String expiryDate,
            @RequestParam String methodCode) {
        partyManageService.completeKyc(partyId, expiryDate, methodCode);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
