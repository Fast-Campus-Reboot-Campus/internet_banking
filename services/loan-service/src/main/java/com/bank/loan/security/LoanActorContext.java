package com.bank.loan.security;

import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.review.domain.LoanReview;
import org.springframework.security.core.Authentication;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * API 계층에서 추출한 호출자 컨텍스트.
 * SecurityContext Authentication → actorId·branch·roles 로 정규화한다.
 */
public record LoanActorContext(
        Long actorId,
        String branch,
        Set<String> roles
) {

    public static LoanActorContext from(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return new LoanActorContext(null, null, Set.of());
        }

        Long actorId = auth.getPrincipal() instanceof Long id ? id : null;

        String branch = null;
        if (auth.getDetails() instanceof GatewayAuthDetails details) {
            branch = details.branch();
        }

        Set<String> roles = auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toUnmodifiableSet());

        return new LoanActorContext(actorId, branch, roles);
    }

    public boolean hasRole(LoanRole role) {
        return roles.contains(role.authority());
    }

    /**
     * 이 건에 대한 PII 노출 수준을 판정한다.
     * checkScope() 통과 후 호출하는 것을 전제로 한다.
     *
     * 판정 우선순위:
     *  1. OPS/INTERNAL/ADMIN — FULL
     *  2. 담당자·심사자(라인) — FULL
     *  3. 승인자(라인) — MASKED
     *  4. 지점장(같은 지점) — MASKED
     *  5. 본사 담당자(상신 건) — MASKED
     *  6. 감사/컴플라이언스 — MASKED
     *  7. 고객 본인 및 그 외 — REDACTED
     */
    public PiiLevel piiLevel(LoanApplication application, LoanReview review) {
        if (hasRole(LoanRole.OPS) || hasRole(LoanRole.INTERNAL) || hasRole(LoanRole.ADMIN)) {
            return PiiLevel.FULL;
        }

        Long id = actorId();

        if (id != null && (id.equals(review.getOwnerId()) || id.equals(review.getReviewerId()))) {
            return PiiLevel.FULL;
        }

        if (id != null && id.equals(review.getApproverId())) {
            return PiiLevel.MASKED;
        }

        if (hasRole(LoanRole.BRANCH_MANAGER)
                && branch() != null && branch().equals(application.getBranchId())) {
            return PiiLevel.MASKED;
        }

        if (hasRole(LoanRole.HQ_REVIEWER) && review.isEscalated()) {
            return PiiLevel.MASKED;
        }

        if (hasRole(LoanRole.COMPLIANCE)) {
            return PiiLevel.MASKED;
        }

        return PiiLevel.REDACTED;
    }
}
