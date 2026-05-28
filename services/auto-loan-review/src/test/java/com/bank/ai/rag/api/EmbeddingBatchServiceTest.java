package com.bank.ai.rag.api;

import com.bank.ai.privacy.PiiLeakageException;
import com.bank.ai.privacy.PiiMaskingFilter;
import com.bank.ai.rag.api.dto.ChunkBatchItem;
import com.bank.ai.rag.embedding.StubEmbeddingClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * EmbeddingBatchService 단위 테스트 — D3-1.
 * pgvector SQL 은 실행 불가 환경이므로 JdbcClient mock + StubEmbeddingClient 사용.
 */
@ExtendWith(MockitoExtension.class)
class EmbeddingBatchServiceTest {

    @Mock
    private JdbcClient jdbcClient;
    @Mock
    private JdbcClient.StatementSpec statementSpec;
    @Mock
    private PiiMaskingFilter piiMaskingFilter;

    private EmbeddingBatchService service;

    @BeforeEach
    void setUp() {
        service = new EmbeddingBatchService(jdbcClient, new StubEmbeddingClient(), piiMaskingFilter);
        lenient().when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        lenient().when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        lenient().when(statementSpec.update()).thenReturn(1);
    }

    @Test
    void 정상_2건_upsert_완료() {
        var items = List.of(
                new ChunkBatchItem("similar_cases", "rev-001", 0, "승인 케이스 페르소나 요약", null, Map.of()),
                new ChunkBatchItem("similar_cases", "rev-002", 0, "반려 케이스 DSR 초과", "DSR 초과", Map.of("decision", "REJECT"))
        );

        int result = service.upsertAll(items);

        assertThat(result).isEqualTo(2);
        verify(jdbcClient, times(2)).sql(anyString());
        verify(statementSpec, times(2)).update();
    }

    @Test
    void PII_감지시_예외_throw_upsert_미실행() {
        doThrow(new PiiLeakageException("PII detected"))
                .when(piiMaskingFilter).assertNoSensitivePii(anyString(), anyString());

        var items = List.of(
                new ChunkBatchItem("similar_cases", "rev-003", 0, "홍길동 주민번호 노출 텍스트", null, Map.of())
        );

        assertThatThrownBy(() -> service.upsertAll(items))
                .isInstanceOf(PiiLeakageException.class);
        verify(jdbcClient, never()).sql(anyString());
    }

    @Test
    void 빈_목록은_0건_반환_DB_호출_없음() {
        int result = service.upsertAll(List.of());

        assertThat(result).isEqualTo(0);
        verifyNoInteractions(jdbcClient);
    }
}
