package com.bank.loan.schedule.strategy;

import com.bank.common.web.BusinessException;
import com.bank.loan.support.LoanErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 상환방식 코드 → {@link RepaymentScheduleGenerator} 라우터.
 *
 * Spring 이 컨텍스트의 모든 RepaymentScheduleGenerator 빈을 주입하면,
 * supportedMethod() 값을 키로 하는 불변 Map 을 빌드한다.
 *
 * 새 상환방식을 추가하려면 RepaymentScheduleGenerator 를 구현한 빈을 등록하기만 하면 된다 —
 * 본 클래스도, 호출자(RepaymentScheduleService)도 손댈 필요 없다.
 *
 * 미지원 방식은 LOAN_084 로 명시적 차단.
 */
@Component
public class RepaymentScheduleGeneratorRegistry {

    private final Map<String, RepaymentScheduleGenerator> byMethod;

    public RepaymentScheduleGeneratorRegistry(List<RepaymentScheduleGenerator> generators) {
        this.byMethod = generators.stream()
                .collect(Collectors.toUnmodifiableMap(
                        RepaymentScheduleGenerator::supportedMethod,
                        g -> g));
    }

    public RepaymentScheduleGenerator find(String repaymentMethodCd) {
        RepaymentScheduleGenerator g = byMethod.get(repaymentMethodCd);
        if (g == null) {
            throw new BusinessException(LoanErrorCode.LOAN_084,
                    "repaymentMethodCd=" + repaymentMethodCd);
        }
        return g;
    }
}
