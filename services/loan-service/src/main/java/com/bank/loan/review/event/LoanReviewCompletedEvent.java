package com.bank.loan.review.event;

/**
 * 본심사 완료 이벤트 (APPROVED / REJECTED 모두). advisory SYNC 룰 트리거용.
 * AdvisoryReviewListener 가 AFTER_COMMIT 비동기로 수신한다.
 */
public record LoanReviewCompletedEvent(
        Long revId,
        Long applId,
        Long reviewerId,
        String revDecisionCd,
        String revTypeCd
) {}
