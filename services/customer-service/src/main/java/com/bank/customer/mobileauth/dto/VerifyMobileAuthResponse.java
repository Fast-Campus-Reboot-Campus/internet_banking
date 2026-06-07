package com.bank.customer.mobileauth.dto;

/**
 * 휴대폰 인증 검증 응답.
 *
 * @param verificationId 신원확인(SIGNUP/IDENTITY_VERIFY) 목적이면 생성된 본인확인 이력 id,
 *                       그 외 목적이면 null. 가입 시 이 값을 {@code /auth/register} 로 전달한다.
 */
public record VerifyMobileAuthResponse(Long verificationId) {}
