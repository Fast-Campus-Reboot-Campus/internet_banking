package com.bank.loan.calendar.dto;

/**
 * 영업일 캘린더 자동 시드 결과.
 *
 *   year         시드한 연도 (YYYY)
 *   totalDays    그 해 총 일수 (평년 365 / 윤년 366)
 *   inserted     신규 적재된 row 수
 *   skipped      이미 존재해 skip 된 row 수 (멱등)
 */
public record CalendarSeederRunResponse(
        int year,
        int totalDays,
        int inserted,
        int skipped
) {
    public static CalendarSeederRunResponse of(int year, int totalDays, int inserted, int skipped) {
        return new CalendarSeederRunResponse(year, totalDays, inserted, skipped);
    }
}
