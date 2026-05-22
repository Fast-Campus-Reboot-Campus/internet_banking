package com.bank.loan.creditreport.channel;

import com.bank.loan.creditreport.domain.CreditInfoReport;
import com.bank.loan.creditreport.service.ExternalTxNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * plan 02 step 2: 채널 어댑터 + 레지스트리 분기 단위 검증.
 */
class CreditInfoReportChannelRegistryTest {

    private StubKcbAdapter kcb;
    private StubNiceAdapter nice;
    private CreditInfoReportChannelRegistry registry;

    @BeforeEach
    void setUp() {
        ExternalTxNumberGenerator gen = new ExternalTxNumberGenerator();
        kcb = new StubKcbAdapter(gen);
        nice = new StubNiceAdapter(gen);
        registry = new CreditInfoReportChannelRegistry(List.of(kcb, nice));
    }

    @Test
    void KCB_NICE_각각_매핑된다() {
        assertThat(registry.resolve("KCB")).isSameAs(kcb);
        assertThat(registry.resolve("NICE")).isSameAs(nice);
    }

    @Test
    void 미등록_agency_는_IllegalStateException() {
        assertThatThrownBy(() -> registry.resolve("KIS"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("KIS");
    }

    @Test
    void stub_어댑터는_성공_응답과_TX_채번() {
        CreditInfoReport dummy = CreditInfoReport.builder()
                .cntrId(1L).customerId(1L)
                .crptTypeCd("DELINQUENCY").crptAgencyCd("KCB")
                .crptStatusCd(CreditInfoReport.STATUS_REQUESTED)
                .reportTargetCd("EXISTING").build();

        CreditInfoReportChannelAdapter.SendResult r = kcb.send(dummy);
        assertThat(r.success()).isTrue();
        assertThat(r.externalTxNo()).startsWith("TX-").hasSize(24);
        assertThat(r.responseCode()).isEqualTo("0000");
    }
}
