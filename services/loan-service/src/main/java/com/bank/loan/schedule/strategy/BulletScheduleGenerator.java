package com.bank.loan.schedule.strategy;

import com.bank.loan.contract.domain.LoanContract;
import com.bank.loan.schedule.domain.RepaymentSchedule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 만기일시상환(BULLET) 회차 생성기.
 *
 * 매 회차 동일 이자만 납입하고, 마지막 회차에 원금 전액 + 이자를 납입한다.
 *   interest_i  = principal × annualRateBps / 10000 / 12   (HALF_EVEN, 회차 동일)
 *   principal_i = 0                                        (i < n)
 *   principal_n = principal                                (마지막 회차)
 *   remaining_i = principal                                (i < n)
 *   remaining_n = 0
 *
 * 라운딩 흡수: Bullet 은 회차당 이자가 동일해 별도 잔여처리 불필요.
 * 다만 매 회차 이자 합과 정확한 누적이자가 1원 단위에서 어긋날 수 있는데, 본 단계는 회차 표시값 기준으로만 합산한다.
 */
@Component
public class BulletScheduleGenerator implements RepaymentScheduleGenerator {

    public static final String METHOD = "BULLET";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal BPS_TO_DECIMAL = BigDecimal.valueOf(10_000);
    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);

    @Override
    public String supportedMethod() {
        return METHOD;
    }

    @Override
    public List<RepaymentSchedule> generate(LoanContract contract) {
        long principal = contract.getContractedAmount();
        int rateBps = contract.getTotalRateBps();
        int n = contract.getContractedPeriodMo();

        if (principal <= 0) throw new IllegalArgumentException("principal must be > 0");
        if (rateBps < 0)    throw new IllegalArgumentException("rateBps must be >= 0");
        if (n <= 0)         throw new IllegalArgumentException("periodMonths must be > 0");

        long monthlyInterest = BigDecimal.valueOf(principal)
                .multiply(BigDecimal.valueOf(rateBps), MC)
                .divide(BPS_TO_DECIMAL.multiply(MONTHS_PER_YEAR), MC)
                .setScale(0, RoundingMode.HALF_EVEN)
                .longValueExact();

        LocalDate startDate = LocalDate.parse(contract.getCntrStartDate(), DATE);
        List<RepaymentSchedule> rows = new ArrayList<>(n);

        for (int i = 1; i <= n; i++) {
            boolean isLast = (i == n);
            long scheduledPrincipal = isLast ? principal : 0L;
            long remaining = isLast ? 0L : principal;
            long scheduledInterest = monthlyInterest;
            long scheduledTotal = scheduledPrincipal + scheduledInterest;
            LocalDate dueDate = startDate.plusMonths(i);

            rows.add(RepaymentSchedule.builder()
                    .cntrId(contract.getCntrId())
                    .installmentNo(i)
                    .dueDate(dueDate.format(DATE))
                    .scheduledPrincipal(scheduledPrincipal)
                    .scheduledInterest(scheduledInterest)
                    .scheduledTotal(scheduledTotal)
                    .remainingBalance(remaining)
                    .appliedRateBps(rateBps)
                    .rschStatusCd(RepaymentSchedule.STATUS_DUE)
                    .rschVersionCd(RepaymentSchedule.VERSION_INITIAL)
                    .build());
        }

        return rows;
    }
}
