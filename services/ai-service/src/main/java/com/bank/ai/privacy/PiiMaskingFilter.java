package com.bank.ai.privacy;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;

/**
 * 외부 LLM 호출 직전 호출되는 단방향-가역 PII 마스킹 게이트웨이.
 *
 * 같은 원문은 같은 토큰으로 치환되도록 요청 단위 매핑을 보존한다(LLM 응답에서 역치환 가능).
 * 매핑은 요청 lifetime 동안만 in-memory 로 유지. 운영시 Redis TTL 로 이동 예정.
 *
 * 적용 순서는 enum 선언 순서를 따른다 — 더 구체적인 패턴(주민번호/카드)을
 * 일반적인 패턴(이메일/이름)보다 먼저 두어 부분매칭 충돌을 줄인다.
 */
@Component
public class PiiMaskingFilter {

    /**
     * 마스킹 결과와 역치환용 매핑을 함께 보관한다.
     */
    public record MaskingResult(String maskedText, Map<String, String> mapping) {

        /** LLM 응답 텍스트에서 마스킹 토큰을 원문으로 복원한다. */
        public String unmask(String llmResponse) {
            String restored = llmResponse;
            for (Map.Entry<String, String> e : mapping.entrySet()) {
                restored = restored.replace(e.getKey(), e.getValue());
            }
            return restored;
        }
    }

    /**
     * 입력 텍스트에서 모든 PII 패턴을 마스킹 토큰으로 치환한다.
     *
     * @param text 원문
     * @return 마스킹 결과 + 역치환 매핑
     */
    public MaskingResult mask(String text) {
        if (text == null || text.isEmpty()) {
            return new MaskingResult(text, Map.of());
        }
        // 매핑은 삽입 순서 보존(역치환 시 긴 토큰부터 매칭되도록).
        Map<String, String> mapping = new LinkedHashMap<>();
        Map<String, String> reverseCache = new LinkedHashMap<>(); // 원문 -> 토큰 (중복 토큰 방지)

        String working = text;
        for (PiiPattern p : PiiPattern.values()) {
            Matcher m = p.pattern().matcher(working);
            StringBuilder buf = new StringBuilder();
            while (m.find()) {
                String match = m.group();
                String token = reverseCache.computeIfAbsent(match, k -> generateToken(p));
                mapping.putIfAbsent(token, match);
                m.appendReplacement(buf, Matcher.quoteReplacement(token));
            }
            m.appendTail(buf);
            working = buf.toString();
        }
        return new MaskingResult(working, mapping);
    }

    private String generateToken(PiiPattern p) {
        // [[RRN_a1b2c3d4]] 형식 — LLM 이 원문으로 오해하지 않도록 대괄호 2중 + 짧은 UUID.
        return "[[" + p.token() + "_" + UUID.randomUUID().toString().substring(0, 8) + "]]";
    }
}
