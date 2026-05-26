package com.bank.loan.document.dto;

import org.springframework.core.io.Resource;

/**
 * 서류 다운로드 응답 묶음. 컨트롤러에서 ResponseEntity 헤더 구성에 사용.
 */
public record LoanDocumentDownload(
        Resource resource,
        long sizeBytes,
        String mimeType,
        String docName
) {
}
