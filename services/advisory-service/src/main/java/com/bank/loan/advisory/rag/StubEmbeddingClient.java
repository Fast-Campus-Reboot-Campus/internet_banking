package com.bank.loan.advisory.rag;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * 결정론적 단위벡터를 생성하는 스텁 임베딩 클라이언트.
 *
 * 활성 조건: advisory.rag.embed.provider=stub (기본값 — 미설정 시 동작)
 * 테스트에서 동일 텍스트로 문서/사례를 적재하면 코사인 유사도 = 1.0 보장.
 * 운영 전환: .env 에 ADVISORY_RAG_EMBED_PROVIDER=openai 설정 시 비활성.
 */
@Component
@ConditionalOnProperty(name = "advisory.rag.embed.provider", havingValue = "stub", matchIfMissing = true)
public class StubEmbeddingClient implements EmbeddingClient {

    private static final String MODEL_CD  = "SBERT_KO_V1";
    private static final int    DIMENSION = 1536;

    @Override
    public String defaultModelCd() {
        return MODEL_CD;
    }

    @Override
    public int dimension() {
        return DIMENSION;
    }

    @Override
    public float[] embed(String text) {
        // text.hashCode() × 31 + length 로 시드 — 같은 문자열이면 항상 같은 결과
        long seed = (long) text.hashCode() * 31L + text.length();
        Random rng = new Random(seed);

        float[] v = new float[DIMENSION];
        float norm = 0f;
        for (int i = 0; i < DIMENSION; i++) {
            v[i] = rng.nextFloat() * 2f - 1f;   // [-1, 1) 균등 분포
            norm += v[i] * v[i];
        }
        norm = (float) Math.sqrt(norm);
        for (int i = 0; i < DIMENSION; i++) {
            v[i] /= norm;                         // L2 정규화
        }
        return v;
    }
}
