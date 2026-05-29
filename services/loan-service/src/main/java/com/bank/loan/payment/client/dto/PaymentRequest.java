package com.bank.loan.payment.client.dto;

import java.math.BigDecimal;

public record PaymentRequest(
        String senderAccountId,
        String receiverBankCode,
        String receiverAccountNo,
        String receiverHolderName,
        BigDecimal transferAmount,
        String senderMemo,
        String receiverMemo,
        String channel,
        String receiverPassbookSenderDisplay
) {}
