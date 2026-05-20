package com.bank.loan.accrual.service;

import com.bank.common.web.BusinessException;
import com.bank.loan.accrual.dto.InterestAccrualListResponse;
import com.bank.loan.accrual.dto.InterestAccrualResponse;
import com.bank.loan.accrual.repository.InterestAccrualRepository;
import com.bank.loan.contract.repository.LoanContractRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InterestAccrualQueryService {

    private final InterestAccrualRepository repository;
    private final LoanContractRepository contractRepository;

    @Transactional(readOnly = true)
    public InterestAccrualListResponse list(Long cntrId, String from, String to) {
        contractRepository.findByCntrIdAndDeletedAtIsNull(cntrId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_062));

        List<InterestAccrualResponse> items;
        if (from == null && to == null) {
            items = repository.findByCntrIdOrderByAccrualDateAsc(cntrId)
                    .stream().map(InterestAccrualResponse::of).toList();
        } else {
            String f = from == null ? "00000000" : from;
            String t = to   == null ? "99991231" : to;
            items = repository.findByCntrIdAndAccrualDateBetweenOrderByAccrualDateAsc(cntrId, f, t)
                    .stream().map(InterestAccrualResponse::of).toList();
        }
        return InterestAccrualListResponse.of(cntrId, items);
    }
}
