package com.bank.loan.review.dto;

import com.bank.loan.review.domain.ReviewCheckLog;

import java.time.OffsetDateTime;

public record ReviewCheckLogResponse(
        Long rchkId,
        Long revId,
        String checkItemCd,
        String checkResultCd,
        String checkRemark,
        Long checkerId,
        OffsetDateTime checkedAt
) {
    public static ReviewCheckLogResponse of(ReviewCheckLog l) {
        return new ReviewCheckLogResponse(
                l.getRchkId(), l.getRevId(),
                l.getCheckItemCd(), l.getCheckResultCd(),
                l.getCheckRemark(),
                l.getCheckerId(), l.getCheckedAt()
        );
    }
}
