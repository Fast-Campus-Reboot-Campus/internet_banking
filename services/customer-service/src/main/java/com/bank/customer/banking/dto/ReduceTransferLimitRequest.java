package com.bank.customer.banking.dto;

/** 이체한도 감액 요청 — 새 1일/1회 한도(원). 현재보다 작거나 같아야 한다(감액만). */
public record ReduceTransferLimitRequest(long dailyLimit, long onceLimit) {}
