package com.bank.customer.settings.dto;

/** 인터넷뱅킹 해지 요청 — 고객 해지(WithdrawRequest)와 구분한다. 본인확인용 현재 비밀번호만 받는다. */
public record InternetBankingCancelRequest(String currentPassword) {}
