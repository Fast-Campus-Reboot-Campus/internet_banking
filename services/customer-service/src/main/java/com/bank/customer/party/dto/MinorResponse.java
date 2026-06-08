package com.bank.customer.party.dto;

/**
 * 미성년(만 19세 미만) 행 — 미성년 검토 화면(/admin/minor)의 진입점.
 *
 * <p>party_person.birth_date로 만 19세 미만인 개인 party를 이름과 함께 반환한다(신규 테이블 없음).
 * 법정대리인 관계는 기존 {@code GET /internal/party/{partyId}/relations}로 조회한다(PARENT 관계 재사용).
 * 검토상태(검토대기/거절) 워크플로는 별도 스키마 신설이 필요해 본 목록에는 포함하지 않는다.
 */
public record MinorResponse(
        Long   partyId,
        String partyName,
        String birthDate,
        String genderCode,
        String nationalityCode
) {
}
