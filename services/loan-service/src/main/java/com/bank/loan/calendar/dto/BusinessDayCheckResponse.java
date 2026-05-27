package com.bank.loan.calendar.dto;

public record BusinessDayCheckResponse(
        String calDate,
        boolean businessDay,
        String source,
        String holidayTypeCd,
        String holidayName
) {
    public static BusinessDayCheckResponse fromCalendar(String calDate, boolean businessDay,
                                                        String holidayTypeCd, String holidayName) {
        return new BusinessDayCheckResponse(calDate, businessDay, "CALENDAR", holidayTypeCd, holidayName);
    }

    public static BusinessDayCheckResponse fallback(String calDate, boolean businessDay) {
        return new BusinessDayCheckResponse(calDate, businessDay, "WEEKDAY_FALLBACK", null, null);
    }
}
