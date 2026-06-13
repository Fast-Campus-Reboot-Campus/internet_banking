package com.bank.customer.client.dto;

/** deposit-service 계좌 소유주 검증 요청 — 계좌번호 + 계좌비밀번호. */
public record VerifyOwnerRequest(String accountNumber, String accountPassword) {}
