package com.bank.loan.collateral.service;

import com.bank.common.web.BusinessException;
import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.application.repository.LoanApplicationRepository;
import com.bank.loan.collateral.domain.Collateral;
import com.bank.loan.collateral.dto.CollateralResponse;
import com.bank.loan.collateral.dto.CreateCollateralRequest;
import com.bank.loan.collateral.repository.CollateralRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class CollateralService {

    private static final String DEFAULT_CURRENCY = "KRW";
    private static final String DEFAULT_NO = "N";

    private final CollateralRepository repository;
    private final LoanApplicationRepository applicationRepository;
    private final CollateralNumberGenerator colNoGenerator;

    @Transactional
    public CollateralResponse register(Long applId, CreateCollateralRequest req) {
        LoanApplication application = applicationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_012));

        Collateral saved = repository.save(Collateral.builder()
                .applId(application.getApplId())
                .colTypeCd(req.colTypeCd())
                .colStatusCd(Collateral.STATUS_REGISTERED)
                .colNo(colNoGenerator.generate(OffsetDateTime.now()))
                .colName(req.colName())
                .colAddress(req.colAddress())
                .colRegistryNo(req.colRegistryNo())
                .declaredValue(req.declaredValue())
                .currencyCd(req.currencyCd() == null ? DEFAULT_CURRENCY : req.currencyCd())
                .ownershipTypeCd(req.ownershipTypeCd())
                .seniorLienYn(req.seniorLienYn() == null ? DEFAULT_NO : req.seniorLienYn())
                .seniorLienAmount(req.seniorLienAmount())
                .build());

        return CollateralResponse.of(saved);
    }
}
