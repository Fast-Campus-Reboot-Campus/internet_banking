package com.bank.customer.party.service;

import com.bank.common.web.BusinessException;
import com.bank.customer.customer.domain.Customer;
import com.bank.customer.customer.repository.CustomerRepository;
import com.bank.customer.party.domain.ForeignerInfo;
import com.bank.customer.party.domain.PartyPerson;
import com.bank.customer.party.domain.TaxResidencyInfo;
import com.bank.customer.party.dto.AddTaxResidencyRequest;
import com.bank.customer.party.dto.ForeignerInfoResponse;
import com.bank.customer.party.dto.PersonInfoResponse;
import com.bank.customer.party.dto.TaxResidencyResponse;
import com.bank.customer.party.dto.UpdatePersonInfoRequest;
import com.bank.customer.party.repository.ForeignerInfoRepository;
import com.bank.customer.party.repository.PartyPersonRepository;
import com.bank.customer.party.repository.TaxResidencyInfoRepository;
import com.bank.customer.support.CustomerErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonInfoService {

    private final CustomerRepository        customerRepository;
    private final PartyPersonRepository     partyPersonRepository;
    private final ForeignerInfoRepository   foreignerInfoRepository;
    private final TaxResidencyInfoRepository taxResidencyInfoRepository;

    // ── PartyPerson ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PersonInfoResponse getPersonInfo(Long customerId) {
        Long partyId = resolvePartyId(customerId);
        PartyPerson person = partyPersonRepository.findByPartyIdAndDeletedAtIsNull(partyId)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_002));
        return PersonInfoResponse.from(person);
    }

    @Transactional
    public PersonInfoResponse updatePersonInfo(Long customerId, UpdatePersonInfoRequest req) {
        Long partyId = resolvePartyId(customerId);
        PartyPerson person = partyPersonRepository.findByPartyIdAndDeletedAtIsNull(partyId)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_002));
        person.updatePersonalInfo(
                req.occupationCode(), req.occupationName(), req.workplaceName(),
                req.annualIncomeAmount(), req.incomeProofCode(), req.maritalStatusCode());
        return PersonInfoResponse.from(person);
    }

    // ── ForeignerInfo ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ForeignerInfoResponse getForeignerInfo(Long customerId) {
        Long partyId = resolvePartyId(customerId);
        ForeignerInfo info = foreignerInfoRepository.findById(partyId)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_120));
        return ForeignerInfoResponse.from(info);
    }

    @Transactional
    public ForeignerInfoResponse updatePassport(Long customerId,
                                                String passportNo, String countryCode, String expiryDate) {
        Long partyId = resolvePartyId(customerId);
        ForeignerInfo info = foreignerInfoRepository.findById(partyId)
                .orElseGet(() -> foreignerInfoRepository.save(
                        ForeignerInfo.builder().partyId(partyId).build()));
        info.updatePassport(passportNo, countryCode, expiryDate);
        return ForeignerInfoResponse.from(info);
    }

    @Transactional
    public ForeignerInfoResponse updateStay(Long customerId,
                                            String stayQualificationCode, String stayExpiryDate) {
        Long partyId = resolvePartyId(customerId);
        ForeignerInfo info = foreignerInfoRepository.findById(partyId)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_120));
        info.updateStay(stayQualificationCode, stayExpiryDate);
        return ForeignerInfoResponse.from(info);
    }

    // ── TaxResidencyInfo ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TaxResidencyResponse> getTaxResidencies(Long customerId) {
        Long partyId = resolvePartyId(customerId);
        return taxResidencyInfoRepository.findByPartyIdAndDeletedAtIsNull(partyId)
                .stream().map(TaxResidencyResponse::from).toList();
    }

    @Transactional
    public TaxResidencyResponse addTaxResidency(Long customerId, AddTaxResidencyRequest req) {
        Long partyId = resolvePartyId(customerId);
        TaxResidencyInfo info = taxResidencyInfoRepository.save(TaxResidencyInfo.builder()
                .partyId(partyId)
                .residentTypeCode(req.residentTypeCode())
                .taxCountryCode(req.taxCountryCode())
                .foreignTin(req.foreignTin())
                .withholdingRateBps(req.withholdingRateBps())
                .taxResidencyConfirmDate(req.taxResidencyConfirmDate())
                .build());
        return TaxResidencyResponse.from(info);
    }

    @Transactional
    public void deleteTaxResidency(Long customerId, Long taxResidencyId) {
        Long partyId = resolvePartyId(customerId);
        TaxResidencyInfo info = taxResidencyInfoRepository.findById(taxResidencyId)
                .filter(t -> t.getPartyId().equals(partyId) && t.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_121));
        info.softDelete(customerId);
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private Long resolvePartyId(Long customerId) {
        return customerRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .map(Customer::getPartyId)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_002));
    }
}
