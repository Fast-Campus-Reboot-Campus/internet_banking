package com.bank.ai.rag.ingestion;

import com.bank.ai.rag.chunk.domain.RagChunk;
import com.bank.ai.rag.chunk.repository.RagChunkRepository;
import com.bank.ai.rag.document.domain.RagDocument;
import com.bank.ai.rag.document.repository.RagDocumentRepository;
import com.bank.ai.rag.ingestion.domain.RagIngestionLog;
import com.bank.ai.rag.ingestion.repository.RagIngestionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * RAG ingestion 의 트랜잭션 경계를 별도 빈에 분리한다.
 *
 * 이유: 외부 API(embed) 호출은 트랜잭션 바깥이어야 하므로 {@link IngestionService#ingest}
 * 메소드 전체에 {@code @Transactional} 을 걸 수 없다. 대신 DB 쓰기만 분리된 메소드에
 * 트랜잭션을 걸어야 하는데, 같은 클래스 내 self-invocation 은 Spring AOP 가 적용되지
 * 않아 트랜잭션 어드바이스가 통과한다. 별도 빈으로 분리해 호출 시 프록시를 거치게 한다.
 */
@Component
@RequiredArgsConstructor
public class IngestionPersistence {

    private final RagDocumentRepository    documentRepository;
    private final RagChunkRepository       chunkRepository;
    private final RagIngestionLogRepository logRepository;

    /** 기존 청크 삭제 후 신규 청크 저장 + 문서 체크섬·ingestedAt 갱신. */
    @Transactional
    public int upsertChunks(Long docId, List<String> chunks, List<float[]> vectors, String checksum) {
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

    @Transactional
    public void saveLog(Long docId, String phase, String status, int chunkCnt, String model) {
        RagIngestionLog entry = RagIngestionLog.builder()
                .docId(docId).phaseCd(phase).statusCd(status)
                .chunkCnt(chunkCnt == 0 ? null : chunkCnt)
                .modelName(model)
                .startedAt(OffsetDateTime.now())
                .build();
        if (RagIngestionLog.STATUS_SUCCESS.equals(status) || RagIngestionLog.STATUS_SKIP.equals(status)) {
            entry.complete(chunkCnt);
        }
        logRepository.save(entry);
    }

    @Transactional
    public void saveFailLog(Long docId, String phase, OffsetDateTime startedAt, String maskedError) {
        RagIngestionLog entry = RagIngestionLog.builder()
                .docId(docId).phaseCd(phase).statusCd(RagIngestionLog.STATUS_FAIL)
                .startedAt(startedAt)
                .build();
        entry.fail(maskedError);
        logRepository.save(entry);
    }
}
