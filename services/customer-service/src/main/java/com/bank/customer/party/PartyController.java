package com.bank.customer.party;

import com.bank.common.web.ApiResponse;
import com.bank.customer.party.domain.ComplianceInfo;
import com.bank.customer.party.dto.AddPartyRelationRequest;
import com.bank.customer.party.dto.AgentReviewResponse;
import com.bank.customer.party.dto.DuplicateReviewResponse;
import com.bank.customer.party.dto.EddPendingResponse;
import com.bank.customer.party.dto.FatcaReportableResponse;
import com.bank.customer.party.dto.KycExpiringResponse;
import com.bank.customer.party.dto.MinorResponse;
import com.bank.customer.party.dto.PartyRelationResponse;
import com.bank.customer.party.dto.PartyRoleResponse;
import com.bank.customer.party.dto.SanctionHitResponse;
import com.bank.customer.party.dto.SanctionedPartyResponse;
import com.bank.customer.party.service.PartyManageService;
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

    // ── 대리인 위임장 검토 (직원용) ────────────────────────────────────────────

    /** 대리인 위임장 검토 대기 목록 — 대리인 검토 화면의 진입점 */
    @GetMapping("/internal/party/relations/review-pending")
    public ResponseEntity<ApiResponse<Page<AgentReviewResponse>>> listPendingAgentReviews(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(partyManageService.listPendingAgentReviews(pageable)));
    }

    /** 위임장 검토 승인 (권한 액팅) */
    @PatchMapping("/internal/party/relations/{relationId}/approve")
    public ResponseEntity<ApiResponse<PartyRelationResponse>> approveReview(
            @PathVariable Long relationId) {
        return ResponseEntity.ok(ApiResponse.ok(partyManageService.approveReview(relationId)));
    }

    /** 위임장 검토 거절 (위조 의심 등) */
    @PatchMapping("/internal/party/relations/{relationId}/reject")
    public ResponseEntity<ApiResponse<PartyRelationResponse>> rejectReview(
            @PathVariable Long relationId) {
        return ResponseEntity.ok(ApiResponse.ok(partyManageService.rejectReview(relationId)));
    }

    // ── 컴플라이언스 ──────────────────────────────────────────────────────────

    /** EDD 심사 대기 목록 (직원용) — EDD 심사·승인 화면의 진입점 */
    @GetMapping("/internal/compliance/edd-pending")
    public ResponseEntity<ApiResponse<Page<EddPendingResponse>>> listEddPending(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(partyManageService.listEddPending(pageable)));
    }

    /** 제재대상 스크리닝 목록 (직원용) — 제재대상 Hit 검토 화면의 진입점 */
    @GetMapping("/internal/compliance/sanctioned")
    public ResponseEntity<ApiResponse<Page<SanctionedPartyResponse>>> listSanctioned(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(partyManageService.listSanctioned(pageable)));
    }

    /** FATCA/CRS 보고대상 목록 (직원용) — FATCA/CRS 화면의 진입점 */
    @GetMapping("/internal/compliance/fatca-crs")
    public ResponseEntity<ApiResponse<Page<FatcaReportableResponse>>> listFatcaCrsReportable(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(partyManageService.listFatcaCrsReportable(pageable)));
    }

    /** KYC 만료 예정 목록 (직원용) — targetDate(YYYYMMDD) 이하 만료분 */
    @GetMapping("/internal/compliance/kyc-expiring")
    public ResponseEntity<ApiResponse<Page<KycExpiringResponse>>> listKycExpiring(
            @RequestParam String targetDate,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(partyManageService.listKycExpiring(targetDate, pageable)));
    }

    /** 미성년(만 19세 미만) 목록 (직원용) — 미성년 검토 화면의 진입점 */
    @GetMapping("/internal/party/minors")
    public ResponseEntity<ApiResponse<Page<MinorResponse>>> listMinors(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(partyManageService.listMinors(pageable)));
    }

    // ── 제재 스크리닝 Hit 검토 (직원용) ────────────────────────────────────────

    /** 제재 스크리닝 검토 대기 큐 — 제재대상 Hit 검토 화면의 진입점 */
    @GetMapping("/internal/compliance/screening-hits/pending")
    public ResponseEntity<ApiResponse<Page<SanctionHitResponse>>> listPendingScreeningHits(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(partyManageService.listPendingScreeningHits(pageable)));
    }

    /** Hit 무혐의 처리 (동명이인 등) */
    @PatchMapping("/internal/compliance/screening-hits/{hitId}/clear")
    public ResponseEntity<ApiResponse<Void>> clearScreeningHit(
            @PathVariable Long hitId,
            @RequestHeader(value = "X-Employee-Id", required = false) String employeeIdHeader,
            @RequestParam(required = false) String comment) {
        partyManageService.clearScreeningHit(hitId, parseEmployeeId(employeeIdHeader), comment);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /** Hit 제재 확정 처리 */
    @PatchMapping("/internal/compliance/screening-hits/{hitId}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmScreeningHit(
            @PathVariable Long hitId,
            @RequestHeader(value = "X-Employee-Id", required = false) String employeeIdHeader,
            @RequestParam(required = false) String comment) {
        partyManageService.confirmScreeningHit(hitId, parseEmployeeId(employeeIdHeader), comment);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ── 중복고객 검토 (직원용) ─────────────────────────────────────────────────

    /** 중복고객 검토 대기 큐 — 중복고객 검토 화면의 진입점 */
    @GetMapping("/internal/party/duplicates/pending")
    public ResponseEntity<ApiResponse<Page<DuplicateReviewResponse>>> listPendingDuplicates(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(partyManageService.listPendingDuplicates(pageable)));
    }

    /** 복본 확정 */
    @PatchMapping("/internal/party/duplicates/{caseId}/duplicate")
    public ResponseEntity<ApiResponse<Void>> markDuplicate(
            @PathVariable Long caseId,
            @RequestHeader(value = "X-Employee-Id", required = false) String employeeIdHeader,
            @RequestParam(required = false) String comment) {
        partyManageService.markDuplicate(caseId, parseEmployeeId(employeeIdHeader), comment);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /** 별개(동명이인 등) 확정 */
    @PatchMapping("/internal/party/duplicates/{caseId}/distinct")
    public ResponseEntity<ApiResponse<Void>> markDistinct(
            @PathVariable Long caseId,
            @RequestHeader(value = "X-Employee-Id", required = false) String employeeIdHeader,
            @RequestParam(required = false) String comment) {
        partyManageService.markDistinct(caseId, parseEmployeeId(employeeIdHeader), comment);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

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
            @RequestHeader(value = "X-Employee-Id", required = false) String employeeIdHeader,
            @RequestParam String riskLevel) {
        partyManageService.updateAmlRisk(partyId, riskLevel, parseEmployeeId(employeeIdHeader));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /** KYC 완료 처리 (직원용) */
    @PatchMapping("/internal/party/{partyId}/compliance/kyc-complete")
    public ResponseEntity<ApiResponse<Void>> completeKyc(
            @PathVariable Long partyId,
            @RequestHeader(value = "X-Employee-Id", required = false) String employeeIdHeader,
            @RequestParam String expiryDate,
            @RequestParam String methodCode) {
        partyManageService.completeKyc(partyId, expiryDate, methodCode, parseEmployeeId(employeeIdHeader));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * 게이트웨이가 주입한 X-Employee-Id 헤더를 검토 직원 employee_id 로 파싱한다.
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
