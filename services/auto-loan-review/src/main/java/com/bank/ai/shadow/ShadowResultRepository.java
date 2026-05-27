package com.bank.ai.shadow;

import com.bank.ai.agent.AgentOpinion;
import com.bank.ai.rule.domain.Track;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * shadow_run_result 테이블 접근 — NamedParameterJdbcTemplate 기반.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ShadowResultRepository {

    private static final String INSERT_SQL = """
            INSERT INTO shadow_run_result
                (rev_id, prod_opinion_json, shadow_opinion_json, diverged, diverge_reasons,
                 prod_track, shadow_track, prod_decision_score, shadow_decision_score,
                 shadow_model, shadow_prompt_version, rag_enabled)
            VALUES
                (:revId, CAST(:prodOpinionJson AS VARCHAR), CAST(:shadowOpinionJson AS VARCHAR),
                 :diverged, :divergeReasons,
                 :prodTrack, :shadowTrack, :prodDecisionScore, :shadowDecisionScore,
                 :shadowModel, :shadowPromptVersion, :ragEnabled)
            """;

    private static final String COUNT_DIVERGED_SQL = """
            SELECT COUNT(*) FROM shadow_run_result
            WHERE diverged = TRUE
              AND created_at >= CAST(:from AS TIMESTAMP WITH TIME ZONE)
              AND created_at <  CAST(:to   AS TIMESTAMP WITH TIME ZONE)
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    /** shadow 비교 결과 INSERT. */
    public void insert(ShadowComparisonResult result) {
        try {
            String prodJson   = serialize(result.prodOpinion());
            String shadowJson = serialize(result.shadowOpinion());
            String reasonsJson = objectMapper.writeValueAsString(result.divergeReasons());

            var params = new MapSqlParameterSource()
                    .addValue("revId",              result.revId())
                    .addValue("prodOpinionJson",     prodJson)
                    .addValue("shadowOpinionJson",   shadowJson)
                    .addValue("diverged",            result.diverged())
                    .addValue("divergeReasons",      reasonsJson)
                    .addValue("prodTrack",           result.track().name())
                    .addValue("shadowTrack",         result.track().name())
                    .addValue("prodDecisionScore",   decisionScore(result.prodOpinion()))
                    .addValue("shadowDecisionScore", decisionScore(result.shadowOpinion()))
                    .addValue("shadowModel",         result.shadowModel())
                    .addValue("shadowPromptVersion", result.shadowPromptVersion())
                    .addValue("ragEnabled",          result.ragEnabled());

            jdbc.update(INSERT_SQL, params);
        } catch (JsonProcessingException e) {
            log.error("[Shadow] INSERT 직렬화 실패 revId={}", result.revId(), e);
            throw new RuntimeException("shadow_run_result 직렬화 오류", e);
        }
    }

    /** 기간 내 diverged=true 건수 (어드민/리포트용). */
    public int countDiverged(LocalDate from, LocalDate to) {
        Integer count = jdbc.queryForObject(
                COUNT_DIVERGED_SQL,
                Map.of("from", from.atStartOfDay().toString(),
                       "to",   to.plusDays(1).atStartOfDay().toString()),
                Integer.class);
        return count != null ? count : 0;
    }

    // ─────────────────────────────────────────────────────────────────────

    private String serialize(AgentOpinion opinion) throws JsonProcessingException {
        return objectMapper.writeValueAsString(opinion);
    }

    private static Double decisionScore(AgentOpinion opinion) {
        return opinion.decisionScore();
    }
}
