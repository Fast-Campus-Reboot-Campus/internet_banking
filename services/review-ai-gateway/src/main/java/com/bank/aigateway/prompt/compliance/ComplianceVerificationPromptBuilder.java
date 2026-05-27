package com.bank.aigateway.prompt.compliance;

import com.bank.aigateway.audit.dto.AuditAnalysisRequest;
import com.bank.aigateway.prompt.PromptRenderer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ComplianceVerificationPromptBuilder {

    private static final String SYSTEM = """
            당신은 금융기관의 여신 규정 준수 여부를 검증하는 감사 전문가입니다.
            DSR/LTV 한도 초과, 예외 처리 남용, 내부 규정 우회 패턴을 탐지합니다.
            분석 결과는 반드시 아래 JSON 형식으로만 응답하십시오. 다른 텍스트를 포함하지 마십시오.
            {
              "conclusion": "VIOLATION_SUSPECTED | COMPLIANT | INSUFFICIENT_DATA",
              "reasoningSummary": "한국어로 된 200자 이내 감사 의견",
              "confidenceScore": 0.0 ~ 1.0
            }
            """;

    private static final String USER_TEMPLATE = """
            [심사 건 정보]
            심사 ID: {{revId}}
            심사관 ID: {{reviewerId}}

            [탐지된 규정 위반 신호]
            {{signals}}

            [심사관 의견서]
            {{reviewOpinion}}

            [여신 규정집 관련 조항]
            {{ragChunks}}

            위 규정 조항과 심사 내용을 비교하여 규정 우회 또는 위반 여부를 판단하십시오.
            """;

    public String buildSystem() {
        return SYSTEM;
    }

    public String buildUser(AuditAnalysisRequest req) {
        return PromptRenderer.render(USER_TEMPLATE, Map.of(
                "revId",         String.valueOf(req.revId()),
                "reviewerId",    String.valueOf(req.reviewerId()),
                "signals",       formatSignals(req),
                "reviewOpinion", req.reviewOpinionText() == null ? "(없음)" : req.reviewOpinionText(),
                "ragChunks",     formatRagChunks(req)
        ));
    }

    private String formatSignals(AuditAnalysisRequest req) {
        if (req.signals() == null || req.signals().isEmpty()) return "(없음)";
        return req.signals().stream()
                .map(s -> "- [%s/%s] metric=%s observed=%.4f threshold=%.4f".formatted(
                        s.ruleCd(), s.severityCd(), s.signalMetric(),
                        s.observedValue(), s.thresholdValue()))
                .collect(Collectors.joining("\n"));
    }

    private String formatRagChunks(AuditAnalysisRequest req) {
        if (req.ragChunks() == null || req.ragChunks().isEmpty()) return "(없음)";
        return req.ragChunks().stream()
                .map(c -> "출처: %s\n%s".formatted(c.source(), c.content()))
                .collect(Collectors.joining("\n---\n"));
    }
}
