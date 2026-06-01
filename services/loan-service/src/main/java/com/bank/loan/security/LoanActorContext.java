package com.bank.loan.security;

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
}
