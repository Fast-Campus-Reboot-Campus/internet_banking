package com.bank.loan.audit.store;

import java.time.Duration;

public interface BreakGlassGrantStore {
    void grant(Long actorId, String targetType, Long targetId, Duration ttl);
    boolean hasGrant(Long actorId, String targetType, Long targetId);
}
