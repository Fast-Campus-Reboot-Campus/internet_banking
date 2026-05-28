package com.bank.loan.application.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.BusinessException;
import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.application.dto.CancelLoanApplicationRequest;
import com.bank.loan.application.dto.CreateLoanApplicationRequest;
import com.bank.loan.application.dto.LoanApplicationResponse;
import com.bank.loan.application.repository.LoanApplicationRepository;
import com.bank.loan.notification.event.ApplicationSubmittedEvent;
import com.bank.loan.product.domain.LoanProduct;
import com.bank.loan.product.repository.LoanProductRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanApplicationService {

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_TABLE_CD = "LOAN_APPLICATION";
    private static final String REASON_SUBMITTED = "APPLICATION_SUBMITTED";
    private static final String REASON_CANCELED  = "CUSTOMER_CANCEL";

    private final LoanApplicationRepository repository;
    private final LoanProductRepository productRepository;
    private final ApplicationNumberGenerator applNoGenerator;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public LoanApplicationResponse create(CreateLoanApplicationRequest req,
                                          String idempotencyKey,
                                          String clientIp,
                                          String device,
                                          Long requestingCustomerId) {
        // 멱등성: 동일 키로 이전에 처리된 신청이 있으면 그대로 반환
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = repository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) return LoanApplicationResponse.of(existing.get());
        }

        LoanProduct product = productRepository.findByProdIdAndDeletedAtIsNull(req.prodId())
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_002));
        validateProductSellable(product);
        validateRequestedRanges(req, product);

        // JWT principal 의 customerId 를 사용한다 — 요청 바디의 customerId 는 무시
        Long effectiveCustomerId = requestingCustomerId != null ? requestingCustomerId : req.customerId();

        OffsetDateTime now = OffsetDateTime.now();
        LoanApplication saved = repository.save(LoanApplication.builder()
                .applNo(applNoGenerator.generate(now))
                .customerId(effectiveCustomerId)
                .prodId(product.getProdId())
                .channelCd(req.channelCd())
                .requestedAmount(req.requestedAmount())
                .requestedPeriodMo(req.requestedPeriodMo())
                .loanPurposeCd(req.loanPurposeCd())
                .repaymentMethodCd(req.repaymentMethodCd() != null
                        ? req.repaymentMethodCd()
                        : product.getRepaymentMethodCd())
                .estimatedIncomeAmt(req.estimatedIncomeAmt())
                .employmentTypeCd(req.employmentTypeCd())
                .applStatusCd(LoanApplication.STATUS_SUBMITTED)
                .appliedAt(now)
                .clientIp(clientIp)
                .device(device)
                .idempotencyKey(idempotencyKey)
                .build());

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_TABLE_CD, saved.getApplId(),
                null, LoanApplication.STATUS_SUBMITTED,
                REASON_SUBMITTED, null,
                currentActor.currentActorId()
        ));

        eventPublisher.publishEvent(new ApplicationSubmittedEvent(
                saved.getApplId(), saved.getApplNo(), saved.getCustomerId(), saved.getProdId()
        ));

        return LoanApplicationResponse.of(saved);
    }

    @Transactional
    public LoanApplicationResponse cancel(Long applId, CancelLoanApplicationRequest req,
                                          Long requestingCustomerId) {
        LoanApplication application = repository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_012));

        // 소유권 검증: requestingCustomerId 가 null 이면 관리자 요청으로 스킵
        if (requestingCustomerId != null
                && !requestingCustomerId.equals(application.getCustomerId())) {
            throw new BusinessException(LoanErrorCode.LOAN_012);
        }

        if (!application.isCancellable()) {
            throw new BusinessException(LoanErrorCode.LOAN_013);
        }

        String before = application.currentStatus();
        application.cancel();

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_TABLE_CD, application.getApplId(),
                before, LoanApplication.STATUS_CANCELED,
                req.cancelReasonCd() == null ? REASON_CANCELED : req.cancelReasonCd(),
                req.cancelRemark(),
                currentActor.currentActorId()
        ));

        return LoanApplicationResponse.of(application);
    }

    @Transactional(readOnly = true)
    public LoanApplicationResponse get(Long applId, Long requestingCustomerId) {
        LoanApplication application = repository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_012));

        // 소유권 검증: requestingCustomerId 가 null 이면 관리자 요청으로 스킵
        if (requestingCustomerId != null
                && !requestingCustomerId.equals(application.getCustomerId())) {
            throw new BusinessException(LoanErrorCode.LOAN_012);
        }

        return LoanApplicationResponse.of(application);
    }

    @Transactional(readOnly = true)
    public List<LoanApplicationResponse> list(Long customerId) {
        return repository.findByCustomerIdAndDeletedAtIsNullOrderByApplIdDesc(customerId)
                .stream()
                .map(LoanApplicationResponse::of)
                .toList();
    }

    private void validateProductSellable(LoanProduct product) {
        if (!LoanProduct.STATUS_ACTIVE.equals(product.getProdStatusCd())) {
            throw new BusinessException(LoanErrorCode.LOAN_010);
        }
    }

    private void validateRequestedRanges(CreateLoanApplicationRequest req, LoanProduct product) {
        if (req.requestedAmount() < product.getMinAmount()
                || req.requestedAmount() > product.getMaxAmount()) {
            throw new BusinessException(LoanErrorCode.LOAN_011, "requestedAmount out of product range");
        }
        if (req.requestedPeriodMo() < product.getMinPeriodMo()
                || req.requestedPeriodMo() > product.getMaxPeriodMo()) {
            throw new BusinessException(LoanErrorCode.LOAN_011, "requestedPeriodMo out of product range");
        }
    }
}
