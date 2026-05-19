package com.bank.loan.document.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * 서류 파일 저장소 추상화. 추후 S3/MinIO 어댑터로 교체 가능하도록 분리.
 */
public interface DocumentStorage {

    StoredFile store(Long applId, MultipartFile file);

    Resource load(String url);

    record StoredFile(
            String url,
            String hash,
            long sizeBytes,
            String mimeType,
            String originalName
    ) {}
}
