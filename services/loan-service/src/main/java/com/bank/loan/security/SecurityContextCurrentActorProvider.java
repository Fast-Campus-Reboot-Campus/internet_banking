package com.bank.loan.security;

import com.bank.common.persistence.CurrentActorProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * SecurityContext 기반 CurrentActorProvider.
 * JpaAuditingConfig 의 기본 구현(시스템 사용자 0L)을 대체한다.
 *
 * JWT 인증 흐름에서 principal 은 customerId(Long).
 * 인증 정보가 없거나 principal 이 Long 이 아닌 경우 SYSTEM(0L) 을 반환한다.
 */
@Primary
@Component
public class SecurityContextCurrentActorProvider implements CurrentActorProvider {

    @Override
    public Long currentActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return SYSTEM;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Long id) {
            return id;
        }
        return SYSTEM;
    }
}
