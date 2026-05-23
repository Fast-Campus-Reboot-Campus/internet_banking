package com.bank.loan.notification.event;

/**
 * 보증 약정 취소 도메인 이벤트.
 *
 * GuarantorAgreementService.cancel() 에서 상태 전이 직후 발행.
 * prevStatusCd 가 SIGNED 인 경우 약정·실행 사전조건(LOAN_176) 위반 가능성이 생기므로
 * GuarantorNotificationListener 가 운영자 알람 outbox 를 적재한다.
 */
public record GuarantorCanceledEvent(
        Long applId,
        Long gagrId,
        String prevStatusCd
) {
}
