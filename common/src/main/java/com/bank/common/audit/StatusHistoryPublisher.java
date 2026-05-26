package com.bank.common.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 도메인 측에서 상태 변경 시 호출하는 진입점.
 * ApplicationEventPublisher 를 감싸 의존성을 단일화하고, 테스트 시 모킹을 쉽게 한다.
 */
@Component
@RequiredArgsConstructor
public class StatusHistoryPublisher {

    private final ApplicationEventPublisher delegate;

    public void publish(StatusChangeEvent event) {
        delegate.publishEvent(event);
    }
}
