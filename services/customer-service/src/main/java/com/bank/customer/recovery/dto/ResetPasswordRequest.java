package com.bank.customer.recovery.dto;

/** 비로그인 사용자암호 재설정 요청 — 계좌 본인확인(계좌번호+계좌비밀번호) + 대상 loginId + 새 암호. */
public record ResetPasswordRequest(
        String loginId, String accountNumber, String accountPassword, String newPassword) {}
