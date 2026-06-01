package com.bank.customer.mypage.dto;

import java.time.OffsetDateTime;

public record MyPageResponse(
        Long customerId,
        String loginId,
        String name,
        String email,
        String phone,
        String zipCode,
        String address,
        String addressDetail,
        String birthDate,
        String genderCode,
        String customerGradeCode,
        String customerStatusCode,
        OffsetDateTime joinedAt,
        OffsetDateTime lastLoginAt
) {}
