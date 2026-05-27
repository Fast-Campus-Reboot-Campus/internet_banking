package com.bank.loan.advisory.rag;

import com.bank.common.web.BusinessException;
import com.bank.loan.advisory.domain.AdvisoryDocument;
import com.bank.loan.advisory.dto.DocumentRegisterRequest;
import com.bank.loan.advisory.dto.DocumentRegisterResponse;
import com.bank.loan.advisory.repository.AdvisoryDocumentChunkRepository;
import com.bank.loan.advisory.repository.AdvisoryDocumentRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 정책문서 등록 + 청크 분할 + 임베딩 적재 (plan §11.4.1 — Task 6-3).
 *
 * 처리 순서:
 *   1. ADVISORY_DOCUMENT INSERT (active_yn='N')
 *   2. content 청크 분할 (800자, 100자 overlap)
 *   3. 청크별 embed → ADVISORY_DOCUMENT_CHUNK INSERT (CAST(? AS vector))
 *   4. ADVISORY_DOCUMENT.active_yn='Y' 로 활성화
 *
 * embedding 컬럼(VECTOR(1536))은 Hibernate ORM 대신 JdbcTemplate + CAST 로 직접 삽입.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private static final int CHUNK_SIZE    = 800;   // 문자 기준 (토큰 기준 800≒문자 약 1000 수준으로 보수적 설정)
    private static final int CHUNK_OVERLAP  = 100;

    private final AdvisoryDocumentRepository    docRepo;
    private final AdvisoryDocumentChunkRepository chunkRepo;
    private final EmbeddingClient               embeddingClient;
    private final JdbcTemplate                  jdbcTemplate;

    @Transactional
    public DocumentRegisterResponse register(DocumentRegisterRequest req, Long actorId) {
        if (req.content() == null || req.content().isBlank()) {
            throw new BusinessException(LoanErrorCode.LOAN_003,
                    "content 가 비어있습니다. 현 구현에서는 content 직접 제공이 필요합니다.");
        }
        // 동일 doc_cd + doc_version 중복 검증
        docRepo.findByDocCdAndDocVersionAndDeletedAtIsNull(req.docCd(), req.docVersion())
                .ifPresent(d -> { throw new BusinessException(LoanErrorCode.LOAN_001); });

        // 1. 문서 마스터 INSERT
        AdvisoryDocument doc = docRepo.save(AdvisoryDocument.builder()
                .docCd(req.docCd())
                .docTitle(req.docTitle())
                .docCategoryCd(req.docCategoryCd())
                .docVersion(req.docVersion())
                .effectiveStartDate(req.effectiveStartDate())
                .effectiveEndDate(req.effectiveEndDate())
                .sourceUri(req.sourceUri())
                .activeYn("N")
                .docDesc(req.docDesc())
                .build());

        // 2~3. 청크 분할 + 임베딩 INSERT
        int chunkCount = ingest(doc.getDocId(), req.content(), actorId);

        // 4. 활성화
        doc.activate();

        log.info("정책문서 인입 완료 — docId={} docCd={} chunkCount={}", doc.getDocId(), doc.getDocCd(), chunkCount);
        return DocumentRegisterResponse.of(doc, chunkCount);
    }

    /** 문서 비활성화 (재인입 준비 또는 만료 처리). */
    @Transactional
    public void deactivate(Long docId, Long actorId) {
        AdvisoryDocument doc = docRepo.findByDocIdAndDeletedAtIsNull(docId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_002));
        doc.deactivate();
        log.info("정책문서 비활성화 — docId={}", docId);
    }

    /** 문서 활성화 (비활성 → 활성). */
    @Transactional
    public void activate(Long docId, Long actorId) {
        AdvisoryDocument doc = docRepo.findByDocIdAndDeletedAtIsNull(docId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_002));
        if (chunkRepo.countByDocId(docId) == 0) {
            throw new BusinessException(LoanErrorCode.LOAN_003,
                    "청크가 없는 문서는 활성화할 수 없습니다. 먼저 content 를 인입하세요.");
        }
        doc.activate();
    }

    // ──────────────────────────────────────────────────
    // internal helpers
    // ──────────────────────────────────────────────────

    private int ingest(Long docId, String content, Long actorId) {
        List<String[]> chunks = splitChunks(content, CHUNK_SIZE, CHUNK_OVERLAP);
        String modelCd = embeddingClient.defaultModelCd();
        OffsetDateTime now = OffsetDateTime.now();

        for (int i = 0; i < chunks.size(); i++) {
            String text        = chunks.get(i)[0];
            String sectionPath = chunks.get(i)[1];
            float[] vec        = embeddingClient.embed(text);
            String  vecStr     = EmbeddingClient.toVectorString(vec);

            jdbcTemplate.update("""
                INSERT INTO advisory_document_chunk
                  (doc_id, chunk_seq, chunk_text, section_path, chunk_token_count,
                   embedding_model_cd, embedding, indexed_at, created_at, created_by)
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS vector), ?, now(), ?)
                """,
                    docId, i + 1, text, sectionPath, estimateTokens(text),
                    modelCd, vecStr, now, actorId);
        }
        return chunks.size();
    }

    /**
     * 텍스트를 size 자 단위로 overlap 자 중복 포함해 분할.
     * 반환: [chunkText, "char:{startPos}"] 쌍 목록.
     */
    static List<String[]> splitChunks(String content, int size, int overlap) {
        List<String[]> result = new ArrayList<>();
        int pos = 0;
        while (pos < content.length()) {
            int end  = Math.min(pos + size, content.length());
            String text = content.substring(pos, end);
            result.add(new String[]{text, "char:" + pos});
            if (end >= content.length()) break;
            pos += size - overlap;
        }
        return result;
    }

    /** 토큰 수 근사치 (한국어 문자 기준 ÷ 1.5). */
    private static int estimateTokens(String text) {
        return Math.max(1, (int) (text.length() / 1.5));
    }
}
