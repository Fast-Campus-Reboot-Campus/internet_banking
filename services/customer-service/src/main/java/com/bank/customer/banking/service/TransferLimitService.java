package com.bank.customer.banking.service;

import com.bank.common.web.BusinessException;
import com.bank.customer.banking.domain.TransferLimit;
import com.bank.customer.banking.dto.ReduceTransferLimitRequest;
import com.bank.customer.banking.dto.TransferLimitResponse;
import com.bank.customer.banking.repository.TransferLimitRepository;
import com.bank.customer.support.CustomerErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 고객당 인터넷뱅킹 이체한도(1일/1회) 조회·감액.
 * 온라인에서는 감액만 허용한다(증액은 영업점·본인인증). 행이 없으면 기본 100만원으로 간주한다.
 * (실제 이체 시점의 한도 적용·집계는 결제계 소관 — 여기서는 한도 값 저장·조회만 담당한다.)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferLimitService {

    private final TransferLimitRepository transferLimitRepository;

    @Transactional(readOnly = true)
    public TransferLimitResponse getLimit(Long customerId) {
        return transferLimitRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .map(t -> new TransferLimitResponse(t.getDailyLimit(), t.getOnceLimit()))
                .orElseGet(() -> new TransferLimitResponse(TransferLimit.DEFAULT_LIMIT, TransferLimit.DEFAULT_LIMIT));
    }

    @Transactional
    public void reduce(Long customerId, ReduceTransferLimitRequest req) {
        if (req.dailyLimit() <= 0 || req.onceLimit() <= 0) {
            throw new BusinessException(CustomerErrorCode.CUST_151);
        }
        if (req.onceLimit() > req.dailyLimit()) {
            throw new BusinessException(CustomerErrorCode.CUST_152);
        }

        TransferLimit limit = transferLimitRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseGet(() -> transferLimitRepository.save(TransferLimit.builder()
                        .customerId(customerId)
                        .dailyLimit(TransferLimit.DEFAULT_LIMIT)
                        .onceLimit(TransferLimit.DEFAULT_LIMIT)
                        .build()));

        // 온라인은 감액만 — 현재보다 큰 값은 거부
        if (req.dailyLimit() > limit.getDailyLimit() || req.onceLimit() > limit.getOnceLimit()) {
            throw new BusinessException(CustomerErrorCode.CUST_150);
        }

        limit.updateLimits(req.dailyLimit(), req.onceLimit());
        log.info("transfer-limit reduced: customerId={}, daily={}, once={}",
                customerId, req.dailyLimit(), req.onceLimit());
    }
}
