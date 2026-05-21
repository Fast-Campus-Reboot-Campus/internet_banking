package com.bank.loan.calendar.service;

import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.BusinessException;
import com.bank.loan.calendar.domain.BusinessCalendar;
import com.bank.loan.calendar.dto.BusinessCalendarListResponse;
import com.bank.loan.calendar.dto.BusinessCalendarResponse;
import com.bank.loan.calendar.dto.BusinessDayCheckResponse;
import com.bank.loan.calendar.dto.RegisterBusinessCalendarRequest;
import com.bank.loan.calendar.dto.UpdateBusinessCalendarRequest;
import com.bank.loan.calendar.repository.BusinessCalendarRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * 영업일 캘린더 서비스 (flows §2.2).
 *
 * 조회 정책:
 *   - DB row 가 있으면 그대로 사용
 *   - DB row 가 없으면 요일 기반 fallback: 토/일은 비영업일, 그 외 영업일
 *   - 운영자는 한국 공휴일/임시휴일을 명시적으로 등록해서 fallback 을 덮어쓴다
 *
 * 일자 표기: VARCHAR(8) YYYYMMDD — 도메인 전반 통일.
 */
@Service
@RequiredArgsConstructor
public class BusinessDayService {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.BASIC_ISO_DATE;

    private final BusinessCalendarRepository repository;
    private final CurrentActorProvider currentActor;

    @Transactional(readOnly = true)
    public boolean isBusinessDay(String calDate) {
        LocalDate parsed = parseOrThrow(calDate);
        return repository.findByCalDateAndDeletedAtIsNull(calDate)
                .map(BusinessCalendar::isBusinessDay)
                .orElseGet(() -> weekdayFallback(parsed));
    }

    @Transactional(readOnly = true)
    public BusinessDayCheckResponse check(String calDate) {
        LocalDate parsed = parseOrThrow(calDate);
        Optional<BusinessCalendar> row = repository.findByCalDateAndDeletedAtIsNull(calDate);
        return row
                .map(c -> BusinessDayCheckResponse.fromCalendar(
                        c.getCalDate(), c.isBusinessDay(),
                        c.getHolidayTypeCd(), c.getHolidayName()))
                .orElseGet(() -> BusinessDayCheckResponse.fallback(calDate, weekdayFallback(parsed)));
    }

    @Transactional
    public BusinessCalendarResponse register(RegisterBusinessCalendarRequest req) {
        parseOrThrow(req.calDate());
        repository.findByCalDateAndDeletedAtIsNull(req.calDate())
                .ifPresent(existing -> {
                    throw new BusinessException(LoanErrorCode.LOAN_161, "calDate=" + req.calDate());
                });

        BusinessCalendar saved = repository.save(BusinessCalendar.builder()
                .calDate(req.calDate())
                .businessDayYn(req.businessDayYn())
                .holidayTypeCd(req.holidayTypeCd())
                .holidayName(req.holidayName())
                .baseCountryCd(req.baseCountryCd() == null ? BusinessCalendar.COUNTRY_KR : req.baseCountryCd())
                .build());
        return BusinessCalendarResponse.of(saved);
    }

    @Transactional
    public BusinessCalendarResponse update(Long calId, UpdateBusinessCalendarRequest req) {
        BusinessCalendar entity = repository.findByCalIdAndDeletedAtIsNull(calId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_160));
        entity.update(
                req.businessDayYn(),
                req.holidayTypeCd(),
                req.holidayName(),
                req.baseCountryCd() == null ? entity.getBaseCountryCd() : req.baseCountryCd()
        );
        return BusinessCalendarResponse.of(entity);
    }

    @Transactional(readOnly = true)
    public BusinessCalendarResponse getByDate(String calDate) {
        parseOrThrow(calDate);
        return repository.findByCalDateAndDeletedAtIsNull(calDate)
                .map(BusinessCalendarResponse::of)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_160, "calDate=" + calDate));
    }

    @Transactional(readOnly = true)
    public BusinessCalendarListResponse listRange(String fromDate, String toDate) {
        parseOrThrow(fromDate);
        parseOrThrow(toDate);
        List<BusinessCalendarResponse> items = repository
                .findByCalDateBetweenAndDeletedAtIsNullOrderByCalDateAsc(fromDate, toDate)
                .stream()
                .map(BusinessCalendarResponse::of)
                .toList();
        return BusinessCalendarListResponse.of(fromDate, toDate, items);
    }

    @Transactional
    public void delete(Long calId) {
        BusinessCalendar entity = repository.findByCalIdAndDeletedAtIsNull(calId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_160));
        entity.softDelete(currentActor.currentActorId());
    }

    private LocalDate parseOrThrow(String yyyymmdd) {
        try {
            return LocalDate.parse(yyyymmdd, YYYYMMDD);
        } catch (DateTimeParseException e) {
            throw new BusinessException(LoanErrorCode.LOAN_162, "value=" + yyyymmdd);
        }
    }

    private boolean weekdayFallback(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    }
}
