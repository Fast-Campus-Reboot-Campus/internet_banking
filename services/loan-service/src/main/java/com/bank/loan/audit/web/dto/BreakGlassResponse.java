package com.bank.loan.audit.web.dto;

import java.time.OffsetDateTime;

public record BreakGlassResponse(Long logId, OffsetDateTime grantExpiresAt) {}
