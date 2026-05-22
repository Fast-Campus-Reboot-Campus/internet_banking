package com.bank.ai.rag.admin;

import com.bank.ai.rag.admin.dto.RagDocumentCreateRequest;
import com.bank.ai.rag.admin.dto.RagDocumentResponse;
import com.bank.ai.rag.admin.dto.RagIngestionLogResponse;
import com.bank.ai.rag.chunk.repository.RagChunkRepository;
import com.bank.ai.rag.config.RagProperties;
import com.bank.ai.rag.document.domain.RagDocument;
import com.bank.ai.rag.document.repository.RagDocumentRepository;
import com.bank.ai.rag.ingestion.IngestionService;
import com.bank.ai.rag.ingestion.repository.RagIngestionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RAG 문서 관리 서비스 (관리자 전용).
 * CSV 부트스트랩, 단건 등록, 재인제스트, soft-delete 를 담당.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagDocumentAdminService {

    /** 최초 등록 시 체크섬 미확정 플레이스홀더 (SHA-256 과 충돌 없음) */
    private static final String PENDING_CHECKSUM = "0".repeat(64);
    private static final String CORPUS_SCHEME    = "corpus://";

    private final RagDocumentRepository    documentRepository;
    private final RagIngestionLogRepository logRepository;
    private final RagChunkRepository       chunkRepository;
    private final IngestionService         ingestionService;
    private final RagProperties            ragProperties;

    // ── 단건 등록 ──────────────────────────────────────────────────

    @Transactional
    public RagDocumentResponse register(RagDocumentCreateRequest req) {
        documentRepository.findBySourceUriAndDeletedAtIsNull(req.sourceUri())
                .ifPresent(d -> { throw new IllegalStateException("이미 등록된 URI: " + req.sourceUri()); });

        RagDocument doc = RagDocument.builder()
                .docTypeCd(req.docTypeCd())
                .title(req.title())
                .sourceUri(req.sourceUri())
                .jurisdiction(req.jurisdiction())
                .sensitivityCd(req.sensitivityCd())
                .docVersion(req.docVersion())
                .effectiveFrom(req.effectiveFrom())
                .effectiveTo(req.effectiveTo())
                .checksum(PENDING_CHECKSUM)
                .build();
        return RagDocumentResponse.from(documentRepository.save(doc));
    }

    // ── 목록 / 단건 조회 ────────────────────────────────────────────

    public List<RagDocumentResponse> findAll(String docTypeCd, String sensitivityCd, String asOfDate) {
        return documentRepository.search(docTypeCd, sensitivityCd, asOfDate)
                .stream().map(RagDocumentResponse::from).toList();
    }

    public RagDocumentResponse findById(Long docId) {
        return RagDocumentResponse.from(requireDoc(docId));
    }

    // ── 재인제스트 ──────────────────────────────────────────────────

    /**
     * 문서 sourceUri 를 기준으로 파일을 찾아 전체 파이프라인을 재실행.
     * 체크섬 동일 시 SKIP(0 반환).
     */
    public int reindex(Long docId) {
        RagDocument doc = requireDoc(docId);
        Path filePath   = resolveFilePath(doc.getSourceUri());
        return ingestionService.ingest(docId, filePath);
    }

    // ── soft-delete ─────────────────────────────────────────────────

    @Transactional
    public void softDelete(Long docId) {
        requireDoc(docId).softDelete(0L);   // system actor
    }

    // ── 인제스트 로그 조회 ───────────────────────────────────────────

    public List<RagIngestionLogResponse> findLogs(Long docId) {
        requireDoc(docId);
        return logRepository.findAllByDocIdOrderByStartedAtDesc(docId)
                .stream().map(RagIngestionLogResponse::from).toList();
    }

    // ── CSV 부트스트랩 ──────────────────────────────────────────────

    /**
     * {@code {baseDir}/_meta/index.csv} 를 읽어 미등록 문서를 일괄 등록.
     * 이미 등록된 sourceUri 는 건너뜀. 실제 인제스트는 별도 reindex 호출로 진행.
     *
     * @return 신규 등록된 문서 수
     */
    @Transactional
    public int bootstrapFromCsv() throws IOException {
        Path csvPath = Paths.get(ragProperties.corpus().baseDir(), "_meta", "index.csv");
        if (!Files.exists(csvPath)) {
            log.warn("[bootstrap] CSV 파일 없음: {}", csvPath.toAbsolutePath());
            return 0;
        }

        List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
        int registered = 0;
        for (int i = 1; i < lines.size(); i++) {   // 헤더(0행) 제외
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            try {
                if (registerFromCsvRow(line)) registered++;
            } catch (Exception e) {
                log.warn("[bootstrap] 행 처리 실패 (line {}): {}", i + 1, e.getMessage());
            }
        }
        log.info("[bootstrap] 완료: registered={} / total={}", registered, lines.size() - 1);
        return registered;
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────────────

    /**
     * CSV 행 하나를 파싱해 신규 문서를 등록.
     * CSV 컬럼: relativePath,sensitivityCd,jurisdiction,effectiveTo,prodId,notes
     */
    private boolean registerFromCsvRow(String line) {
        String[] cols = line.split(",", 6);
        if (cols.length < 3) return false;

        String relativePath  = cols[0].trim();
        String sensitivityCd = cols[1].trim();
        String jurisdiction  = cols[2].trim();
        String effectiveTo   = cols.length > 3 && !cols[3].trim().isEmpty() ? cols[3].trim() : null;
        String notes         = cols.length > 5 ? cols[5].trim() : "";

        String sourceUri = CORPUS_SCHEME + relativePath;
        if (documentRepository.findBySourceUriAndDeletedAtIsNull(sourceUri).isPresent()) {
            return false;   // 이미 등록됨
        }

        String filename      = Paths.get(relativePath).getFileName().toString();
        String title         = notes.isEmpty() ? deriveTitle(filename) : notes;
        String docTypeCd     = deriveDocTypeCd(relativePath);
        String effectiveFrom = extractEffectiveFrom(filename);

        RagDocument doc = RagDocument.builder()
                .docTypeCd(docTypeCd)
                .title(title)
                .sourceUri(sourceUri)
                .jurisdiction(jurisdiction.isEmpty() ? "KR" : jurisdiction)
                .sensitivityCd(sensitivityCd.isEmpty() ? RagDocument.SENSITIVITY_PUBLIC : sensitivityCd)
                .effectiveFrom(effectiveFrom)
                .effectiveTo(effectiveTo)
                .checksum(PENDING_CHECKSUM)
                .build();
        documentRepository.save(doc);
        return true;
    }

    private RagDocument requireDoc(Long docId) {
        return documentRepository.findByDocIdAndDeletedAtIsNull(docId)
                .orElseThrow(() -> new NoSuchElementException("doc_id 없음: " + docId));
    }

    private Path resolveFilePath(String sourceUri) {
        String relative = sourceUri.startsWith(CORPUS_SCHEME)
                ? sourceUri.substring(CORPUS_SCHEME.length()) : sourceUri;
        return Paths.get(ragProperties.corpus().baseDir()).resolve(relative);
    }

    /**
     * relativePath 접두사로 docTypeCd 자동 분류.
     * law/ → LAW, regulation/fsc/ → SUPERVISION_GUIDE, regulation/self/ → INTERNAL_RULE,
     * policy/ → POLICY, product/ → PRODUCT_TERMS, fairness/ → FAIR_LENDING
     */
    private String deriveDocTypeCd(String relativePath) {
        if (relativePath.startsWith("law/"))             return RagDocument.DOC_TYPE_LAW;
        if (relativePath.startsWith("regulation/fsc/"))  return RagDocument.DOC_TYPE_SUPERVISION_GUIDE;
        if (relativePath.startsWith("regulation/self/")) return RagDocument.DOC_TYPE_INTERNAL_RULE;
        if (relativePath.startsWith("policy/"))          return RagDocument.DOC_TYPE_POLICY;
        if (relativePath.startsWith("product/"))         return RagDocument.DOC_TYPE_PRODUCT_TERMS;
        if (relativePath.startsWith("fairness/"))        return RagDocument.DOC_TYPE_FAIR_LENDING;
        return RagDocument.DOC_TYPE_POLICY;
    }

    /**
     * 파일명에서 시행일(YYYYMMDD) 추출.
     * <ol>
     *   <li>{@code (YYYYMMDD)} 8자리 괄호</li>
     *   <li>{@code (YYMMDD)} 6자리 괄호 → {@code 20YYMMDD}</li>
     *   <li>{@code _YYMMDD.} 언더스코어+6자리+점 → {@code 20YYMMDD}</li>
     *   <li>선두 {@code YYMMDD} 6자리 → {@code 20YYMMDD}</li>
     * </ol>
     */
    private String extractEffectiveFrom(String filename) {
        Matcher m8 = Pattern.compile("\\((\\d{8})\\)").matcher(filename);
        if (m8.find()) return m8.group(1);

        Matcher m6p = Pattern.compile("\\((\\d{6})\\)").matcher(filename);
        if (m6p.find()) return "20" + m6p.group(1);

        Matcher m6u = Pattern.compile("_(\\d{6})\\.").matcher(filename);
        if (m6u.find()) return "20" + m6u.group(1);

        Matcher m6s = Pattern.compile("^(\\d{6})[^\\d]").matcher(filename);
        if (m6s.find()) return "20" + m6s.group(1);

        return null;
    }

    /** 파일명(확장자 제거) → 제목 간이 변환 */
    private String deriveTitle(String filename) {
        int dot = filename.lastIndexOf('.');
        String name = dot > 0 ? filename.substring(0, dot) : filename;
        return name.replace('_', ' ').trim();
    }
}
