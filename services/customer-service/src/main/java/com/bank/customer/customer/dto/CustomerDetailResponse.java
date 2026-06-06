package com.bank.customer.customer.dto;

import com.bank.customer.customer.domain.Customer;
import com.bank.customer.party.domain.Party;
import com.bank.customer.party.domain.PartyPerson;

import java.time.OffsetDateTime;

/**
 * 직원용 고객(회원) 상세 — 회원 상세 화면(/admin/members/[id])의 데이터원.
 *
 * <p>한 사람의 정보가 세 테이블에 흩어져 있어 합쳐서 반환한다:
 * <ul>
 *   <li>customer  — 등급·신용·상태·연락처·생애주기 일자</li>
 *   <li>party      — 이름·party 상태</li>
 *   <li>party_person — 생년월일·성별·국적·PEP 여부 (개인 한정, 없으면 null)</li>
 * </ul>
 *
 * <p>주민등록번호(rrn_encrypted)는 암호화 저장이라 본 응답에서 제외한다 — 복호화·마스킹·조회사유·
 * 감사로그가 필요한 별도 엔드포인트로 분리(PII 최소노출). 보유 계좌·잔액(deposit), 동의이력은
 * 타 도메인이라 화면에서 별도 호출한다.
 */
public record CustomerDetailResponse(
        // ── customer ──
        Long           customerId,
        Long           partyId,
        String         customerGradeCode,
        String         creditRatingCode,
        String         creditEvaluationDate,
        String         customerStatusCode,
        String         email,
        String         phone,
        String         zipCode,
        String         address,
        String         addressDetail,
        String         joinChannelCode,
        String         firstJoinDate,
        OffsetDateTime joinedAt,
        OffsetDateTime lastTransactionAt,
        OffsetDateTime dormantAt,
        OffsetDateTime closedAt,
        // ── party ──
        String         partyName,
        String         partyStatusCode,
        // ── party_person (개인 한정, 없으면 null) ──
        String         birthDate,
        String         genderCode,
        String         nationalityCode,
        Boolean        pep
) {
    public static CustomerDetailResponse of(Customer c, Party party, PartyPerson person) {
        return new CustomerDetailResponse(
                c.getCustomerId(), c.getPartyId(),
                c.getCustomerGradeCode(), c.getCreditRatingCode(), c.getCreditEvaluationDate(),
                c.getCustomerStatusCode(),
                c.getEmail(), c.getPhone(),
                c.getZipCode(), c.getAddress(), c.getAddressDetail(),
                c.getJoinChannelCode(), c.getFirstJoinDate(),
                c.getJoinedAt(), c.getLastTransactionAt(), c.getDormantAt(), c.getClosedAt(),
                party != null ? party.getPartyName()       : null,
                party != null ? party.getPartyStatusCode() : null,
                person != null ? person.getBirthDate()       : null,
                person != null ? person.getGenderCode()      : null,
                person != null ? person.getNationalityCode() : null,
                person != null ? person.isPep()              : null);
    }
}
