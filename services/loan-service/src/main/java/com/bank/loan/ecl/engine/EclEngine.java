package com.bank.loan.ecl.engine;

/**
 * IFRS9 ECL 산정 엔진. Mock / Http 구현체로 분기 (loan.ecl.engine.type).
 *
 * 입력: cntrId, 원금잔액 (EAD 기준), 연체 stage code (없으면 null), 보증보험 ISSUED 여부, 담보 등록 여부.
 * 출력: IFRS stage, PD/LGD (bps), EAD, ECL, engine_version.
 */
public interface EclEngine {

    EclResult calculate(EclInput input);

    record EclInput(
            Long cntrId,
            long principalBalance,
            String delinquencyStageCd,   // STAGE_0/1/2/3 or null (비연체)
            boolean hasActiveGuaranteeInsurance,
            boolean hasCollateral
    ) {}

    record EclResult(
            String ifrsStageCd,
            int pdBps,
            int lgdBps,
            long ead,
            long ecl,
            String engineVersion
    ) {}
}
