package com.bank.loan.creditreport.channel;

import com.bank.loan.creditreport.domain.CreditInfoReport;
import com.bank.loan.creditreport.service.ExternalTxNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * KCB 어댑터 stub. 실 SDK 도입 전까지 무조건 성공 응답을 만든다.
 * 실패 케이스 시뮬레이션은 테스트에서 spy/override 로 주입.
 */
@Component
@RequiredArgsConstructor
public class StubKcbAdapter implements CreditInfoReportChannelAdapter {

    public static final String AGENCY_CD = "KCB";

    private final ExternalTxNumberGenerator txNoGenerator;

    @Override
    public String getAgencyCd() {
        return AGENCY_CD;
    }

    @Override
    public SendResult send(CreditInfoReport report) {
        String txNo = txNoGenerator.generate(OffsetDateTime.now());
        return new SendResult(true, txNo, "0000", "OK");
    }
}
