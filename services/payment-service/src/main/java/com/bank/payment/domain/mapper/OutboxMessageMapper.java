package com.bank.payment.domain.mapper;

import com.bank.payment.domain.OutboxMessage;

public interface OutboxMessageMapper {

    void insert(OutboxMessage outboxMessage);
}
