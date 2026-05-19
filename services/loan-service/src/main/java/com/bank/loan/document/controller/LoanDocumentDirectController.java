package com.bank.loan.document.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.document.dto.LoanDocumentDownload;
import com.bank.loan.document.dto.LoanDocumentResponse;
import com.bank.loan.document.service.LoanDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 서류 ID 기반 직접 접근 엔드포인트. 다운로드 등 신청 경로 없이 docId 로 식별.
 */
@Tag(name = "신청서류", description = "LoanDocument - 직접 접근")
@RestController
@RequestMapping("/api/loan-documents")
@RequiredArgsConstructor
public class LoanDocumentDirectController {

    private final LoanDocumentService service;

    @Operation(summary = "서류 삭제",
            description = "soft delete + doc_status_cd 를 DELETED 로 전이. 실제 파일은 retention 정책에 따라 별도 정리.")
    @DeleteMapping("/{docId}")
    public ApiResponse<LoanDocumentResponse> delete(@PathVariable Long docId) {
        return ApiResponse.ok(service.delete(docId));
    }

    @Operation(summary = "서류 다운로드",
            description = "doc_url 기반 파일 스트림 반환. Content-Disposition: attachment 로 다운로드 강제.")
    @GetMapping("/{docId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long docId) {
        LoanDocumentDownload d = service.download(docId);

        String filename = d.docName() == null ? ("doc_" + docId) : d.docName();
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        MediaType mediaType = d.mimeType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(d.mimeType());

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded);
        if (d.sizeBytes() > 0) {
            builder.contentLength(d.sizeBytes());
        }
        return builder.body(d.resource());
    }
}
