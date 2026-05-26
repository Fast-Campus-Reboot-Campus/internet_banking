package com.bank.customer.register.service;

import com.bank.common.web.BusinessException;
import com.bank.customer.customer.domain.Credential;
import com.bank.customer.customer.domain.Customer;
import com.bank.customer.customer.domain.CustomerStatusHistory;
import com.bank.customer.customer.repository.CredentialRepository;
import com.bank.customer.customer.repository.CustomerRepository;
import com.bank.customer.customer.repository.CustomerStatusHistoryRepository;
import com.bank.customer.party.domain.Party;
import com.bank.customer.party.domain.PartyPerson;
import com.bank.customer.party.repository.PartyPersonRepository;
import com.bank.customer.party.repository.PartyRepository;
import com.bank.customer.register.dto.RegisterRequest;
import com.bank.customer.register.dto.RegisterResponse;
import com.bank.customer.support.CustomerErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Transactional
public class RegisterService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int MAX_PASSWORD_FAILURE = 5;

    private final PartyRepository partyRepository;
    private final PartyPersonRepository partyPersonRepository;
    private final CustomerRepository customerRepository;
    private final CredentialRepository credentialRepository;
    private final CustomerStatusHistoryRepository customerStatusHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final MeterRegistry meterRegistry;

    public RegisterResponse register(RegisterRequest request) {

        if (credentialRepository.existsByLoginIdAndDeletedAtIsNull(request.loginId())) {
            meterRegistry.counter("customer.register.failure", "reason", "duplicate_login_id").increment();
            throw new BusinessException(CustomerErrorCode.CUST_001);
        }

        OffsetDateTime now = OffsetDateTime.now();
        String today = now.format(DATE_FMT);

        Party party = partyRepository.save(Party.builder()
                .partyTypeCode(Party.TYPE_PERSONAL)
                .partyName(request.name())
                .partyStatusCode(Party.STATUS_ACTIVE)
                .build());

        partyPersonRepository.save(PartyPerson.builder()
                .partyId(party.getPartyId())
                .birthDate(request.birthDate())
                .genderCode(request.genderCode())
                .isPepYn("F")
                .build());

        Customer customer = customerRepository.save(Customer.builder()
                .partyId(party.getPartyId())
                .customerStatusCode(Customer.STATUS_ACTIVE)
                .customerGradeCode(Customer.GRADE_NORMAL)
                .mainCustomerYn("F")
                .smsReceiveYn("F")
                .emailReceiveYn("F")
                .postalReceiveYn("F")
                .joinChannelCode("ONLINE")
                .firstJoinDate(today)
                .joinedAt(now)
                .phone(request.phone())
                .email(request.email())
                .build());

        credentialRepository.save(Credential.builder()
                .customerId(customer.getCustomerId())
                .loginId(request.loginId())
                .passwordHash(passwordEncoder.encode(request.password()))
                .accountStatusCode(Credential.STATUS_ACTIVE)
                .passwordLoginFailureCount(0)
                .maxPasswordLoginFailureCount(MAX_PASSWORD_FAILURE)
                .passwordChangedAt(now)
                .build());

        customerStatusHistoryRepository.save(
                CustomerStatusHistory.ofInitial(customer.getCustomerId(), now));

        meterRegistry.counter("customer.register.success").increment();
        return new RegisterResponse(customer.getCustomerId(), request.loginId());
    }
}
