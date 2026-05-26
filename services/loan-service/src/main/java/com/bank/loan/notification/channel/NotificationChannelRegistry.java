package com.bank.loan.notification.channel;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * channelCd → adapter 룩업.
 *
 * 같은 channelCd 로 중복 등록되면 부팅 시점에 즉시 실패시킨다 (Map 빌드의 duplicate key 예외).
 */
@Component
public class NotificationChannelRegistry {

    private final Map<String, NotificationChannelAdapter> byChannel;

    public NotificationChannelRegistry(List<NotificationChannelAdapter> adapters) {
        this.byChannel = adapters.stream()
                .collect(Collectors.toUnmodifiableMap(
                        NotificationChannelAdapter::getChannelCd,
                        Function.identity()));
    }

    public NotificationChannelAdapter resolve(String channelCd) {
        NotificationChannelAdapter adapter = byChannel.get(channelCd);
        if (adapter == null) {
            // 운영 구성 오류 — outbox 가 알 수 없는 channelCd 로 적재됐다는 뜻.
            throw new IllegalStateException("no adapter for channelCd=" + channelCd);
        }
        return adapter;
    }
}
