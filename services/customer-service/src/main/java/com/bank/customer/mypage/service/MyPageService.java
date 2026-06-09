package com.bank.customer.mypage.service;

import com.bank.common.web.BusinessException;
import com.bank.customer.customer.domain.Credential;
import com.bank.customer.customer.domain.Customer;
import com.bank.customer.customer.repository.CredentialRepository;
import com.bank.customer.customer.repository.CustomerGradeHistoryRepository;
import com.bank.customer.customer.repository.CustomerRepository;
import com.bank.customer.customer.repository.CustomerStatusHistoryRepository;
import com.bank.customer.mypage.dto.MyPageResponse;
import com.bank.customer.party.domain.Party;
import com.bank.customer.party.domain.PartyPerson;
import com.bank.customer.party.repository.PartyPersonRepository;
import com.bank.customer.party.repository.PartyRepository;
import com.bank.customer.support.CustomerErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final CustomerRepository              customerRepository;
    private final PartyRepository                 partyRepository;
    private final PartyPersonRepository           partyPersonRepository;
    private final CredentialRepository            credentialRepository;
    private final CustomerGradeHistoryRepository  gradeHistoryRepository;
    private final CustomerStatusHistoryRepository statusHistoryRepository;

    public MyPageResponse getMyPage(Long customerId) {

        Customer customer = customerRepository
                .findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_002));

        if (!customer.isActive()) {
            throw new BusinessException(CustomerErrorCode.CUST_012);
        }

        Party party = partyRepository
                .findByPartyIdAndDeletedAtIsNull(customer.getPartyId())
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_002));

        PartyPerson partyPerson = partyPersonRepository
                .findByPartyIdAndDeletedAtIsNull(customer.getPartyId())
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_002));

        // credential(ID/PW)은 loginId·최근 로그인 시각 표시에만 쓰는 비핵심 정보다.
        // 인증서 전용 고객(certPinHash로 로그인, ID/PW 미발급)은 credential 이 없을 수 있으므로
        // 이력 조회처럼 없으면 null 로 두고 마이페이지 응답 자체는 막지 않는다.
        Credential credential = credentialRepository
                .findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElse(null);

        // 최근 등급 이력 (가입 초기 이력 제외 — previousGradeCode 가 null 이 아닌 첫 번째)
        MyPageResponse.GradeInfo latestGrade = gradeHistoryRepository
                .findTopByCustomerIdOrderByCustomerGradeHistoryIdDesc(customerId)
                .filter(h -> h.getPreviousCustomerGradeCode() != null)
                .map(h -> new MyPageResponse.GradeInfo(
                        h.getPreviousCustomerGradeCode(),
                        h.getCustomerGradeCode(),
                        h.getCustomerGradeChangeReasonCode(),
                        h.getCustomerGradeEffectiveStartDate()))
                .orElse(null);

        // 최근 상태 이력 (가입 초기 이력 제외)
        MyPageResponse.StatusInfo latestStatus = statusHistoryRepository
                .findTopByCustomerIdOrderByCustomerStatusHistoryIdDesc(customerId)
                .filter(h -> h.getPreviousCustomerStatusCode() != null)
                .map(h -> new MyPageResponse.StatusInfo(
                        h.getPreviousCustomerStatusCode(),
                        h.getCustomerStatusCode(),
                        h.getCustomerStatusChangeReasonCode(),
                        h.getCustomerStatusEffectiveStartAt()))
                .orElse(null);

        return new MyPageResponse(
                customer.getCustomerId(),
                credential != null ? credential.getLoginId() : null,
                party.getPartyName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getZipCode(),
                customer.getAddress(),
                customer.getAddressDetail(),
                partyPerson.getBirthDate(),
                partyPerson.getGenderCode(),
                customer.getCustomerGradeCode(),
                customer.getCustomerStatusCode(),
                customer.getCreditRatingCode(),
                customer.getJoinedAt(),
                credential != null ? credential.getPasswordLastLoginAt() : null,
                latestGrade,
                latestStatus
        );
    }
}
