package com.bank.common.security;

import java.util.Set;
import java.util.stream.Collectors;

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

    /**
     * 직원 직급(고객 제외) — {@code /api/v1/internal/**} 관리 API 접근 허용 대상의 단일 소스.
     *
     * <p>SecurityConfig(hasAnyRole)와 InternalApiRoleInterceptor(헤더 매칭)가 각각
     * 별도 목록을 두면 서로 어긋나 본사 직급이 통째로 차단되는 사고가 났다(인터셉터가 더 좁아
     * 후순위로 이김). 두 곳 모두 이 집합을 참조해 화이트리스트를 일치시킨다.
     */
    public static final Set<BankRole> EMPLOYEE_ROLES = Set.of(
            TELLER, DEPUTY_MANAGER, BRANCH_MANAGER,
            HQ_REVIEWER, HQ_RISK, HQ_MARKETING, COMPLIANCE, OPS, ADMIN);

    /** Spring Security {@code hasAnyRole(...)} 인자용 — ROLE_ 접두어 제거 직급명 배열 */
    public static String[] employeeRolesForHasRole() {
        return EMPLOYEE_ROLES.stream().map(BankRole::spring).toArray(String[]::new);
    }

    /** {@code X-User-Role} 헤더 매칭용 — ROLE_ 접두어 포함 authority 집합 */
    public static Set<String> employeeAuthorities() {
        return EMPLOYEE_ROLES.stream().map(BankRole::authority).collect(Collectors.toUnmodifiableSet());
    }
}
