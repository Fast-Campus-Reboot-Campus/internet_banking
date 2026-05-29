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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThatCode;
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
        // run() 은 @Autowired llmExecutor 에 seed 작업을 위임한다(필드 주입).
        // 단위 테스트에서는 호출 스레드에서 즉시 실행하는 동기 Executor 를 주입해
        // 비동기 완료를 기다리지 않고 결과를 검증한다.
        ReflectionTestUtils.setField(loader, "executor", (Executor) Runnable::run);
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
    void seed_중_예외는_격리되어_run_으로_전파되지_않는다() {
        when(statementSpec.update()).thenThrow(new RuntimeException("DB 오류"));
        // run() 은 seed 작업을 비동기로 위임하고 즉시 반환한다(기동 readiness probe 보호).
        // seed 중 예외는 CompletableFuture.exceptionally 에서 로깅·격리되어 전파되지 않는다.
        assertThatCode(() -> loader.run(null)).doesNotThrowAnyException();
        // 그래도 seed 시도 자체는 이뤄졌어야 한다.
        verify(jdbcClient, atLeastOnce()).sql(anyString());
    }
}
