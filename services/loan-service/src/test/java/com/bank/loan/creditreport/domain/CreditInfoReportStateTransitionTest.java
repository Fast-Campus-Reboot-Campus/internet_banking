package com.bank.loan.creditreport.domain;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * plan 02 step 1: CreditInfoReport 상태 머신 단위 검증.
 *
 * markSent / markAcked / markFailed / markDead / markRequeued 가 status 와
 * 관련 컬럼(reported_at, ack_at, external_tx_no) 을 의도대로 갱신하는지 확인.
 */
class CreditInfoReportStateTransitionTest {

    private CreditInfoReport newRequested() {
        return CreditInfoReport.builder()
                .cntrId(1L)
                .customerId(1L)
                .crptTypeCd("DELINQUENCY")
                .crptAgencyCd("KCB")
                .crptStatusCd(CreditInfoReport.STATUS_REQUESTED)
                .reportTargetCd("EXISTING")
                .build();
    }

    @Test
    void markSent_은_externalTxNo_와_reportedAt_을_채운다() {
        CreditInfoReport r = newRequested();
        OffsetDateTime now = OffsetDateTime.now();

        r.markSent("TX-001", now);

        assertThat(r.currentStatus()).isEqualTo(CreditInfoReport.STATUS_SENT);
        assertThat(r.getExternalTxNo()).isEqualTo("TX-001");
        assertThat(r.getReportedAt()).isEqualTo(now);
    }

    @Test
    void markAcked_는_status_ACKED_ackAt_externalAckNo_채움() {
        CreditInfoReport r = newRequested();
        r.markSent("TX-001", OffsetDateTime.now());
        OffsetDateTime ackedAt = OffsetDateTime.now();

        r.markAcked(ackedAt, "ACK-001");

        assertThat(r.currentStatus()).isEqualTo(CreditInfoReport.STATUS_ACKED);
        assertThat(r.getAckAt()).isEqualTo(ackedAt);
        assertThat(r.getExternalAckNo()).isEqualTo("ACK-001");
    }

    @Test
    void markFailed_는_FAILED_로만_전이() {
        CreditInfoReport r = newRequested();
        r.markFailed();
        assertThat(r.currentStatus()).isEqualTo(CreditInfoReport.STATUS_FAILED);
    }

    @Test
    void markDead_는_DEAD_로_전이() {
        CreditInfoReport r = newRequested();
        r.markFailed();
        r.markDead();
        assertThat(r.currentStatus()).isEqualTo(CreditInfoReport.STATUS_DEAD);
    }

    @Test
    void markRequeued_는_REQUESTED_복귀_externalTxNo_와_reportedAt_초기화() {
        CreditInfoReport r = newRequested();
        r.markSent("TX-001", OffsetDateTime.now());
        r.markFailed();

        r.markRequeued();

        assertThat(r.currentStatus()).isEqualTo(CreditInfoReport.STATUS_REQUESTED);
        assertThat(r.getExternalTxNo()).isNull();
        assertThat(r.getReportedAt()).isNull();
    }
}
