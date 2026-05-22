package com.bank.ai.rag.ingestion.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * RAG 임베딩 파이프라인 단계별 감사 이력.
 * 불변 로그 — 한 번 INSERT 후 수정 없음 (finishedAt, errorMsg 는 업데이트 허용).
 */
@Getter
@Entity
@Table(name = "rag_ingestion_log")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class RagIngestionLog {

    public static final String PHASE_PARSE  = "PARSE";
    public static final String PHASE_CHUNK  = "CHUNK";
    public static final String PHASE_EMBED  = "EMBED";
    public static final String PHASE_UPSERT = "UPSERT";

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAIL    = "FAIL";
    public static final String STATUS_SKIP    = "SKIP";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "doc_id", nullable = false)
    private Long docId;

    @Column(name = "phase_cd", nullable = false, length = 30)
    private String phaseCd;

    @Column(name = "status_cd", nullable = false, length = 20)
    private String statusCd;

    @Column(name = "chunk_cnt")
    private Integer chunkCnt;

    @Column(name = "model_name", length = 100)
    private String modelName;

    /** 마스킹 후 저장 — PII 포함 금지 */
    @Column(name = "error_msg", length = 500)
    private String errorMsg;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Builder
    public RagIngestionLog(Long docId, String phaseCd, String statusCd,
                           Integer chunkCnt, String modelName, String errorMsg,
                           OffsetDateTime startedAt) {
        this.docId     = docId;
        this.phaseCd   = phaseCd;
        this.statusCd  = statusCd;
        this.chunkCnt  = chunkCnt;
        this.modelName = modelName;
        this.errorMsg  = errorMsg;
        this.startedAt = startedAt != null ? startedAt : OffsetDateTime.now();
    }

    public void complete(int chunkCount) {
        this.statusCd   = STATUS_SUCCESS;
        this.chunkCnt   = chunkCount;
        this.finishedAt = OffsetDateTime.now();
    }

    public void fail(String maskedError) {
        this.statusCd   = STATUS_FAIL;
        this.errorMsg   = maskedError;
        this.finishedAt = OffsetDateTime.now();
    }
}
