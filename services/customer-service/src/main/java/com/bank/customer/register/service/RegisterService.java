package com.bank.customer.register.service;

import com.bank.common.web.BusinessException;
import com.bank.customer.customer.domain.Credential;
import com.bank.customer.customer.domain.Customer;
import com.bank.customer.customer.domain.CustomerGradeHistory;
import com.bank.customer.customer.domain.CustomerStatusHistory;
import com.bank.customer.customer.repository.CredentialRepository;
import com.bank.customer.customer.repository.CustomerGradeHistoryRepository;
import com.bank.customer.customer.repository.CustomerRepository;
import com.bank.customer.customer.repository.CustomerStatusHistoryRepository;
import com.bank.customer.party.domain.ComplianceInfo;
import com.bank.customer.party.domain.Party;
import com.bank.customer.party.domain.PartyPerson;
import com.bank.customer.party.domain.PartyRole;
import com.bank.customer.party.repository.ComplianceInfoRepository;
import com.bank.customer.party.repository.PartyPersonRepository;
import com.bank.customer.party.repository.PartyRepository;
import com.bank.customer.party.repository.PartyRoleRepository;
import com.bank.customer.register.dto.RegisterRequest;
import com.bank.customer.register.dto.RegisterResponse;
import com.bank.customer.support.CustomerErrorCode;
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

    private static final DateTimeFormatter DATE_FMT      = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int               MAX_PWD_FAIL  = 5;

    private final PartyRepository                  partyRepository;
    private final PartyPersonRepository            partyPersonRepository;
    private final PartyRoleRepository              partyRoleRepository;
    private final ComplianceInfoRepository         complianceInfoRepository;
    private final CustomerRepository               customerRepository;
    private final CredentialRepository             credentialRepository;
    private final CustomerStatusHistoryRepository  customerStatusHistoryRepository;
    private final CustomerGradeHistoryRepository   customerGradeHistoryRepository;
    private final PasswordEncoder                  passwordEncoder;

    public RegisterResponse register(RegisterRequest request) {

        if (credentialRepository.existsByLoginIdAndDeletedAtIsNull(request.loginId())) {
            throw new BusinessException(CustomerErrorCode.CUST_001);
        }

        OffsetDateTime now   = OffsetDateTime.now();
        String         today = now.format(DATE_FMT);

        // 1) Party
        Party party = partyRepository.save(Party.builder()
                .partyTypeCode(Party.TYPE_PERSONAL)
                .partyName(request.name())
                .partyStatusCode(Party.STATUS_ACTIVE)
                .build());

        // 2) PartyPerson
        partyPersonRepository.save(PartyPerson.builder()
                .partyId(party.getPartyId())
                .birthDate(request.birthDate())
                .genderCode(request.genderCode())
                .isPepYn("F")
                .build());

        // 3) PartyRole — CUSTOMER 역할 부여 (법인과 동일)
        partyRoleRepository.save(PartyRole.builder()
                .partyId(party.getPartyId())
                .roleTypeCode(PartyRole.TYPE_CUSTOMER)
                .roleStatusCode(PartyRole.STATUS_ACTIVE)
                .roleStartDate(today)
                .build());

        // 4) ComplianceInfo — AML/KYC 초기화 (법인과 동일)
        complianceInfoRepository.save(ComplianceInfo.builder()
                .partyId(party.getPartyId())
                .amlRiskLevelCode(ComplianceInfo.AML_LOW)
                .isOfacSanctionedYn("F")
                .isUnSanctionedYn("F")
                .isEuSanctionedYn("F")
                .isKrSanctionedYn("F")
                .kycStatusCode(ComplianceInfo.KYC_PENDING)
                .cddLevelCode(ComplianceInfo.CDD_STANDARD)
                .eddRequiredYn("F")
                .fatcaStatusCode(ComplianceInfo.FATCA_PENDING)
                .fatcaReportableYn("F")
                .crsStatusCode(ComplianceInfo.CRS_PENDING)
                .crsReportableYn("F")
                .build());

        // 5) Customer
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

        // 6) Credential
        credentialRepository.save(Credential.builder()
                .customerId(customer.getCustomerId())
                .loginId(request.loginId())
                .passwordHash(passwordEncoder.encode(request.password()))
                .accountStatusCode(Credential.STATUS_ACTIVE)
                .passwordLoginFailureCount(0)
                .maxPasswordLoginFailureCount(MAX_PWD_FAIL)
                .passwordChangedAt(now)
                .build());

        // 7) 이력
        customerStatusHistoryRepository.save(
                CustomerStatusHistory.ofInitial(customer.getCustomerId(), now));
        customerGradeHistoryRepository.save(
                CustomerGradeHistory.ofInitial(customer.getCustomerId(), Customer.GRADE_NORMAL, today, now));

        return new RegisterResponse(customer.getCustomerId(), request.loginId());
    }
}
