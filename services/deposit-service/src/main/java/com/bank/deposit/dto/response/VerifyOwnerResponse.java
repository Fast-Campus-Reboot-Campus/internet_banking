package com.bank.deposit.dto.response;

/** 계좌 소유주 검증 결과 — matched=true 면 소유 고객 ID 동봉, 실패 시 matched=false·customerId=null. */
public record VerifyOwnerResponse(boolean matched, String customerId) {}
