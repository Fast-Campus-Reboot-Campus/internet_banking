package com.bank.loan.review.service;

import com.bank.loan.review.domain.ReviewCheckLog;
import com.bank.loan.review.repository.ReviewCheckLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * 본심사 체크 로그 자동 적재 헬퍼.
 * LoanReviewService 가 본심사 결정 시점에 호출 — 같은 트랜잭션에서 5개 항목 적재.
 */
@Component
@RequiredArgsConstructor
public class ReviewCheckLogger {

    private final ReviewCheckLogRepository repository;

    public void log(Long revId, String itemCd, String resultCd, String remark, Long checkerId) {
        repository.save(ReviewCheckLog.builder()
                .revId(revId)
                .checkItemCd(itemCd)
                .checkResultCd(resultCd)
                .checkRemark(remark)
                .checkerId(checkerId)
                .checkedAt(OffsetDateTime.now())
                .build());
    }
}
