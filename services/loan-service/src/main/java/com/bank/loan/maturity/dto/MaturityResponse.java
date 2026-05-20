package com.bank.loan.maturity.dto;

import com.bank.loan.maturity.domain.Maturity;

import java.time.OffsetDateTime;

public record MaturityResponse(
        Long matId,
        Long cntrId,
        String originalMaturityDate,
        String currentMaturityDate,
        String matStatusCd,
        String extensionTypeCd,
        Integer extensionCount,
        String lastExtendedDate,
        Integer extendedPeriodMo,
        String noticeStatusCd,
        OffsetDateTime lastNoticeAt
) {
    public static MaturityResponse of(Maturity m) {
        return new MaturityResponse(
                m.getMatId(), m.getCntrId(),
                m.getOriginalMaturityDate(), m.getCurrentMaturityDate(),
                m.getMatStatusCd(),
                m.getExtensionTypeCd(), m.getExtensionCount(),
                m.getLastExtendedDate(), m.getExtendedPeriodMo(),
                m.getNoticeStatusCd(), m.getLastNoticeAt()
        );
    }
}
