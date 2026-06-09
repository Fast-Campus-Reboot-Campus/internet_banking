package com.bank.customer.customer.service;

import com.bank.common.web.BusinessException;
import com.bank.customer.customer.domain.Customer;
import com.bank.customer.customer.dto.CustomerDetailResponse;
import com.bank.customer.customer.dto.CustomerSummaryResponse;
import com.bank.customer.customer.dto.JoinStatsResponse;
import com.bank.customer.customer.repository.CustomerRepository;
import com.bank.customer.party.domain.Party;
import com.bank.customer.party.domain.PartyPerson;
import com.bank.customer.party.repository.PartyPersonRepository;
import com.bank.customer.party.repository.PartyRepository;
import com.bank.customer.support.CustomerErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 직원용 고객 조회(목록·검색·상세) 서비스. 상태 전이·이력은 {@link CustomerLifecycleService}가 담당하고,
 * 본 서비스는 읽기 전용 진입점만 제공한다.
 */
@Service
@RequiredArgsConstructor
public class CustomerQueryService {

    private final CustomerRepository    customerRepository;
    private final PartyRepository       partyRepository;
    private final PartyPersonRepository partyPersonRepository;

    @Transactional(readOnly = true)
    public Page<CustomerSummaryResponse> searchCustomers(
            String keyword, String status, String grade, Pageable pageable) {
        // 정렬은 JPQL의 ORDER BY(customer_id DESC)로 고정 — 클라이언트 sort와 충돌하지 않도록 제거
        Pageable paging = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return customerRepository.searchCustomers(
                normalize(keyword), normalize(status), normalize(grade), paging);
    }

    /**
     * 고객(회원) 상세. 한 사람의 정보가 customer·party·party_person 세 테이블에 흩어져 있어 합친다.
     * party_person은 개인 한정이라 없을 수 있으며(법인 등) 그 경우 인적사항은 null로 채운다.
     */
    @Transactional(readOnly = true)
    public CustomerDetailResponse getCustomerDetail(Long customerId) {
        Customer customer = customerRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_002));
        Party party = partyRepository.findByPartyIdAndDeletedAtIsNull(customer.getPartyId())
                .orElse(null);
        PartyPerson person = partyPersonRepository.findByPartyIdAndDeletedAtIsNull(customer.getPartyId())
                .orElse(null);
        return CustomerDetailResponse.of(customer, party, person);
    }

    /** 가입 현황 통계 — customer 집계(총원·오늘/이번달 가입·상태/등급/채널별 분포). 가입 대시보드의 데이터원. */
    @Transactional(readOnly = true)
    public JoinStatsResponse getJoinStats() {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        OffsetDateTime todayStart = OffsetDateTime.now(zone).toLocalDate()
                .atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime monthStart = OffsetDateTime.now(zone).toLocalDate()
                .withDayOfMonth(1).atStartOfDay(zone).toOffsetDateTime();

        return new JoinStatsResponse(
                customerRepository.countByDeletedAtIsNull(),
                customerRepository.countByJoinedAtGreaterThanEqualAndDeletedAtIsNull(todayStart),
                customerRepository.countByJoinedAtGreaterThanEqualAndDeletedAtIsNull(monthStart),
                toCodeCounts(customerRepository.countByStatus()),
                toCodeCounts(customerRepository.countByGrade()),
                toCodeCounts(customerRepository.countByChannel()));
    }

    private List<JoinStatsResponse.CodeCount> toCodeCounts(List<CustomerRepository.CodeCount> rows) {
        return rows.stream()
                .map(r -> new JoinStatsResponse.CodeCount(r.getCode(), r.getCount()))
                .toList();
    }

    /** 공백·빈 문자열은 "조건 없음"으로 취급해 null로 정규화한다. */
    private String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
