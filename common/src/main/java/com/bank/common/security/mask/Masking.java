package com.bank.common.security.mask;

/**
 * 키 없이 가능한 정적 마스킹 유틸. ERD 의 *_masked 컬럼 채울 때 사용.
 * 입력이 null / blank / 형식 불일치인 경우 원문을 그대로 반환한다 (호출부에서 별도 검증).
 */
public final class Masking {

    private Masking() {}

    /** 홍길동 → 홍*동 / 김철 → 김* / 김 → 김 */
    public static String name(String name) {
        if (name == null || name.isBlank()) return name;
        int len = name.length();
        if (len == 1) return name;
        if (len == 2) return name.charAt(0) + "*";
        StringBuilder sb = new StringBuilder().append(name.charAt(0));
        for (int i = 1; i < len - 1; i++) sb.append('*');
        sb.append(name.charAt(len - 1));
        return sb.toString();
    }

    /** 01012345678 → 010-****-5678 */
    public static String mobile(String mobile) {
        if (mobile == null) return null;
        String digits = mobile.replaceAll("\\D", "");
        if (digits.length() < 7) return mobile;
        int len = digits.length();
        return digits.substring(0, 3) + "-****-" + digits.substring(len - 4);
    }

    /** 계좌번호 → 앞 3자리 + **** + 뒤 4자리 (1102345678901 → 110-****-8901) */
    public static String account(String accountNo) {
        if (accountNo == null) return null;
        String digits = accountNo.replaceAll("\\D", "");
        if (digits.length() < 7) return accountNo;
        return digits.substring(0, 3) + "-****-" + digits.substring(digits.length() - 4);
    }

    /** 주민번호 → 901231-1****** */
    public static String rrn(String rrn) {
        if (rrn == null) return null;
        String digits = rrn.replaceAll("\\D", "");
        if (digits.length() != 13) return rrn;
        return digits.substring(0, 6) + "-" + digits.charAt(6) + "******";
    }

    /** local@domain → l***@domain */
    public static String email(String email) {
        if (email == null || !email.contains("@")) return email;
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 1) return local + domain;
        return local.charAt(0) + "***" + domain;
    }
}
