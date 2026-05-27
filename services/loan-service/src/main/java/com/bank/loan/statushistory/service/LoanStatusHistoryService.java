package com.bank.loan.statushistory.service;

import com.bank.common.audit.StatusHistoryRepository;
import com.bank.loan.statushistory.dto.StatusHistoryListResponse;
import com.bank.loan.statushistory.dto.StatusHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * status_history 조회 서비스.
 *
 * loan-service 도메인 한정이므로 targetDomainCd 는 LOAN 으로 고정한다.
 * (분산 보관 정책: 각 도메인 DB 가 자체 status_history 를 보유 — StatusHistory javadoc)
 *
 * 적재 진입점은 com.bank.common.audit.StatusHistoryPublisher 다.
 */
@Service
@RequiredArgsConstructor
public class LoanStatusHistoryService {

    private static final String DOMAIN_CD = "LOAN";

    private final StatusHistoryRepository repository;

    @Transactional(readOnly = true)
    public StatusHistoryListResponse list(String targetTableCd, Long targetId) {
        List<StatusHistoryResponse> items = repository
                .findByTargetDomainCdAndTargetTableCdAndTargetIdOrderByChangedAtAsc(
                        DOMAIN_CD, targetTableCd, targetId)
                .stream()
                .map(StatusHistoryResponse::of)
                .toList();
        return StatusHistoryListResponse.of(DOMAIN_CD, targetTableCd, targetId, items);
    }
}
