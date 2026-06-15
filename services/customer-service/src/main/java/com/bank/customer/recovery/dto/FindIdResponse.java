package com.bank.customer.recovery.dto;

/** ID 조회 결과 — 본인확인 통과 시 로그인 ID 와 고객명. */
public record FindIdResponse(String loginId, String customerName) {}
