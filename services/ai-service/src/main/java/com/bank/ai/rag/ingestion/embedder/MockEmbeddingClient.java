package com.bank.ai.rag.ingestion.embedder;

import com.bank.ai.rag.config.RagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 외부 API 호출 없는 임베딩 stub.
 * rag.embed.provider=mock 일 때만 활성화.
 * 벡터 값은 결정적(텍스트 해시 seed)으로 생성해 같은 입력은 같은 벡터를 반환.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "rag.embed.provider", havingValue = "mock", matchIfMissing = true)
@RequiredArgsConstructor
public class MockEmbeddingClient implements EmbeddingClient {

    private final RagProperties ragProperties;

    @Override
    public List<float[]> embed(List<String> texts) {
        log.debug("[embed:mock] texts={}", texts.size());
        List<float[]> result = new ArrayList<>(texts.size());
        for (String text : texts) {
            result.add(deterministicVector(text, ragProperties.embed().dimension()));
        }
        return result;
    }

    @Override
    public int dimension() {
        return ragProperties.embed().dimension();
    }

    /** 텍스트 해시를 seed 로 결정적 unit-norm 벡터 생성 */
    private float[] deterministicVector(String text, int dim) {
        Random rng = new Random(text.hashCode());
        float[] v = new float[dim];
        double norm = 0.0;
        for (int i = 0; i < dim; i++) {
            v[i] = rng.nextFloat() * 2 - 1;
            norm += v[i] * v[i];
        }
        norm = Math.sqrt(norm);
        for (int i = 0; i < dim; i++) v[i] /= (float) norm;
        return v;
    }
}
