package com.bank.loan.advisory.controller;

import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.ApiResponse;
import com.bank.loan.advisory.dto.DocumentRegisterRequest;
import com.bank.loan.advisory.dto.DocumentRegisterResponse;
import com.bank.loan.advisory.rag.CaseIndexingService;
import com.bank.loan.advisory.rag.DocumentIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * RAG 내부 관리 API (plan §11.5 — Task 6-8).
 *
 * 권한: admin/system (X-Actor-Role=ADMIN 헤더로 보호 — 현 단계 컨트롤러 레벨 미검증,
 * 운영 환경에서는 API Gateway/서비스 메시 네트워크 정책으로 보호).
 */
@Tag(name = "어드바이저리 RAG 관리", description = "Advisory - 정책문서 인입 / 사례 인덱싱 (internal admin)")
@RestController
@RequestMapping("/api/internal/advisory")
@RequiredArgsConstructor
@Validated
public class InternalAdvisoryRagController {

    private final DocumentIngestionService ingestionService;
    private final CaseIndexingService      caseIndexingService;
    private final CurrentActorProvider     currentActor;

    @Operation(summary = "정책문서 등록 및 인입",
            description = "정책문서를 등록하고 청크 분할·임베딩·적재 후 자동 활성화한다. " +
                          "content 필드로 문서 본문을 직접 제공 (source_uri fetch 는 현 단계 미지원).")
    @PostMapping("/documents")
    public ResponseEntity<ApiResponse<DocumentRegisterResponse>> registerDocument(
            @Valid @RequestBody DocumentRegisterRequest req) {
        Long actorId = currentActor.currentActorId();
        DocumentRegisterResponse res = ingestionService.register(req, actorId);
        return ResponseEntity.status(201).body(ApiResponse.ok(res));
    }

    @Operation(summary = "문서 활성화/비활성화",
            description = "active=true 면 활성화(검색 대상 포함), false 면 비활성화(검색 제외).")
    @PutMapping("/documents/{docId}/activate")
    public ApiResponse<Void> toggleActivate(
            @PathVariable Long docId,
            @RequestParam(defaultValue = "true") boolean active) {
        Long actorId = currentActor.currentActorId();
        if (active) {
            ingestionService.activate(docId, actorId);
        } else {
            ingestionService.deactivate(docId, actorId);
        }
        return ApiResponse.ok();
    }

    @Operation(summary = "심사 사례 인덱싱",
            description = "지정 심사(revId) 또는 전체 COMPLETED 심사를 사례 인덱스에 적재한다. " +
                          "revId 생략 시 일괄 인덱싱 (초기 구동·모델 교체 후 재인덱싱용).")
    @PostMapping("/index/cases")
    public ApiResponse<IndexCasesResult> indexCases(
            @RequestParam(required = false) Long revId,
            @RequestParam(defaultValue = "N") String overturnYn) {
        Long actorId = currentActor.currentActorId();
        if (revId != null) {
            Long caseIdxId = caseIndexingService.index(revId, overturnYn, actorId);
            return ApiResponse.ok(new IndexCasesResult(1, caseIdxId));
        } else {
            int count = caseIndexingService.indexAll(actorId);
            return ApiResponse.ok(new IndexCasesResult(count, null));
        }
    }

    public record IndexCasesResult(int indexedCount, Long lastCaseIdxId) {}
}
