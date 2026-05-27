package com.bank.loan.delinquency.dto;

import com.bank.loan.delinquency.domain.DelinquencyDailySnapshot;

import java.time.OffsetDateTime;

public record DelinquencySnapshotResponse(
        Long dlqsId,
        Long dlqId,
        Long cntrId,
        String snapshotDate,
        Integer dlqDays,
        Long dlqPrincipalAmt,
        Long dlqInterestAmt,
        Long dlqTotalAmt,
        Integer overdueRateBps,
        String dlqStageCd,
        OffsetDateTime snapshottedAt
) {
    public static DelinquencySnapshotResponse of(DelinquencyDailySnapshot s) {
        return new DelinquencySnapshotResponse(
                s.getDlqsId(), s.getDlqId(), s.getCntrId(),
                s.getSnapshotDate(),
                s.getDlqDays(),
                s.getDlqPrincipalAmt(), s.getDlqInterestAmt(), s.getDlqTotalAmt(),
                s.getOverdueRateBps(), s.getDlqStageCd(),
                s.getSnapshottedAt()
        );
    }
}
