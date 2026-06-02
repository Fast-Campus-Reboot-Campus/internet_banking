package com.bank.loan.audit.store;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisBreakGlassGrantStore implements BreakGlassGrantStore {

    private final StringRedisTemplate redis;

    @Override
    public void grant(Long actorId, String targetType, Long targetId, Duration ttl) {
        redis.opsForValue().set(key(actorId, targetType, targetId), "1", ttl);
    }

    @Override
    public boolean hasGrant(Long actorId, String targetType, Long targetId) {
        return Boolean.TRUE.equals(redis.hasKey(key(actorId, targetType, targetId)));
    }

    private String key(Long actorId, String targetType, Long targetId) {
        return "bg:" + actorId + ":" + targetType + ":" + targetId;
    }
}
