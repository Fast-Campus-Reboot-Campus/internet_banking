package com.bank.loan.calendar.dto;

import java.util.List;

public record BusinessCalendarListResponse(
        String fromDate,
        String toDate,
        int count,
        List<BusinessCalendarResponse> items
) {
    public static BusinessCalendarListResponse of(String from, String to,
                                                  List<BusinessCalendarResponse> items) {
        return new BusinessCalendarListResponse(from, to, items.size(), items);
    }
}
