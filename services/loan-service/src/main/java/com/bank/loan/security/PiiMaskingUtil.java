package com.bank.loan.security;

/**
 * PII 마스킹 정적 유틸.
 * 응답 DTO 팩토리에서만 사용한다.
 */
public final class PiiMaskingUtil {

    private PiiMaskingUtil() {}

    /**
     * 전화번호 재마스킹.
     * 이미 부분 마스킹된 저장값을 MASKED 수준으로 추가 마스킹한다.
     * 예) "010-1234-5678" → "010-****-****"
     */
    public static String maskPhone(String stored) {
        if (stored == null) return null;
        return stored.replaceAll("(\\d{2,4})-[0-9*]{3,4}-[0-9*]{4}", "$1-****-****");
    }

    /**
     * 금액대 문자열.
     * REDACTED 수준에서 정확한 금액 대신 사용한다.
     * 예) 52_000_000 → "5천만원대", 150_000_000 → "1억원대"
     */
    public static String amountRange(Long amount) {
        if (amount == null) return null;
        if (amount >= 100_000_000L) {
            return (amount / 100_000_000L) + "억원대";
        }
        if (amount >= 10_000_000L) {
            return (amount / 10_000_000L) + "천만원대";
        }
        if (amount >= 1_000_000L) {
            return (amount / 1_000_000L) + "백만원대";
        }
        return "100만원 미만";
    }
}
