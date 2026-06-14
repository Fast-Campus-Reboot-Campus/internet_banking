package com.bank.customer.recovery.dto;

/** 비로그인 ID 조회 요청 — 휴대폰 본인확인 결과(verificationId)로 본인확인. */
public record FindIdRequest(Long verificationId) {}
