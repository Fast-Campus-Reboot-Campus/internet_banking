package com.bank.ai.rag.retriever;

import com.bank.ai.rag.retriever.dto.RagSearchRequest;
import com.bank.ai.rag.retriever.dto.RagSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * RAG 검색 API.
 *
 * <pre>
 * POST /rag/search
 * {
 *   "query":        "최고이자율 초과 시 처리 방법은?",
 *   "profile":      "review",          // product | review | bias-audit
 *   "sensitivityCd": null,
 *   "asOfDate":     "20260101",        // null = 유효기간 필터 없음
 *   "topK":         5
 * }
 * </pre>
 */
@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagSearchController {

    private final RetrieverService retrieverService;

    @PostMapping("/search")
    public RagSearchResponse search(@RequestBody RagSearchRequest req) {
        return retrieverService.search(req);
    }
}
