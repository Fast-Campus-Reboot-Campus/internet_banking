package com.bank.customer.fds.dto;

import com.bank.customer.fds.domain.FdsDetection;

import java.time.OffsetDateTime;

public record FdsDetectionResponse(
        Long           fdsDetectionId,
        Long           customerId,
        Long           fdsRuleId,
        String         fdsDetectionEventTypeCode,
        Long           fdsDetectionEventReferenceId,
        OffsetDateTime fdsDetectedAt,
        String         fdsDetectionStatusCode
) {
    public static FdsDetectionResponse from(FdsDetection d) {
        return new FdsDetectionResponse(
                d.getFdsDetectionId(), d.getCustomerId(), d.getFdsRuleId(),
                d.getFdsDetectionEventTypeCode(), d.getFdsDetectionEventReferenceId(),
                d.getFdsDetectedAt(), d.getFdsDetectionStatusCode());
    }
}
