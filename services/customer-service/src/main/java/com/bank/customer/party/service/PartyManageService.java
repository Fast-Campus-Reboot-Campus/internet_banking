package com.bank.customer.party.service;

import com.bank.common.web.BusinessException;
import com.bank.customer.customer.domain.Customer;
import com.bank.customer.customer.repository.CustomerRepository;
import com.bank.customer.party.domain.ComplianceInfo;
import com.bank.customer.party.domain.PartyRelation;
import com.bank.customer.party.domain.PartyRole;
import com.bank.customer.party.dto.AddPartyRelationRequest;
import com.bank.customer.party.dto.PartyRelationResponse;
import com.bank.customer.party.dto.PartyRoleResponse;
import com.bank.customer.party.repository.ComplianceInfoRepository;
import com.bank.customer.party.repository.PartyRelationRepository;
import com.bank.customer.party.repository.PartyRoleRepository;
import com.bank.customer.support.CustomerErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PartyManageService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PartyRoleRepository      partyRoleRepository;
    private final PartyRelationRepository  partyRelationRepository;
    private final ComplianceInfoRepository complianceInfoRepository;
    private final CustomerRepository       customerRepository;

    // ── 역할 관리 ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PartyRoleResponse> getRoles(Long partyId) {
        return partyRoleRepository.findByPartyIdAndDeletedAtIsNull(partyId)
                .stream().map(PartyRoleResponse::from).toList();
    }

    @Transactional
    public PartyRoleResponse addRole(Long partyId, String roleTypeCode, String startDate) {
        partyRoleRepository.findByPartyIdAndRoleTypeCodeAndRoleStatusCodeAndDeletedAtIsNull(
                partyId, roleTypeCode, PartyRole.STATUS_ACTIVE)
                .ifPresent(r -> { throw new BusinessException(CustomerErrorCode.CUST_110); });

        PartyRole role = partyRoleRepository.save(PartyRole.builder()
                .partyId(partyId)
                .roleTypeCode(roleTypeCode)
                .roleStatusCode(PartyRole.STATUS_ACTIVE)
                .roleStartDate(startDate != null ? startDate : LocalDate.now().format(DATE_FMT))
                .build());
        return PartyRoleResponse.from(role);
    }

    @Transactional
    public PartyRoleResponse closeRole(Long roleId, String endDate, String reasonCode) {
        PartyRole role = partyRoleRepository.findById(roleId)
                .filter(r -> r.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_111));
        role.close(endDate != null ? endDate : LocalDate.now().format(DATE_FMT), reasonCode);
        return PartyRoleResponse.from(role);
    }

    // ── 관계 관리 ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PartyRelationResponse> getRelations(Long partyId) {
        return partyRelationRepository.findAllActiveRelations(partyId)
                .stream().map(PartyRelationResponse::from).toList();
    }

    @Transactional
    public PartyRelationResponse addRelation(Long fromPartyId, AddPartyRelationRequest req) {
        if (fromPartyId.equals(req.toPartyId())) {
            throw new BusinessException(CustomerErrorCode.CUST_112);
        }
        if (partyRelationRepository
                .existsByFromPartyIdAndToPartyIdAndRelationTypeCodeAndRelationEndDateIsNullAndDeletedAtIsNull(
                        fromPartyId, req.toPartyId(), req.relationTypeCode())) {
            throw new BusinessException(CustomerErrorCode.CUST_113);
        }

        PartyRelation relation = partyRelationRepository.save(PartyRelation.builder()
                .fromPartyId(fromPartyId)
                .toPartyId(req.toPartyId())
                .relationTypeCode(req.relationTypeCode())
                .relationDetailCode(req.relationDetailCode())
                .equityRatioBps(req.equityRatioBps())
                .representationScope(req.representationScope())
                .proofUrl(req.proofUrl())
                .relationStartDate(req.relationStartDate())
                .build());
        return PartyRelationResponse.from(relation);
    }

    @Transactional
    public PartyRelationResponse endRelation(Long relationId, String endDate, String reasonCode) {
        PartyRelation relation = partyRelationRepository.findById(relationId)
                .filter(r -> r.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_114));
        relation.end(endDate != null ? endDate : LocalDate.now().format(DATE_FMT), reasonCode);
        return PartyRelationResponse.from(relation);
    }

    // ── 컴플라이언스 조회/수정 (직원용) ────────────────────────────────────────

    @Transactional(readOnly = true)
    public ComplianceInfo getCompliance(Long partyId) {
        return complianceInfoRepository.findByPartyIdAndDeletedAtIsNull(partyId)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_115));
    }

    @Transactional
    public void updateAmlRisk(Long partyId, String riskLevel) {
        ComplianceInfo info = complianceInfoRepository.findByPartyIdAndDeletedAtIsNull(partyId)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_115));
        info.updateAmlRisk(riskLevel);
    }

    @Transactional
    public void completeKyc(Long partyId, String expiryDate, String methodCode) {
        ComplianceInfo info = complianceInfoRepository.findByPartyIdAndDeletedAtIsNull(partyId)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_115));
        info.completeKyc(expiryDate, methodCode);
    }

    /** 고객 ID → partyId 변환 헬퍼 */
    public Long resolvePartyId(Long customerId) {
        return customerRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .map(Customer::getPartyId)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_002));
    }
}
