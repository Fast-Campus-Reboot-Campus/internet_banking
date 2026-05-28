package com.bank.ai.rag.seed;

import com.bank.ai.llm.policy.InlinePolicyIndex;
import com.bank.ai.llm.policy.PolicyIndex;
import com.bank.ai.rag.embedding.StubEmbeddingClient;
import com.bank.ai.rule.config.RuleEngineProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyCorpusSeedLoaderTest {

    @Mock
    private JdbcClient jdbcClient;
    @Mock
    private JdbcClient.StatementSpec statementSpec;
    @Mock
    private JdbcClient.StatementSpec paramSpec;

    private PolicyCorpusSeedLoader loader;

    private static final InlinePolicyIndex POLICY_INDEX = new InlinePolicyIndex(Map.of(
            "MORT_DSR_LIMIT_V1", new PolicyIndex.PolicyEntry("DSR 40% 이하", "internal_policy"),
            "CRED_SCORE_MIN_V1", new PolicyIndex.PolicyEntry("최저 신용점수 600", "internal_policy")
    ));

    private static final RuleEngineProperties RULE_ENGINE_PROPS = new RuleEngineProperties(
            new RuleEngineProperties.HardConstraints(0.40, 0.70, 600, 0, 19),
            0.30, 0.50,
            Map.of("MORT_001", Map.of("regular", 0.347, "young", 0.750, "senior", 0.788)),
            0.95, 0.20, true, false
    );

    @BeforeEach
    void setUp() {
        loader = new PolicyCorpusSeedLoader(
                jdbcClient, new StubEmbeddingClient(), POLICY_INDEX, RULE_ENGINE_PROPS);
        // JdbcClient 플루언트 체인 설정
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        lenient().when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        lenient().when(statementSpec.update()).thenReturn(1);
    }

    @Test
    void run_호출_시_inline_2개_matrix_3개_총_5회_upsert() throws Exception {
        loader.run(null);
        // POLICY_INDEX 2개 + MORT_001 matrix 3개(regular, young, senior) = 5회
        verify(jdbcClient, times(5)).sql(anyString());
    }

    @Test
    void 예외_발생_시_런타임_예외_전파() throws Exception {
        when(statementSpec.update()).thenThrow(new RuntimeException("DB 오류"));
        // run이 예외 전파하는지 확인 (seed 실패는 치명적)
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> loader.run(null))
                .isInstanceOf(RuntimeException.class);
    }
}
