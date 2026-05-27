package com.bank.loan.calendar.repository;

import com.bank.loan.calendar.domain.BusinessCalendar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusinessCalendarRepository extends JpaRepository<BusinessCalendar, Long> {

    Optional<BusinessCalendar> findByCalDateAndDeletedAtIsNull(String calDate);

    Optional<BusinessCalendar> findByCalIdAndDeletedAtIsNull(Long calId);

    List<BusinessCalendar> findByCalDateBetweenAndDeletedAtIsNullOrderByCalDateAsc(
            String fromDate, String toDate);
}
