package com.bank.common.security;

/**
 * 전 서비스 공통 역할 상수 (single source of truth).
 *
 * <p>기존에 customer-service {@code CustomerRole} 과 loan-service {@code LoanRole} 로
 * 중복 관리되던 동일 집합을 common 으로 통합한 것이다. 각 서비스는 이 enum 을 참조한다.
 *
 * <p>Spring Security hasRole() 은 "ROLE_" 접두어를 자동으로 붙이므로
 * {@link #authority()} 는 "ROLE_" 포함 전체 문자열, {@link #spring()} 은 접두어 제거본이다.
 *
 * <p><b>정적 직급 vs 동적 관계</b>
 * 여기 정의된 값은 JWT claim 으로 전달되는 <em>정적 직급·직무</em>다.
 * "이 직원이 이 고객의 담당이냐(PRIMARY_OWNER)" "같은 지점이냐(OTHER_BRANCH)" 같은
 * <em>동적 관계</em>는 역할이 아니라 데이터(party_relation·branch 비교)로 판정한다.
 */
public enum BankRole {

    CUSTOMER("ROLE_CUSTOMER"),
    TELLER("ROLE_TELLER"),
    DEPUTY_MANAGER("ROLE_DEPUTY_MANAGER"),
    BRANCH_MANAGER("ROLE_BRANCH_MANAGER"),
    HQ_REVIEWER("ROLE_HQ_REVIEWER"),
    HQ_RISK("ROLE_HQ_RISK"),            // 본사 리스크관리부 (관리자 콘솔 RBAC)
    HQ_MARKETING("ROLE_HQ_MARKETING"),  // 본사 마케팅/기획부 (관리자 콘솔 RBAC)
    COMPLIANCE("ROLE_COMPLIANCE"),      // 컴플라이언스/감사
    OPS("ROLE_OPS"),
    INTERNAL("ROLE_INTERNAL"),
    ADMIN("ROLE_ADMIN");

    private final String authority;

    BankRole(String authority) {
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
