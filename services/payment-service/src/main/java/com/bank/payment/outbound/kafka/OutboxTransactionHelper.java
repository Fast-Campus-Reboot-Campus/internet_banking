package com.bank.payment.outbound.kafka;

import com.bank.payment.domain.mapper.OutboxMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbox 상태 업데이트 전용 트랜잭션 헬퍼.
 *
 * OutboxPublisher는 @Transactional 금지(Kafka send가 DB TX 안에 들어가면 P-028 위반).
 * markSent/markFailed는 짧은 독립 트랜잭션이 필요하므로 별도 Bean으로 분리한다.
 */
@Component
@RequiredArgsConstructor
public class OutboxTransactionHelper {

    private final OutboxMessageMapper outboxMessageMapper;

    @Transactional
    public void markSent(String messageId) {
        outboxMessageMapper.markSent(messageId);
    }

    @Transactional
    public void markFailed(String messageId, String lastError) {
        outboxMessageMapper.markFailed(messageId, lastError);
    }
}
