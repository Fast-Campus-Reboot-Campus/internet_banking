package com.bank.customer.customer.dto;

/**
 * 예금주 확인 응답 — 서비스 간 내부 호출용(A-2 흐름).
 *
 * <p>deposit-service 가 Feign 으로 호출해 계좌 예금주 실명·사망 여부를 채우고,
 * payment-service 는 이 값으로 이체 전 예금주 일치 여부를 검증한다.
 *
 * @param customerId   요청한 고객 ID(문자열)
 * @param holderName   예금주 실명 (party.party_name)
 * @param holderType   예금주 유형 INDIVIDUAL / CORPORATE (party_type_code 매핑, Party 모델에 JOINT 없음)
 * @param deceasedFlag 사망 여부 — Party 에 사망 컬럼이 없어 현재 false 고정(추후 wiring)
 */
public record HolderInfoResponse(
        String  customerId,
        String  holderName,
        String  holderType,
        boolean deceasedFlag
) {
}
