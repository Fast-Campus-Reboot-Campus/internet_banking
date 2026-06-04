package com.bank.loan.rag.outbox;

import com.bank.loan.application.repository.LoanApplicationRepository;
import com.bank.loan.review.repository.LoanReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * 유사 케이스 outbox 필러 — Phase E (E3-4).
 *
 * <p>결정 완료된 심사({@link LoanReviewRepository#findExportable})를 주기적으로 조회해
 * {@link SimilarCaseOutboxPublisher} 로 outbox 에 적재(멱등). 핵심 심사 플로우를 건드리지 않고
 * near-real-time 으로 케이스를 수집한다.
 *
 * <p>{@code ai.case-outbox.enabled=true} 시에만 활성. 기존 HTTP 푸시 exporter 와 택일.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai.case-outbox", name = "enabled", havingValue = "true")
public class SimilarCaseOutboxFiller {

    private final LoanReviewRepository reviewRepository;
    private final LoanApplicationRepository applicationRepository;
    private final SimilarCaseOutboxPublisher publisher;
    private final CaseOutboxProperties props;

    @Scheduled(fixedDelayString = "${ai.case-outbox.fill-interval-ms:60000}")
    public void fill() {
        OffsetDateTime since = OffsetDateTime.now(ZoneOffset.UTC)
                .toLocalDate()
                .minusDays(props.lookbackDays())
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC);

        var reviews = reviewRepository.findExportable(since);
        if (reviews.isEmpty()) return;

        int enqueued = 0;
        for (var review : reviews) {
            var application = applicationRepository.findById(review.getApplId()).orElse(null);
            if (application == null) {
                log.warn("[case-outbox] appl_id={} 미존재 — revId={} 스킵",
                        review.getApplId(), review.getRevId());
                continue;
            }
            publisher.enqueueInCurrentTx(review, application);
            enqueued++;
        }
        log.info("[case-outbox] 필러 완료 — 조회 {} / 적재 시도 {} (since={})",
                reviews.size(), enqueued, since);
    }
}
