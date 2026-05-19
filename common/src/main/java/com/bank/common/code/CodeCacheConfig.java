package com.bank.common.code;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * CODE_MASTER 조회 캐시. Redis 기반, TTL 1h.
 *   - 캐시명: "code" (단건), "code:group" (그룹 전체)
 *   - 키 prefix: "code:" / "code:group:"
 */
@Configuration
@EnableCaching
@ConditionalOnClass({RedisConnectionFactory.class, CacheManager.class})
public class CodeCacheConfig {

    public static final String CACHE_CODE = "code";
    public static final String CACHE_CODE_GROUP = "code:group";

    @Bean
    public RedisCacheManager codeCacheManager(RedisConnectionFactory cf, ObjectMapper objectMapper) {
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(objectMapper)));

        return RedisCacheManager.builder(cf)
                .cacheDefaults(base)
                .withCacheConfiguration(CACHE_CODE,       base.prefixCacheNameWith("code:"))
                .withCacheConfiguration(CACHE_CODE_GROUP, base.prefixCacheNameWith("code:group:"))
                .build();
    }
}
