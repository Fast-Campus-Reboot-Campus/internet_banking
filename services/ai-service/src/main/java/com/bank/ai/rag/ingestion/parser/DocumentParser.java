package com.bank.ai.rag.ingestion.parser;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 파일 → 평문 추출 인터페이스.
 * PDF·DOCX·HWP 구현체는 Tika 기반, 추가 포맷은 구현체 추가만으로 지원.
 */
public interface DocumentParser {

    /**
     * @param filePath 원본 파일 경로
     * @return 추출된 평문 (빈 파일이면 빈 문자열)
     * @throws IOException 파일 읽기 또는 파싱 실패
     */
    String parse(Path filePath) throws IOException;
}
