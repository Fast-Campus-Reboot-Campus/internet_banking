package com.bank.ai.rag.admin;

import com.bank.ai.rag.admin.dto.RagDocumentCreateRequest;
import com.bank.ai.rag.admin.dto.RagDocumentResponse;
import com.bank.ai.rag.admin.dto.RagIngestionLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * RAG 문서 관리자 API.
 * 모든 엔드포인트는 내부 네트워크 전용 ({@code /internal/rag/**}).
 * 인증은 API-GW 레이어에서 처리.
 */
@RestController
@RequestMapping("/internal/rag")
@RequiredArgsConstructor
public class RagAdminController {

    private final RagDocumentAdminService adminService;

    /**
     * 문서 단건 등록.
     * 실제 임베딩은 포함하지 않음 — 등록 후 {@code /reindex} 를 별도 호출.
     */
    @PostMapping("/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public RagDocumentResponse register(@RequestBody RagDocumentCreateRequest req) {
        return adminService.register(req);
    }

    /** 문서 목록 조회 (docTypeCd·sensitivityCd·asOfDate 필터 선택) */
    @GetMapping("/documents")
    public List<RagDocumentResponse> list(
            @RequestParam(required = false) String docTypeCd,
            @RequestParam(required = false) String sensitivityCd,
            @RequestParam(required = false) String asOfDate) {
        return adminService.findAll(docTypeCd, sensitivityCd, asOfDate);
    }

    /** 문서 단건 조회 */
    @GetMapping("/documents/{docId}")
    public RagDocumentResponse get(@PathVariable Long docId) {
        return adminService.findById(docId);
    }

    /**
     * 문서 재인제스트 트리거.
     * sourceUri 경로의 파일을 파싱→청크→임베딩→upsert. 체크섬 동일 시 SKIP.
     */
    @PostMapping("/documents/{docId}/reindex")
    public Map<String, Object> reindex(@PathVariable Long docId) {
        int savedChunks = adminService.reindex(docId);
        return Map.of("docId", docId, "savedChunks", savedChunks);
    }

    /** 문서 soft-delete (청크·로그는 보존) */
    @DeleteMapping("/documents/{docId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long docId) {
        adminService.softDelete(docId);
    }

    /** 문서별 인제스트 로그 조회 (최신순) */
    @GetMapping("/documents/{docId}/ingestion-logs")
    public List<RagIngestionLogResponse> getLogs(@PathVariable Long docId) {
        return adminService.findLogs(docId);
    }

    /**
     * _meta/index.csv 일괄 부트스트랩.
     * 미등록 문서를 자동 등록(체크섬=PENDING). 이미 등록된 URI 는 건너뜀.
     * 임베딩을 포함하지 않으므로 이후 reindex 를 별도 실행할 것.
     */
    @PostMapping("/documents/bootstrap")
    public Map<String, Object> bootstrap() throws IOException {
        int registered = adminService.bootstrapFromCsv();
        return Map.of("registered", registered);
    }
}
