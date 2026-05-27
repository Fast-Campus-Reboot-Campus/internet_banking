package com.bank.ai.rag.chunk.domain;

import com.bank.ai.rag.store.VectorConverter;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

/**
 * RAG 임베딩 단위. 1 문서 = N 청크.
 * 청크는 개별 soft-delete 없이 문서 삭제 시 일괄 물리 삭제 (문서가 soft-delete 되면 검색 제외).
 */
@Getter
@Entity
@Table(name = "rag_chunk")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class RagChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chunk_id")
    private Long chunkId;

    @Column(name = "doc_id", nullable = false)
    private Long docId;

    @Column(name = "chunk_seq", nullable = false)
    private Integer chunkSeq;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "token_cnt")
    private Integer tokenCnt;

    /** float[] ↔ PostgreSQL vector("[v1,v2,...]") 변환은 VectorConverter 가 담당 */
    @Convert(converter = VectorConverter.class)
    @Column(name = "embedding", columnDefinition = "vector(1536)")
    private float[] embedding;

    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Builder
    public RagChunk(Long docId, Integer chunkSeq, String content,
                    Integer tokenCnt, float[] embedding, String metadata) {
        this.docId     = docId;
        this.chunkSeq  = chunkSeq;
        this.content   = content;
        this.tokenCnt  = tokenCnt;
        this.embedding = embedding;
        this.metadata  = metadata;
    }

    public void applyEmbedding(float[] vector) {
        this.embedding = vector;
    }
}
