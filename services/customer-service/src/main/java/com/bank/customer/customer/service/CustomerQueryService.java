package com.bank.customer.customer.service;

import com.bank.customer.customer.dto.CustomerSummaryResponse;
import com.bank.customer.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 직원용 고객 조회(목록·검색) 서비스. 상태 전이·이력은 {@link CustomerLifecycleService}가 담당하고,
 * 본 서비스는 읽기 전용 진입점만 제공한다.
 */
@Service
@RequiredArgsConstructor
public class CustomerQueryService {

    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public Page<CustomerSummaryResponse> searchCustomers(
            String keyword, String status, String grade, Pageable pageable) {
        // 정렬은 JPQL의 ORDER BY(customer_id DESC)로 고정 — 클라이언트 sort와 충돌하지 않도록 제거
        Pageable paging = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return customerRepository.searchCustomers(
                normalize(keyword), normalize(status), normalize(grade), paging);
    }

    /** 공백·빈 문자열은 "조건 없음"으로 취급해 null로 정규화한다. */
    private String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
