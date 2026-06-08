package com.bank.customer.security;

/**
 * customer-service 에서 사용하는 역할 상수.
 *
 * <p>Spring Security hasRole() 은 "ROLE_" 접두어를 자동으로 붙이므로
 * authority() 값은 "ROLE_" 포함 전체 문자열, spring() 값은 접두어 제거본이다.
 *
 * <p>역할 문자열은 JWT claim(= employee-directory 설정)에서 전달된다.
 * loan-service 의 LoanRole 과 동일 집합 — 추후 common 으로 통합 추출 예정.
 */
public enum CustomerRole {

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

    CustomerRole(String authority) {
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
