package com.bank.loan.rag;

import com.bank.loan.application.repository.LoanApplicationRepository;
import com.bank.loan.review.repository.LoanReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 유사 케이스 코퍼스 증분 내보내기 서비스 — D3-2.
 *
 * <p>흐름:
 * <ol>
 *   <li>전일 결정 완료 LOAN_REVIEW 증분 조회 ({@code updated_at >= since})</li>
 *   <li>각 케이스의 LOAN_APPLICATION 로드</li>
 *   <li>{@link SimilarCaseChunkTemplate} 로 PII-free 청크 텍스트 생성</li>
 *   <li>{@link AutoReviewAiClient} 로 배치 전송</li>
 * </ol>
 *
 * <p>{@code ai.similar-case-export.enabled=true} 시에만 빈 활성.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "ai.similar-case-export", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class SimilarCaseExporter {

    private final LoanReviewRepository reviewRepository;
    private final LoanApplicationRepository applicationRepository;
    private final AutoReviewAiClient aiClient;
    private final SimilarCaseExportProperties props;

    /**
     * 지정 시각 이후 완료된 케이스를 임베딩 코퍼스로 내보낸다.
     *
     * @param since 조회 기준 시각 (포함)
     * @return 전송된 건수
     */
    @Transactional(readOnly = true)
    public int export(OffsetDateTime since) {
        var reviews = reviewRepository.findExportable(since);
        if (reviews.isEmpty()) {
            log.info("SimilarCaseExporter: 내보낼 케이스 없음 since={}", since);
            return 0;
        }
        log.info("SimilarCaseExporter: 내보낼 케이스 {} 건 since={}", reviews.size(), since);

        List<AutoReviewAiClient.ChunkItemDto> batch = new ArrayList<>();
        for (var review : reviews) {
            applicationRepository.findById(review.getApplId()).ifPresentOrElse(
                    application -> {
                        String chunkText = SimilarCaseChunkTemplate.build(review, application);
                        batch.add(new AutoReviewAiClient.ChunkItemDto(
                                SimilarCaseChunkTemplate.corpus(),
                                SimilarCaseChunkTemplate.sourceId(review.getRevId()),
                                0,
                                chunkText,
                                null,
                                Map.of("rev_type", review.getRevTypeCd(),
                                       "decision", String.valueOf(review.getRevDecisionCd()))
                        ));
                    },
                    () -> log.warn("SimilarCaseExporter: appl_id={} 미존재 — revId={} 스킵",
                            review.getApplId(), review.getRevId())
            );

            if (batch.size() >= props.batchSize()) {
                flush(batch);
            }
        }
        if (!batch.isEmpty()) flush(batch);

        log.info("SimilarCaseExporter: 내보내기 완료 총 {} 건", reviews.size());
        return reviews.size();
    }

    /** 전일 00:00:00 UTC 기준 내보내기. */
    public int exportYesterday() {
        OffsetDateTime since = OffsetDateTime.now(ZoneOffset.UTC)
                .toLocalDate()
                .minusDays(props.lookbackDays())
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC);
        return export(since);
    }

    // ─────────────────────────────────────────────────────────────────────

    private void flush(List<AutoReviewAiClient.ChunkItemDto> batch) {
        int sent = aiClient.sendBatch(new ArrayList<>(batch));
        log.debug("SimilarCaseExporter: flush {} 건 전송", sent);
        batch.clear();
    }
}
