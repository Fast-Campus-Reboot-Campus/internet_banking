package com.bank.deposit.dto.request;

/** 계좌 소유주 검증 요청 — 계좌번호 + 계좌비밀번호. (서비스 간 내부 호출용) */
public record VerifyOwnerRequest(String accountNumber, String accountPassword) {}
