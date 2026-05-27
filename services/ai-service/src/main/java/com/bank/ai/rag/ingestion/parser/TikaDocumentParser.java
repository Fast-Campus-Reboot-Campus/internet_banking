package com.bank.ai.rag.ingestion.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Apache Tika 기반 문서 파서.
 * PDF·DOCX·HWP 등 주요 포맷 지원. 미지원 포맷은 예외 메시지를 로그 후 빈 문자열 반환.
 */
@Slf4j
@Component
public class TikaDocumentParser implements DocumentParser {

    private static final int MAX_LENGTH = 10_000_000; // 10MB 텍스트 상한
    private final Tika tika;

    public TikaDocumentParser() {
        // parseToString(InputStream, Metadata, maxLength) 의 metadata 가 null 이면 Tika 내부에서 NPE.
        // setMaxStringLength + 1-arg 시그니처로 우회.
        this.tika = new Tika();
        this.tika.setMaxStringLength(MAX_LENGTH);
    }

    @Override
    public String parse(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new IOException("파일이 존재하지 않음: " + filePath);
        }
        try (InputStream in = Files.newInputStream(filePath)) {
            String text = tika.parseToString(in);
            log.debug("[parser] 추출 완료: file={} chars={}", filePath.getFileName(), text.length());
            return text == null ? "" : text.strip();
        } catch (TikaException e) {
            log.warn("[parser] 파싱 실패 (미지원 포맷 가능성): file={} msg={}", filePath.getFileName(), e.getMessage());
            return "";
        }
    }
}
