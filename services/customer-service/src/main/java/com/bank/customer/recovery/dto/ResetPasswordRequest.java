package com.bank.customer.recovery.dto;

/** 비로그인 사용자암호 재설정 요청 — 휴대폰 본인확인(verificationId) + 새 암호. */
public record ResetPasswordRequest(Long verificationId, String newPassword) {}
