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

        Credential credential = credentialRepository
                .findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_002));

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
                credential.getLoginId(),
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
                credential.getPasswordLastLoginAt(),
                latestGrade,
                latestStatus
        );
    }
}
