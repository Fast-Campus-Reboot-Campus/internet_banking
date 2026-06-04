package com.bank.common.code;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.Optional;

/**
 * Redis 채널 "code:invalidate" 구독 → 로컬 코드 캐시 무효화.
 *
 * 캐시 키 정밀 무효화는 단건/그룹 키 prefix 가 분리되어 있어 그룹 단위는 그룹 캐시를,
 * 단건 변경은 단건 캐시를 evict 한다. (캐시 prefix 가 그룹별로 다시 분기되지 않으므로
 * 안전 측면에서 그룹 무효화 시 단건 캐시 전체를 비운다.)
 */
@Slf4j
@Configuration
@ConditionalOnClass({RedisConnectionFactory.class, CacheManager.class})
@RequiredArgsConstructor
public class CodeInvalidationSubscriber {

    public static final String CHANNEL = "code:invalidate";

    private final ObjectMapper objectMapper;

    @Bean
    @ConditionalOnProperty(name = "code.cache.subscriber.enabled", havingValue = "true", matchIfMissing = true)
    public RedisMessageListenerContainer codeInvalidationListenerContainer(
            RedisConnectionFactory cf, CacheManager cacheManager) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(cf);
        container.addMessageListener(invalidationListener(cacheManager), new PatternTopic(CHANNEL));
        return container;
    }

    private MessageListener invalidationListener(CacheManager cacheManager) {
        return (Message message, byte[] pattern) -> {
            try {
                CodeInvalidationEvent ev = objectMapper.readValue(message.getBody(), CodeInvalidationEvent.class);
                evict(cacheManager, ev);
            } catch (Exception e) {
                log.warn("코드 무효화 이벤트 파싱 실패: {}", new String(message.getBody()), e);
            }
        };
    }

    private void evict(CacheManager cacheManager, CodeInvalidationEvent ev) {
        Optional.ofNullable(cacheManager.getCache(CodeCacheConfig.CACHE_CODE_GROUP)).ifPresent(Cache::clear);

        Cache singleCache = cacheManager.getCache(CodeCacheConfig.CACHE_CODE);
        if (singleCache == null) return;

        if (ev.groupCd() == null) {
            singleCache.clear();
            log.info("[code-cache] 전체 무효화");
            return;
        }
        if (ev.codeCd() == null) {
            singleCache.clear();
            log.info("[code-cache] 그룹 무효화: {}", ev.groupCd());
            return;
        }
        singleCache.evict(ev.groupCd() + ":" + ev.codeCd());
        log.info("[code-cache] 단건 무효화: {}:{}", ev.groupCd(), ev.codeCd());
    }
}
