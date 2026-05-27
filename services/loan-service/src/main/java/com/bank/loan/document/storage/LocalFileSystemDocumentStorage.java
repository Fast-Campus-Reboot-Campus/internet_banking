package com.bank.loan.document.storage;

import com.bank.common.web.BusinessException;
import com.bank.loan.support.LoanErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * 로컬 파일시스템 기반 서류 저장소. MVP 단계 기본 구현.
 *
 * 저장 경로: {baseDir}/{applId}/{uuid}_{원본파일명}
 * 반환 URL: 절대 경로의 file:// URI
 */
@Component
public class LocalFileSystemDocumentStorage implements DocumentStorage {

    private final Path baseDir;

    public LocalFileSystemDocumentStorage(
            @Value("${loan.document.storage-dir:./storage/loan-documents}") String baseDir) {
        this.baseDir = Paths.get(baseDir).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(Long applId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(LoanErrorCode.LOAN_040, "업로드 파일이 비어있습니다.");
        }
        try {
            Path applDir = baseDir.resolve(String.valueOf(applId));
            Files.createDirectories(applDir);

            String original = sanitize(file.getOriginalFilename());
            String stored = UUID.randomUUID().toString().replace("-", "") + "_" + original;
            Path target = applDir.resolve(stored);

            String hash;
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target);
                hash = sha256Hex(target);
            }

            return new StoredFile(
                    target.toUri().toString(),
                    hash,
                    file.getSize(),
                    file.getContentType(),
                    original
            );
        } catch (IOException e) {
            throw new BusinessException(LoanErrorCode.LOAN_040, e.getMessage());
        }
    }

    @Override
    public Resource load(String url) {
        if (url == null || url.isBlank()) {
            throw new BusinessException(LoanErrorCode.LOAN_041, "doc_url 이 비어있습니다.");
        }
        try {
            Path target = Paths.get(URI.create(url)).normalize();
            if (!Files.exists(target) || !Files.isReadable(target)) {
                throw new BusinessException(LoanErrorCode.LOAN_041, "파일을 찾을 수 없습니다: " + url);
            }
            return new FileSystemResource(target);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(LoanErrorCode.LOAN_041, "잘못된 doc_url: " + url);
        }
    }

    private String sanitize(String name) {
        if (name == null || name.isBlank()) return "unnamed";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String sha256Hex(Path file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }
}
