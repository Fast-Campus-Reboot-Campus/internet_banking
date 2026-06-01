package com.bank.loan.rag.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * SyntheticCaseSeedLoader 단위 테스트 — Phase E (E3-8).
 */
class SyntheticCaseSeedLoaderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final CaseSeedProperties PROPS =
            new CaseSeedProperties(true, 10000, 500, 9_000_000L);

    private static final Pattern RESIDENT_NO = Pattern.compile("\\d{6}-[1-4]\\d{6}");
    private static final Pattern PHONE_NO    = Pattern.compile("01[016789][- ]?\\d{3,4}[- ]?\\d{4}");
    private static final Pattern ACCOUNT_NO  = Pattern.compile("\\d{10,}");

    private SyntheticCaseSeedLoader loader() {
        return new SyntheticCaseSeedLoader(mock(JdbcTemplate.class), PROPS);
    }

    @ParameterizedTest(name = "idx={0}")
    @ValueSource(ints = {1, 100, 999, 5000, 9999, 10000})
    void payload_JSON_필드_포함(int idx) throws Exception {
        long aggregateId = PROPS.aggregateBase() + idx;
        String json = loader().buildPayload(aggregateId, idx);
        JsonNode node = MAPPER.readTree(json);

        assertThat(node.get("corpus").asText()).isEqualTo("similar_cases");
        assertThat(node.get("source_id").asText()).isEqualTo("rev-" + aggregateId);
        assertThat(node.get("chunk_seq").asInt()).isZero();
        assertThat(node.get("chunk_text").asText()).startsWith("[유사심사]");
        assertThat(node.get("metadata").get("rev_type").asText()).isNotBlank();
        assertThat(node.get("metadata").get("decision").asText()).isNotBlank();
    }

    @ParameterizedTest(name = "idx={0}")
    @ValueSource(ints = {1, 1000, 5000, 10000})
    void payload_PII_없음(int idx) throws Exception {
        String json = loader().buildPayload(PROPS.aggregateBase() + idx, idx);

        assertThat(RESIDENT_NO.matcher(json).find())
                .as("주민번호 패턴 검출: %s", json).isFalse();
        assertThat(PHONE_NO.matcher(json).find())
                .as("전화번호 패턴 검출: %s", json).isFalse();
        assertThat(ACCOUNT_NO.matcher(json).find())
                .as("계좌번호 패턴 검출: %s", json).isFalse();
    }

    @Test
    void 모든_idx_고용유형_다양() {
        SyntheticCaseSeedLoader l = loader();
        java.util.Set<String> employments = new java.util.HashSet<>();
        for (int i = 1; i <= 100; i++) {
            try {
                JsonNode node = MAPPER.readTree(l.buildPayload(PROPS.aggregateBase() + i, i));
                String text = node.get("chunk_text").asText();
                // 고용= 뒤의 값 추출
                String empl = text.replaceAll(".*고용=([^ ]+).*", "$1");
                employments.add(empl);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        // 5종 고용 유형 모두 출현
        assertThat(employments).hasSize(5);
    }

    @Test
    void incomeQuintile_경계값() {
        assertThat(SyntheticCaseSeedLoader.incomeQuintile(0L)).isEqualTo("Q1");
        assertThat(SyntheticCaseSeedLoader.incomeQuintile(29_999_999L)).isEqualTo("Q1");
        assertThat(SyntheticCaseSeedLoader.incomeQuintile(30_000_000L)).isEqualTo("Q2");
        assertThat(SyntheticCaseSeedLoader.incomeQuintile(79_999_999L)).isEqualTo("Q3");
        assertThat(SyntheticCaseSeedLoader.incomeQuintile(120_000_000L)).isEqualTo("Q5");
    }

    @Test
    void amountRange_경계값() {
        assertThat(SyntheticCaseSeedLoader.amountRange(1_000_000L)).isEqualTo("5천만미만");
        assertThat(SyntheticCaseSeedLoader.amountRange(50_000_000L)).isEqualTo("5천~1억");
        assertThat(SyntheticCaseSeedLoader.amountRange(200_000_000L)).isEqualTo("2억~3억");
        assertThat(SyntheticCaseSeedLoader.amountRange(500_000_000L)).isEqualTo("5억이상");
    }
}
