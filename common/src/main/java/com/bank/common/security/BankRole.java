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
        return rolesForHasRole(EMPLOYEE_ROLES);
    }

    /** {@code X-User-Role} 헤더 매칭용 — ROLE_ 접두어 포함 authority 집합 */
    public static Set<String> employeeAuthorities() {
        return EMPLOYEE_ROLES.stream().map(BankRole::authority).collect(Collectors.toUnmodifiableSet());
    }

    // ── /internal 직무별 인가 그룹 ───────────────────────────────────────────────
    //
    // 관리자 콘솔 프론트(web/lib/admin-auth.ts·components/admin/AdminSidebar.tsx)의 메뉴
    // 게이팅 어휘(CUSTOMER_VIEW·AUDIT_VIEW·HQ_DESK …)와 동일 집합을 백엔드 API 인가에서도
    // 본다. 프론트의 show/hide 는 표시 통제(presentation)일 뿐이라 API 직호출로 우회되므로,
    // customer-service SecurityConfig 가 이 집합으로 {@code /api/v1/internal/**} 를 직무별로
    // 게이팅해 실보안 통제를 둔다. 모든 그룹에 {@link #ADMIN} 을 포함해 시스템관리자는 전
    // 직무 API 를 통과한다(프론트 {@code hasAnyRole} 가 ADMIN 을 항상 통과시키는 것과 동일).

    /** 고객 데이터 열람·회원 라이프사이클(등급·신용·정지·해지 등) — admin web CUSTOMER_VIEW. */
    public static final Set<BankRole> CUSTOMER_VIEW_ROLES = Set.of(
            COMPLIANCE, HQ_REVIEWER, HQ_RISK, BRANCH_MANAGER, DEPUTY_MANAGER, TELLER, ADMIN);

    /** 감사 접근로그 조회 — admin web AUDIT_VIEW. */
    public static final Set<BankRole> AUDIT_VIEW_ROLES = Set.of(
            COMPLIANCE, HQ_REVIEWER, BRANCH_MANAGER, TELLER, ADMIN);

    /** KYC·AML·제재·세무·관계/대리인 심사 데스크 — admin web HQ_DESK. */
    public static final Set<BankRole> COMPLIANCE_DESK_ROLES = Set.of(
            COMPLIANCE, HQ_REVIEWER, HQ_RISK, ADMIN);

    /** 가입 통계 대시보드 — 컴플라이언스·리스크. */
    public static final Set<BankRole> JOIN_STATS_ROLES = Set.of(COMPLIANCE, HQ_RISK, ADMIN);

    /** FDS(이상거래) 룰·탐지·사고 관리 — 리스크·컴플라이언스·운영. */
    public static final Set<BankRole> FDS_ROLES = Set.of(COMPLIANCE, HQ_RISK, OPS, ADMIN);

    /** Spring Security {@code hasAnyRole(...)} 인자용 — 임의 역할 집합의 ROLE_ 접두어 제거 배열. */
    public static String[] rolesForHasRole(Set<BankRole> roles) {
        return roles.stream().map(BankRole::spring).toArray(String[]::new);
    }
}
