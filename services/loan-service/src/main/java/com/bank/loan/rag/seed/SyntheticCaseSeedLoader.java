package com.bank.loan.rag.seed;

import com.bank.loan.rag.outbox.LoanReviewOutbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 합성 케이스 outbox Seed 로더 — Phase E (E3-8).
 *
 * <p>HMDA / Home Credit 스타일의 합성 케이스 데이터를 {@code loan_review_outbox} 에
 * 직접 적재. 결정론적 생성(고정 시드)으로 재현성 보장.
 *
 * <p>적재 흐름:
 * <ol>
 *   <li>기동 시 {@link ApplicationRunner} 실행</li>
 *   <li>이미 적재된 건 → {@code ON CONFLICT (idempotency_key) DO NOTHING} 으로 멱등 skip</li>
 *   <li>outbox dispatcher 가 Kafka 발행 → enricher 임베딩 → ES 색인</li>
 * </ol>
 *
 * <p>aggregate_id 범위: {@code [aggregateBase + 1, aggregateBase + count]} —
 * 실제 loan_review.rev_id 와 충돌하지 않도록 고정 오프셋 사용 (기본 9,000,001 ~).
 *
 * <p>{@code ai.case-seed.enabled=true} 시에만 활성.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai.case-seed", name = "enabled", havingValue = "true")
public class SyntheticCaseSeedLoader implements ApplicationRunner {

    // ── 합성 데이터 팔레트 ────────────────────────────────────────────────────
    private static final String[] EMPLOYMENTS  = {"EMPLOYEE", "SELF_EMPLOYED", "FREELANCE", "RETIRED", "UNEMPLOYED"};
    private static final long[]   INCOMES      = {
            20_000_000L, 35_000_000L, 45_000_000L,       // Q1, Q2
            65_000_000L, 75_000_000L,                     // Q3
            95_000_000L, 110_000_000L,                    // Q4
            150_000_000L, 250_000_000L                    // Q5
    };
    private static final long[]   AMOUNTS      = {
            30_000_000L, 45_000_000L,                     // 5천만 미만
            70_000_000L, 90_000_000L,                     // 5천~1억
            130_000_000L, 170_000_000L,                   // 1억~2억
            230_000_000L, 280_000_000L,                   // 2억~3억
            380_000_000L, 450_000_000L,                   // 3억~5억
            600_000_000L                                   // 5억이상
    };
    private static final int[]    PERIODS      = {12, 24, 36, 60, 120, 180, 240, 360};
    private static final String[] PURPOSES     = {"HOUSING", "INVESTMENT", "LIVING", "REFINANCE", "ETC"};
    private static final String[] DECISIONS    = {"APPROVE", "APPROVE", "APPROVE", "REJECT", "REJECT", "DEFER"};
    private static final String[] REV_TYPES    = {"AUTO", "AUTO", "MANUAL"};

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JdbcTemplate jdbcTemplate;
    private final CaseSeedProperties props;

    @Override
    @Transactional
    public void run(org.springframework.boot.ApplicationArguments args) {
        log.info("[case-seed] 합성 케이스 seed 시작 — count={} base={}", props.count(), props.aggregateBase());

        int total = props.count();
        int batch = props.batchSize();
        long base = props.aggregateBase();

        int inserted = 0;
        List<Object[]> rows = new ArrayList<>(batch);

        for (int i = 1; i <= total; i++) {
            long aggregateId = base + i;
            String payload   = buildPayload(aggregateId, i);
            String ikey      = LoanReviewOutbox.idempotencyKeyOf(
                    LoanReviewOutbox.EVENT_TYPE_CASE_INDEXED, aggregateId);

            rows.add(new Object[]{aggregateId, LoanReviewOutbox.EVENT_TYPE_CASE_INDEXED, payload, ikey});

            if (rows.size() >= batch || i == total) {
                inserted += flushBatch(rows);
                rows.clear();
            }
        }
        log.info("[case-seed] seed 완료 — 요청 {} / 신규 적재 {} (나머지는 이미 존재)", total, inserted);
    }

    /** 단일 케이스 payload JSON 구성 (SimilarCaseChunkTemplate 포맷 준수). */
    String buildPayload(long aggregateId, int idx) {
        String empl     = EMPLOYMENTS[idx % EMPLOYMENTS.length];
        long income     = INCOMES    [idx % INCOMES.length];
        long amount     = AMOUNTS    [idx % AMOUNTS.length];
        int  period     = PERIODS    [idx % PERIODS.length];
        String purpose  = PURPOSES   [idx % PURPOSES.length];
        String decision = DECISIONS  [idx % DECISIONS.length];
        String revType  = REV_TYPES  [idx % REV_TYPES.length];

        String sourceId  = "rev-" + aggregateId;
        String chunkText = "[유사심사] 고용=%s 소득분위=%s 신청=%s/%d개월/%s 결정=%s 유형=%s"
                .formatted(empl, incomeQuintile(income), amountRange(amount),
                        period, purpose, decision, revType);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("rev_type", revType);
        metadata.put("decision", decision);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("corpus",    "similar_cases");
        payload.put("source_id", sourceId);
        payload.put("chunk_seq", 0);
        payload.put("chunk_text", chunkText);
        payload.put("metadata",  metadata);

        try {
            return MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("seed payload 직렬화 실패 aggregateId=" + aggregateId, e);
        }
    }

    private int flushBatch(List<Object[]> rows) {
        String sql = """
                INSERT INTO loan_review_outbox
                    (aggregate_id, event_type_cd, payload, status, attempt_no, max_attempt,
                     next_attempt_at, idempotency_key, created_at, updated_at)
                VALUES (?, ?, ?::jsonb, 'PENDING', 0, 5, now(), ?, now(), now())
                ON CONFLICT (idempotency_key) DO NOTHING
                """;
        int[] counts = jdbcTemplate.batchUpdate(sql, rows);
        int sum = 0;
        for (int c : counts) if (c > 0) sum++;
        return sum;
    }

    // ── 청크 텍스트 생성 유틸 (SimilarCaseChunkTemplate 복제 — 의존성 분리) ──

    static String incomeQuintile(long amt) {
        if (amt < 30_000_000L)  return "Q1";
        if (amt < 50_000_000L)  return "Q2";
        if (amt < 80_000_000L)  return "Q3";
        if (amt < 120_000_000L) return "Q4";
        return "Q5";
    }

    static String amountRange(long amt) {
        if (amt < 50_000_000L)  return "5천만미만";
        if (amt < 100_000_000L) return "5천~1억";
        if (amt < 200_000_000L) return "1억~2억";
        if (amt < 300_000_000L) return "2억~3억";
        if (amt < 500_000_000L) return "3억~5억";
        return "5억이상";
    }
}
