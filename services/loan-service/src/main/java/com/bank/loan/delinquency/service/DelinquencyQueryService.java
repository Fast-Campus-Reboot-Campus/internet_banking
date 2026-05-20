package com.bank.loan.delinquency.service;

import com.bank.common.web.BusinessException;
import com.bank.loan.contract.repository.LoanContractRepository;
import com.bank.loan.delinquency.domain.Delinquency;
import com.bank.loan.delinquency.dto.DelinquencyResponse;
import com.bank.loan.delinquency.dto.DelinquencySnapshotListResponse;
import com.bank.loan.delinquency.dto.DelinquencySnapshotResponse;
import com.bank.loan.delinquency.repository.DelinquencyDailySnapshotRepository;
import com.bank.loan.delinquency.repository.DelinquencyRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DelinquencyQueryService {

    private final DelinquencyRepository delinquencyRepository;
    private final DelinquencyDailySnapshotRepository snapshotRepository;
    private final LoanContractRepository contractRepository;

    @Transactional(readOnly = true)
    public DelinquencyResponse getActive(Long cntrId) {
        contractRepository.findByCntrIdAndDeletedAtIsNull(cntrId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_062));
        Delinquency dlq = delinquencyRepository
                .findByCntrIdAndDlqStatusCdAndDeletedAtIsNull(cntrId, Delinquency.STATUS_ACTIVE)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_100));
        return DelinquencyResponse.of(dlq);
    }

    @Transactional(readOnly = true)
    public DelinquencySnapshotListResponse listSnapshots(Long cntrId) {
        contractRepository.findByCntrIdAndDeletedAtIsNull(cntrId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_062));
        Delinquency dlq = delinquencyRepository
                .findByCntrIdAndDlqStatusCdAndDeletedAtIsNull(cntrId, Delinquency.STATUS_ACTIVE)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_100));
        List<DelinquencySnapshotResponse> items = snapshotRepository
                .findByDlqIdOrderBySnapshotDateAsc(dlq.getDlqId())
                .stream()
                .map(DelinquencySnapshotResponse::of)
                .toList();
        return DelinquencySnapshotListResponse.of(cntrId, dlq.getDlqId(), items);
    }
}
