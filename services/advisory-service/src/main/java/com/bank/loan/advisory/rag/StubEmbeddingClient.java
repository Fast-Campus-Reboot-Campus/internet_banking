package com.bank.loan.advisory.rag;

import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * 결정론적 단위벡터를 생성하는 스텁 임베딩 클라이언트.
 *
 * 운영 원칙:
 *   - 실제 모델 호출 없음 — 외부 서비스 의존 없이 전체 RAG 파이프라인을 구동 가능.
 *   - 동일 입력 → 동일 벡터 (text.hashCode + length 기반 Seed).
 *   - L2 정규화(norm = 1.0) 후 반환 → cosine similarity 계산에 바로 사용 가능.
 *   - 테스트에서 동일 텍스트로 문서/사례를 적재하면 코사인 유사도 = 1.0 보장.
 *
 * 운영 전환: BGE-M3 / SBERT-Ko 자체 호스팅 어댑터 빈을 @Primary 로 등록하면
 * 스프링이 이 빈 대신 새 구현체를 우선 주입한다.
 */
@Component
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
