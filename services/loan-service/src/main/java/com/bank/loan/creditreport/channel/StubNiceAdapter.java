package com.bank.loan.creditreport.channel;

import com.bank.loan.creditreport.domain.CreditInfoReport;
import com.bank.loan.creditreport.service.ExternalTxNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * NICE 어댑터 stub.
 */
@Component
@RequiredArgsConstructor
public class StubNiceAdapter implements CreditInfoReportChannelAdapter {

    public static final String AGENCY_CD = "NICE";

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
