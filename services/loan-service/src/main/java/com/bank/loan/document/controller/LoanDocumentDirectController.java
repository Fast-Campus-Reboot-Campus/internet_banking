package com.bank.loan.document.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.document.dto.LoanDocumentResponse;
import com.bank.loan.document.service.LoanDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "신청서류", description = "LoanDocument - 직접 접근")
@RestController
@RequestMapping("/api/loan-documents")
@RequiredArgsConstructor
public class LoanDocumentDirectController {

    private final LoanDocumentService service;

    @Operation(summary = "서류 삭제",
            description = "soft delete + doc_status_cd 를 DELETED 로 전이.")
    @DeleteMapping("/{docId}")
    public ApiResponse<LoanDocumentResponse> delete(@PathVariable Long docId) {
        return ApiResponse.ok(service.delete(docId));
    }
}
