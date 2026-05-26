package com.bank.loan.creditreport.channel;

import com.bank.loan.creditreport.domain.CreditInfoReport;

/**
 * 외부 신용평가기관(KCB/NICE/...) 어댑터 SPI.
 *
 * 실 구현체는 SDK·HTTP 호출을 책임진다. dispatch 배치가 본 인터페이스 한 곳만 호출하면
 * 도메인 트랜잭션은 외부 RTT 에 의존하지 않는다 (AI_GUIDELINES: 트랜잭션 내 외부 API 금지).
 */
public interface CreditInfoReportChannelAdapter {

    /** 매핑 키. CreditInfoReport.crptAgencyCd 와 1:1 (KCB / NICE / ...). */
    String getAgencyCd();

    SendResult send(CreditInfoReport report);

    /**
     * 외부 전송 결과.
     *
     * success      true 면 SENT 로 전이, false 면 FAILED + outbox attemptNo 증가
     * externalTxNo 외부 기관이 반환한 트래킹 번호 (없으면 자체 채번 유지)
     * responseCode 외부 기관 응답 코드 (관측/감사용)
     * responseMessage 사람이 읽는 메시지 (실패 시 lastError 로 저장 — PII 마스킹은 호출자 책임)
     */
    record SendResult(boolean success, String externalTxNo, String responseCode, String responseMessage) {}
}
