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

    /** 휴일 보정 lookahead 상한 — 31 일이면 한국 최장 연속 휴일(설/추석 연휴 + 임시휴일) 모두 흡수. */
    public static final int MAX_LOOKAHEAD_DAYS = 31;

    private final BusinessCalendarRepository repository;
    private final CurrentActorProvider currentActor;

    @Transactional(readOnly = true)
    public boolean isBusinessDay(String calDate) {
        LocalDate parsed = parseOrThrow(calDate);
        return repository.findByCalDateAndDeletedAtIsNull(calDate)
                .map(BusinessCalendar::isBusinessDay)
                .orElseGet(() -> weekdayFallback(parsed));
    }

    /**
     * 입력 일자가 이미 영업일이면 그대로, 아니면 다음 영업일로 이동한 YYYYMMDD 를 반환한다.
     * (한국 여신 관행 — `following` 정책)
     *
     * 사용처: schedule 생성 시 dueDate 휴일 보정, 만기일 보정.
     * 31 일 안에 영업일이 없으면 LOAN_163 — 캘린더 시드 오류 신호.
     */
    @Transactional(readOnly = true)
    public String nextBusinessDay(String calDate) {
        return advance(parseOrThrow(calDate), 1);
    }

    /**
     * 입력 일자가 이미 영업일이면 그대로, 아니면 직전 영업일로 이동한 YYYYMMDD 를 반환한다.
     * 만기·이자 정산 정합성용 (예: 영업일 마감 처리).
     */
    @Transactional(readOnly = true)
    public String previousBusinessDay(String calDate) {
        return advance(parseOrThrow(calDate), -1);
    }

    /**
     * 입력 일자 **이전** (미포함) 의 마지막 영업일.
     * autodebit 배치가 휴일 이월 회차 범위를 잡을 때 하한선으로 사용한다.
     */
    @Transactional(readOnly = true)
    public String lastBusinessDayBefore(String calDate) {
        return advance(parseOrThrow(calDate).minusDays(1), -1);
    }

    private String advance(LocalDate start, int step) {
        LocalDate cursor = start;
        for (int i = 0; i <= MAX_LOOKAHEAD_DAYS; i++) {
            String yyyymmdd = cursor.format(YYYYMMDD);
            if (isBusinessDayInternal(cursor, yyyymmdd)) {
                return yyyymmdd;
            }
            cursor = cursor.plusDays(step);
        }
        throw new BusinessException(LoanErrorCode.LOAN_163,
                "no business day within " + MAX_LOOKAHEAD_DAYS + " days from " + start.format(YYYYMMDD));
    }

    private boolean isBusinessDayInternal(LocalDate parsed, String yyyymmdd) {
        return repository.findByCalDateAndDeletedAtIsNull(yyyymmdd)
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
