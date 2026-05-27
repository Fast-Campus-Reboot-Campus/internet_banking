package com.bank.common.persistence;

import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.OffsetDateTime;
import java.util.Optional;

@Configuration
@ConditionalOnClass(EntityManager.class)
@EnableJpaAuditing(auditorAwareRef = "auditorAware", dateTimeProviderRef = "offsetDateTimeProvider")
public class JpaAuditingConfig {

    @Bean
    public DateTimeProvider offsetDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }

    @Bean
    @ConditionalOnMissingBean
    public CurrentActorProvider currentActorProvider() {
        return () -> CurrentActorProvider.SYSTEM;
    }

    @Bean
    public AuditorAware<Long> auditorAware(CurrentActorProvider provider) {
        return () -> Optional.ofNullable(provider.currentActorId()).or(() -> Optional.of(CurrentActorProvider.SYSTEM));
    }

    @Bean
    public DateTimeProvider auditingDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }
}