package com.bank.loan.security;

/**
 * loan-service 에서 사용하는 역할 상수.
 *
 * Spring Security hasRole() 은 "ROLE_" 접두어를 자동으로 붙이므로
 * authority() 값은 "ROLE_" 포함 전체 문자열, spring() 값은 접두어 제거본이다.
 *
 * 직급(정적) vs 라인 배정(동적)
 *   - 여기 정의된 역할은 JWT claim 에서 전달되는 직급·직무 코드다.
 *   - 실제 결재 권한은 loan_review.reviewer_id / approver_id 배정으로 확정된다.
 */
public enum LoanRole {

    CUSTOMER("ROLE_CUSTOMER"),
    TELLER("ROLE_TELLER"),
    DEPUTY_MANAGER("ROLE_DEPUTY_MANAGER"),
    BRANCH_MANAGER("ROLE_BRANCH_MANAGER"),
    HQ_REVIEWER("ROLE_HQ_REVIEWER"),
    COMPLIANCE("ROLE_COMPLIANCE"),
    OPS("ROLE_OPS"),
    INTERNAL("ROLE_INTERNAL"),
    ADMIN("ROLE_ADMIN");

    private final String authority;

    LoanRole(String authority) {
        this.authority = authority;
    }

    /** Spring Security GrantedAuthority 문자열 (ROLE_ 접두어 포함) */
    public String authority() {
        return authority;
    }

    /** SecurityConfig hasRole() 인자 (ROLE_ 접두어 제거) */
    public String spring() {
        return authority.substring("ROLE_".length());
    }
}
