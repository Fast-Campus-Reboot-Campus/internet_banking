package com.bank.loan.guaranteeinsurance.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.BusinessException;
import com.bank.loan.contract.domain.LoanContract;
import com.bank.loan.contract.repository.LoanContractRepository;
import com.bank.loan.guaranteeinsurance.domain.GuaranteeInsurance;
import com.bank.loan.guaranteeinsurance.dto.CancelGuaranteeInsuranceRequest;
import com.bank.loan.guaranteeinsurance.dto.GuaranteeInsuranceResponse;
import com.bank.loan.guaranteeinsurance.dto.IssueGuaranteeInsuranceRequest;
import com.bank.loan.guaranteeinsurance.repository.GuaranteeInsuranceRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * 보증보험 서비스 (flows §1.1 — CONTRACTED → DISBURSED 전제조건 중 필요시).
 *
 * 외부기관(SGI/HUG/HF 등) 발급은 본 단계 stub — request 즉시 ISSUED 처리.
 * idv/CB 와 동일 패턴 (외부 API 없이 PASS 가정).
 *
 * 룰:
 *   - 발급 가능 계약 상태: SIGNED 또는 ACTIVE (CLOSED 차단)
 *   - 계약당 활성(ISSUED) 보증보험 1건 (LOAN_181)
 *   - 시작일/종료일 미지정 시 계약 시작일/종료일 자동 적용
 *   - 취소는 ISSUED 만 허용
 */
@Service
@RequiredArgsConstructor
public class GuaranteeInsuranceService {

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_TABLE_CD = "GUARANTEE_INSURANCE";
    private static final String REASON_ISSUED   = "GUARANTEE_INSURANCE_ISSUED";
    private static final String REASON_CANCELED = "GUARANTEE_INSURANCE_CANCELED";

    private static final Set<String> ISSUABLE_CONTRACT_STATUSES = Set.of(
            LoanContract.STATUS_SIGNED, LoanContract.STATUS_ACTIVE
    );

    private final GuaranteeInsuranceRepository repository;
    private final LoanContractRepository contractRepository;
    private final PolicyNumberGenerator policyNoGenerator;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;

    @Transactional
    public GuaranteeInsuranceResponse issue(Long cntrId, IssueGuaranteeInsuranceRequest req) {
        LoanContract contract = contractRepository.findByCntrIdAndDeletedAtIsNull(cntrId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_062));
        if (!ISSUABLE_CONTRACT_STATUSES.contains(contract.currentStatus())) {
            throw new BusinessException(LoanErrorCode.LOAN_183,
                    "current=" + contract.currentStatus());
        }

        repository.findByCntrIdAndGinsStatusCdAndDeletedAtIsNull(
                        cntrId, GuaranteeInsurance.STATUS_ISSUED)
                .ifPresent(existing -> {
                    throw new BusinessException(LoanErrorCode.LOAN_181,
                            "existingGinsId=" + existing.getGinsId());
                });

        OffsetDateTime now = OffsetDateTime.now();
        String startDate = req.ginsStartDate() == null ? contract.getCntrStartDate() : req.ginsStartDate();
        String endDate   = req.ginsEndDate()   == null ? contract.getCntrEndDate()   : req.ginsEndDate();

        GuaranteeInsurance saved = repository.save(GuaranteeInsurance.builder()
                .cntrId(cntrId)
                .ginsAgencyCd(req.ginsAgencyCd())
                .ginsPolicyNo(policyNoGenerator.generate(now))
                .guaranteeAmount(req.guaranteeAmount())
                .guaranteeRatioBps(req.guaranteeRatioBps())
                .premiumAmount(req.premiumAmount())
                .ginsStatusCd(GuaranteeInsurance.STATUS_ISSUED)
                .ginsStartDate(startDate)
                .ginsEndDate(endDate)
                .ginsDocUrl(req.ginsDocUrl())
                .ginsDocHash(req.ginsDocHash())
                .issuedAt(now)
                .build());

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_TABLE_CD, saved.getGinsId(),
                null, GuaranteeInsurance.STATUS_ISSUED,
                REASON_ISSUED,
                "agency=" + req.ginsAgencyCd() + ", policyNo=" + saved.getGinsPolicyNo(),
                currentActor.currentActorId()
        ));

        return GuaranteeInsuranceResponse.of(saved);
    }

    @Transactional
    public GuaranteeInsuranceResponse cancel(Long cntrId, Long ginsId,
                                             CancelGuaranteeInsuranceRequest req) {
        GuaranteeInsurance gins = requireGins(cntrId, ginsId);
        if (!gins.isCancellable()) {
            throw new BusinessException(LoanErrorCode.LOAN_182,
                    "current=" + gins.currentStatus());
        }

        String before = gins.currentStatus();
        gins.markCanceled();

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_TABLE_CD, gins.getGinsId(),
                before, GuaranteeInsurance.STATUS_CANCELED,
                req == null || req.cancelReasonCd() == null ? REASON_CANCELED : req.cancelReasonCd(),
                req == null ? null : req.cancelRemark(),
                currentActor.currentActorId()
        ));

        return GuaranteeInsuranceResponse.of(gins);
    }

    @Transactional(readOnly = true)
    public GuaranteeInsuranceResponse get(Long cntrId, Long ginsId) {
        return GuaranteeInsuranceResponse.of(requireGins(cntrId, ginsId));
    }

    private GuaranteeInsurance requireGins(Long cntrId, Long ginsId) {
        GuaranteeInsurance gins = repository.findByGinsIdAndDeletedAtIsNull(ginsId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_180));
        if (!gins.getCntrId().equals(cntrId)) {
            throw new BusinessException(LoanErrorCode.LOAN_180,
                    "cntrId mismatch: gins.cntrId=" + gins.getCntrId() + ", path=" + cntrId);
        }
        return gins;
    }
}
