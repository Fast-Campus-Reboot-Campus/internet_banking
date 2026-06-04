package com.bank.customer.mypage.dto;

import java.time.OffsetDateTime;

public record MyPageResponse(
        Long           customerId,
        String         loginId,
        String         name,
        String         email,
        String         phone,
        String         zipCode,
        String         address,
        String         addressDetail,
        String         birthDate,
        String         genderCode,
        String         customerGradeCode,
        String         customerStatusCode,
        String         creditRatingCode,
        OffsetDateTime joinedAt,
        OffsetDateTime lastLoginAt,
        /** 최근 등급 변경 정보 (null = 변경 이력 없음) */
        GradeInfo      latestGrade,
        /** 최근 상태 변경 정보 (null = 변경 이력 없음) */
        StatusInfo     latestStatus
) {
    public record GradeInfo(
            String previousGradeCode,
            String newGradeCode,
            String changeReasonCode,
            String effectiveStartDate
    ) {}

    public record StatusInfo(
            String previousStatusCode,
            String newStatusCode,
            String changeReasonCode,
            OffsetDateTime effectiveStartAt
    ) {}
}
