package com.bank.ai.rag.admin.dto;

import com.bank.ai.rag.ingestion.domain.RagIngestionLog;

import java.time.OffsetDateTime;

/**
 * RAG 인제스트 단계별 로그 응답 DTO.
 */
public record RagIngestionLogResponse(
        Long logId,
        Long docId,
        String phaseCd,
        String statusCd,
        Integer chunkCnt,
        String modelName,
        String errorMsg,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt
) {
    public static RagIngestionLogResponse from(RagIngestionLog log) {
        return new RagIngestionLogResponse(
                log.getLogId(),
                log.getDocId(),
                log.getPhaseCd(),
                log.getStatusCd(),
                log.getChunkCnt(),
                log.getModelName(),
                log.getErrorMsg(),
                log.getStartedAt(),
                log.getFinishedAt()
        );
    }
}
