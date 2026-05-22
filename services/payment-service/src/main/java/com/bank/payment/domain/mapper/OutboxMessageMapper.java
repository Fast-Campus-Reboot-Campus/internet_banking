package com.bank.payment.domain.mapper;

import com.bank.payment.domain.OutboxMessage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OutboxMessageMapper {

    void insert(OutboxMessage outboxMessage);

    List<OutboxMessage> selectPending();

    void markSent(String messageId);

    void markFailed(@Param("messageId") String messageId, @Param("lastError") String lastError);
}
