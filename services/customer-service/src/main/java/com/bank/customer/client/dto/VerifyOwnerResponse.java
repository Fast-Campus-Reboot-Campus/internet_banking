package com.bank.customer.client.dto;

/** deposit-service 계좌 소유주 검증 결과 — matched=true 면 소유 고객 ID(문자열). */
public record VerifyOwnerResponse(boolean matched, String customerId) {}
