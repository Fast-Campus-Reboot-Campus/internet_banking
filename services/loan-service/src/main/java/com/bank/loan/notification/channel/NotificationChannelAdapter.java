package com.bank.loan.notification.channel;

import com.bank.loan.notification.outbox.NotificationOutbox;

/**
 * 알림 채널 어댑터 SPI. dispatch 배치가 channelCd 매칭해 호출한다.
 *
 * 실 구현체(SMS 게이트웨이, 카카오 알림톡 SDK, 메일 서버, 푸시 서버)는 본 인터페이스만 채우면 된다.
 * 도메인 트랜잭션은 본 인터페이스 어디서도 호출되지 않는다 (AI_GUIDELINES).
 */
public interface NotificationChannelAdapter {

    /** outbox.channel_cd 와 1:1. SMS / KAKAO_ALIMTALK / EMAIL / APP_PUSH. */
    String getChannelCd();

    SendResult send(NotificationOutbox row);

    /**
     *   success         true 면 SENT 로 전이, false 면 FAILED + attemptNo++
     *   providerMsgId   외부 사업자 트래킹 ID (감사용)
     *   responseCode    외부 응답 코드
     *   responseMessage 사람이 읽는 메시지. 실패 시 outbox.lastError 로 기록.
     */
    record SendResult(boolean success, String providerMsgId, String responseCode, String responseMessage) {}
}
