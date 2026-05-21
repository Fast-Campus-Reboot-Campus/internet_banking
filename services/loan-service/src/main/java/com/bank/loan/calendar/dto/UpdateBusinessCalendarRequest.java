package com.bank.loan.calendar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateBusinessCalendarRequest(
        @NotBlank @Pattern(regexp = "[YN]") String businessDayYn,
        @Size(max = 50) String holidayTypeCd,
        @Size(max = 100) String holidayName,
        @Size(max = 10) String baseCountryCd
) {
}
