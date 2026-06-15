package com.bank.customer.banking.dto;

/** 고객당 인터넷뱅킹 이체한도 — 1일/1회(원). */
public record TransferLimitResponse(long dailyLimit, long onceLimit) {}
