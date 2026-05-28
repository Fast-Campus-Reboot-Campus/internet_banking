package com.bank.ai.rag.retrieval;

import com.bank.ai.agent.guard.AgentLoopGuard;
import com.bank.ai.rag.RagProperties;
import com.bank.ai.rag.search.Chunk;
import com.bank.ai.rag.search.RagSearchProperties;
import com.bank.ai.rag.search.RagSearchService;
import com.bank.ai.rule.domain.Track;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 코드 오케스트레이션 진입점 — phase-d-rag.md D2-3.
 *
 * <p>Java 코드가 검색 순서를 결정 (LLM 자율 tool calling X). 트랙별 callCapsPerTrack 상한과
 * {@link AgentLoopGuard} 양쪽이 소진되면 현재까지 수집된 청크를 반환 (partial result).
 *
 * <p>{@code ai.rag.enabled=false} 시 즉시 빈 리스트 반환 — kill switch.
 *
 * <p>D2: 정책 코퍼스(policy_regulation) 1회 검색. D3 에서 similar_cases 검색 추가.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagRetrievalService {

    static final String POLICY_CORPUS = "policy_regulation";

    private final RagSearchService ragSearchService;
    private final RagSearchProperties searchProps;
    private final RagProperties ragProps;

    /**
     * AgentLoopGuard 통합 정책 코퍼스 검색.
     *
     * <p>guard.acquireTool() 이 false 를 반환하거나 callCapsPerTrack 을 초과하면 즉시 중단.
     * 각 RAG 검색마다 tool 슬롯 1개 소비 — 기존 도구 호출과 동일한 예산 풀.
     *
     * @param track       현재 트랙 (cap 계산 기준)
     * @param policyQuery 정책 검색 자연어 질의
     * @param guard       AgentLoopGuard — null 이면 cap 만으로 제한 (비에이전트 경로)
     * @return 검색된 Chunk 목록 (점수 내림차순). 비활성·cap 초과 시 빈 리스트.
     */
    public List<Chunk> retrieve(Track track, String policyQuery, AgentLoopGuard guard) {
        if (!ragProps.enabled()) {
            return List.of();
        }

        int cap = ragProps.capForTrack(track.name());
        List<Chunk> result = new ArrayList<>();

        // D2: 정책 코퍼스 1회 검색 (cap >= 1 인 모든 트랙)
        if (cap >= 1) {
            if (guard != null && !guard.acquireTool()) {
                log.warn("RagRetrievalService: LOOP_GUARD_HIT before policy search track={}", track);
                return result;
            }
            var chunks = ragSearchService.search(
                    POLICY_CORPUS, policyQuery, null, searchProps.defaultK());
            result.addAll(chunks);
            log.debug("RagRetrievalService: policy search track={} hits={}", track, chunks.size());
        }

        // D3: similar_cases 검색 (cap >= 2, 구현 예정)

        return result;
    }
}
