package com.bank.loan.schedule.strategy;

import com.bank.loan.contract.domain.LoanContract;
import com.bank.loan.schedule.domain.RepaymentSchedule;

import java.util.List;

/**
 * 상환방식(repayment_method_cd) 별 회차 생성 전략.
 *
 * 빈 추가만으로 새 상환방식이 자동 인식되도록 {@link RepaymentScheduleGeneratorRegistry} 가
 * Spring 컨텍스트의 모든 구현체를 supportedMethod() 키로 모은다.
 *
 * 구현체는 호출 시점에 회차 행을 만들기만 한다 — DB 저장(JDBC batch insert) 은 호출자가 담당.
 */
public interface RepaymentScheduleGenerator {

    /** 본 구현체가 처리하는 repayment_method_cd. 예: "EQUAL", "BULLET". */
    String supportedMethod();

    /**
     * 계약 정보로부터 회차 N개(=contractedPeriodMo) 를 생성한다.
     * 모든 회차의 rsch_status_cd 는 DUE, rsch_version_cd 는 V1.
     * due_date 는 cntr_start_date 의 매월 같은 일자 (month-end 보정 LocalDate.plusMonths 정책 사용).
     */
    List<RepaymentSchedule> generate(LoanContract contract);
}
