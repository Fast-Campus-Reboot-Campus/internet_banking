package com.bank.loan.contract.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.BusinessException;
import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.application.repository.LoanApplicationRepository;
import com.bank.loan.contract.domain.LoanContract;
import com.bank.loan.contract.dto.CreateContractRequest;
import com.bank.loan.contract.dto.LoanContractResponse;
import com.bank.loan.contract.repository.LoanContractRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 약정한도 설정 서비스.
 *
 * 흐름:
 *   1) 신청이 APPROVED 인지 검증 (LOAN_060)
 *   2) 약정금액·기간이 신청 범위 이내인지 검증 (LOAN_061)
 *      - 본심사(loan_review) 미구현이므로 application.requested_* 를 상한으로 사용
 *      - 본심사 도입 시 loan_review.approved_* 로 교체
 *   3) loan_contract INSERT (status=SIGNED)
 *   4) application status APPROVED → CONTRACTED
 *   5) status_history 양쪽 모두 기록 (BEFORE_COMMIT 동일 트랜잭션)
 */
@Service
@RequiredArgsConstructor
public class LoanContractService {

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_CONTRACT = "LOAN_CONTRACT";
    private static final String TARGET_APPLICATION = "LOAN_APPLICATION";
    private static final String DEFAULT_CURRENCY = "KRW";
    private static final String REASON_CONTRACT_SIGNED = "CONTRACT_SIGNED";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final LoanContractRepository repository;
    private final LoanApplicationRepository applicationRepository;
    private final ContractNumberGenerator cntrNoGenerator;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;

    @Transactional
    public LoanContractResponse create(CreateContractRequest req) {
        LoanApplication application = applicationRepository.findByApplIdAndDeletedAtIsNull(req.applId())
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_012));

        if (!application.isApproved()) {
            throw new BusinessException(LoanErrorCode.LOAN_060);
        }

        validateRanges(req, application);

        int spread = req.spreadBps() == null ? 0 : req.spreadBps();
        int preferential = req.preferentialRateBps() == null ? 0 : req.preferentialRateBps();
        int totalRate = req.totalRateBps() == null
                ? Math.max(0, req.baseRateBps() + spread - preferential)
                : req.totalRateBps();

        OffsetDateTime now = OffsetDateTime.now();
        LocalDate startDate = req.cntrStartDate() != null
                ? LocalDate.parse(req.cntrStartDate(), DATE)
                : now.toLocalDate();
        LocalDate endDate = req.cntrEndDate() != null
                ? LocalDate.parse(req.cntrEndDate(), DATE)
                : startDate.plusMonths(req.contractedPeriodMo());

        LoanContract saved = repository.save(LoanContract.builder()
                .cntrNo(cntrNoGenerator.generate(now))
                .applId(application.getApplId())
                .customerId(application.getCustomerId())
                .prodId(application.getProdId())
                .contractedAmount(req.contractedAmount())
                .currencyCd(req.currencyCd() == null ? DEFAULT_CURRENCY : req.currencyCd())
                .contractedPeriodMo(req.contractedPeriodMo())
                .totalRateBps(totalRate)
                .baseRateBps(req.baseRateBps())
                .spreadBps(spread)
                .preferentialRateBps(preferential)
                .rateTypeCd(req.rateTypeCd())
                .repaymentMethodCd(req.repaymentMethodCd())
                .cntrStatusCd(LoanContract.STATUS_SIGNED)
                .cntrStartDate(startDate.format(DATE))
                .cntrEndDate(endDate.format(DATE))
                .cntrDocUrl(req.cntrDocUrl())
                .cntrDocHash(req.cntrDocHash())
                .signedAt(now)
                .build());

        // 신청 상태 전이
        String applBefore = application.currentStatus();
        application.markContracted();

        Long actor = currentActor.currentActorId();

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_CONTRACT, saved.getCntrId(),
                null, LoanContract.STATUS_SIGNED,
                REASON_CONTRACT_SIGNED, null, actor
        ));
        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_APPLICATION, application.getApplId(),
                applBefore, LoanApplication.STATUS_CONTRACTED,
                REASON_CONTRACT_SIGNED, null, actor
        ));

        return LoanContractResponse.of(saved);
    }

    @Transactional(readOnly = true)
    public LoanContractResponse get(Long cntrId) {
        return repository.findByCntrIdAndDeletedAtIsNull(cntrId)
                .map(LoanContractResponse::of)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_062));
    }

    private void validateRanges(CreateContractRequest req, LoanApplication application) {
        if (req.contractedAmount() > application.getRequestedAmount()) {
            throw new BusinessException(LoanErrorCode.LOAN_061,
                    "contractedAmount > requestedAmount");
        }
        if (req.contractedPeriodMo() > application.getRequestedPeriodMo()) {
            throw new BusinessException(LoanErrorCode.LOAN_061,
                    "contractedPeriodMo > requestedPeriodMo");
        }
    }
}
