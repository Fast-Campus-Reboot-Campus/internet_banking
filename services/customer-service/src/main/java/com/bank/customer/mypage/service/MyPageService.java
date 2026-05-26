package com.bank.customer.mypage.service;

import com.bank.common.web.BusinessException;
import com.bank.customer.customer.domain.Credential;
import com.bank.customer.customer.domain.Customer;
import com.bank.customer.customer.repository.CredentialRepository;
import com.bank.customer.customer.repository.CustomerRepository;
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

    private final CustomerRepository customerRepository;
    private final PartyRepository partyRepository;
    private final PartyPersonRepository partyPersonRepository;
    private final CredentialRepository credentialRepository;

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
                customer.getJoinedAt()
        );
    }
}
