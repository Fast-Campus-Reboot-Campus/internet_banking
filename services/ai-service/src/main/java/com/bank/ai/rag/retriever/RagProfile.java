package com.bank.ai.rag.retriever;

import com.bank.ai.rag.document.domain.RagDocument;

import java.util.List;

/**
 * RAG 검색 프로파일.
 * 프로파일마다 접근 가능한 docTypeCd 목록이 다르다.
 *
 * <ul>
 *   <li>{@code PRODUCT}    — 챗봇: 상품 설명·FAQ 전용</li>
 *   <li>{@code REVIEW}     — 자동심사 LLM: 법령·규정·정책·내부규칙·상품약관</li>
 *   <li>{@code BIAS_AUDIT} — 편향감사 LLM: 법령·규정·공정대출·편향사례</li>
 * </ul>
 */
public enum RagProfile {

    PRODUCT("product",
            List.of(RagDocument.DOC_TYPE_PRODUCT_TERMS,
                    RagDocument.DOC_TYPE_FAQ)),

    REVIEW("review",
            List.of(RagDocument.DOC_TYPE_LAW,
                    RagDocument.DOC_TYPE_SUPERVISION_GUIDE,
                    RagDocument.DOC_TYPE_INTERNAL_RULE,
                    RagDocument.DOC_TYPE_POLICY,
                    RagDocument.DOC_TYPE_PRODUCT_TERMS)),

    BIAS_AUDIT("bias-audit",
            List.of(RagDocument.DOC_TYPE_LAW,
                    RagDocument.DOC_TYPE_SUPERVISION_GUIDE,
                    RagDocument.DOC_TYPE_FAIR_LENDING,
                    RagDocument.DOC_TYPE_BIAS_CASE));

    private final String code;
    private final List<String> docTypes;

    RagProfile(String code, List<String> docTypes) {
        this.code     = code;
        this.docTypes = docTypes;
    }

    public String getCode()           { return code; }
    public List<String> getDocTypes() { return docTypes; }

    public static RagProfile fromCode(String code) {
        for (RagProfile p : values()) {
            if (p.code.equalsIgnoreCase(code)) return p;
        }
        throw new IllegalArgumentException("지원하지 않는 profile: " + code);
    }
}
