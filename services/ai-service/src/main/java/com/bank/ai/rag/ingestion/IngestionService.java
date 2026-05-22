package com.bank.ai.rag.ingestion;

import com.bank.ai.privacy.PiiMaskingFilter;
import com.bank.ai.rag.chunk.domain.RagChunk;
import com.bank.ai.rag.chunk.repository.RagChunkRepository;
import com.bank.ai.rag.config.RagProperties;
import com.bank.ai.rag.document.domain.RagDocument;
import com.bank.ai.rag.document.repository.RagDocumentRepository;
import com.bank.ai.rag.ingestion.chunker.TextChunker;
import com.bank.ai.rag.ingestion.domain.RagIngestionLog;
import com.bank.ai.rag.ingestion.embedder.EmbeddingClient;
import com.bank.ai.rag.ingestion.parser.DocumentParser;
import com.bank.ai.rag.ingestion.repository.RagIngestionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * RAG ingestion 파이프라인 오케스트레이터.
 *
 * 흐름: checksum 확인 → parse → maskPii → chunk → embed(트랜잭션 외부) → upsert
 *
 * 외부 API 호출(embed)은 @Transactional 바깥에서 수행해 AI_GUIDELINES 준수.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private final DocumentParser       documentParser;
    private final TextChunker          textChunker;
    private final EmbeddingClient      embeddingClient;
    private final PiiMaskingFilter     piiMaskingFilter;
    private final RagDocumentRepository documentRepository;
    private final RagChunkRepository    chunkRepository;
    private final RagIngestionLogRepository logRepository;
    private final RagProperties        ragProperties;

    /**
     * 문서 하나를 처음부터 전체 파이프라인으로 처리.
     * 체크섬 동일 시 SKIP 상태로 조기 종료.
     *
     * @param docId    이미 rag_document 에 등록된 doc_id
     * @param filePath 원본 파일 경로
     * @return 적재된 청크 수 (SKIP 시 0)
     */
    public int ingest(Long docId, Path filePath) {
        RagDocument doc = documentRepository.findByDocIdAndDeletedAtIsNull(docId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 doc_id: " + docId));

        // 체크섬 확인 — 변경 없으면 SKIP
        String checksum = computeChecksum(filePath);
        if (doc.isSameContent(checksum)) {
            saveLog(docId, RagIngestionLog.PHASE_UPSERT, RagIngestionLog.STATUS_SKIP, 0, null);
            log.info("[ingest] SKIP (체크섬 동일): docId={}", docId);
            return 0;
        }

        // ── 1. PARSE (외부 IO, 트랜잭션 없음) ──────────────────────────
        OffsetDateTime parseStart = OffsetDateTime.now();
        String rawText;
        try {
            rawText = documentParser.parse(filePath);
        } catch (IOException e) {
            saveFailLog(docId, RagIngestionLog.PHASE_PARSE, parseStart, e.getMessage());
            throw new IngestionException("파싱 실패: " + filePath.getFileName(), e);
        }
        saveLog(docId, RagIngestionLog.PHASE_PARSE, RagIngestionLog.STATUS_SUCCESS, 0, null);

        // ── 2. PII 마스킹 + 청크 분할 ────────────────────────────────
        String maskedText = piiMaskingFilter.mask(rawText).maskedText();
        List<String> chunks = textChunker.chunk(maskedText);
        saveLog(docId, RagIngestionLog.PHASE_CHUNK, RagIngestionLog.STATUS_SUCCESS, chunks.size(), null);
        log.info("[ingest] 청크 분할 완료: docId={} chunks={}", docId, chunks.size());

        if (chunks.isEmpty()) {
            log.warn("[ingest] 텍스트 추출 결과 없음: docId={} file={}", docId, filePath.getFileName());
            return 0;
        }

        // ── 3. 임베딩 (외부 API, 트랜잭션 바깥) ──────────────────────
        int batchSize = ragProperties.embed().batchSize();
        List<float[]> vectors = embedInBatches(docId, chunks, batchSize);

        // ── 4. UPSERT (트랜잭션 내부) ─────────────────────────────────
        int saved = upsertChunks(docId, chunks, vectors, checksum);
        saveLog(docId, RagIngestionLog.PHASE_UPSERT, RagIngestionLog.STATUS_SUCCESS, saved, null);
        log.info("[ingest] 완료: docId={} saved={}", docId, saved);
        return saved;
    }

    /** 청크 배치 임베딩. 배치 단위로 embed 호출해 외부 API 부하 분산. */
    private List<float[]> embedInBatches(Long docId, List<String> chunks, int batchSize) {
        List<float[]> allVectors = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i += batchSize) {
            List<String> batch = chunks.subList(i, Math.min(i + batchSize, chunks.size()));
            allVectors.addAll(embeddingClient.embed(batch));
        }
        saveLog(docId, RagIngestionLog.PHASE_EMBED, RagIngestionLog.STATUS_SUCCESS, chunks.size(),
                ragProperties.embed().model());
        return allVectors;
    }

    /** 기존 청크 삭제 후 신규 청크 저장, 문서 체크섬·ingested_at 갱신. */
    @Transactional
    protected int upsertChunks(Long docId, List<String> chunks, List<float[]> vectors,
                                String checksum) {
        chunkRepository.deleteAllByDocId(docId);

        List<RagChunk> entities = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            entities.add(RagChunk.builder()
                    .docId(docId)
                    .chunkSeq(i)
                    .content(chunks.get(i))
                    .tokenCnt(chunks.get(i).split("\\s+").length)
                    .embedding(vectors.get(i))
                    .build());
        }
        chunkRepository.saveAll(entities);

        RagDocument doc = documentRepository.findByDocIdAndDeletedAtIsNull(docId).orElseThrow();
        doc.updateChecksum(checksum);
        doc.markIngested(OffsetDateTime.now());
        documentRepository.save(doc);

        return entities.size();
    }

    // ── 로그 헬퍼 ──────────────────────────────────────────────────
    @Transactional
    protected void saveLog(Long docId, String phase, String status, int chunkCnt, String model) {
        var log = RagIngestionLog.builder()
                .docId(docId).phaseCd(phase).statusCd(status)
                .chunkCnt(chunkCnt == 0 ? null : chunkCnt)
                .modelName(model)
                .startedAt(OffsetDateTime.now())
                .build();
        if (RagIngestionLog.STATUS_SUCCESS.equals(status) || RagIngestionLog.STATUS_SKIP.equals(status)) {
            log.complete(chunkCnt);
        }
        logRepository.save(log);
    }

    @Transactional
    protected void saveFailLog(Long docId, String phase, OffsetDateTime startedAt, String error) {
        var entry = RagIngestionLog.builder()
                .docId(docId).phaseCd(phase).statusCd(RagIngestionLog.STATUS_FAIL)
                .startedAt(startedAt)
                .build();
        entry.fail(truncate(error, 500));
        logRepository.save(entry);
    }

    // ── 유틸 ──────────────────────────────────────────────────────
    private String computeChecksum(Path filePath) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = java.nio.file.Files.readAllBytes(filePath);
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IngestionException("체크섬 계산 실패: " + filePath, e);
        }
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : s;
    }

    public static class IngestionException extends RuntimeException {
        public IngestionException(String msg, Throwable cause) { super(msg, cause); }
    }
}
