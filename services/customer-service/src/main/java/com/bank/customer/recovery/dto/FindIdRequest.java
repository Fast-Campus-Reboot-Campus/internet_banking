package com.bank.customer.recovery.dto;

/** 비로그인 ID 조회 요청 — 성명 + 계좌번호 + 계좌비밀번호로 본인확인. */
public record FindIdRequest(String name, String accountNumber, String accountPassword) {}
