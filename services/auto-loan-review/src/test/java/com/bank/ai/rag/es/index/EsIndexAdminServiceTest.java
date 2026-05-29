package com.bank.ai.rag.es.index;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.bank.ai.rag.es.config.EsClientConfig;
import com.bank.ai.rag.es.config.EsProperties;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EsIndexAdminService testcontainers 통합 테스트 — E1-6.
 *
 * <p>ES 8.15.0 단일 노드 컨테이너 (xpack.security=false) 를 띄운 뒤
 * 인덱스 생성·alias 생성·alias swap 흐름을 검증.
 */
@Testcontainers
class EsIndexAdminServiceTest {

    @Container
    static ElasticsearchContainer esContainer =
            new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.15.0")
                    .withEnv("xpack.security.enabled", "false")
                    .withStartupTimeout(Duration.ofMinutes(3));

    private static ElasticsearchClient esClient;
    private static EsIndexAdminService adminService;

    @BeforeAll
    static void setUp() {
        String uri = "http://" + esContainer.getHttpHostAddress();
        RestClient restClient = RestClient.builder(HttpHost.create(uri)).build();
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        esClient = new ElasticsearchClient(transport);
        adminService = new EsIndexAdminService(esClient);
    }

    @AfterAll
    static void tearDown() throws IOException {
        if (esClient != null) {
            esClient._transport().close();
        }
    }

    @Test
    void 인덱스_없을때_indexExists_false() throws IOException {
        assertThat(adminService.indexExists("nonexistent_index")).isFalse();
    }

    @Test
    void createIndex_후_indexExists_true() throws IOException {
        String indexName = "test_policy_v1";
        adminService.createIndex(indexName, "es/mappings/kb_policy_v1.json");
        assertThat(adminService.indexExists(indexName)).isTrue();
    }

    @Test
    void ensureAliasExists_alias_생성_성공() throws IOException {
        String indexName = "test_faq_v1";
        String alias = "test_faq";
        adminService.createIndex(indexName, "es/mappings/kb_internal_faq_v1.json");
        adminService.ensureAliasExists(indexName, alias);

        boolean aliasExists = esClient.indices().existsAlias(r -> r.name(alias)).value();
        assertThat(aliasExists).isTrue();
    }

    @Test
    void ensureAliasExists_중복_호출해도_예외_없음() throws IOException {
        String indexName = "test_cases_v1";
        String alias = "test_cases";
        adminService.createIndex(indexName, "es/mappings/kb_similar_cases_v1.json");
        adminService.ensureAliasExists(indexName, alias);
        adminService.ensureAliasExists(indexName, alias); // 중복 — 예외 없어야 함
    }

    @Test
    void swapAlias_원자적_교체_성공() throws IOException {
        String alias = "swap_test_alias";
        String oldIndex = "swap_old_v1";
        String newIndex = "swap_new_v1";

        adminService.createIndex(oldIndex, "es/mappings/kb_policy_v1.json");
        adminService.createIndex(newIndex, "es/mappings/kb_policy_v1.json");
        adminService.ensureAliasExists(oldIndex, alias);

        adminService.swapAlias(alias, oldIndex, newIndex);

        // alias 가 newIndex 를 가리키는지 확인
        var aliasInfo = esClient.indices().getAlias(r -> r.name(alias));
        assertThat(aliasInfo.result()).containsKey(newIndex);
        assertThat(aliasInfo.result()).doesNotContainKey(oldIndex);
    }

    @Test
    void initializeAllIndexes_3_코퍼스_인덱스_생성() throws IOException {
        adminService.initializeAllIndexes();

        assertThat(adminService.indexExists("kb_policy_v1")).isTrue();
        assertThat(adminService.indexExists("kb_similar_cases_v1")).isTrue();
        assertThat(adminService.indexExists("kb_internal_faq_v1")).isTrue();

        boolean policyAlias = esClient.indices().existsAlias(r -> r.name("kb_policy")).value();
        boolean casesAlias  = esClient.indices().existsAlias(r -> r.name("kb_similar_cases")).value();
        boolean faqAlias    = esClient.indices().existsAlias(r -> r.name("kb_internal_faq")).value();

        assertThat(policyAlias).isTrue();
        assertThat(casesAlias).isTrue();
        assertThat(faqAlias).isTrue();
    }
}
