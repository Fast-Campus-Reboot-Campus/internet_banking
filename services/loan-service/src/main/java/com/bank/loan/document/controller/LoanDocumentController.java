package com.bank.loan.document.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.document.dto.LoanDocumentListResponse;
import com.bank.loan.document.dto.LoanDocumentResponse;
import com.bank.loan.document.service.LoanDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "신청서류", description = "LoanDocument - 대출 신청서류")
@RestController
@RequestMapping("/api/loan-applications/{applId}/documents")
@RequiredArgsConstructor
public class LoanDocumentController {

    private final LoanDocumentService service;

    @Operation(summary = "서류 목록 조회",
            description = "신청에 제출된 활성 서류 목록을 submitted_at 오름차순으로 반환한다.")
    @GetMapping
    public ApiResponse<LoanDocumentListResponse> list(@PathVariable Long applId) {
        return ApiResponse.ok(service.list(applId));
    }

    @Operation(summary = "신청서류 업로드",
            description = "multipart/form-data. docTypeCd 필수, file 필수. doc-agent에 서류를 제출하고 검증 결과(AUTO_PASS/NEEDS_RESUBMIT/HOLD)를 반영한다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<LoanDocumentResponse>> upload(
            @PathVariable Long applId,
            @RequestParam("docTypeCd") String docTypeCd,
            @RequestParam(value = "docSourceCd", required = false) String docSourceCd,
            @RequestPart("file") MultipartFile file) {

        LoanDocumentResponse saved = service.upload(applId, docTypeCd, docSourceCd, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }
}
