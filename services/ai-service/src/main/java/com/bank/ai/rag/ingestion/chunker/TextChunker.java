package com.bank.ai.rag.ingestion.chunker;

import com.bank.ai.rag.config.RagProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 토큰(단어) 기반 슬라이딩 윈도우 청크 분할기.
 * 실제 토크나이저 없이 공백 분리 단어 수를 토큰 근사값으로 사용.
 * 단락 경계(\n\n) 를 우선 분리 기준으로 삼아 의미 단위를 최대한 보존.
 */
@Component
@RequiredArgsConstructor
public class TextChunker {

    private final RagProperties ragProperties;

    /**
     * @param text 평문 전체
     * @return 청크 목록 (빈 텍스트이면 빈 리스트)
     */
    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) return List.of();

        int size    = ragProperties.chunk().size();
        int overlap = ragProperties.chunk().overlap();

        String[] words = text.split("\\s+");
        if (words.length == 0) return List.of();

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < words.length) {
            int end = Math.min(start + size, words.length);
            chunks.add(String.join(" ", java.util.Arrays.copyOfRange(words, start, end)));
            if (end >= words.length) break;
            start = end - overlap;
        }
        return chunks;
    }
}
