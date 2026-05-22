package com.bank.loan.creditreport.listener;

import com.bank.loan.creditreport.dto.SubmitReportRequest;
import com.bank.loan.creditreport.service.CreditInfoReportService;
import com.bank.loan.notification.event.ContractSignedEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * plan 01 step 5: 약정 체결 자동 신고 listener 분기 검증 (Mockito).
 */
class ContractCreditReportListenerTest {

    @Test
    void onContractSigned_은_NEW_LOAN_CONTRACTED_로_적재() {
        CreditInfoReportService service = mock(CreditInfoReportService.class);
        ContractCreditReportListener listener = new ContractCreditReportListener(service);

        listener.onContractSigned(new ContractSignedEvent(777L, "CN-20330101-0001", 333L, 5050L));

        ArgumentCaptor<SubmitReportRequest> captor = ArgumentCaptor.forClass(SubmitReportRequest.class);
        verify(service).submit(eq(777L), captor.capture());
        SubmitReportRequest req = captor.getValue();
        assertThat(req.reportTypeCd()).isEqualTo("NEW_LOAN");
        assertThat(req.agencyCd()).isEqualTo("KCB");
        assertThat(req.reportTargetCd()).isEqualTo("NEW");
        assertThat(req.reportReasonCd()).isEqualTo("NEW_LOAN_CONTRACTED");
        assertThat(req.reportPayload())
                .contains("\"cntrId\":777")
                .contains("\"cntrNo\":\"CN-20330101-0001\"")
                .contains("\"applId\":333")
                .contains("\"customerId\":5050");
    }
}
