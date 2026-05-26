package com.bank.loan.creditreport.listener;

import com.bank.loan.creditreport.dto.SubmitReportRequest;
import com.bank.loan.creditreport.service.CreditInfoReportService;
import com.bank.loan.delinquency.domain.Delinquency;
import com.bank.loan.notification.event.DelinquencyOpenedEvent;
import com.bank.loan.notification.event.DelinquencyResolvedEvent;
import com.bank.loan.notification.event.DelinquencyStageAdvancedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * plan 01 step 4: 연체 이벤트 → 자동 신고 listener 분기 검증 (Mockito).
 *
 *   1) OPENED → submit(type=DELINQUENCY, reason=OPENED, dlqId 전달)
 *   2) STAGE_ADVANCED(STAGE_1→STAGE_2) → submit, reason=STAGE_ADVANCED
 *   3) STAGE_ADVANCED(STAGE_0→STAGE_1) → submit 호출 없음 (내부 단계)
 *   4) RESOLVED → submit(type=RESOLUTION, reason=RESOLVED)
 */
class DelinquencyCreditReportListenerTest {

    private CreditInfoReportService reportService;
    private DelinquencyCreditReportListener listener;

    @BeforeEach
    void setUp() {
        reportService = mock(CreditInfoReportService.class);
        listener = new DelinquencyCreditReportListener(reportService);
    }

    @Test
    void onOpened_은_DELINQUENCY_OPENED_로_적재() {
        listener.onOpened(new DelinquencyOpenedEvent(100L, 9L, "20330115", Delinquency.STAGE_0));

        ArgumentCaptor<SubmitReportRequest> captor = ArgumentCaptor.forClass(SubmitReportRequest.class);
        verify(reportService).submit(eqLong(100L), eqLong(9L), captor.capture());
        SubmitReportRequest req = captor.getValue();
        assertThat(req.reportTypeCd()).isEqualTo("DELINQUENCY");
        assertThat(req.reportReasonCd()).isEqualTo("DELINQUENCY_OPENED");
        assertThat(req.agencyCd()).isEqualTo("KCB");
        assertThat(req.reportPayload()).contains("\"dlqId\":9").contains("\"dlqStartDate\":\"20330115\"");
    }

    @Test
    void stage_advanced_STAGE_2_진입은_신고() {
        listener.onStageAdvanced(new DelinquencyStageAdvancedEvent(
                100L, 9L, Delinquency.STAGE_1, Delinquency.STAGE_2, 30));

        ArgumentCaptor<SubmitReportRequest> captor = ArgumentCaptor.forClass(SubmitReportRequest.class);
        verify(reportService).submit(eqLong(100L), eqLong(9L), captor.capture());
        assertThat(captor.getValue().reportReasonCd()).isEqualTo("DELINQUENCY_STAGE_ADVANCED");
        assertThat(captor.getValue().reportPayload())
                .contains("\"fromStage\":\"STAGE_1\"").contains("\"toStage\":\"STAGE_2\"");
    }

    @Test
    void stage_advanced_STAGE_1_진입은_신고_없음() {
        listener.onStageAdvanced(new DelinquencyStageAdvancedEvent(
                100L, 9L, Delinquency.STAGE_0, Delinquency.STAGE_1, 5));

        verify(reportService, never()).submit(anyLong(), anyLong(), any(SubmitReportRequest.class));
    }

    @Test
    void onResolved_는_RESOLUTION_으로_적재() {
        listener.onResolved(new DelinquencyResolvedEvent(100L, 9L, OffsetDateTime.now()));

        ArgumentCaptor<SubmitReportRequest> captor = ArgumentCaptor.forClass(SubmitReportRequest.class);
        verify(reportService).submit(eqLong(100L), eqLong(9L), captor.capture());
        assertThat(captor.getValue().reportTypeCd()).isEqualTo("RESOLUTION");
        assertThat(captor.getValue().reportReasonCd()).isEqualTo("DELINQUENCY_RESOLVED");
    }

    private static long eqLong(long v) { return org.mockito.ArgumentMatchers.eq(v); }
}
