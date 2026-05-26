package com.bank.ai.rag.scheduler;

import com.bank.ai.rag.admin.RagDocumentAdminService;
import com.bank.ai.rag.document.domain.RagDocument;
import com.bank.ai.rag.document.repository.RagDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RAG 정기 인제스트 스케줄러.
 *
 * 활성 조건: {@code rag.scheduler.enabled=true}. 운영에서만 켜고, 로컬·테스트는 기본 비활성.
 *
 * 한 사이클 흐름:
 *   1. {@code RagDocumentAdminService#bootstrapFromCsv()} — {@code _meta/index.csv} 의 미등록 문서 등록
 *   2. {@code ingestedAt IS NULL} 문서 목록 조회
 *   3. 각 문서 {@code reindex} 호출 — 한 건 실패가 전체를 중단시키지 않도록 예외 격리
 *
 * 이미 인제스트된 문서의 변경 감지는 외부 트리거(관리자 API reindex)·후속 단계 책임.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rag.scheduler.enabled", havingValue = "true")
public class RagIngestionScheduler {

    private final RagDocumentAdminService adminService;
    private final RagDocumentRepository    documentRepository;

    /**
     * 기본 cron 매일 03:00 — yml 의 {@code rag.scheduler.cron} 으로 오버라이드.
     */
    @Scheduled(cron = "${rag.scheduler.cron:0 0 3 * * *}")
    public void run() {
        log.info("[rag-scheduler] 한 사이클 시작");
        int registered = bootstrap();
        int ingested   = ingestPending();
        log.info("[rag-scheduler] 완료: registered={} ingested={}", registered, ingested);
    }

    private int bootstrap() {
        try {
            return adminService.bootstrapFromCsv();
        } catch (Exception e) {
            log.warn("[rag-scheduler] bootstrap 실패: {}", e.getMessage());
            return 0;
        }
    }

    /** 인제스트 누락분 처리. 개별 문서 실패는 전체 진행을 중단시키지 않음. */
    private int ingestPending() {
        List<RagDocument> pending = documentRepository.findAllByIngestedAtIsNullAndDeletedAtIsNull();
        int success = 0;
        for (RagDocument doc : pending) {
            try {
                adminService.reindex(doc.getDocId());
                success++;
            } catch (Exception e) {
                log.warn("[rag-scheduler] reindex 실패 docId={}: {}", doc.getDocId(), e.getMessage());
            }
        }
        return success;
    }
}
