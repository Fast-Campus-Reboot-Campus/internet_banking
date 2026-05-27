package com.bank.loan.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 유사 케이스 코퍼스 일배치 — D3-2.
 *
 * <p>매일 새벽 2시(KST = 전일 17:00 UTC) 전일 결정 완료 케이스를 임베딩 코퍼스로 내보낸다.
 * {@code ai.similar-case-export.enabled=true} 시에만 활성.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ai.similar-case-export", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class SimilarCaseExportJob {

    private final SimilarCaseExporter exporter;

    /** 매일 02:00 KST (= 17:00 UTC 전일). cron: 초 분 시 * * * */
    @Scheduled(cron = "${ai.similar-case-export.cron:0 0 17 * * *}")
    public void run() {
        log.info("SimilarCaseExportJob: 유사 케이스 일배치 시작");
        try {
            int count = exporter.exportYesterday();
            log.info("SimilarCaseExportJob: 완료 — {} 건", count);
        } catch (Exception e) {
            log.error("SimilarCaseExportJob: 배치 실패", e);
        }
    }
}
